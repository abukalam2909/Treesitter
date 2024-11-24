package ca.dal.treefactor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.dal.treefactor.model.CodeElementType;
import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.core.LocationInfo;
import ca.dal.treefactor.model.core.UMLAnnotation;
import ca.dal.treefactor.model.core.UMLComment;
import ca.dal.treefactor.model.core.UMLImport;
import ca.dal.treefactor.model.core.UMLParameter;
import ca.dal.treefactor.model.core.UMLType;
import ca.dal.treefactor.model.core.Visibility;
import ca.dal.treefactor.model.elements.UMLAttribute;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;

public class JSASTVisitor extends ASTVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSASTVisitor.class);
    private UMLClass currentClass;
    private final List<String> currentScope;

    public JSASTVisitor(UMLModel model, String sourceCode, String filePath) {
        super(model, sourceCode, filePath);
        this.currentClass = null;
        this.currentScope = new ArrayList<>();
        LOGGER.info("JSASTVisitor initialized with filePath: {}", filePath);
    }

    @Override
    public void visit(ASTUtil.ASTNode node) {
        LOGGER.info("Visiting node of type: {}", node.getType());

        switch(node.getType()) {
            case "program":
                LOGGER.info("Processing program node");
                processModule(node);
                break;
            case "class_declaration":
                LOGGER.info("Found class declaration");
                processClass(node);
                break;
            case "method_definition":
                if (currentClass != null) {
                    LOGGER.info("Processing method in class");
                    processMethod(node);
                }
                break;
            case "function_declaration":
                if (currentClass == null) {
                    LOGGER.info("Processing standalone function");
                    processMethod(node);
                }
                break;
            case "class_field":
            case "field_definition":
            case "property_definition":
                if (currentClass != null) {
                    LOGGER.info("Processing field");
                    processField(node);
                }
                break;
            case "import_statement":
                LOGGER.info("Processing import");
                processImport(node);
                break;
            case "arrow_function":
                if (currentClass == null) {
                    LOGGER.info("Processing arrow function");
                    processMethod(node);
                }
                break;
            case "variable_declaration":
                if (currentClass != null) {
                    LOGGER.info("Processing variable declaration in class context");
                    for (ASTUtil.ASTNode child : node.getChildren()) {
                        if (child.getType().equals("variable_declarator")) {
                            processField(child);
                        }
                    }
                }
                break;
            default:
                LOGGER.info("Unhandled node type: {}", node.getType());
        }

        // Visit children unless it's a method node
        if (!isMethodNode(node)) {
            for (ASTUtil.ASTNode child : node.getChildren()) {
                visit(child);
            }
        }
    }

    private boolean isMethodNode(ASTUtil.ASTNode node) {
        return node.getType().equals("method_definition") ||
                node.getType().equals("function_declaration") ||
                node.getType().equals("arrow_function");
    }

    @Override
    protected void processModule(ASTUtil.ASTNode node) {
        String moduleName = filePath.replace("/", ".").replaceAll("\\.js$", "");
        LOGGER.info("Processing module: {}", moduleName);
        model.addPackage(filePath, moduleName);
    }

    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        LOGGER.info("Processing class node: {}", node.getType());
        printNodeStructure(node, 0);  // Print full node structure

        String className = extractClassName(node);
        LOGGER.info("Extracted class name: {}", className);

        if (className == null) {
            LOGGER.info("Class name is null, returning early");
            return;
        }

        LocationInfo location = new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                CodeElementType.CLASS_DECLARATION);

        UMLClass previousClass = currentClass;
        currentClass = new UMLClass(extractModuleName(filePath), className, location);
        LOGGER.info("Created new UML class: {}", className);

        // Process inheritance - try multiple possible node types
        ASTUtil.ASTNode heritage = findChildByType(node, "extends_clause");
        if (heritage == null) heritage = findChildByType(node, "extends");
        if (heritage == null) heritage = findChildByType(node, "heritage_clause");
        if (heritage == null) heritage = findChildByType(node, "class_heritage");

        if (heritage != null) {
            LOGGER.info("Found heritage node of type: {}", heritage.getType());
            String superclass = getChildText(heritage, "identifier");
            if (superclass == null) {
                // Try getting the text directly
                superclass = heritage.getText(sourceCode).trim();
                if (superclass.startsWith("extends ")) {
                    superclass = superclass.substring(8).trim();
                }
            }
            LOGGER.info("Extracted superclass: {}", superclass);
            if (superclass != null) {
                currentClass.addSuperclass(superclass);
            }
        }

        // Process decorators
        ASTUtil.ASTNode decorator = findChildByType(node, "decorator");
        if (decorator != null) {
            LOGGER.info("Processing decorator");
            LocationInfo decoratorLocation = new LocationInfo(filePath, decorator.getStartPoint(),
                    decorator.getEndPoint(), CodeElementType.ANNOTATION_TYPE_DECLARATION);
            String decoratorName = getChildText(decorator, "identifier");
            if (decoratorName != null) {
                currentClass.addAnnotation(new UMLAnnotation(decoratorName, decoratorLocation));
            }
        }

        // Process class body
        ASTUtil.ASTNode body = findChildByType(node, "class_body");
        if (body != null) {
            LOGGER.info("Processing class body");
            for (ASTUtil.ASTNode child : body.getChildren()) {
                visit(child);
            }
        }

        LOGGER.info("Adding class {} to model", className);
        model.addClass(currentClass);
        LOGGER.info("Current model classes count: {}", model.getClasses().size());
        currentClass = previousClass;
    }

    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        String methodName = extractMethodName(node);
        LOGGER.info("Processing method: {}", methodName);
        if (methodName == null) return;

        LocationInfo location = new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                CodeElementType.METHOD_DECLARATION);

        UMLOperation operation = new UMLOperation(methodName, location);
        operation.setVisibility(Visibility.PUBLIC);

        // Process async decorator if present
        if (findChildByType(node, "async") != null) {
            LocationInfo asyncLocation = new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                    CodeElementType.ANNOTATION_TYPE_DECLARATION);
            operation.addAnnotation(new UMLAnnotation("async", asyncLocation));
        }

        // Process parameters
        ASTUtil.ASTNode params = findChildByType(node, "formal_parameters");
        if (params != null) {
            for (ASTUtil.ASTNode param : params.getChildren()) {
                processParameter(param, operation);
            }
        }

        // Set method body
        ASTUtil.ASTNode body = findChildByType(node, "statement_block");
        if (body != null) {
            operation.setBody(body.getText(sourceCode));
        }

        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            if (methodName.equals("constructor")) {
                operation.setConstructor(true);
            }
            currentClass.addOperation(operation);
            LOGGER.info("Added method {} to class {}", methodName, currentClass.getName());
        } else {
            model.addOperation(operation);
            LOGGER.info("Added standalone method {} to model", methodName);
        }
    }

    private void processParameter(ASTUtil.ASTNode node, UMLOperation operation) {
        LOGGER.info("Processing parameter node: {}", node.getType());

        // Debugging log
        LOGGER.info("Node full text: {}", node.getText(sourceCode));
        printNodeStructure(node, 0);

        String paramName = null;
        String defaultValue = null;

        // Try to find an identifier node
        ASTUtil.ASTNode identifierNode = findChildByType(node, "identifier");
        if (identifierNode != null) {
            paramName = identifierNode.getText(sourceCode);
        } else {
            // Fallback to entire node text if no identifier found
            paramName = node.getText(sourceCode);
        }

        if (node.getType().equals("rest_parameter")) {
            paramName = "..." + getChildText(node, "identifier");
        } else if (node.getType().equals("object_pattern")) {
            paramName = node.getText(sourceCode);
        } else if (node.getType().equals("assignment_pattern")) {
            ASTUtil.ASTNode left = findChildByFieldName(node, "left");
            ASTUtil.ASTNode right = findChildByFieldName(node, "right");
            paramName = left != null ? left.getText(sourceCode) : null;
            defaultValue = right != null ? right.getText(sourceCode) : null;
        }

        if (paramName != null) {
            LocationInfo location = new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                    CodeElementType.PARAMETER_DECLARATION);
            UMLParameter parameter = new UMLParameter(paramName, new UMLType("any"), location);
            if (defaultValue != null) {
                parameter.setDefaultValue(defaultValue);
            }
            operation.addParameter(parameter);
            LOGGER.info("Added parameter {} to operation", paramName);
        } else {
            LOGGER.warn("Could not extract parameter name from node");
        }
    }

    @Override
    protected void processField(ASTUtil.ASTNode node) {
        if (currentClass == null) return;

        String fieldName = extractFieldName(node);
        LOGGER.info("Processing field: {}", fieldName);
        if (fieldName == null) return;

        LocationInfo location = new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                CodeElementType.FIELD_DECLARATION);

        UMLAttribute attribute = new UMLAttribute(fieldName, new UMLType("any"), location);

        // Set visibility based on field name
        attribute.setVisibility(fieldName.startsWith("#") ? Visibility.PRIVATE : Visibility.PUBLIC);

        // Check for static modifier
        if (findChildByType(node, "static") != null) {
            attribute.setStatic(true);
        }

        // Get initial value
        ASTUtil.ASTNode initializer = findChildByType(node, "initializer");
        if (initializer != null) {
            attribute.setInitialValue(initializer.getText(sourceCode));
        }

        currentClass.addAttribute(attribute);
        LOGGER.info("Added field {} to class {}", fieldName, currentClass.getName());
    }

    @Override
    protected void processImport(ASTUtil.ASTNode node) {
        LOGGER.info("Processing import node");
        LocationInfo location = new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                CodeElementType.IMPORT_DECLARATION);

        // Debug print of the AST structure
        LOGGER.info("Import node structure:");
        printNodeStructure(node, 0);

        String modulePath = null;
        List<String> importNames = new ArrayList<>();

        // Get the module path from string node
        ASTUtil.ASTNode string = findChildByType(node, "string");
        if (string != null) {
            modulePath = string.getText(sourceCode).replaceAll("['\"]", "");
        }

        // Find import specifiers directly under import_statement
        processImportChildren(node, importNames);

        if (modulePath != null && !importNames.isEmpty()) {
            for (String importName : importNames) {
                String fullName = modulePath + "." + importName;
                UMLImport umlImport = UMLImport.builder(fullName, location)
                        .type(UMLImport.ImportType.SINGLE)
                        .build();
                model.addImport(filePath, umlImport);
                LOGGER.info("Added import: {}", fullName);
            }
        }
    }

    private void processImportChildren(ASTUtil.ASTNode node, List<String> importNames) {
        for (ASTUtil.ASTNode child : node.getChildren()) {
            if (child.getType().equals("import_clause")) {
                processImportChildren(child, importNames);
            } else if (child.getType().equals("named_imports")) {
                processImportChildren(child, importNames);
            } else if (child.getType().equals("import_specifier")) {
                ASTUtil.ASTNode identifier = findChildByType(child, "identifier");
                if (identifier != null) {
                    String name = identifier.getText(sourceCode);
                    if (name != null && !name.isEmpty()) {
                        importNames.add(name);
                        LOGGER.info("Found import name: {}", name);
                    }
                }
            }
        }
    }

    private void printNodeStructure(ASTUtil.ASTNode node, int depth) {
        String indent = "  ".repeat(depth);
        LOGGER.info("{}[{}] '{}' (children: {})",
                indent,
                node.getType(),
                node.getText(sourceCode).replaceAll("\n", "\\n"),
                node.getChildren().size());

        for (ASTUtil.ASTNode child : node.getChildren()) {
            printNodeStructure(child, depth + 1);
        }
    }

    private String extractClassName(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "identifier");
        String className = nameNode != null ? nameNode.getText(sourceCode) : null;
        LOGGER.info("Extracted class name: {}", className);
        return className;
    }

    private String extractMethodName(ASTUtil.ASTNode node) {
        String methodName = null;
        if (node.getType().equals("method_definition")) {
            methodName = getChildText(node, "property_identifier");
        } else if (node.getType().equals("function_declaration")) {
            methodName = getChildText(node, "identifier");
        } else if (node.getType().equals("arrow_function")) {
            // Get name from parent variable declarator
            Optional<ASTUtil.ASTNode> parentNode = node.getParent();
            if (parentNode.isPresent() && parentNode.get().getType().equals("variable_declarator")) {
                methodName = getChildText(parentNode.get(), "identifier");
            }
        }
        LOGGER.info("Extracted method name: {}", methodName);
        return methodName;
    }

    private String extractFieldName(ASTUtil.ASTNode node) {
        // Try different field name patterns
        ASTUtil.ASTNode nameNode = findChildByType(node, "property_identifier");
        if (nameNode == null) {
            nameNode = findChildByType(node, "private_property_identifier");
        }
        if (nameNode == null) {
            nameNode = findChildByType(node, "identifier");
        }
        String fieldName = nameNode != null ? nameNode.getText(sourceCode) : null;
        LOGGER.info("Extracted field name: {}", fieldName);
        return fieldName;
    }

    private String extractModuleName(String filePath) {
        return filePath.replace("/", ".").replaceAll("\\.js$", "");
    }
}
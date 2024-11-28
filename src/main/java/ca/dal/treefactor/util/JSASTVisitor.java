package ca.dal.treefactor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        processNodeBasedOnType(node);

        // Visit children unless it's a method node
        if (!isMethodNode(node)) {
            for (ASTUtil.ASTNode child : node.getChildren()) {
                visit(child);
            }
        }
    }

    private void processNodeBasedOnType(ASTUtil.ASTNode node) {
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
                processMethodDefinition(node);
                break;
            case "function_declaration":
                processFunctionDeclaration(node);
                break;
            case "class_field":
            case "field_definition":
            case "property_definition":
                processClassField(node);
                break;
            case "import_statement":
                LOGGER.info("Processing import");
                processImport(node);
                break;
            case "arrow_function":
                processArrowFunction(node);
                break;
            case "variable_declaration":
                processVariableDeclaration(node);
                break;
            default:
                LOGGER.info("Unhandled node type: {}", node.getType());
        }
    }

    private void processMethodDefinition(ASTUtil.ASTNode node) {
        if (currentClass != null) {
            LOGGER.info("Processing method in class");
            processMethod(node);
        }
    }

    private void processFunctionDeclaration(ASTUtil.ASTNode node) {
        if (currentClass == null) {
            LOGGER.info("Processing standalone function");
            processMethod(node);
        }
    }

    private void processClassField(ASTUtil.ASTNode node) {
        if (currentClass != null) {
            LOGGER.info("Processing field");
            processField(node);
        }
    }

    private void processArrowFunction(ASTUtil.ASTNode node) {
        if (currentClass == null) {
            LOGGER.info("Processing arrow function");
            processMethod(node);
        }
    }

    private void processVariableDeclaration(ASTUtil.ASTNode node) {
        if (currentClass != null) {
            LOGGER.info("Processing variable declaration in class context");
            for (ASTUtil.ASTNode child : node.getChildren()) {
                if (child.getType().equals("variable_declarator")) {
                    processField(child);
                }
            }
        }
    }

    private static final Set<String> METHOD_NODE_TYPES = Set.of(
            "method_definition",
            "function_declaration",
            "arrow_function"
    );

    private boolean isMethodNode(ASTUtil.ASTNode node) {
        return METHOD_NODE_TYPES.contains(node.getType());
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
        printNodeStructure(node, 0);

        String className = extractClassName(node);
        if (className == null) {
            LOGGER.info("Class name is null, returning early");
            return;
        }

        LocationInfo location = createClassLocationInfo(node);
        UMLClass previousClass = currentClass;
        currentClass = createUMLClass(className, location);

        processClassHeritage(node);
        processClassDecorator(node);
        processClassBody(node);

        addClassToModel(className);
        currentClass = previousClass;
    }

    private LocationInfo createClassLocationInfo(ASTUtil.ASTNode node) {
        return new LocationInfo(
                filePath,
                node.getStartPoint(),
                node.getEndPoint(),
                CodeElementType.CLASS_DECLARATION
        );
    }

    private UMLClass createUMLClass(String className, LocationInfo location) {
        return new UMLClass(extractModuleName(filePath), className, location);
    }

    private void processClassHeritage(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode heritage = findHeritageNode(node);
        if (heritage != null) {
            String superclass = extractSuperclass(heritage);
            if (superclass != null) {
                currentClass.addSuperclass(superclass);
            }
        }
    }

    private ASTUtil.ASTNode findHeritageNode(ASTUtil.ASTNode node) {
        String[] heritageTypes = {
                "extends_clause", "extends", "heritage_clause", "class_heritage"
        };

        for (String type : heritageTypes) {
            ASTUtil.ASTNode heritage = findChildByType(node, type);
            if (heritage != null) return heritage;
        }
        return null;
    }

    private String extractSuperclass(ASTUtil.ASTNode heritage) {
        String superclass = getChildText(heritage, "identifier");
        if (superclass == null) {
            superclass = extractSuperclassFromText(heritage);
        }
        return superclass;
    }

    private static final String EXTENDS_PREFIX = "extends ";

    private String extractSuperclassFromText(ASTUtil.ASTNode heritage) {
        String text = heritage.getText(sourceCode).trim();
        if (text.startsWith(EXTENDS_PREFIX)) {
            return text.substring(EXTENDS_PREFIX.length()).trim();
        }
        return null;
    }

    private void processClassDecorator(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode decorator = findChildByType(node, "decorator");
        if (decorator != null) {
            processDecoratorAnnotation(decorator);
        }
    }

    private void processDecoratorAnnotation(ASTUtil.ASTNode decorator) {
        LocationInfo decoratorLocation = createDecoratorLocation(decorator);

        String decoratorName = getChildText(decorator, "identifier");
        if (decoratorName != null) {
            currentClass.addAnnotation(new UMLAnnotation(decoratorName, decoratorLocation));
        }
    }

    private LocationInfo createDecoratorLocation(ASTUtil.ASTNode decorator) {
        return new LocationInfo(
                filePath,
                decorator.getStartPoint(),
                decorator.getEndPoint(),
                CodeElementType.ANNOTATION_TYPE_DECLARATION
        );
    }

    private void processClassBody(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode body = findChildByType(node, "class_body");
        if (body != null) {
            body.getChildren().forEach(this::visit);
        }
    }

    private void addClassToModel(String className) {
        LOGGER.info("Adding class {} to model", className);
        model.addClass(currentClass);
        LOGGER.info("Current model classes count: {}", model.getClasses().size());
    }

    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        String methodName = extractMethodName(node);
        if (isInvalidMethod(methodName)) return;

        UMLOperation operation = createOperation(node, methodName);
        addOperationToModel(methodName, operation);
    }

    private boolean isInvalidMethod(String methodName) {
        boolean isInvalid = methodName == null;
        if (!isInvalid) {
            LOGGER.info("Processing method: {}", methodName);
        }
        return isInvalid;
    }

    private UMLOperation createOperation(ASTUtil.ASTNode node, String methodName) {
        LocationInfo location = createMethodLocation(node);
        UMLOperation operation = new UMLOperation(methodName, location);
        operation.setVisibility(Visibility.PUBLIC);

        processAsyncDecorator(node, operation);
        processParameters(node, operation);
        setMethodBody(node, operation);

        return operation;
    }

    private LocationInfo createMethodLocation(ASTUtil.ASTNode node) {
        return new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                CodeElementType.METHOD_DECLARATION);
    }

    private void processAsyncDecorator(ASTUtil.ASTNode node, UMLOperation operation) {
        ASTUtil.ASTNode asyncNode = findChildByType(node, "async");
        if (asyncNode != null) {
            LocationInfo asyncLocation = new LocationInfo(filePath, node.getStartPoint(), node.getEndPoint(),
                    CodeElementType.ANNOTATION_TYPE_DECLARATION);
            operation.addAnnotation(new UMLAnnotation("async", asyncLocation));
        }
    }

    private void processParameters(ASTUtil.ASTNode node, UMLOperation operation) {
        ASTUtil.ASTNode params = findChildByType(node, "formal_parameters");
        if (params != null) {
            params.getChildren().forEach(param -> processParameter(param, operation));
        }
    }

    private void setMethodBody(ASTUtil.ASTNode node, UMLOperation operation) {
        ASTUtil.ASTNode body = findChildByType(node, "statement_block");
        if (body != null) {
            operation.setBody(body.getText(sourceCode));
        }
    }

    private void addOperationToModel(String methodName, UMLOperation operation) {
        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            operation.setConstructor(methodName.equals("constructor"));
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
        String sanitizedText = node.getText(sourceCode).replaceAll("\n", "\\n");
        String logMessage = String.format("%s[%s] '%s' (children: %d)",
                indent,
                node.getType(),
                sanitizedText,
                node.getChildren().size()
        );

        LOGGER.info(logMessage);

        node.getChildren().forEach(child -> printNodeStructure(child, depth + 1));
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
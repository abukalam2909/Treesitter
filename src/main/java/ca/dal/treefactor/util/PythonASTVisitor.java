package ca.dal.treefactor.util;

import java.util.ArrayList;
import java.util.List;

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


public class PythonASTVisitor extends ASTVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PythonASTVisitor.class);
    private UMLClass currentClass; // Track current class being processed
    private final List<String> currentScope; // Track nested scopes

    public PythonASTVisitor(UMLModel model, String sourceCode, String filePath) {
        super(model, sourceCode, filePath);  // Parent class will initialize model, sourceCode, and filePath
        this.currentClass = null;
        this.currentScope = new ArrayList<>();
    }

    @Override
    public void visit(ASTUtil.ASTNode node) {
        // Process node based on its type
        switch(node.type) {
            case "module":
                processModule(node);
                break;
            case "class_definition":
            case "decorated_definition":
                processClass(node);
                // Don't visit children for class definitions since we handle them in processClass
                return;
            case "function_definition":
                // Only process standalone functions here
                // Methods inside classes are handled in processClass
                if (currentClass == null) {
                    processMethod(node);
                }
                break;
            case "assignment":
                processField(node);
                break;
            case "import_statement":
                processImport(node);
                break;
            case "import_from_statement":
                processFromImport(node);
                break;
        }

        // Visit children
        for(ASTUtil.ASTNode child : node.children) {
            visit(child);
        }
    }

    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        LOGGER.info("Processing class node: {}", node.type);

        // Find the actual class definition node
        ASTUtil.ASTNode classNode;
        List<ASTUtil.ASTNode> decorators = new ArrayList<>();

        if (node.type.equals("decorated_definition")) {
            LOGGER.info("Found decorated definition");
            // Get decorators directly from decorated_definition
            for (ASTUtil.ASTNode child : node.children) {
                if (child.type.equals("decorator")) {
                    decorators.add(child);
                }
            }
            classNode = findChildByType(node, "class_definition");
        } else {
            classNode = node;
        }

        if (classNode == null) return;

        String className = extractClassName(classNode);
        if (className == null) return;

        LOGGER.info("Found class: {}", className);

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.CLASS_DECLARATION
        );

        UMLClass previousClass = currentClass;
        currentClass = new UMLClass(extractModuleName(filePath), className, locationInfo);

        // Process decorators if any were found
        if (!decorators.isEmpty()) {
            LOGGER.info("Processing {} decorators", decorators.size());
            processDecorators(decorators, currentClass);
        }

        // Process inheritance
        processInheritance(classNode, currentClass);

        // Process docstring
        processDocstring(classNode, currentClass);

// Add to scope and process body
        currentScope.add(className);
        ASTUtil.ASTNode body = findChildByType(classNode, "block");
        if (body != null) {
            // Print AST for debugging
            LOGGER.info("Class body AST:");
            LOGGER.info(ASTUtil.printAST(body, 0));

            // Process body contents directly
            for (ASTUtil.ASTNode child : body.children) {
                LOGGER.info("Processing class body node: {}", child.type);
                if (child.type.equals("function_definition")) {
                    processMethod(child);
                } else if (child.type.equals("decorated_definition")) {
                    ASTUtil.ASTNode methodNode = findChildByType(child, "function_definition");
                    if (methodNode != null) {
                        processMethod(child);
                    }
                } else if (child.type.equals("expression_statement")) {
                    // Check if it's an assignment
                    ASTUtil.ASTNode assignmentNode = findChildByType(child, "assignment");
                    if (assignmentNode != null) {
                        LOGGER.info("Found assignment in class body: {}", assignmentNode.getText(sourceCode));
                        processField(assignmentNode);
                    }
                    // Handle docstrings
                    else if (child.children.size() == 1 &&
                            child.children.get(0).type.equals("string")) {
                        processDocstring(child, currentClass);
                    }
                } else if (child.type.equals("assignment")) {  // Direct assignment case
                    LOGGER.info("Found direct assignment in class body: {}", child.getText(sourceCode));
                    processField(child);
                } else if (child.type.equals("class_definition")) {
                    processClass(child);
                }
            }
        }

        currentScope.remove(currentScope.size() - 1);

        // Finalize class processing
        model.addClass(currentClass);
        currentClass = previousClass;

        LOGGER.info("Finished processing class: {}", className);
    }

    // Add this as a separate helper method in the class
    private Visibility determineVisibility(String name) {
        if (name.startsWith("__")) {
            return Visibility.PRIVATE;
        } else if (name.startsWith("_")) {
            return Visibility.PROTECTED;
        }
        return Visibility.PUBLIC;
    }

    @Override
    protected void processModule(ASTUtil.ASTNode node) {
        // Extract module name from file path
        String moduleName = extractModuleName(filePath);
        model.addPackage(filePath, moduleName);
    }


    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        String functionName = extractFunctionName(node);
        if (functionName == null) return;

        LOGGER.info("Processing method: {}", functionName);

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.METHOD_DECLARATION
        );

        UMLOperation.Builder builder = UMLOperation.builder(functionName, locationInfo);
        builder.visibility(determineVisibility(functionName));

        // Add method to scope before processing body
        currentScope.add("def " + functionName);

        // Process parameters
        processParameters(node, builder);

        // Process return type
        processReturnType(node, builder);

        // Process body
        ASTUtil.ASTNode body = findChildByType(node, "block");
        if (body != null) {
            builder.body(body.getText(sourceCode));
            // Visit body nodes to process attributes
            for (ASTUtil.ASTNode child : body.children) {
                visit(child);
            }
        }

        // Build and add operation
        UMLOperation operation = builder.build();
        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            currentClass.addOperation(operation);
            LOGGER.info("Added method " + functionName + " to class " + currentClass.getName());
        } else {
            model.addOperation(operation);
        }

        // Remove method from scope
        currentScope.remove(currentScope.size() - 1);
    }

    @Override
    protected void processField(ASTUtil.ASTNode node) {
        LOGGER.info("Processing potential field node: {}", node.type);

        // Skip if not in a class context
        if (currentClass == null) {
            LOGGER.info("No current class context");
            return;
        }

        if (node.type.equals("assignment")) {
            ASTUtil.ASTNode leftNode = findChildByFieldName(node, "left");
            LOGGER.info("Left node type: {}", (leftNode != null ? leftNode.type : "null"));

            if (leftNode != null) {
                if (leftNode.type.equals("attribute")) {
                    // Handle instance attributes (self.attribute assignments in methods)
                    ASTUtil.ASTNode objectNode = findChildByFieldName(leftNode, "object");
                    ASTUtil.ASTNode attributeNode = findChildByFieldName(leftNode, "attribute");

                    if (objectNode != null &&
                            attributeNode != null &&
                            objectNode.getText(sourceCode).equals("self")) {

                        String fieldName = attributeNode.getText(sourceCode);
                        LOGGER.info("Found instance attribute: {}", fieldName);

                        LocationInfo locationInfo = new LocationInfo(
                                filePath,
                                node.startPoint,
                                node.endPoint,
                                CodeElementType.FIELD_DECLARATION
                        );

                        UMLAttribute attribute = new UMLAttribute(
                                fieldName,
                                new UMLType("object"),
                                locationInfo
                        );

                        // Instance attributes are not static
                        attribute.setStatic(false);

                        // Set visibility based on name convention
                        attribute.setVisibility(determineVisibility(fieldName));

                        // Get initial value if present
                        ASTUtil.ASTNode rightNode = findChildByFieldName(node, "right");
                        if (rightNode != null) {
                            attribute.setInitialValue(rightNode.getText(sourceCode));
                            LOGGER.info("Set initial value: {}", rightNode.getText(sourceCode));
                        }

                        currentClass.addAttribute(attribute);
                        LOGGER.info("Added instance attribute: {}", fieldName);
                    }
                } else if (leftNode.type.equals("identifier") && !isInMethod()) {
                    // Handle class attributes (direct assignments in class body)
                    String fieldName = leftNode.getText(sourceCode);
                    LOGGER.info("Found class attribute: {}", fieldName);

                    LocationInfo locationInfo = new LocationInfo(
                            filePath,
                            node.startPoint,
                            node.endPoint,
                            CodeElementType.FIELD_DECLARATION
                    );

                    UMLAttribute attribute = new UMLAttribute(
                            fieldName,
                            new UMLType("object"),
                            locationInfo
                    );

                    // Class attributes are static
                    attribute.setStatic(true);

                    // Set visibility based on name convention
                    attribute.setVisibility(determineVisibility(fieldName));

                    // Get initial value if present
                    ASTUtil.ASTNode rightNode = findChildByFieldName(node, "right");
                    if (rightNode != null) {
                        attribute.setInitialValue(rightNode.getText(sourceCode));
                        LOGGER.info("Set initial value: {}", rightNode.getText(sourceCode));
                    }

                    currentClass.addAttribute(attribute);
                    LOGGER.info("Added class attribute: {}", fieldName);
                }
            }
        }

        // Print current attributes for debugging
        LOGGER.info("Current class attributes: {}",
                (currentClass != null ? currentClass.getAttributes().size() : "no class"));
    }

    @Override
    protected void processImport(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "dotted_name");
        if (nameNode == null) return;

        String importedName = nameNode.getText(sourceCode);
        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.IMPORT_DECLARATION
        );

        // Check for 'as' alias
        String alias = extractAlias(node);

        UMLImport umlImport = UMLImport.builder(importedName, locationInfo)
                .type(UMLImport.ImportType.SINGLE)
                .alias(alias)
                .build();

        model.addImport(filePath, umlImport);
    }

    private void processFromImport(ASTUtil.ASTNode node) {
        // Get the module name (os.path)
        ASTUtil.ASTNode moduleNode = findChildByFieldName(node, "module_name");
        if (moduleNode == null) return;

        String moduleName = moduleNode.getText(sourceCode);
        LOGGER.info("Module name: {}", moduleName);

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.IMPORT_DECLARATION
        );

        // Process each imported name
        for (ASTUtil.ASTNode child : node.children) {
            if (child.fieldName != null && child.fieldName.equals("name")) {
                String importedName = child.getText(sourceCode);
                LOGGER.info("Importing: {}", importedName);

                // Construct full import name (e.g., "os.path.join")
                String fullName = moduleName + "." + importedName;

                UMLImport umlImport = UMLImport.builder(fullName, locationInfo)
                        .type(UMLImport.ImportType.SINGLE)
                        .build();

                model.addImport(filePath, umlImport);
                LOGGER.info("Added import: {}", fullName);
            }
        }
    }

    // Helper methods
    private boolean isInMethod() {
        // Check if we're currently processing within a method
        return currentScope.stream().anyMatch(scope ->
                scope.startsWith("def "));
    }

    private boolean isInInitMethod() {
        // Check if we're in the __init__ method
        return currentScope.stream().anyMatch(scope ->
                scope.equals("def __init__"));
    }

    private String extractModuleName(String filePath) {
        // Convert file path to Python module notation
        return filePath.replace("/", ".")
                .replaceAll("\\.py$", "");
    }

    private String extractClassName(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "identifier");
        return nameNode != null ? nameNode.getText(sourceCode) : null;
    }

    private String extractFunctionName(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "identifier");
        return nameNode != null ? nameNode.getText(sourceCode) : null;
    }

    private String extractAlias(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode aliasNode = findChildByType(node, "alias");
        return aliasNode != null ? aliasNode.getText(sourceCode) : null;
    }


    private void processParameters(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        ASTUtil.ASTNode parameters = findChildByType(node, "parameters");
        if (parameters == null) return;

        // Flag to track if we've seen a keyword separator
        boolean keywordOnlyMode = false;

        for (ASTUtil.ASTNode param : parameters.children) {
            LOGGER.info("Parameter node type: {}", param.type);

            // Check for keyword separator (*)
            if (param.type.equals("keyword_separator")) {
                LOGGER.info("Found keyword separator");
                keywordOnlyMode = true;
                continue;
            }

            if (param.type.equals("typed_default_parameter")) {
                // Handle parameters with both type and default value
                ASTUtil.ASTNode nameNode = findChildByFieldName(param, "name");
                ASTUtil.ASTNode typeNode = findChildByFieldName(param, "type");
                ASTUtil.ASTNode valueNode = findChildByFieldName(param, "value");

                if (nameNode != null) {
                    String paramName = nameNode.getText(sourceCode);
                    UMLType paramType = new UMLType("object"); // Default type

                    if (typeNode != null) {
                        String typeName = processGenericType(typeNode);
                        paramType = new UMLType(typeName);
                    }

                    LocationInfo paramLocation = new LocationInfo(
                            filePath,
                            param.startPoint,
                            param.endPoint,
                            CodeElementType.PARAMETER_DECLARATION
                    );

                    UMLParameter parameter = new UMLParameter(paramName, paramType, paramLocation);

                    if (valueNode != null) {
                        parameter.setDefaultValue(valueNode.getText(sourceCode));
                        LOGGER.info("Set default value: {}", valueNode.getText(sourceCode));
                    }
                    parameter.setKeywordOnly(keywordOnlyMode);  // Set keyword-only flag
                    builder.addParameter(parameter);
                    LOGGER.info("Added typed parameter with default: {}", paramName +
                            " type: " + paramType.getTypeName() +
                            (keywordOnlyMode ? " (keyword-only)" : ""));
                }
            }
            else if (param.type.equals("typed_parameter")) {
                // Handle typed parameters
                ASTUtil.ASTNode nameNode = findChildByType(param, "identifier");
                ASTUtil.ASTNode typeNode = findChildByType(param, "type");

                if (nameNode != null) {
                    String paramName = nameNode.getText(sourceCode);
                    UMLType paramType = new UMLType("object"); // Default type

                    if (typeNode != null) {
                        String typeName = processGenericType(typeNode);
                        paramType = new UMLType(typeName);
                    }

                    LocationInfo paramLocation = new LocationInfo(
                            filePath,
                            param.startPoint,
                            param.endPoint,
                            CodeElementType.PARAMETER_DECLARATION
                    );

                    UMLParameter parameter = new UMLParameter(paramName, paramType, paramLocation);
                    parameter.setKeywordOnly(keywordOnlyMode);  // Set keyword-only flag

                    builder.addParameter(parameter);
                    LOGGER.info("Added typed parameter: {}", paramName +
                            " with type: " + paramType.getTypeName() +
                            (keywordOnlyMode ? " (keyword-only)" : ""));
                }
            }
            else if (param.type.equals("default_parameter")) {
                // Handle parameters with default values
                ASTUtil.ASTNode nameNode = findChildByFieldName(param, "name");
                ASTUtil.ASTNode valueNode = findChildByFieldName(param, "value");

                if (nameNode != null) {
                    String paramName = nameNode.getText(sourceCode);
                    LocationInfo paramLocation = new LocationInfo(
                            filePath,
                            param.startPoint,
                            param.endPoint,
                            CodeElementType.PARAMETER_DECLARATION
                    );

                    UMLParameter parameter = new UMLParameter(
                            paramName,
                            new UMLType("object"),
                            paramLocation
                    );

                    if (valueNode != null) {
                        parameter.setDefaultValue(valueNode.getText(sourceCode));
                        LOGGER.info("Set default value: {}", valueNode.getText(sourceCode));
                    }

                    parameter.setKeywordOnly(keywordOnlyMode);  // Set keyword-only flag
                    builder.addParameter(parameter);
                    LOGGER.info("Added parameter with default: {}", paramName);
                }
            }
            else if (param.type.equals("identifier")) {
                // Handle simple parameters without type or default value
                String paramName = param.getText(sourceCode);
                LocationInfo paramLocation = new LocationInfo(
                        filePath,
                        param.startPoint,
                        param.endPoint,
                        CodeElementType.PARAMETER_DECLARATION
                );

                UMLParameter parameter = new UMLParameter(
                        paramName,
                        new UMLType("object"),
                        paramLocation
                );
                parameter.setKeywordOnly(keywordOnlyMode);  // Set keyword-only flag

                builder.addParameter(parameter);
                LOGGER.info("Added simple parameter: " + paramName +
                        (keywordOnlyMode ? " (keyword-only)" : ""));
            }
        }
    }
    private void processReturnType(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        LOGGER.info("in processReturnType()");

        ASTUtil.ASTNode returnType = findChildByFieldName(node, "return_type");
        if (returnType != null) {
            LOGGER.info("return type != null");
            String typeName = processGenericType(returnType);
            builder.returnType(new UMLType(typeName));
        } else {
            builder.returnType(new UMLType("None")); // Default Python return type
        }
    }

    private String processGenericType(ASTUtil.ASTNode typeNode) {
        if (typeNode == null) return "object";

        LOGGER.info("Processing type node: {}", typeNode.type);

        // Get the generic_type node
        ASTUtil.ASTNode genericNode = findChildByType(typeNode, "generic_type");
        if (genericNode != null) {
            StringBuilder sb = new StringBuilder();

            // Get base type name (List, Dict, etc)
            ASTUtil.ASTNode baseType = findChildByType(genericNode, "identifier");
            if (baseType != null) {
                sb.append(baseType.getText(sourceCode));
            }

            // Find all type parameters
            List<String> typeParams = new ArrayList<>();
            for (ASTUtil.ASTNode child : genericNode.children) {
                if (child.type.equals("type_parameter")) {
                    LOGGER.info("Processing type parameter node, child count: {}", child.children.size());

                    // Collect all type nodes within this type_parameter
                    for (ASTUtil.ASTNode paramChild : child.children) {
                        if (paramChild.type.equals("type")) {
                            // Check if inner type is another generic type
                            ASTUtil.ASTNode innerGeneric = findChildByType(paramChild, "generic_type");
                            if (innerGeneric != null) {
                                // Recursively process nested generic type
                                typeParams.add(processGenericType(paramChild));
                            } else {
                                // Simple type
                                ASTUtil.ASTNode innerIdentifier = findChildByType(paramChild, "identifier");
                                if (innerIdentifier != null) {
                                    typeParams.add(innerIdentifier.getText(sourceCode));
                                }
                            }
                        }
                    }
                }
            }

            // Add all type parameters in brackets
            if (!typeParams.isEmpty()) {
                sb.append("[");
                sb.append(String.join(", ", typeParams));
                sb.append("]");
            }

            // Debug output
            LOGGER.info("Found generic type with base: " +
                    (baseType != null ? baseType.getText(sourceCode) : "null"));
            LOGGER.info("Type parameters found: {}", typeParams);

            String result = sb.toString();
            LOGGER.info("Generated type: {}", result);
            return result;
        } else {
            // For simple types
            ASTUtil.ASTNode identifier = findChildByType(typeNode, "identifier");
            String result = identifier != null ? identifier.getText(sourceCode) : "object";
            LOGGER.info("Simple type: {}", result);
            return result;
        }
    }

    private void processDecorators(List<ASTUtil.ASTNode> decorators, Object target) {
        LOGGER.info("Processing {} decorators", decorators.size());
        for (ASTUtil.ASTNode decorator : decorators) {
            // Get the identifier from the decorator
            ASTUtil.ASTNode identifierNode = findChildByType(decorator, "identifier");
            if (identifierNode != null) {
                String decoratorName = identifierNode.getText(sourceCode);
                LOGGER.info("Found decorator: {}", decoratorName);

                LocationInfo location = new LocationInfo(
                        filePath,
                        decorator.startPoint,
                        decorator.endPoint,
                        CodeElementType.ANNOTATION_TYPE_DECLARATION
                );

                UMLAnnotation annotation = new UMLAnnotation(decoratorName, location);

                if (target instanceof UMLClass) {
                    ((UMLClass) target).addAnnotation(annotation);
                    LOGGER.info("Added annotation to class: {}", decoratorName);
                } else if (target instanceof UMLOperation.Builder) {
                    ((UMLOperation.Builder) target).addAnnotation(annotation);
                }
            }
        }
    }

    private void processInheritance(ASTUtil.ASTNode node, UMLClass umlClass) {
        ASTUtil.ASTNode argList = findChildByType(node, "argument_list");
        if (argList == null) return;

        for (ASTUtil.ASTNode arg : argList.children) {
            if (arg.type.equals("identifier")) {
                umlClass.addSuperclass(arg.getText(sourceCode));
            }
        }
    }

    private void processDocstring(ASTUtil.ASTNode node, Object target) {
        ASTUtil.ASTNode body = findChildByType(node, "block");
        if (body == null || body.children.isEmpty()) return;

        ASTUtil.ASTNode firstChild = body.children.get(0);
        if (firstChild.type.equals("string")) {
            LocationInfo location = new LocationInfo(
                    filePath,
                    firstChild.startPoint,
                    firstChild.endPoint,
                    CodeElementType.COMMENT
            );

            UMLComment docComment = new UMLComment(
                    firstChild.getText(sourceCode),
                    location,
                    UMLComment.CommentType.DOC_COMMENT
            );

            if (target instanceof UMLClass) {
                ((UMLClass) target).addComment(docComment);
            } else if (target instanceof UMLOperation.Builder) {
                // Add comment after building the operation
                UMLOperation operation = ((UMLOperation.Builder) target).build();
                operation.addComment(docComment);
            }
        }
    }

}
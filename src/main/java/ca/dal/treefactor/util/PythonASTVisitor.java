package ca.dal.treefactor.util;

import ca.dal.treefactor.model.CodeElementType;
import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLAttribute;
import ca.dal.treefactor.model.core.*;
import io.github.treesitter.jtreesitter.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PythonASTVisitor extends ASTVisitor {
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
                break;
            case "function_definition":
                processMethod(node);
                // Don't visit children here - method handles its own body
                return;
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
    protected void processModule(ASTUtil.ASTNode node) {
        // Extract module name from file path
        String moduleName = extractModuleName(filePath);
        model.addPackage(filePath, moduleName);
    }

    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        System.out.println("Processing class node: " + node.type);

        // Find the actual class definition node
        ASTUtil.ASTNode classNode;
        List<ASTUtil.ASTNode> decorators = new ArrayList<>();

        if (node.type.equals("decorated_definition")) {
            System.out.println("Found decorated definition");
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

        System.out.println("Found class: " + className);

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
            System.out.println("Processing " + decorators.size() + " decorators");
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
            for (ASTUtil.ASTNode child : body.children) {
                visit(child);
            }
        }
        currentScope.remove(currentScope.size() - 1);

        // Finalize class processing
        model.addClass(currentClass);
        currentClass = previousClass;

        System.out.println("Finished processing class: " + className);
    }
    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        String functionName = extractFunctionName(node);
        if (functionName == null) return;

        System.out.println("Processing method: " + functionName);

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
            System.out.println("Added method " + functionName + " to class " + currentClass.getName());
        } else {
            model.addOperation(operation);
        }

        // Remove method from scope
        currentScope.remove(currentScope.size() - 1);
    }

    @Override
    protected void processField(ASTUtil.ASTNode node) {
        System.out.println("Processing potential field node: " + node.type);

        // Skip if not in a class context
        if (currentClass == null) {
            System.out.println("No current class context");
            return;
        }

        // Handle instance attributes (self.attribute assignments in __init__)
        if (node.type.equals("assignment")) {
            // Find left side of assignment
            ASTUtil.ASTNode leftNode = findChildByFieldName(node, "left");
            System.out.println("Left node type: " + (leftNode != null ? leftNode.type : "null"));

            // For instance attributes, the left node should be an "attribute" type
            // that has "self" as its object
            if (leftNode != null && leftNode.type.equals("attribute")) {
                // Get the object part (should be "self")
                ASTUtil.ASTNode objectNode = findChildByFieldName(leftNode, "object");
                if (objectNode != null && objectNode.getText(sourceCode).equals("self")) {
                    // Get the attribute name
                    ASTUtil.ASTNode attributeNode = findChildByFieldName(leftNode, "attribute");
                    if (attributeNode != null) {
                        String fieldName = attributeNode.getText(sourceCode);
                        System.out.println("Found instance attribute: " + fieldName);

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
                        }

                        currentClass.addAttribute(attribute);
                        System.out.println("Added instance attribute: " + fieldName);
                    }
                }
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
        ASTUtil.ASTNode moduleNode = findChildByType(node, "dotted_name");
        if (moduleNode == null) return;

        String moduleName = moduleNode.getText(sourceCode);
        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.IMPORT_DECLARATION
        );

        // Process imported names
        ASTUtil.ASTNode namesNode = findChildByType(node, "import_from_names");
        if (namesNode != null) {
            for (ASTUtil.ASTNode nameNode : namesNode.children) {
                if (nameNode.type.equals("dotted_name")) {
                    String importedName = nameNode.getText(sourceCode);
                    String fullName = moduleName + "." + importedName;

                    // Check for 'as' alias
                    String alias = extractAlias(nameNode);

                    UMLImport umlImport = UMLImport.builder(fullName, locationInfo)
                            .type(UMLImport.ImportType.SINGLE)
                            .alias(alias)
                            .build();

                    model.addImport(filePath, umlImport);
                }
            }
        }
    }

    // Helper methods
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

    private Visibility determineVisibility(String name) {
        if (name.startsWith("__")) {
            return Visibility.PRIVATE;
        } else if (name.startsWith("_")) {
            return Visibility.PROTECTED;
        }
        return Visibility.PUBLIC;
    }

    private void processParameters(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        ASTUtil.ASTNode parameters = findChildByType(node, "parameters");
        if (parameters == null) return;

        for (ASTUtil.ASTNode param : parameters.children) {
            if (param.type.equals("typed_parameter")) {
                // Handle typed parameters
                ASTUtil.ASTNode nameNode = findChildByType(param, "identifier");
                ASTUtil.ASTNode typeNode = findChildByType(param, "type");

                if (nameNode != null) {
                    String paramName = nameNode.getText(sourceCode);
                    UMLType paramType = new UMLType("object"); // Default type

                    if (typeNode != null) {
                        ASTUtil.ASTNode typeIdentifier = findChildByType(typeNode, "identifier");
                        if (typeIdentifier != null) {
                            paramType = new UMLType(typeIdentifier.getText(sourceCode));
                        }
                    }

                    LocationInfo paramLocation = new LocationInfo(
                            filePath,
                            param.startPoint,
                            param.endPoint,
                            CodeElementType.PARAMETER_DECLARATION
                    );

                    builder.addParameter(new UMLParameter(paramName, paramType, paramLocation));
                }
            } else if (param.type.equals("identifier")) {
                // Handle untyped parameters
                String paramName = param.getText(sourceCode);
                UMLType paramType = new UMLType("object"); // Default type for untyped parameters

                LocationInfo paramLocation = new LocationInfo(
                        filePath,
                        param.startPoint,
                        param.endPoint,
                        CodeElementType.PARAMETER_DECLARATION
                );

                builder.addParameter(new UMLParameter(paramName, paramType, paramLocation));
            }
        }
    }

    private void processReturnType(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        System.out.println("in processReturnType()"); // Debug output

        ASTUtil.ASTNode returnType = findChildByFieldName(node, "return_type");
        if (returnType != null) {
            System.out.println("return type != null"); // Debug output

            ASTUtil.ASTNode typeIdentifier = findChildByType(returnType, "identifier");
            if (typeIdentifier != null) {
                String returnTypeStr = typeIdentifier.getText(sourceCode);
                System.out.println("Found return type: " + returnTypeStr); // Debug output
                builder.returnType(new UMLType(returnTypeStr));
            } else {
                builder.returnType(new UMLType("None")); // Default Python return type
            }
        } else {
            builder.returnType(new UMLType("None")); // Default Python return type
        }
    }

    private List<ASTUtil.ASTNode> findDecorators(ASTUtil.ASTNode node) {
        List<ASTUtil.ASTNode> decorators = new ArrayList<>();

        // If this is a decorated_definition, get decorators directly
        if (node.type.equals("decorated_definition")) {
            for (ASTUtil.ASTNode child : node.children) {
                if (child.type.equals("decorator")) {
                    decorators.add(child);
                }
            }
        } else {
            // If this is a class_definition, check if parent is decorated_definition
            ASTUtil.ASTNode parent = node.parent;
            if (parent != null && parent.type.equals("decorated_definition")) {
                for (ASTUtil.ASTNode child : parent.children) {
                    if (child.type.equals("decorator")) {
                        decorators.add(child);
                    }
                }
            }
        }
        return decorators;
    }

    private void processDecorators(List<ASTUtil.ASTNode> decorators, Object target) {
        System.out.println("Processing " + decorators.size() + " decorators");
        for (ASTUtil.ASTNode decorator : decorators) {
            // Get the identifier from the decorator
            ASTUtil.ASTNode identifierNode = findChildByType(decorator, "identifier");
            if (identifierNode != null) {
                String decoratorName = identifierNode.getText(sourceCode);
                System.out.println("Found decorator: " + decoratorName);

                LocationInfo location = new LocationInfo(
                        filePath,
                        decorator.startPoint,
                        decorator.endPoint,
                        CodeElementType.ANNOTATION_TYPE_DECLARATION
                );

                UMLAnnotation annotation = new UMLAnnotation(decoratorName, location);

                if (target instanceof UMLClass) {
                    ((UMLClass) target).addAnnotation(annotation);
                    System.out.println("Added annotation to class: " + decoratorName);
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
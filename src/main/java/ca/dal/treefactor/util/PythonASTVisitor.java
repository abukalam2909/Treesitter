package ca.dal.treefactor.util;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.elements.UMLAttribute;
import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.CodeElementType;
import io.github.treesitter.jtreesitter.Point;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PythonASTVisitor extends ASTVisitor {
    private final String sourceCode;
    private final String filePath;
    private UMLClass currentClass;
    private final List<String> currentScope;

    public PythonASTVisitor(UMLModel umlModel, String sourceCode, String filePath) {
        super(umlModel);
        this.sourceCode = sourceCode;
        this.filePath = filePath;
        this.currentScope = new ArrayList<>();
    }

    @Override
    public void visit(ASTUtil.ASTNode node) {
        switch(node.type) {
            case "class_definition":
                processClass(node);
                break;
            case "function_definition":
                processMethod(node);
                break;
            case "assignment":
                processField(node);
                break;
            // Add more cases as needed for Python-specific constructs
        }

        // Visit children
        for(ASTUtil.ASTNode child : node.children) {
            visit(child);
        }
    }

    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        String className = extractClassName(node);
        if (className == null || className.isEmpty()) return;

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.CLASS_DECLARATION
        );

        UMLClass previousClass = currentClass;
        currentClass = new UMLClass(extractPackageName(), className, locationInfo);

        // Process class inheritance
        ASTUtil.ASTNode baseClasses = findChildByType(node, "argument_list");
        if (baseClasses != null) {
            processInheritance(baseClasses, currentClass);
        }

        // Process decorators
        List<ASTUtil.ASTNode> decorators = findChildrenByType(node, "decorator");
        for (ASTUtil.ASTNode decorator : decorators) {
            processDecorator(decorator, currentClass);
        }

        currentScope.add(className);

        // Process class body
        ASTUtil.ASTNode body = findChildByType(node, "block");
        if (body != null) {
            for (ASTUtil.ASTNode child : body.children) {
                visit(child);
            }
        }

        currentScope.remove(currentScope.size() - 1);
        umlModel.addClass(currentClass);
        currentClass = previousClass;
    }

    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        String methodName = extractMethodName(node);
        if (methodName == null || methodName.isEmpty()) return;

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.METHOD_DECLARATION
        );

        UMLOperation operation = new UMLOperation(methodName, locationInfo);

        // Process method parameters
        ASTUtil.ASTNode parameters = findChildByType(node, "parameters");
        if (parameters != null) {
            processParameters(parameters, operation);
        }

        // Process return type hints
        ASTUtil.ASTNode returnType = findChildByType(node, "type");
        if (returnType != null) {
            processReturnType(returnType, operation);
        } else {
            // Set default return type if none is specified
            operation.setReturnType(UMLType.createVoidType());
        }

        // Process decorators
        List<ASTUtil.ASTNode> decorators = findChildrenByType(node, "decorator");
        for (ASTUtil.ASTNode decorator : decorators) {
            processDecorator(decorator, operation);
        }

        // Set visibility based on name convention (_private, __private)
        setVisibilityFromName(methodName, operation);

        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            currentClass.addOperation(operation);
        }
    }

    private void processReturnType(ASTUtil.ASTNode returnTypeNode, UMLOperation operation) {
        String returnTypeText = getNodeText(returnTypeNode);
        UMLType returnType = new UMLType(returnTypeText);
        operation.setReturnType(returnType);
    }


    @Override
    protected void processField(ASTUtil.ASTNode node) {
        // Only process assignments at class level
        if (currentClass == null || !isClassLevelAssignment(node)) return;

        String fieldName = extractFieldName(node);
        if (fieldName == null || fieldName.isEmpty()) return;

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.FIELD_DECLARATION
        );

        // Try to determine type from annotation or assignment
        UMLType fieldType = determineFieldType(node);

        UMLAttribute attribute = new UMLAttribute(fieldName, fieldType, locationInfo);

        // Set visibility based on name convention
        setVisibilityFromName(fieldName, attribute);

        // Process field value
        ASTUtil.ASTNode value = findChildByType(node, "expression");
        if (value != null) {
            String initialValue = getNodeText(value);
            attribute.setInitialValue(initialValue);
        }

        attribute.setClassName(currentClass.getName());
        currentClass.addAttribute(attribute);
    }

    // Helper methods
    protected String extractPackageName() {
        Path path = Paths.get(filePath);
        Path parentPath = path.getParent();
        if (parentPath == null) {
            return ""; // Return an empty string if there is no parent path
        }
        String packagePath = parentPath.toString().replace('/', '.').replace('\\', '.');
        return packagePath.startsWith(".") ? packagePath.substring(1) : packagePath;
    }

    protected String extractClassName(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "identifier");
        return nameNode != null ? getNodeText(nameNode) : null;
    }

    protected String extractMethodName(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "identifier");
        return nameNode != null ? getNodeText(nameNode) : null;
    }

    protected String extractFieldName(ASTUtil.ASTNode node) {
        // For assignments, get the left-hand side identifier
        ASTUtil.ASTNode lhs = findChildByType(node, "identifier");
        return lhs != null ? getNodeText(lhs) : null;
    }

    protected String getNodeText(ASTUtil.ASTNode node) {
        int startOffset = getOffset(node.startPoint);
        int endOffset = getOffset(node.endPoint);
        return sourceCode.substring(startOffset, endOffset);
    }

    private int getOffset(Point point) {
        String[] lines = sourceCode.split("\n", -1);
        int offset = 0;
        for (int i = 0; i < point.row(); i++) {
            offset += lines[i].length() + 1; // +1 for newline
        }
        return offset + point.column();
    }

    private void processInheritance(ASTUtil.ASTNode baseClasses, UMLClass umlClass) {
        for (ASTUtil.ASTNode baseClass : baseClasses.children) {
            if (baseClass.type.equals("identifier")) {
                String baseClassName = getNodeText(baseClass);
                umlClass.addSuperclass(baseClassName);
            }
        }
    }

    private void processParameters(ASTUtil.ASTNode parameters, UMLOperation operation) {
        for (ASTUtil.ASTNode param : parameters.children) {
            if (param.type.equals("identifier")) {
                String paramName = getNodeText(param);
                UMLType paramType = UMLType.createVoidType(); // Default type if no type hint

                // Check for type hint
                ASTUtil.ASTNode typeHint = findChildByType(param, "type");
                if (typeHint != null) {
                    paramType = new UMLType(getNodeText(typeHint));
                }

                LocationInfo paramLocation = new LocationInfo(
                        filePath,
                        param.startPoint,
                        param.endPoint,
                        CodeElementType.PARAMETER_DECLARATION
                );

                operation.addParameter(new UMLParameter(paramName, paramType, paramLocation));
            }
        }
    }

    private void processDecorator(ASTUtil.ASTNode decorator, Object target) {
        String decoratorName = getNodeText(findChildByType(decorator, "identifier"));
        LocationInfo location = new LocationInfo(
                filePath,
                decorator.startPoint,
                decorator.endPoint,
                CodeElementType.ANNOTATION_TYPE_DECLARATION
        );

        UMLAnnotation annotation = new UMLAnnotation(decoratorName, location);

        if (target instanceof UMLClass) {
            ((UMLClass) target).addAnnotation(annotation);
        } else if (target instanceof UMLOperation) {
            ((UMLOperation) target).addAnnotation(annotation);
        }
    }

    private void setVisibilityFromName(String name, Object target) {
        Visibility visibility;
        if (name.startsWith("__")) {
            visibility = Visibility.PRIVATE;
        } else if (name.startsWith("_")) {
            visibility = Visibility.PROTECTED;
        } else {
            visibility = Visibility.PUBLIC;
        }

        if (target instanceof UMLOperation) {
            ((UMLOperation) target).setVisibility(visibility);
        } else if (target instanceof UMLAttribute) {
            ((UMLAttribute) target).setVisibility(visibility);
        }
    }

    private boolean isClassLevelAssignment(ASTUtil.ASTNode node) {
        return currentClass != null &&
                node.getParent().isPresent() && // Check if parent is present
                findChildByType(node.getParent().get(), "class_definition") != null; // Unwrap the parent
    }


    private UMLType determineFieldType(ASTUtil.ASTNode node) {
        // Try to get type from annotation
        ASTUtil.ASTNode typeHint = findChildByType(node, "type");
        if (typeHint != null) {
            return new UMLType(getNodeText(typeHint));
        }

        // Try to infer type from assignment
        ASTUtil.ASTNode value = findChildByType(node, "expression");
        if (value != null) {
            return inferTypeFromExpression(value);
        }

        return new UMLType("object"); // Default Python type
    }

    private UMLType inferTypeFromExpression(ASTUtil.ASTNode expression) {
        // Basic type inference based on literal types
        switch (expression.type) {
            case "integer":
                return new UMLType("int");
            case "float":
                return new UMLType("float");
            case "string":
                return new UMLType("str");
            case "true":
            case "false":
                return new UMLType("bool");
            case "list":
                return new UMLType("list");
            case "dictionary":
                return new UMLType("dict");
            default:
                return new UMLType("object");
        }
    }

    private ASTUtil.ASTNode findChildByType(ASTUtil.ASTNode parent, String type) {
        if (parent == null) return null;
        for (ASTUtil.ASTNode child : parent.children) {
            if (child.type.equals(type)) {
                return child;
            }
        }
        return null;
    }

    private List<ASTUtil.ASTNode> findChildrenByType(ASTUtil.ASTNode parent, String type) {
        List<ASTUtil.ASTNode> result = new ArrayList<>();
        if (parent == null) return result;
        for (ASTUtil.ASTNode child : parent.children) {
            if (child.type.equals(type)) {
                result.add(child);
            }
        }
        return result;
    }


}
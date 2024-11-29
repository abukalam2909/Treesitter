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
        switch(node.getType()) {
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
        for(ASTUtil.ASTNode child : node.getChildren()) {
            visit(child);
        }
    }


    /**
     * Process a class node to create a UML class representation.
     * Handles both direct class definitions and decorated classes.
     */
    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        LOGGER.info("Processing class node: {}", node.getType());

        // Extract class definition and any decorators
        ClassDefinitionInfo classInfo = getClassDefinitionInfo(node);
        if (classInfo.classNode == null) return;

        // Get class name
        String className = extractClassName(classInfo.classNode);
        if (className == null) return;

        LOGGER.info("Found class: {}", className);

        // Store current class and create new one
        UMLClass previousClass = currentClass;
        currentClass = createUMLClass(node, className);

        // Process all class elements (decorators, inheritance, body, etc)
        processClassElements(classInfo);

        // Add class to model and restore previous class context
        model.addClass(currentClass);
        currentClass = previousClass;

        LOGGER.info("Finished processing class: {}", className);
    }

    /**
     * Helper class to hold class definition node and its decorators.
     * Used to pass around related class information together.
     */
    private static class ClassDefinitionInfo {
        final ASTUtil.ASTNode classNode;
        final List<ASTUtil.ASTNode> decorators;

        ClassDefinitionInfo(ASTUtil.ASTNode classNode, List<ASTUtil.ASTNode> decorators) {
            this.classNode = classNode;
            this.decorators = decorators;
        }
    }

    /**
     * Extracts the class definition node and any decorators from the input node.
     * Handles both decorated and non-decorated class definitions.
     *
     * @param node The input node that could be either a class_definition or decorated_definition
     * @return ClassDefinitionInfo containing the class node and any decorators found
     */
    private ClassDefinitionInfo getClassDefinitionInfo(ASTUtil.ASTNode node) {
        List<ASTUtil.ASTNode> decorators = new ArrayList<>();
        ASTUtil.ASTNode classNode;

        if (node.getType().equals("decorated_definition")) {
            LOGGER.info("Found decorated definition");
            // Extract decorators from decorated definition
            for (ASTUtil.ASTNode child : node.getChildren()) {
                if (child.getType().equals("decorator")) {
                    decorators.add(child);
                }
            }
            classNode = findChildByType(node, "class_definition");
        } else {
            // Direct class definition without decorators
            classNode = node;
        }

        return new ClassDefinitionInfo(classNode, decorators);
    }

    /**
     * Creates a new UMLClass instance with the given name and location information.
     */
    private UMLClass createUMLClass(ASTUtil.ASTNode node, String className) {
        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.getStartPoint(),
                node.getEndPoint(),
                CodeElementType.CLASS_DECLARATION
        );
        return new UMLClass(extractModuleName(filePath), className, locationInfo);
    }

    /**
     * Processes all elements of a class including decorators, inheritance,
     * docstrings, and class body.
     */
    private void processClassElements(ClassDefinitionInfo classInfo) {
        // Process class features
        if (!classInfo.decorators.isEmpty()) {
            LOGGER.info("Processing {} decorators", classInfo.decorators.size());
            processDecorators(classInfo.decorators, currentClass);
        }

        // Process inheritance and documentation
        processInheritance(classInfo.classNode, currentClass);
        processDocstring(classInfo.classNode, currentClass);

        // Add to scope and process body
        currentScope.add(currentClass.getName());
        processClassBody(classInfo.classNode);
        currentScope.remove(currentScope.size() - 1);
    }

    /**
     * Processes the class body, including methods, fields, and nested classes.
     * @param classNode The class definition node containing the body
     */
    private void processClassBody(ASTUtil.ASTNode classNode) {
        ASTUtil.ASTNode body = findChildByType(classNode, "block");
        if (body == null) return;

        LOGGER.info("Class body AST:");
        LOGGER.info(ASTUtil.printAST(body, 0));

        for (ASTUtil.ASTNode child : body.getChildren()) {
            processClassBodyNode(child);
        }
    }

    /**
     * Processes a single node in the class body.
     * Routes each node to its appropriate processor based on its type.
     */
    private void processClassBodyNode(ASTUtil.ASTNode child) {
        LOGGER.info("Processing class body node: {}", child.getType());

        switch(child.getType()) {
            case "function_definition":
                processMethod(child);
                break;
            case "decorated_definition":
                processDecoratedMethod(child);
                break;
            case "expression_statement":
                processClassExpression(child);
                break;
            case "assignment":
                processField(child);
                break;
            case "class_definition":
                processClass(child);  // Handle nested classes
                break;
        }
    }

    /**
     * Processes a decorated method definition.
     * Extracts the method node from the decorator and processes it.
     */
    private void processDecoratedMethod(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode methodNode = findChildByType(node, "function_definition");
        if (methodNode != null) {
            processMethod(node);
        }
    }

    /**
     * Processes a class expression statement.
     * Handles both assignments and docstrings.
     */
    private void processClassExpression(ASTUtil.ASTNode child) {
        // Handle assignments
        ASTUtil.ASTNode assignmentNode = findChildByType(child, "assignment");
        if (assignmentNode != null) {
            LOGGER.info("Found assignment in class body: {}", assignmentNode.getText(sourceCode));
            processField(assignmentNode);
        }
        // Handle docstrings
        else if (child.getChildren().size() == 1 && child.getChildren().get(0).getType().equals("string")) {
            processDocstring(child, currentClass);
        }
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
                node.getStartPoint(),
                node.getEndPoint(),
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
            for (ASTUtil.ASTNode child : body.getChildren()) {
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

    protected void processField(ASTUtil.ASTNode node) {
        if (!shouldProcessField(node)) return;

        ASTUtil.ASTNode leftNode = findChildByFieldName(node, "left");
        if (leftNode == null) return;

        if (leftNode.getType().equals("attribute")) {
            processInstanceAttribute(node, leftNode);
        } else if (leftNode.getType().equals("identifier") && !isInMethod()) {
            processClassAttribute(node, leftNode);
        }
    }

    private boolean shouldProcessField(ASTUtil.ASTNode node) {
        if (currentClass == null) {
            LOGGER.info("No current class context");
            return false;
        }
        return node.getType().equals("assignment");
    }

    private void processInstanceAttribute(ASTUtil.ASTNode node, ASTUtil.ASTNode leftNode) {
        ASTUtil.ASTNode objectNode = findChildByFieldName(leftNode, "object");
        ASTUtil.ASTNode attributeNode = findChildByFieldName(leftNode, "attribute");

        if (!isValidSelfAttribute(objectNode, attributeNode)) return;

        String fieldName = attributeNode.getText(sourceCode);
        UMLAttribute attribute = createAttribute(node, fieldName, false);
        currentClass.addAttribute(attribute);
    }

    private boolean isValidSelfAttribute(ASTUtil.ASTNode objectNode, ASTUtil.ASTNode attributeNode) {
        return objectNode != null &&
                attributeNode != null &&
                objectNode.getText(sourceCode).equals("self");
    }

    private void processClassAttribute(ASTUtil.ASTNode node, ASTUtil.ASTNode leftNode) {
        String fieldName = leftNode.getText(sourceCode);
        UMLAttribute attribute = createAttribute(node, fieldName, true);
        currentClass.addAttribute(attribute);
    }

    private UMLAttribute createAttribute(ASTUtil.ASTNode node, String fieldName, boolean isStatic) {
        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.getStartPoint(),
                node.getEndPoint(),
                CodeElementType.FIELD_DECLARATION
        );

        UMLAttribute attribute = new UMLAttribute(
                fieldName,
                new UMLType("object"),
                locationInfo
        );

        attribute.setStatic(isStatic);
        attribute.setVisibility(determineVisibility(fieldName));
        setInitialValue(node, attribute);

        return attribute;
    }

    private void setInitialValue(ASTUtil.ASTNode node, UMLAttribute attribute) {
        ASTUtil.ASTNode rightNode = findChildByFieldName(node, "right");
        if (rightNode != null) {
            attribute.setInitialValue(rightNode.getText(sourceCode));
        }
    }

    @Override
    protected void processImport(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "dotted_name");
        if (nameNode == null) return;

        String importedName = nameNode.getText(sourceCode);
        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.getStartPoint(),
                node.getEndPoint(),
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
                node.getStartPoint(),
                node.getEndPoint(),
                CodeElementType.IMPORT_DECLARATION
        );

        // Process each imported name
        for (ASTUtil.ASTNode child : node.getChildren()) {
            if (child.getFieldName() != null && child.getFieldName().equals("name")) {
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

    /**
     * Processes method parameters, handling different types of parameters including:
     * - Simple parameters
     * - Typed parameters
     * - Parameters with default values
     * - Keyword-only parameters
     */
    private void processParameters(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        ASTUtil.ASTNode parameters = findChildByType(node, "parameters");
        if (parameters == null) return;

        boolean keywordOnlyMode = false;

        for (ASTUtil.ASTNode param : parameters.getChildren()) {
            LOGGER.info("Parameter node type: {}", param.getType());

            if (param.getType().equals("keyword_separator")) {
                LOGGER.info("Found keyword separator");
                keywordOnlyMode = true;
                continue;
            }

            processParameter(param, builder, keywordOnlyMode);
        }
    }

    /**
     * Routes parameter processing based on parameter type
     */
    private void processParameter(ASTUtil.ASTNode param, UMLOperation.Builder builder, boolean keywordOnlyMode) {
        switch (param.getType()) {
            case "typed_default_parameter":
                processTypedDefaultParameter(param, builder, keywordOnlyMode);
                break;
            case "typed_parameter":
                processTypedParameter(param, builder, keywordOnlyMode);
                break;
            case "default_parameter":
                processDefaultParameter(param, builder, keywordOnlyMode);
                break;
            case "identifier":
                processSimpleParameter(param, builder, keywordOnlyMode);
                break;
        }
    }

    /**
     * Processes a parameter with both type annotation and default value
     * Example: def func(param: str = "default")
     */
    private void processTypedDefaultParameter(ASTUtil.ASTNode param, UMLOperation.Builder builder, boolean keywordOnlyMode) {
        ASTUtil.ASTNode nameNode = findChildByFieldName(param, "name");
        if (nameNode == null) return;

        String paramName = nameNode.getText(sourceCode);
        UMLType paramType = getParameterType(param);
        UMLParameter parameter = createParameter(param, paramName, paramType);

        setParameterProperties(parameter, param, keywordOnlyMode);
        builder.addParameter(parameter);

        LOGGER.info("Added typed parameter with default: {} type: {} {}",
                paramName, paramType.getTypeName(),
                keywordOnlyMode ? "(keyword-only)" : "");
    }

    /**
     * Processes a parameter with type annotation only
     * Example: def func(param: str)
     */
    private void processTypedParameter(ASTUtil.ASTNode param, UMLOperation.Builder builder, boolean keywordOnlyMode) {
        ASTUtil.ASTNode nameNode = findChildByType(param, "identifier");
        if (nameNode == null) return;

        String paramName = nameNode.getText(sourceCode);
        UMLType paramType = getParameterType(param);
        UMLParameter parameter = createParameter(param, paramName, paramType);

        parameter.setKeywordOnly(keywordOnlyMode);
        builder.addParameter(parameter);

        LOGGER.info("Added typed parameter: {} with type: {} {}",
                paramName, paramType.getTypeName(),
                keywordOnlyMode ? "(keyword-only)" : "");
    }

    /**
     * Processes a parameter with default value only
     * Example: def func(param="default")
     */
    private void processDefaultParameter(ASTUtil.ASTNode param, UMLOperation.Builder builder, boolean keywordOnlyMode) {
        ASTUtil.ASTNode nameNode = findChildByFieldName(param, "name");
        if (nameNode == null) return;

        String paramName = nameNode.getText(sourceCode);
        UMLParameter parameter = createParameter(param, paramName, new UMLType("object"));

        setParameterProperties(parameter, param, keywordOnlyMode);
        builder.addParameter(parameter);

        LOGGER.info("Added parameter with default: {}", paramName);
    }

    /**
     * Processes a simple parameter without type or default value
     * Example: def func(param)
     */
    private void processSimpleParameter(ASTUtil.ASTNode param, UMLOperation.Builder builder, boolean keywordOnlyMode) {
        String paramName = param.getText(sourceCode);
        UMLParameter parameter = createParameter(param, paramName, new UMLType("object"));

        parameter.setKeywordOnly(keywordOnlyMode);
        builder.addParameter(parameter);

        LOGGER.info("Added simple parameter: {} {}",
                paramName, keywordOnlyMode ? "(keyword-only)" : "");
    }

    /**
     * Creates a base UMLParameter with location information
     */
    private UMLParameter createParameter(ASTUtil.ASTNode param, String name, UMLType type) {
        LocationInfo paramLocation = new LocationInfo(
                filePath,
                param.getStartPoint(),
                param.getEndPoint(),
                CodeElementType.PARAMETER_DECLARATION
        );
        return new UMLParameter(name, type, paramLocation);
    }

    /**
     * Gets the parameter type from type annotation, defaulting to "object" if not specified
     */
    private UMLType getParameterType(ASTUtil.ASTNode param) {
        ASTUtil.ASTNode typeNode = findChildByFieldName(param, "type");
        if (typeNode == null) {
            return new UMLType("object");
        }
        String typeName = processGenericType(typeNode);
        return new UMLType(typeName);
    }

    /**
     * Sets common parameter properties like default value and keyword-only flag
     */
    private void setParameterProperties(UMLParameter parameter, ASTUtil.ASTNode param, boolean keywordOnlyMode) {
        ASTUtil.ASTNode valueNode = findChildByFieldName(param, "value");
        if (valueNode != null) {
            parameter.setDefaultValue(valueNode.getText(sourceCode));
            LOGGER.info("Set default value: {}", valueNode.getText(sourceCode));
        }
        parameter.setKeywordOnly(keywordOnlyMode);
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
    /**
     * Process a type node and extract its type information.
     * Handles both simple types (e.g., "str", "int") and generic types (e.g., "List[str]", "Dict[str, Any]").
     *
     * @param typeNode The AST node representing the type
     * @return String representation of the type
     */
    private String processGenericType(ASTUtil.ASTNode typeNode) {
        if (typeNode == null) return "object";

        LOGGER.info("Processing type node: {}", typeNode.getType());

        // Check if this is a generic type (e.g., List[str]) or a simple type (e.g., int)
        ASTUtil.ASTNode genericNode = findChildByType(typeNode, "generic_type");
        if (genericNode != null) {
            return processGenericTypeNode(genericNode);
        } else {
            return processSimpleType(typeNode);
        }
    }

    /**
     * Process a generic type node and build its complete type representation.
     * Example: For List[str], processes both the 'List' part and the '[str]' part.
     *
     * @param genericNode The AST node representing the generic type
     * @return Complete string representation of the generic type
     */
    private String processGenericTypeNode(ASTUtil.ASTNode genericNode) {
        StringBuilder sb = new StringBuilder();

        // Process the base type (e.g., the 'List' in 'List[str]')
        processBaseType(genericNode, sb);

        // Process any type parameters (e.g., the 'str' in 'List[str]')
        List<String> typeParams = collectTypeParameters(genericNode);
        appendTypeParameters(sb, typeParams);

        // Log debug information about the processed type
        logGenericTypeInfo(genericNode, typeParams);

        return sb.toString();
    }

    /**
     * Process the base type of a generic type (e.g., the 'List' in 'List[str]').
     * Appends the base type name to the provided StringBuilder.
     *
     * @param genericNode The generic type AST node
     * @param sb StringBuilder to append the base type to
     */
    private void processBaseType(ASTUtil.ASTNode genericNode, StringBuilder sb) {
        ASTUtil.ASTNode baseType = findChildByType(genericNode, "identifier");
        if (baseType != null) {
            sb.append(baseType.getText(sourceCode));
        }
    }

    /**
     * Collect and process all type parameters from a generic type.
     * For example, in Dict[str, Any], collects both 'str' and 'Any'.
     *
     * @param genericNode The generic type AST node
     * @return List of processed type parameter strings
     */
    private List<String> collectTypeParameters(ASTUtil.ASTNode genericNode) {
        List<String> typeParams = new ArrayList<>();

        for (ASTUtil.ASTNode child : genericNode.getChildren()) {
            if (child.getType().equals("type_parameter")) {
                processTypeParameter(child, typeParams);
            }
        }

        return typeParams;
    }

    /**
     * Process a single type parameter node and add its type to the typeParams list.
     * Handles both simple type parameters and nested generic types.
     *
     * @param paramNode The type parameter AST node
     * @param typeParams List to add the processed type parameter to
     */
    private void processTypeParameter(ASTUtil.ASTNode paramNode, List<String> typeParams) {
        LOGGER.info("Processing type parameter node, child count: {}", paramNode.getChildren().size());

        for (ASTUtil.ASTNode paramChild : paramNode.getChildren()) {
            if (paramChild.getType().equals("type")) {
                String paramType = extractParameterType(paramChild);
                if (paramType != null) {
                    typeParams.add(paramType);
                }
            }
        }
    }

    /**
     * Extract the type from a parameter type node.
     * Handles both simple types and nested generic types recursively.
     *
     * @param typeNode The type AST node
     * @return String representation of the parameter type
     */
    private String extractParameterType(ASTUtil.ASTNode typeNode) {
        // Handle nested generic types (e.g., List[List[str]])
        ASTUtil.ASTNode innerGeneric = findChildByType(typeNode, "generic_type");
        if (innerGeneric != null) {
            return processGenericType(typeNode);  // Recursive call for nested generic types
        }

        // Handle simple types
        ASTUtil.ASTNode innerIdentifier = findChildByType(typeNode, "identifier");
        return innerIdentifier != null ? innerIdentifier.getText(sourceCode) : null;
    }

    /**
     * Append type parameters to the generic type representation.
     * Adds the square brackets and joins multiple type parameters with commas.
     *
     * @param sb StringBuilder to append to
     * @param typeParams List of type parameter strings to append
     */
    private void appendTypeParameters(StringBuilder sb, List<String> typeParams) {
        if (!typeParams.isEmpty()) {
            sb.append("[");
            sb.append(String.join(", ", typeParams));
            sb.append("]");
        }
    }

    /**
     * Log debug information about the processed generic type.
     * Includes the base type and all found type parameters.
     *
     * @param genericNode The generic type AST node
     * @param typeParams List of processed type parameters
     */
    private void logGenericTypeInfo(ASTUtil.ASTNode genericNode, List<String> typeParams) {
        ASTUtil.ASTNode baseType = findChildByType(genericNode, "identifier");
        LOGGER.info("Found generic type with base: {}",
                baseType != null ? baseType.getText(sourceCode) : "null");
        LOGGER.info("Type parameters found: {}", typeParams);
    }

    /**
     * Process a simple (non-generic) type node.
     * Handles basic types like 'str', 'int', etc.
     *
     * @param typeNode The type AST node
     * @return String representation of the simple type
     */
    private String processSimpleType(ASTUtil.ASTNode typeNode) {
        ASTUtil.ASTNode identifier = findChildByType(typeNode, "identifier");
        String result = identifier != null ? identifier.getText(sourceCode) : "object";
        LOGGER.info("Simple type: {}", result);
        return result;
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
                        decorator.getStartPoint(),
                        decorator.getEndPoint(),
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

        for (ASTUtil.ASTNode arg : argList.getChildren()) {
            if (arg.getType().equals("identifier")) {
                umlClass.addSuperclass(arg.getText(sourceCode));
            }
        }
    }

    private void processDocstring(ASTUtil.ASTNode node, Object target) {
        ASTUtil.ASTNode body = findChildByType(node, "block");
        if (body == null || body.getChildren().isEmpty()) return;

        ASTUtil.ASTNode firstChild = body.getChildren().get(0);
        if (firstChild.getType().equals("string")) {
            LocationInfo location = new LocationInfo(
                    filePath,
                    firstChild.getStartPoint(),
                    firstChild.getEndPoint(),
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
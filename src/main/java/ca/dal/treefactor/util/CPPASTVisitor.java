package ca.dal.treefactor.util;

import ca.dal.treefactor.model.CodeElementType;
import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLAttribute;
import ca.dal.treefactor.model.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPPASTVisitor extends ASTVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CPPASTVisitor.class);
    private UMLClass currentClass;
    private final List<String> currentScope;
    private String currentNamespace;
    private Visibility currentVisibility;  // Add this field

    public CPPASTVisitor(UMLModel model, String sourceCode, String filePath) {
        super(model, sourceCode, filePath);
        this.currentClass = null;
        this.currentScope = new ArrayList<>();
        this.currentNamespace = "";
        this.currentVisibility = Visibility.PRIVATE;  // Default visibility in C++
    }

    @Override
    public void visit(ASTUtil.ASTNode node) {
        LOGGER.info("Visiting node type: {}", node.getType());

        if (node.getType().equals("template_declaration")) {
            // Process the function definition inside the template
            ASTUtil.ASTNode funcDef = findFirstNodeOfType(node, "function_definition");
            if (funcDef != null) {
                processMethod(funcDef);
            }
        } else {
            switch (node.getType()) {
                case "translation_unit":
                    for (ASTUtil.ASTNode child : node.getChildren()) {
                        visit(child);
                    }
                    break;
                case "class_specifier":
                    LOGGER.info("Processing class_specifier: {}", node.getText(sourceCode));
                    processClass(node);
                    break;
                case "function_definition":
                    if (currentClass == null) {
                        LOGGER.info("Processing standalone function: {}", node.getText(sourceCode));
                        processMethod(node);
                    }
                    break;
                case "declaration":
                    if (currentClass == null) {
                        LOGGER.info("Processing declaration: {}", node.getText(sourceCode));
                        processDeclaration(node);
                    }
                    break;
                case "field_declaration":
                    LOGGER.info("Processing field_declaration: {}", node.getText(sourceCode));
                    processField(node);
                    break;
                case "preproc_include":
                    processImport(node);
                    break;
            }
        }
    }

    private String getDefaultValue(ASTUtil.ASTNode defaultValueNode) {
        if (defaultValueNode == null) {
            return null;
        }

        // Get the first child of default_value node
        if (!defaultValueNode.getChildren().isEmpty()) {
            ASTUtil.ASTNode valueNode = defaultValueNode.getChildren().get(0);

            // Direct text extraction based on node type
            switch (valueNode.getType()) {
                case "string_literal":
                    return valueNode.getText(sourceCode); // Returns the full text including quotes
                case "number_literal":
                    return valueNode.getText(sourceCode);
                case "false":
                    return "false";
                case "true":
                    return "true";
                case "null":
                    return "nullptr";
                default:
                    return valueNode.getText(sourceCode);
            }
        }
        return null;
    }

    private void processParameter(ASTUtil.ASTNode paramNode, UMLOperation operation) {
        boolean isOptional = paramNode.getType().equals("optional_parameter_declaration");

        // Build the parameter
        StringBuilder typeStr = new StringBuilder();
        String paramName = null;
        boolean isPointer = false;
        boolean isReference = false;
        boolean isRValueReference = false;
        boolean isConst = false;

        // Handle const qualifier
        ASTUtil.ASTNode typeQualifier = findFirstNodeOfType(paramNode, "type_qualifier");
        if (typeQualifier != null && typeQualifier.getText(sourceCode).equals("const")) {
            isConst = true;
        }

        // Get qualified type
        ASTUtil.ASTNode qualifiedType = findFirstNodeOfType(paramNode, "qualified_identifier");
        if (qualifiedType != null) {
            processQualifiedType(qualifiedType, typeStr);
        } else {
            ASTUtil.ASTNode primitiveType = findFirstNodeOfType(paramNode, "primitive_type");
            if (primitiveType != null) {
                typeStr.append(primitiveType.getText(sourceCode));
            }
        }

        // Get parameter name and handle declarators
        paramName = getParameterName(paramNode);

        if (paramName != null) {
            LocationInfo location = LocationInfo.builder()
                    .filePath(filePath)
                    .startPoint(paramNode.getStartPoint())
                    .endPoint(paramNode.getEndPoint())
                    .type(CodeElementType.PARAMETER_DECLARATION)
                    .build();

            UMLParameter parameter = new UMLParameter(paramName, new UMLType(typeStr.toString()), location);

            // Handle pointer and reference declarators
            ASTUtil.ASTNode pointerDec = findFirstNodeOfType(paramNode, "pointer_declarator");
            if (pointerDec != null) {
                parameter.setPointer(true);
            }

            ASTUtil.ASTNode refDec = findFirstNodeOfType(paramNode, "reference_declarator");
            if (refDec != null) {
                String refText = refDec.getText(sourceCode);
                if (refText.contains("&&")) {
                    parameter.setRValueReference(true);
                } else {
                    parameter.setReference(true);
                }
            }

            parameter.setConst(isConst);

            if (isOptional) {
                ASTUtil.ASTNode defaultValueNode = findFirstNodeOfType(paramNode, "default_value");
                if (defaultValueNode != null) {
                    String defaultValue = getDefaultValue(defaultValueNode);
                    if (defaultValue != null) {
                        parameter.setDefaultValue(defaultValue);
                    }
                }
            }

            operation.addParameter(parameter);
        }
    }

    private String getIdentifierFromNode(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode idNode = findFirstNodeOfType(node, "identifier");
        return idNode != null ? idNode.getText(sourceCode) : null;
    }

    private String getParameterName(ASTUtil.ASTNode paramNode) {
        // Try direct identifier first
        ASTUtil.ASTNode idNode = findFirstNodeOfType(paramNode, "identifier");
        if (idNode != null) {
            return idNode.getText(sourceCode);
        }

        // Try reference declarator
        ASTUtil.ASTNode refDec = findFirstNodeOfType(paramNode, "reference_declarator");
        if (refDec != null) {
            idNode = findFirstNodeOfType(refDec, "identifier");
            if (idNode != null) {
                return idNode.getText(sourceCode);
            }
        }

        // Try pointer declarator
        ASTUtil.ASTNode ptrDec = findFirstNodeOfType(paramNode, "pointer_declarator");
        if (ptrDec != null) {
            idNode = findFirstNodeOfType(ptrDec, "identifier");
            if (idNode != null) {
                return idNode.getText(sourceCode);
            }
        }

        return null;
    }


    private void processAccessSpecifiers(ASTUtil.ASTNode node) {
        for (ASTUtil.ASTNode child : node.getChildren()) {
            LOGGER.info("Processing body node: {}", child.getType());

            if (child.getType().equals("access_specifier")) {
                String specifier = child.getText(sourceCode);
                LOGGER.info("Found access specifier: {}", specifier);
                switch (specifier.toLowerCase()) {
                    case "public":
                        currentVisibility = Visibility.PUBLIC;
                        break;
                    case "protected":
                        currentVisibility = Visibility.PROTECTED;
                        break;
                    case "private":
                        currentVisibility = Visibility.PRIVATE;
                        break;
                }
                continue;
            }

            if (child.getType().equals("function_definition")) {
                LOGGER.info("Processing method within class");
                processMethod(child);
            }

            if (child.getType().equals("field_declaration")) {
                LOGGER.info("Processing field within class");
                processField(child);
            }
        }
    }

    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        LOGGER.info("Starting method processing");

        // Get function declarator
        ASTUtil.ASTNode declaratorNode = findFirstNodeOfType(node, "function_declarator");
        if (declaratorNode == null) {
            LOGGER.info("No function declarator found");
            return;
        }

        // Get direct child identifier of declarator (this will be the method name)
        String methodName = null;
        for (ASTUtil.ASTNode child : declaratorNode.getChildren()) {
            if (child.getType().equals("identifier") || child.getType().equals("field_identifier")) {
                methodName = child.getText(sourceCode);
                break;
            }
        }

        if (methodName == null) {
            LOGGER.info("No method name found");
            return;
        }

        LOGGER.info("Found method name: {}", methodName);

        LocationInfo location = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.getStartPoint())
                .endPoint(node.getEndPoint())
                .type(CodeElementType.METHOD_DECLARATION)
                .build();

        UMLOperation operation = new UMLOperation(methodName, location);
        operation.setVisibility(currentVisibility);

        // Process method details
        processMethodDetails(node, operation);

        // Add method to class or model
        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            if (methodName.equals(currentClass.getName())) {
                LOGGER.info("Found constructor: {}", methodName);
                operation.setConstructor(true);
            }
            currentClass.addOperation(operation);
            LOGGER.info("Added method {} to class {}", methodName, currentClass.getName());
            LOGGER.info("Class now has {} operations", currentClass.getOperations().size());
        } else {
            LOGGER.info("Adding standalone method: {}", methodName);
            model.addOperation(operation);
        }
    }

    private void processMethodDetails(ASTUtil.ASTNode node, UMLOperation operation) {
        // Get return type
        ASTUtil.ASTNode typeNode = findFirstNodeOfType(node, "primitive_type");
        if (typeNode != null) {
            operation.setReturnType(new UMLType(getNodeText(typeNode)));
            LOGGER.info("Set return type: {}", getNodeText(typeNode));
        }

        // Get function declarator
        ASTUtil.ASTNode declaratorNode = findFirstNodeOfType(node, "function_declarator");
        if (declaratorNode == null) return;

        // Process parameters
        ASTUtil.ASTNode paramListNode = findFirstNodeOfType(declaratorNode, "parameter_list");
        if (paramListNode != null) {
            for (ASTUtil.ASTNode paramNode : paramListNode.getChildren()) {
                if (paramNode.getType().equals("parameter_declaration") ||
                        paramNode.getType().equals("optional_parameter_declaration")) {
                    processParameter(paramNode, operation);
                }
            }
            LOGGER.info("Processed {} parameters", operation.getParameters().size());
        }

        // Process modifiers
        String declaratorText = declaratorNode.getText(sourceCode);
        LOGGER.info("Processing declarator: {}", declaratorText);

        // Check for const modifier
        ASTUtil.ASTNode constNode = findFirstNodeOfType(declaratorNode, "type_qualifier");
        if (constNode != null && getNodeText(constNode).equals("const")) {
            operation.setConst(true);
            LOGGER.info("Set const modifier");
        }

        // Check for noexcept
        ASTUtil.ASTNode noexceptNode = findFirstNodeOfType(declaratorNode, "noexcept");
        if (noexceptNode != null) {
            operation.setNoexcept(true);
            LOGGER.info("Set noexcept modifier");
        }

        // Check for virtual
        if (declaratorText.contains("virtual")) {
            operation.setVirtual(true);
            LOGGER.info("Set virtual modifier");
        }

        // Check for static
        if (declaratorText.contains("static")) {
            operation.setStatic(true);
            LOGGER.info("Set static modifier");
        }

        // Check if it's a pure virtual method (abstract)
        if (declaratorText.contains("= 0")) {
            operation.setAbstract(true);
            LOGGER.info("Set abstract (pure virtual)");
        }
    }

    private void processDeclaration(ASTUtil.ASTNode node) {
        LOGGER.info("Processing declaration: {}", node.getText(sourceCode));
        String fullText = node.getText(sourceCode);

        // Check if this is a static field initialization
        if (fullText.contains("::")) {
            processStaticFieldInitialization(node, fullText);
            return;
        }

        // Check if this is a method declaration
        ASTUtil.ASTNode functionDeclarator = findFirstNodeOfType(node, "function_declarator");
        if (functionDeclarator != null) {
            processMethodDeclaration(node, functionDeclarator);
        }
    }

    private void processMethodDeclaration(ASTUtil.ASTNode node, ASTUtil.ASTNode declaratorNode) {
        // Get return type
        ASTUtil.ASTNode typeNode = findFirstNodeOfType(node, "primitive_type");
        String returnType = typeNode != null ? getNodeText(typeNode) : "void";

        // Get method name
        ASTUtil.ASTNode nameNode = findFirstNodeOfType(declaratorNode, "identifier");
        if (nameNode == null) return;
        String methodName = getNodeText(nameNode);

        LOGGER.info("Processing method declaration: {}", methodName);

        // Create operation
        LocationInfo location = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.getStartPoint())
                .endPoint(node.getEndPoint())
                .type(CodeElementType.METHOD_DECLARATION)
                .build();

        UMLOperation operation = new UMLOperation(methodName, location);
        operation.setReturnType(new UMLType(returnType));

        // Get full declaration text to check modifiers
        String fullText = node.getText(sourceCode);

        // Check virtual
        if (fullText.contains("virtual")) {
            LOGGER.info("Setting virtual modifier");
            operation.setVirtual(true);
        }

        // Check pure virtual (abstract)
        if (fullText.contains("= 0")) {
            LOGGER.info("Setting abstract modifier");
            operation.setAbstract(true);
        }

        // Process parameters
        ASTUtil.ASTNode paramListNode = findFirstNodeOfType(declaratorNode, "parameter_list");
        if (paramListNode != null) {
            for (ASTUtil.ASTNode paramNode : paramListNode.getChildren()) {
                if (paramNode.getType().equals("parameter_declaration") ||
                        paramNode.getType().equals("optional_parameter_declaration")) {
                    processParameter(paramNode, operation);
                }
            }
        }

        LOGGER.info("Adding method declaration to model: {}", methodName);
        model.addOperation(operation);
    }

    private void processQualifiedType(ASTUtil.ASTNode qualifiedType, StringBuilder typeStr) {
        // Handle namespace (std::)
        ASTUtil.ASTNode scopeNode = findFirstNodeOfType(qualifiedType, "namespace_identifier");
        if (scopeNode != null) {
            typeStr.append(scopeNode.getText(sourceCode)).append("::");
        }

        // Handle template types
        ASTUtil.ASTNode templateType = findFirstNodeOfType(qualifiedType, "template_type");
        if (templateType != null) {
            // Get base type name (e.g., vector)
            ASTUtil.ASTNode baseType = findFirstNodeOfType(templateType, "type_identifier");
            if (baseType != null) {
                typeStr.append(baseType.getText(sourceCode));
            }

            // Handle template arguments
            ASTUtil.ASTNode argList = findFirstNodeOfType(templateType, "template_argument_list");
            if (argList != null) {
                typeStr.append("<");
                StringBuilder args = new StringBuilder();
                boolean first = true;

                for (ASTUtil.ASTNode arg : argList.getChildren()) {
                    if (!first) args.append(", ");
                    first = false;

                    ASTUtil.ASTNode typeDesc = findFirstNodeOfType(arg, "type_descriptor");
                    if (typeDesc != null) {
                        // Check for function type
                        ASTUtil.ASTNode funcDec = findFirstNodeOfType(typeDesc, "abstract_function_declarator");
                        if (funcDec != null) {
                            processFunctionTemplateArg(typeDesc, funcDec, args);
                        } else {
                            // Regular type
                            ASTUtil.ASTNode typeId = findFirstNodeOfType(typeDesc, "type_identifier");
                            if (typeId != null) {
                                args.append(typeId.getText(sourceCode));
                            } else {
                                ASTUtil.ASTNode primType = findFirstNodeOfType(typeDesc, "primitive_type");
                                if (primType != null) {
                                    args.append(primType.getText(sourceCode));
                                }
                            }
                        }
                    }
                }
                typeStr.append(args).append(">");
            }
        } else {
            // Handle non-template type
            ASTUtil.ASTNode typeId = findFirstNodeOfType(qualifiedType, "type_identifier");
            if (typeId != null) {
                typeStr.append(typeId.getText(sourceCode));
            }
        }
    }

    private void processFunctionTemplateArg(ASTUtil.ASTNode typeDesc, ASTUtil.ASTNode funcDec, StringBuilder args) {
        // Get return type
        ASTUtil.ASTNode returnType = findFirstNodeOfType(typeDesc, "primitive_type");
        if (returnType != null) {
            args.append(returnType.getText(sourceCode));
        }

        // Process parameters
        ASTUtil.ASTNode paramList = findFirstNodeOfType(funcDec, "parameter_list");
        if (paramList != null) {
            args.append("(");
            boolean first = true;
            for (ASTUtil.ASTNode param : paramList.getChildren()) {
                if (!first) args.append(", ");
                first = false;

                // Handle const qualifier
                ASTUtil.ASTNode constQual = findFirstNodeOfType(param, "type_qualifier");
                if (constQual != null && constQual.getText(sourceCode).equals("const")) {
                    args.append("const ");
                }

                // Add type
                ASTUtil.ASTNode paramTypeId = findFirstNodeOfType(param, "type_identifier");
                if (paramTypeId != null) {
                    args.append(paramTypeId.getText(sourceCode));
                }

                // Handle reference
                ASTUtil.ASTNode refDec = findFirstNodeOfType(param, "abstract_reference_declarator");
                if (refDec != null) {
                    args.append("&");
                }
            }
            args.append(")");
        }
    }

    private void processStaticFieldInitialization(ASTUtil.ASTNode node, String fullText) {
        // Parse the declaration format: "type Class::field = value;"
        final int DECLARATION_PARTS = 2;
        String[] parts = fullText.split("::");
        if (parts.length != DECLARATION_PARTS) return;

        String className = parts[0].trim().split(" ")[1]; // Skip type, get class name
        String remainingPart = parts[1].trim(); // field = value;

        // Split into field name and value
        String[] fieldParts = remainingPart.split("=");
        if (fieldParts.length != DECLARATION_PARTS) return;

        String fieldName = fieldParts[0].trim();
        // Store the final value to use in lambda
        final String value = fieldParts[1].replace(";", "").trim();

        LOGGER.info("Found static field initialization:");
        LOGGER.info("Class: {}", className);
        LOGGER.info("Field: {}", fieldName);
        LOGGER.info("Value: {}", value);

        // Find the class and update the field
        UMLClass classNode = model.getClass(className).orElse(null);
        if (classNode != null) {
            classNode.getAttributes().stream()
                    .filter(attr -> attr.getName().equals(fieldName))
                    .findFirst()
                    .ifPresent(attr -> {
                        attr.setInitialValue(value);
                        LOGGER.info("Updated initial value for {} to {}", fieldName, value);
                    });
        }
    }

    @Override
    protected void processField(ASTUtil.ASTNode node) {
        if (currentClass == null) return;

        // Get type
        ASTUtil.ASTNode typeNode = findFirstNodeOfType(node, "primitive_type");
        if (typeNode == null) {
            typeNode = findFirstNodeOfType(node, "qualified_identifier");
        }

        // Get name
        ASTUtil.ASTNode nameNode = findFirstNodeOfType(node, "field_identifier");
        if (nameNode == null) {
            nameNode = findFirstNodeOfType(node, "identifier");
        }

        if (typeNode == null || nameNode == null) return;

        String fieldName = nameNode.getText(sourceCode);
        String fieldType = typeNode.getText(sourceCode);

        LOGGER.info("Processing field: {} {}", fieldType, fieldName);

        LocationInfo locationInfo = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.getStartPoint())
                .endPoint(node.getEndPoint())
                .type(CodeElementType.FIELD_DECLARATION)
                .build();

        UMLAttribute attribute = new UMLAttribute(fieldName, new UMLType(fieldType), locationInfo);

        // Set visibility from current context
        attribute.setVisibility(currentVisibility);

        // Check for static modifier
        ASTUtil.ASTNode storageNode = findFirstNodeOfType(node, "storage_class_specifier");
        if (storageNode != null && storageNode.getText(sourceCode).equals("static")) {
            LOGGER.info("Found static field: {}", fieldName);
            attribute.setStatic(true);
        }

        // Check for immediate initialization
        ASTUtil.ASTNode initNode = findFirstNodeOfType(node, "initializer");
        if (initNode != null) {
            String initialValue = initNode.getText(sourceCode);
            LOGGER.info("Found initial value: {}", initialValue);
            attribute.setInitialValue(initialValue);
        }

        currentClass.addAttribute(attribute);
        LOGGER.info("Added attribute {} to class {}", fieldName, currentClass.getName());
    }

    @Override
    protected void processModule(ASTUtil.ASTNode node) {
        // Extract module name from file path
        String moduleName = extractModuleName(filePath);
        model.addPackage(filePath, moduleName);
    }

    @Override
    protected void processImport(ASTUtil.ASTNode node) {

        ASTUtil.ASTNode libraryNode = findChildByType(node, "system_lib_string"); // type 1: system_lib_string
        String importedName = null;
        if (libraryNode != null) {
            importedName = libraryNode.getText(sourceCode).replaceAll("<|>", "");
        }
        else {
            ASTUtil.ASTNode localNode = findChildByType(node, "string_literal"); // type 2: string_literal
            if (localNode != null){
                importedName = getChildText(localNode, "string_content");
            } else {
                return;
            }
        }

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.getStartPoint(),
                node.getEndPoint(),
                CodeElementType.IMPORT_DECLARATION
        );

        UMLImport umlImport = UMLImport.builder(importedName, locationInfo)
                .type(UMLImport.ImportType.SINGLE)
                .build();

        model.addImport(filePath, umlImport);
    }

    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        LOGGER.info("Starting class processing");
        ASTUtil.ASTNode nameNode = findFirstNodeOfType(node, "type_identifier");
        if (nameNode == null) return;

        String className = nameNode.getText(sourceCode);
        LOGGER.info("Found class: {}", className);

        LocationInfo location = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.getStartPoint())
                .endPoint(node.getEndPoint())
                .type(CodeElementType.CLASS_DECLARATION)
                .build();

        currentClass = new UMLClass(extractModuleName(filePath), className, location);
        LOGGER.info("Created new UMLClass: {}", currentClass.getName());

        // Process inheritance
        processInheritance(node);

        // Process class body
        ASTUtil.ASTNode bodyNode = findFirstNodeOfType(node, "field_declaration_list");
        if (bodyNode != null) {
            currentVisibility = Visibility.PRIVATE; // C++ default
            LOGGER.info("Processing class body");
            processAccessSpecifiers(bodyNode);
        }

        LOGGER.info("Adding class to model with {} operations", currentClass.getOperations().size());
        model.addClass(currentClass);

        LOGGER.info("Model state after adding class:");
        LOGGER.info("Total classes: {}", model.getClasses().size());
        LOGGER.info("Total operations: {}", model.getOperations().size());
        if (!currentClass.getSuperclasses().isEmpty()) {
            LOGGER.info("Superclasses: {}", currentClass.getSuperclasses());
        }

        currentClass = null;  // Reset current class
    }

    private void processInheritance(ASTUtil.ASTNode node) {
        // Find base class clause
        ASTUtil.ASTNode baseClassClause = findFirstNodeOfType(node, "base_class_clause");
        if (baseClassClause == null) {
            LOGGER.info("No inheritance found");
            return;
        }

        LOGGER.info("Processing base class clause: {}", baseClassClause.getText(sourceCode));

        // Process each base class
        for (ASTUtil.ASTNode child : baseClassClause.getChildren()) {
            if (child.getType().equals("type_identifier")) {
                String baseClassName = child.getText(sourceCode);
                LOGGER.info("Found base class: {}", baseClassName);
                currentClass.addSuperclass(baseClassName);
            }
            // Handle access specifier if present (public/protected/private inheritance)
            if (child.getType().equals("access_specifier")) {
                LOGGER.info("Found inheritance access specifier: {}", child.getText(sourceCode));
            }
        }

        // Check if we found and added any superclasses
        if (!currentClass.getSuperclasses().isEmpty()) {
            LOGGER.info("Added superclasses: {}", currentClass.getSuperclasses());
        }
    }
    private String extractModuleName(String filePath) {
        return filePath.replaceAll("[/\\\\]", "::")
                .replaceAll("\\.cpp$|\\.h$|\\.hpp$", "");
    }
}
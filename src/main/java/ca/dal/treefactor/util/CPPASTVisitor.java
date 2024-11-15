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

public class CPPASTVisitor extends ASTVisitor {
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
        System.out.println("Visiting node type: " + node.type);

        if (node.type.equals("template_declaration")) {
            // Process the function definition inside the template
            ASTUtil.ASTNode funcDef = findFirstNodeOfType(node, "function_definition");
            if (funcDef != null) {
                processMethod(funcDef);
            }
        } else {
            switch (node.type) {
                case "translation_unit":
                    for (ASTUtil.ASTNode child : node.children) {
                        visit(child);
                    }
                    break;
                case "class_specifier":
                    System.out.println("Processing class_specifier: " + node.getText(sourceCode));
                    processClass(node);
                    break;
                case "function_definition":
                    if (currentClass == null) {
                        System.out.println("Processing standalone function: " + node.getText(sourceCode));
                        processMethod(node);
                    }
                    break;
                case "declaration":
                    if (currentClass == null) {
                        System.out.println("Processing declaration: " + node.getText(sourceCode));
                        processDeclaration(node);
                    }
                    break;
                case "field_declaration":
                    System.out.println("Processing field_declaration: " + node.getText(sourceCode));
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
        if (!defaultValueNode.children.isEmpty()) {
            ASTUtil.ASTNode valueNode = defaultValueNode.children.get(0);

            // Direct text extraction based on node type
            switch (valueNode.type) {
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
        boolean isOptional = paramNode.type.equals("optional_parameter_declaration");

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
                    .startPoint(paramNode.startPoint)
                    .endPoint(paramNode.endPoint)
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
        for (ASTUtil.ASTNode child : node.children) {
            System.out.println("Processing body node: " + child.type);

            if (child.type.equals("access_specifier")) {
                String specifier = child.getText(sourceCode);
                System.out.println("Found access specifier: " + specifier);
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

            if (child.type.equals("function_definition")) {
                System.out.println("Processing method within class");
                processMethod(child);
            }

            if (child.type.equals("field_declaration")) {
                System.out.println("Processing field within class");
                processField(child);
            }
        }
    }

    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        System.out.println("Starting method processing");

        // Get function declarator
        ASTUtil.ASTNode declaratorNode = findFirstNodeOfType(node, "function_declarator");
        if (declaratorNode == null) {
            System.out.println("No function declarator found");
            return;
        }

        // Get function name - handle both identifier and field_identifier
        ASTUtil.ASTNode nameNode = findFirstNodeOfType(declaratorNode, "identifier");
        if (nameNode == null) {
            nameNode = findFirstNodeOfType(declaratorNode, "field_identifier");
        }

        if (nameNode == null) {
            System.out.println("No name node found");
            return;
        }

        String methodName = nameNode.getText(sourceCode);
        System.out.println("Found method name: " + methodName);

        LocationInfo location = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.startPoint)
                .endPoint(node.endPoint)
                .type(CodeElementType.METHOD_DECLARATION)
                .build();

        UMLOperation operation = new UMLOperation(methodName, location);
        operation.setVisibility(currentVisibility); // Set visibility from current context

        // Process method details
        processMethodDetails(node, operation);

        // Add method to class or model
        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            if (methodName.equals(currentClass.getName())) {
                System.out.println("Found constructor: " + methodName);
                operation.setConstructor(true);
            }
            currentClass.addOperation(operation);
            System.out.println("Added method " + methodName + " to class " + currentClass.getName());
            System.out.println("Class now has " + currentClass.getOperations().size() + " operations");
        } else {
            System.out.println("Adding standalone method: " + methodName);
            model.addOperation(operation);
        }
    }


    private void processMethodModifiers(ASTUtil.ASTNode declaratorNode, UMLOperation.Builder builder) {
        // Check for const qualifier
        for (ASTUtil.ASTNode child : declaratorNode.children) {
            if (child.type.equals("type_qualifier") &&
                    getNodeText(child).equals("const")) {
                builder.setConst(true);
            }
        }

        // Check for noexcept
        ASTUtil.ASTNode noexceptNode = findFirstNodeOfType(declaratorNode, "noexcept");
        if (noexceptNode != null) {
            builder.setNoexcept(true);
        }

        // Check for virtual/static
        ASTUtil.ASTNode storageNode = findFirstNodeOfType(declaratorNode, "storage_class_specifier");
        if (storageNode != null) {
            String storage = getNodeText(storageNode);
            if (storage.equals("virtual")) {
                builder.setVirtual(true);
            } else if (storage.equals("static")) {
                builder.setStatic(true);
            }
        }
    }


    private void processMethodDetails(ASTUtil.ASTNode node, UMLOperation operation) {
        // Get return type
        ASTUtil.ASTNode typeNode = findFirstNodeOfType(node, "primitive_type");
        if (typeNode != null) {
            operation.setReturnType(new UMLType(getNodeText(typeNode)));
            System.out.println("Set return type: " + getNodeText(typeNode));
        }

        // Get function declarator
        ASTUtil.ASTNode declaratorNode = findFirstNodeOfType(node, "function_declarator");
        if (declaratorNode == null) return;

        // Process parameters
        ASTUtil.ASTNode paramListNode = findFirstNodeOfType(declaratorNode, "parameter_list");
        if (paramListNode != null) {
            for (ASTUtil.ASTNode paramNode : paramListNode.children) {
                if (paramNode.type.equals("parameter_declaration") ||
                        paramNode.type.equals("optional_parameter_declaration")) {
                    processParameter(paramNode, operation);
                }
            }
            System.out.println("Processed " + operation.getParameters().size() + " parameters");
        }

        // Process modifiers
        String declaratorText = declaratorNode.getText(sourceCode);
        System.out.println("Processing declarator: " + declaratorText);

        // Check for const modifier
        ASTUtil.ASTNode constNode = findFirstNodeOfType(declaratorNode, "type_qualifier");
        if (constNode != null && getNodeText(constNode).equals("const")) {
            operation.setConst(true);
            System.out.println("Set const modifier");
        }

        // Check for noexcept
        ASTUtil.ASTNode noexceptNode = findFirstNodeOfType(declaratorNode, "noexcept");
        if (noexceptNode != null) {
            operation.setNoexcept(true);
            System.out.println("Set noexcept modifier");
        }

        // Check for virtual
        if (declaratorText.contains("virtual")) {
            operation.setVirtual(true);
            System.out.println("Set virtual modifier");
        }

        // Check for static
        if (declaratorText.contains("static")) {
            operation.setStatic(true);
            System.out.println("Set static modifier");
        }

        // Check if it's a pure virtual method (abstract)
        if (declaratorText.contains("= 0")) {
            operation.setAbstract(true);
            System.out.println("Set abstract (pure virtual)");
        }
    }

    private void processDeclaration(ASTUtil.ASTNode node) {
        System.out.println("Processing declaration: " + node.getText(sourceCode));
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

        System.out.println("Processing method declaration: " + methodName);

        // Create operation
        LocationInfo location = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.startPoint)
                .endPoint(node.endPoint)
                .type(CodeElementType.METHOD_DECLARATION)
                .build();

        UMLOperation operation = new UMLOperation(methodName, location);
        operation.setReturnType(new UMLType(returnType));

        // Get full declaration text to check modifiers
        String fullText = node.getText(sourceCode);

        // Check virtual
        if (fullText.contains("virtual")) {
            System.out.println("Setting virtual modifier");
            operation.setVirtual(true);
        }

        // Check pure virtual (abstract)
        if (fullText.contains("= 0")) {
            System.out.println("Setting abstract modifier");
            operation.setAbstract(true);
        }

        // Process parameters
        ASTUtil.ASTNode paramListNode = findFirstNodeOfType(declaratorNode, "parameter_list");
        if (paramListNode != null) {
            for (ASTUtil.ASTNode paramNode : paramListNode.children) {
                if (paramNode.type.equals("parameter_declaration") ||
                        paramNode.type.equals("optional_parameter_declaration")) {
                    processParameter(paramNode, operation);
                }
            }
        }

        System.out.println("Adding method declaration to model: " + methodName);
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

                for (ASTUtil.ASTNode arg : argList.children) {
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
            for (ASTUtil.ASTNode param : paramList.children) {
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

    private void buildTemplateType(ASTUtil.ASTNode templateType, StringBuilder typeStr) {
        // Add base type name
        ASTUtil.ASTNode baseType = findFirstNodeOfType(templateType, "type_identifier");
        if (baseType != null) {
            typeStr.append(baseType.getText(sourceCode));
        }

        // Process template arguments
        ASTUtil.ASTNode argList = findFirstNodeOfType(templateType, "template_argument_list");
        if (argList != null) {
            typeStr.append("<");
            processTemplateArguments(argList, typeStr);
            typeStr.append(">");
        }
    }

    private void processTemplateArguments(ASTUtil.ASTNode argList, StringBuilder typeStr) {
        for (int i = 0; i < argList.children.size(); i++) {
            if (i > 0) typeStr.append(", ");
            ASTUtil.ASTNode arg = argList.children.get(i);
            ASTUtil.ASTNode typeDesc = findFirstNodeOfType(arg, "type_descriptor");
            if (typeDesc != null) {
                processTemplateArgument(typeDesc, typeStr);
            }
        }
    }

    private void processTemplateArgument(ASTUtil.ASTNode typeDesc, StringBuilder typeStr) {
        // Handle function type arguments
        ASTUtil.ASTNode funcDecl = findFirstNodeOfType(typeDesc, "abstract_function_declarator");
        if (funcDecl != null) {
            // Get return type
            ASTUtil.ASTNode returnType = findFirstNodeOfType(typeDesc, "primitive_type");
            if (returnType != null) {
                typeStr.append(returnType.getText(sourceCode));
            }

            // Get parameters
            ASTUtil.ASTNode paramList = findFirstNodeOfType(funcDecl, "parameter_list");
            if (paramList != null) {
                typeStr.append("(");
                for (int i = 0; i < paramList.children.size(); i++) {
                    if (i > 0) typeStr.append(", ");
                    processFunctionParameter(paramList.children.get(i), typeStr);
                }
                typeStr.append(")");
            }
        } else {
            // Handle regular type arguments
            ASTUtil.ASTNode typeId = findFirstNodeOfType(typeDesc, "type_identifier");
            if (typeId != null) {
                typeStr.append(typeId.getText(sourceCode));
            } else {
                ASTUtil.ASTNode primType = findFirstNodeOfType(typeDesc, "primitive_type");
                if (primType != null) {
                    typeStr.append(primType.getText(sourceCode));
                }
            }
        }
    }

    private void processFunctionParameter(ASTUtil.ASTNode param, StringBuilder typeStr) {
        // Handle const qualifier
        ASTUtil.ASTNode constQual = findFirstNodeOfType(param, "type_qualifier");
        if (constQual != null && constQual.getText(sourceCode).equals("const")) {
            typeStr.append("const ");
        }

        // Handle type
        ASTUtil.ASTNode typeId = findFirstNodeOfType(param, "type_identifier");
        if (typeId != null) {
            typeStr.append(typeId.getText(sourceCode));
        }

        // Handle reference
        ASTUtil.ASTNode refDec = findFirstNodeOfType(param, "abstract_reference_declarator");
        if (refDec != null) {
            typeStr.append("&");
        }
    }

    private UMLParameter buildParameter(ASTUtil.ASTNode paramNode) {
        StringBuilder typeStr = new StringBuilder();
        String paramName = null;
        boolean isPointer = false;
        boolean isReference = false;
        boolean isRValueReference = false;
        boolean isConst = false;

        // Check for const qualifier
        ASTUtil.ASTNode typeQualifier = findFirstNodeOfType(paramNode, "type_qualifier");
        if (typeQualifier != null && typeQualifier.getText(sourceCode).equals("const")) {
            isConst = true;
        }

        // Get qualified type info
        ASTUtil.ASTNode qualifiedType = findFirstNodeOfType(paramNode, "qualified_identifier");
        if (qualifiedType != null) {
            processQualifiedType(qualifiedType, typeStr);
        } else {
            ASTUtil.ASTNode primitiveType = findFirstNodeOfType(paramNode, "primitive_type");
            if (primitiveType != null) {
                typeStr.append(primitiveType.getText(sourceCode));
            }
        }

        // Check for pointer/reference declarators
        ASTUtil.ASTNode pointerDec = findFirstNodeOfType(paramNode, "pointer_declarator");
        if (pointerDec != null) {
            isPointer = true;
            ASTUtil.ASTNode idNode = findFirstNodeOfType(pointerDec, "identifier");
            if (idNode != null) {
                paramName = idNode.getText(sourceCode);
            }
        }

        ASTUtil.ASTNode refDec = findFirstNodeOfType(paramNode, "reference_declarator");
        if (refDec != null) {
            String refText = refDec.getText(sourceCode);
            if (refText.contains("&&")) {
                isRValueReference = true;
            } else {
                isReference = true;
            }
            ASTUtil.ASTNode idNode = findFirstNodeOfType(refDec, "identifier");
            if (idNode != null) {
                paramName = idNode.getText(sourceCode);
            }
        }

        // Get regular identifier if not found in declarators
        if (paramName == null) {
            ASTUtil.ASTNode idNode = findFirstNodeOfType(paramNode, "identifier");
            if (idNode != null) {
                paramName = idNode.getText(sourceCode);
            }
        }

        LocationInfo location = buildLocation(paramNode);
        UMLParameter parameter = new UMLParameter(paramName, new UMLType(typeStr.toString()), location);

        parameter.setPointer(isPointer);
        parameter.setReference(isReference);
        parameter.setRValueReference(isRValueReference);
        parameter.setConst(isConst);

        return parameter;
    }

    private void processTemplateType(ASTUtil.ASTNode templateType, StringBuilder typeStr) {
        // Add base type name
        ASTUtil.ASTNode baseType = findFirstNodeOfType(templateType, "type_identifier");
        if (baseType != null) {
            typeStr.append(baseType.getText(sourceCode));
        }

        // Process template arguments
        ASTUtil.ASTNode argList = findFirstNodeOfType(templateType, "template_argument_list");
        if (argList != null) {
            typeStr.append("<");
            processFunctionTemplateArgs(argList, typeStr);
            typeStr.append(">");
        }
    }

    private void processFunctionTemplateArgs(ASTUtil.ASTNode argList, StringBuilder typeStr) {
        for (int i = 0; i < argList.children.size(); i++) {
            if (i > 0) typeStr.append(", ");
            ASTUtil.ASTNode arg = argList.children.get(i);
            ASTUtil.ASTNode typeDesc = findFirstNodeOfType(arg, "type_descriptor");
            if (typeDesc != null) {
                processFunctionArgType(typeDesc, typeStr);
            }
        }
    }

    private void processFunctionArgType(ASTUtil.ASTNode typeDesc, StringBuilder typeStr) {
        // Check for function type
        ASTUtil.ASTNode funcDec = findFirstNodeOfType(typeDesc, "abstract_function_declarator");
        if (funcDec != null) {
            processAbstractFunctionType(typeDesc, funcDec, typeStr);
        } else {
            // Handle regular type
            processRegularType(typeDesc, typeStr);
        }
    }

    private void processAbstractFunctionType(ASTUtil.ASTNode typeDesc, ASTUtil.ASTNode funcDec, StringBuilder typeStr) {
        // Get return type
        ASTUtil.ASTNode primType = findFirstNodeOfType(typeDesc, "primitive_type");
        if (primType != null) {
            typeStr.append(primType.getText(sourceCode));
        }

        // Process parameters
        ASTUtil.ASTNode paramList = findFirstNodeOfType(funcDec, "parameter_list");
        if (paramList != null) {
            typeStr.append("(");
            for (int i = 0; i < paramList.children.size(); i++) {
                if (i > 0) typeStr.append(", ");
                ASTUtil.ASTNode param = paramList.children.get(i);
                processParameter(param, typeStr);
            }
            typeStr.append(")");
        }
    }

    private void processParameter(ASTUtil.ASTNode param, StringBuilder typeStr) {
        // Handle const qualifier
        ASTUtil.ASTNode constQual = findFirstNodeOfType(param, "type_qualifier");
        if (constQual != null && constQual.getText(sourceCode).equals("const")) {
            typeStr.append("const ");
        }

        // Handle type
        ASTUtil.ASTNode typeId = findFirstNodeOfType(param, "type_identifier");
        if (typeId != null) {
            typeStr.append(typeId.getText(sourceCode));
        }

        // Handle reference
        ASTUtil.ASTNode refDec = findFirstNodeOfType(param, "abstract_reference_declarator");
        if (refDec != null) {
            typeStr.append("&");
        }
    }

    private void processRegularType(ASTUtil.ASTNode typeDesc, StringBuilder typeStr) {
        ASTUtil.ASTNode primType = findFirstNodeOfType(typeDesc, "primitive_type");
        if (primType != null) {
            typeStr.append(primType.getText(sourceCode));
        } else {
            ASTUtil.ASTNode typeId = findFirstNodeOfType(typeDesc, "type_identifier");
            if (typeId != null) {
                typeStr.append(typeId.getText(sourceCode));
            }
        }
    }

    private LocationInfo buildLocation(ASTUtil.ASTNode node) {
        return LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.startPoint)
                .endPoint(node.endPoint)
                .type(CodeElementType.PARAMETER_DECLARATION)
                .build();
    }

    private String buildFunctionType(ASTUtil.ASTNode typeDesc) {
        StringBuilder funcType = new StringBuilder("void");
        ASTUtil.ASTNode paramList = findFirstNodeOfType(typeDesc, "parameter_list");
        if (paramList != null) {
            funcType.append("(");
            for (int i = 0; i < paramList.children.size(); i++) {
                if (i > 0) funcType.append(", ");
                ASTUtil.ASTNode param = paramList.children.get(i);

                // Handle const qualifier
                ASTUtil.ASTNode constQual = findFirstNodeOfType(param, "type_qualifier");
                if (constQual != null && constQual.getText(sourceCode).equals("const")) {
                    funcType.append("const ");
                }

                // Handle type
                ASTUtil.ASTNode paramType = findFirstNodeOfType(param, "type_identifier");
                if (paramType != null) {
                    funcType.append(paramType.getText(sourceCode));
                }

                // Handle reference
                ASTUtil.ASTNode refDec = findFirstNodeOfType(param, "abstract_reference_declarator");
                if (refDec != null) {
                    funcType.append("&");
                }
            }
            funcType.append(")");
        }
        return funcType.toString();
    }

    private String extractDefaultValue(ASTUtil.ASTNode defaultValueNode) {
        if (defaultValueNode == null || defaultValueNode.children.isEmpty()) {
            return null;
        }

        ASTUtil.ASTNode valueNode = defaultValueNode.children.get(0);
        if (valueNode.type.equals("string_literal")) {
            return valueNode.getText(sourceCode);
        } else if (valueNode.type.equals("null")) {
            return "nullptr";
        }
        return valueNode.getText(sourceCode);
    }

    private void processStaticFieldInitialization(ASTUtil.ASTNode node, String fullText) {
        // Parse the declaration format: "type Class::field = value;"
        String[] parts = fullText.split("::");
        if (parts.length != 2) return;

        String className = parts[0].trim().split(" ")[1]; // Skip type, get class name
        String remainingPart = parts[1].trim(); // field = value;

        // Split into field name and value
        String[] fieldParts = remainingPart.split("=");
        if (fieldParts.length != 2) return;

        String fieldName = fieldParts[0].trim();
        // Store the final value to use in lambda
        final String value = fieldParts[1].replace(";", "").trim();

        System.out.println("Found static field initialization:");
        System.out.println("Class: " + className);
        System.out.println("Field: " + fieldName);
        System.out.println("Value: " + value);

        // Find the class and update the field
        UMLClass classNode = model.getClass(className).orElse(null);
        if (classNode != null) {
            classNode.getAttributes().stream()
                    .filter(attr -> attr.getName().equals(fieldName))
                    .findFirst()
                    .ifPresent(attr -> {
                        attr.setInitialValue(value);
                        System.out.println("Updated initial value for " + fieldName + " to " + value);
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

        System.out.println("Processing field: " + fieldType + " " + fieldName);

        LocationInfo locationInfo = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.startPoint)
                .endPoint(node.endPoint)
                .type(CodeElementType.FIELD_DECLARATION)
                .build();

        UMLAttribute attribute = new UMLAttribute(fieldName, new UMLType(fieldType), locationInfo);

        // Set visibility from current context
        attribute.setVisibility(currentVisibility);

        // Check for static modifier
        ASTUtil.ASTNode storageNode = findFirstNodeOfType(node, "storage_class_specifier");
        if (storageNode != null && storageNode.getText(sourceCode).equals("static")) {
            System.out.println("Found static field: " + fieldName);
            attribute.setStatic(true);
        }

        // Check for immediate initialization
        ASTUtil.ASTNode initNode = findFirstNodeOfType(node, "initializer");
        if (initNode != null) {
            String initialValue = initNode.getText(sourceCode);
            System.out.println("Found initial value: " + initialValue);
            attribute.setInitialValue(initialValue);
        }

        currentClass.addAttribute(attribute);
        System.out.println("Added attribute " + fieldName + " to class " + currentClass.getName());
    }

    private void processFieldInitializer(ASTUtil.ASTNode node, UMLAttribute attribute) {
        // Try to find direct initializer
        ASTUtil.ASTNode initNode = findFirstNodeOfType(node, "initializer");
        if (initNode != null) {
            attribute.setInitialValue(getNodeText(initNode));
            return;
        }

        // Try to find number_literal for basic initialization
        ASTUtil.ASTNode numberNode = findFirstNodeOfType(node, "number_literal");
        if (numberNode != null) {
            attribute.setInitialValue(getNodeText(numberNode));
        }
    }

    private void processExternalFieldDeclaration(ASTUtil.ASTNode node) {
        String fullText = getNodeText(node);
        System.out.println("Full declaration text: " + fullText);

        ASTUtil.ASTNode initNode = findFirstNodeOfType(node, "initializer");
        if (initNode != null) {
            String initValue = getNodeText(initNode);
            // Find the corresponding class field and update its initial value
            String[] parts = fullText.split("::");
            if (parts.length == 2) {
                String className = parts[0].trim();
                UMLClass classNode = model.getClassByName(className);
                if (classNode != null) {
                    String fieldName = parts[1].split("=")[0].trim();
                    classNode.getAttributes().stream()
                            .filter(attr -> attr.getName().equals(fieldName))
                            .findFirst()
                            .ifPresent(attr -> attr.setInitialValue(initValue.trim()));
                }
            }
        }
    }


    @Override
    protected void processModule(ASTUtil.ASTNode node) {
        // Extract module name from file path
        String moduleName = extractModuleName(filePath);
        model.addPackage(filePath, moduleName);
    }

    private void processNamespace(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode nameNode = findChildByType(node, "identifier");
        if (nameNode == null) return;

        String namespaceName = nameNode.getText(sourceCode);
        String previousNamespace = currentNamespace;

        // Update current namespace
        currentNamespace = currentNamespace.isEmpty() ?
                namespaceName : currentNamespace + "::" + namespaceName;

        // Process namespace body
        ASTUtil.ASTNode body = findChildByType(node, "compound_statement");
        if (body != null) {
            for (ASTUtil.ASTNode child : body.children) {
                visit(child);
            }
        }

        // Restore previous namespace
        currentNamespace = previousNamespace;
    }






    private void processReturnType(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        // Try primitive_type first, then type_identifier
        String returnType = getChildText(node, "primitive_type");
        if (returnType == null) {
            returnType = getChildText(node, "type_identifier");
        }

        builder.returnType(new UMLType(returnType != null ? returnType : "void"));
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
                node.startPoint,
                node.endPoint,
                CodeElementType.IMPORT_DECLARATION
        );

        UMLImport umlImport = UMLImport.builder(importedName, locationInfo)
                .type(UMLImport.ImportType.SINGLE)
                .build();

        model.addImport(filePath, umlImport);
    }

    private void processFieldModifiers(ASTUtil.ASTNode node, UMLAttribute attribute) {
        List<ASTUtil.ASTNode> modifiers = findChildrenByType(node, "modifier");
        for (ASTUtil.ASTNode modifier : modifiers) {
            String mod = modifier.getText(sourceCode);
            if (mod.equals("static")) {
                attribute.setStatic(true);
            } else if (mod.equals("const")) {
                attribute.setFinal(true);
            }
        }
    }

    private void processParameters(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        ASTUtil.ASTNode paramList = findChildByType(node, "parameter_list");
        if (paramList == null) return;

        for (ASTUtil.ASTNode param : paramList.children) {
            if (!param.type.equals("parameter_declaration") &&
                    !param.type.equals("optional_parameter_declaration")) {
                continue;
            }

            // Build full type including templates
            String paramType = buildFullType(param);
            System.out.println("Built parameter type: " + paramType);

            // Get parameter name
            String paramName = getParameterName(param);
            if (paramName == null) continue;

            LocationInfo paramLocation = new LocationInfo(
                    filePath,
                    param.startPoint,
                    param.endPoint,
                    CodeElementType.PARAMETER_DECLARATION
            );

            UMLParameter parameter = new UMLParameter(paramName, new UMLType(paramType), paramLocation);

            // Handle optional parameters
            if (param.type.equals("optional_parameter_declaration")) {
                ASTUtil.ASTNode defaultValueNode = findChildByType(param, "default_value");
                String defaultValue = getDefaultValue(defaultValueNode);
                if (defaultValue != null) {
                    parameter.setDefaultValue(defaultValue);
                    System.out.println("Set default value: " + defaultValue);
                }
            }

            // Handle references
            ASTUtil.ASTNode refDeclarator = findChildByType(param, "reference_declarator");
            if (refDeclarator != null) {
                String refText = refDeclarator.getText(sourceCode);
                if (refText.contains("&&")) {
                    parameter.setRValueReference(true);
                } else {
                    parameter.setReference(true);
                }
            }

            // Handle const
            ASTUtil.ASTNode constQual = findChildByType(param, "type_qualifier");
            if (constQual != null && constQual.getText(sourceCode).equals("const")) {
                parameter.setConst(true);
            }

            builder.addParameter(parameter);
        }
    }

    private String buildFullType(ASTUtil.ASTNode param) {
        ASTUtil.ASTNode qualifiedType = findChildByType(param, "qualified_identifier");
        if (qualifiedType == null) {
            // Try primitive type
            ASTUtil.ASTNode primitiveType = findChildByType(param, "primitive_type");
            return primitiveType != null ? primitiveType.getText(sourceCode) : "void";
        }

        StringBuilder type = new StringBuilder();

        // Handle namespace (std::)
        ASTUtil.ASTNode namespaceNode = findChildByType(qualifiedType, "namespace_identifier");
        if (namespaceNode != null) {
            type.append(namespaceNode.getText(sourceCode)).append("::");
        }

        // Handle template types
        ASTUtil.ASTNode templateType = findChildByType(qualifiedType, "template_type");
        if (templateType != null) {
            // Add base template name
            ASTUtil.ASTNode baseType = findChildByType(templateType, "type_identifier");
            if (baseType != null) {
                type.append(baseType.getText(sourceCode));
            }

            // Add template arguments
            ASTUtil.ASTNode argList = findChildByType(templateType, "template_argument_list");
            if (argList != null) {
                type.append("<");

                for (int i = 0; i < argList.children.size(); i++) {
                    ASTUtil.ASTNode arg = argList.children.get(i);
                    if (i > 0) type.append(", ");

                    // Get the actual type from type_descriptor
                    ASTUtil.ASTNode typeDesc = findChildByType(arg, "type_descriptor");
                    if (typeDesc != null) {
                        // Try primitive type first
                        ASTUtil.ASTNode argType = findChildByType(typeDesc, "primitive_type");
                        if (argType != null) {
                            type.append(argType.getText(sourceCode));
                        }
                        // Then try type identifier
                        else {
                            argType = findChildByType(typeDesc, "type_identifier");
                            if (argType != null) {
                                type.append(argType.getText(sourceCode));
                            }
                        }
                    }
                }

                type.append(">");
            }
        } else {
            // Regular type
            ASTUtil.ASTNode typeId = findChildByType(qualifiedType, "type_identifier");
            if (typeId != null) {
                type.append(typeId.getText(sourceCode));
            }
        }

        return type.toString();
    }

    private String getTemplateArgType(ASTUtil.ASTNode arg) {
        if (!arg.type.equals("type_descriptor")) return null;

        // Try primitive type first
        ASTUtil.ASTNode typeNode = findChildByType(arg, "primitive_type");
        if (typeNode != null) return typeNode.getText(sourceCode);

        // Try type identifier
        typeNode = findChildByType(arg, "type_identifier");
        if (typeNode != null) return typeNode.getText(sourceCode);

        return null;
    }

    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        System.out.println("Starting class processing");
        ASTUtil.ASTNode nameNode = findFirstNodeOfType(node, "type_identifier");
        if (nameNode == null) return;

        String className = nameNode.getText(sourceCode);
        System.out.println("Found class: " + className);

        LocationInfo location = LocationInfo.builder()
                .filePath(filePath)
                .startPoint(node.startPoint)
                .endPoint(node.endPoint)
                .type(CodeElementType.CLASS_DECLARATION)
                .build();

        currentClass = new UMLClass(extractModuleName(filePath), className, location);
        System.out.println("Created new UMLClass: " + currentClass.getName());

        // Process inheritance
        processInheritance(node);

        // Process class body
        ASTUtil.ASTNode bodyNode = findFirstNodeOfType(node, "field_declaration_list");
        if (bodyNode != null) {
            currentVisibility = Visibility.PRIVATE; // C++ default
            System.out.println("Processing class body");
            processAccessSpecifiers(bodyNode);
        }

        System.out.println("Adding class to model with " + currentClass.getOperations().size() + " operations");
        model.addClass(currentClass);

        System.out.println("Model state after adding class:");
        System.out.println("Total classes: " + model.getClasses().size());
        System.out.println("Total operations: " + model.getOperations().size());
        if (!currentClass.getSuperclasses().isEmpty()) {
            System.out.println("Superclasses: " + currentClass.getSuperclasses());
        }

        currentClass = null;  // Reset current class
    }

    private void processInheritance(ASTUtil.ASTNode node) {
        // Find base class clause
        ASTUtil.ASTNode baseClassClause = findFirstNodeOfType(node, "base_class_clause");
        if (baseClassClause == null) {
            System.out.println("No inheritance found");
            return;
        }

        System.out.println("Processing base class clause: " + baseClassClause.getText(sourceCode));

        // Process each base class
        for (ASTUtil.ASTNode child : baseClassClause.children) {
            if (child.type.equals("type_identifier")) {
                String baseClassName = child.getText(sourceCode);
                System.out.println("Found base class: " + baseClassName);
                currentClass.addSuperclass(baseClassName);
            }
            // Handle access specifier if present (public/protected/private inheritance)
            if (child.type.equals("access_specifier")) {
                System.out.println("Found inheritance access specifier: " + child.getText(sourceCode));
            }
        }

        // Check if we found and added any superclasses
        if (!currentClass.getSuperclasses().isEmpty()) {
            System.out.println("Added superclasses: " + currentClass.getSuperclasses());
        }
    }
    private String extractModuleName(String filePath) {
        return filePath.replaceAll("[/\\\\]", "::")
                .replaceAll("\\.cpp$|\\.h$|\\.hpp$", "");
    }
}
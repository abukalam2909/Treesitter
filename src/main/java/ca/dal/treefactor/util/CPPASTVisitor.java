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

    public CPPASTVisitor(UMLModel model, String sourceCode, String filePath) {
        super(model, sourceCode, filePath);
        this.currentClass = null;
        this.currentScope = new ArrayList<>();
        this.currentNamespace = "";
    }

    @Override
    public void visit(ASTUtil.ASTNode node) {
        switch (node.type) {
            case "translation_unit":
                processModule(node);
                break;
            case "class_specifier":
                processClass(node);
                break;
            case "function_definition":
                processMethod(node);
                return;
            case "declaration":
                processDeclaration(node);
                return;
            case "field_declaration":
                processField(node);
                break;
            case "preproc_include":
                processImport(node);
                break;
        }

        // Visit children
        for (ASTUtil.ASTNode child : node.children) {
            visit(child);
        }
    }

    private void processDeclaration(ASTUtil.ASTNode node) {
        // Get the full text of the declaration to check for virtual keyword
        String fullText = node.getText(sourceCode);
        System.out.println("Full declaration text: " + fullText);

        // Handle field initialization if it contains :: but not function parentheses
        if (fullText.contains("::") && !fullText.contains("(")) {
            ASTUtil.ASTNode initDeclarator = findChildByType(node, "init_declarator");
            if (initDeclarator == null) return;

            ASTUtil.ASTNode qualifiedId = findChildByType(initDeclarator, "qualified_identifier");
            ASTUtil.ASTNode numberLiteral = findChildByType(initDeclarator, "number_literal");

            if (qualifiedId != null && numberLiteral != null) {
                ASTUtil.ASTNode scopeNode = findChildByType(qualifiedId, "namespace_identifier");
                ASTUtil.ASTNode nameNode = findChildByType(qualifiedId, "identifier");

                if (scopeNode != null && nameNode != null) {
                    String className = scopeNode.getText(sourceCode);
                    String fieldName = nameNode.getText(sourceCode);
                    String value = numberLiteral.getText(sourceCode);

                    System.out.println(String.format("Found initialization: class=%s, field=%s, value=%s",
                            className, fieldName, value));

                    // Find class and update attribute
                    UMLClass targetClass = model.getClasses().stream()
                            .filter(c -> c.getName().equals(className))
                            .findFirst()
                            .orElse(null);

                    if (targetClass != null) {
                        Optional<UMLAttribute> attr = targetClass.getAttribute(fieldName);
                        if (attr.isPresent()) {
                            attr.get().setInitialValue(value);
                            System.out.println("Updated initial value for " + fieldName + " to " + value);
                        }
                    }
                }
            }
            return;
        }

        boolean isVirtual = fullText.trim().startsWith("virtual");

        ASTUtil.ASTNode initDeclarator = findChildByType(node, "init_declarator");
        if (initDeclarator == null) return;

        ASTUtil.ASTNode functionDeclarator = findChildByType(initDeclarator, "function_declarator");
        if (functionDeclarator == null) return;

        // Get method name
        ASTUtil.ASTNode identifierNode = findChildByType(functionDeclarator, "identifier");
        if (identifierNode == null) return;

        String methodName = identifierNode.getText(sourceCode);
        System.out.println("Found method declaration: " + methodName);
        System.out.println("Is virtual: " + isVirtual);

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.METHOD_DECLARATION
        );

        UMLOperation.Builder builder = UMLOperation.builder(methodName, locationInfo);

        // Set virtual flag
        builder.setVirtual(isVirtual);

        // Process return type
        ASTUtil.ASTNode returnTypeNode = findChildByType(node, "primitive_type");
        if (returnTypeNode != null) {
            builder.returnType(new UMLType(returnTypeNode.getText(sourceCode)));
        } else {
            builder.returnType(new UMLType("void")); // Default
        }

        // Check for pure virtual method (= 0)
        ASTUtil.ASTNode numberLiteralNode = findChildByType(initDeclarator, "number_literal");
        boolean isAbstract = false;
        if (numberLiteralNode != null) {
            String value = numberLiteralNode.getText(sourceCode);
            System.out.println("Found number literal: " + value);
            isAbstract = value.equals("0");
        }

        System.out.println("Setting abstract to: " + isAbstract);
        builder.setAbstract(isAbstract);

        // Process parameters
        processParameters(node, builder);

        // Build and add operation
        UMLOperation operation = builder.build();
        System.out.println("Built operation: " + operation.getName());
        System.out.println("Virtual flag: " + operation.isVirtual());
        System.out.println("Abstract flag: " + operation.isAbstract());

        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            currentClass.addOperation(operation);
        } else {
            model.addOperation(operation);
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

    @Override
    protected void processClass(ASTUtil.ASTNode node) {
        System.out.println("Processing class: Start");
        ASTUtil.ASTNode nameNode = findChildByType(node, "type_identifier");
        if (nameNode == null) return;

        String className = nameNode.getText(sourceCode);
        System.out.println("Found class: " + className);

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.CLASS_DECLARATION
        );

        UMLClass previousClass = currentClass;
        String fullClassName = currentNamespace.isEmpty() ?
                className : currentNamespace + "::" + className;
        currentClass = new UMLClass(extractModuleName(filePath), fullClassName, locationInfo);
        System.out.println("Created UMLClass for: " + fullClassName);

        // Process inheritance
        processInheritance(node, currentClass);

        // Process class body
        currentScope.add(className);
        ASTUtil.ASTNode body = findChildByType(node, "field_declaration_list");
        if (body != null) {
            System.out.println("Processing class body for: " + className);
            for (ASTUtil.ASTNode child : body.children) {
                System.out.println("Processing child node type: " + child.type);
            }
            processAccessSpecifiers(body);
        } else {
            System.out.println("No body found for class: " + className);
        }
        currentScope.remove(currentScope.size() - 1);

        // Finalize class processing
        System.out.println("Adding class to model: " + fullClassName);
        model.addClass(currentClass);
        System.out.println("Operations count: " + currentClass.getOperations().size());
        currentClass = previousClass;
        System.out.println("Processing class: End");
    }

    private void processAccessSpecifiers(ASTUtil.ASTNode node) {
        Visibility currentVisibility = Visibility.PRIVATE;

        for (ASTUtil.ASTNode child : node.children) {
            System.out.println("Processing node type in access specifiers: " + child.type);

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
            } else if (child.type.equals("field_declaration")) {
                processField(child, currentVisibility);
            } else if (child.type.equals("function_definition")) {
                System.out.println("Processing function definition in access specifiers");
                processMethod(child);
                if (currentClass != null && !currentClass.getOperations().isEmpty()) {
                    UMLOperation lastOperation = currentClass.getOperations()
                            .get(currentClass.getOperations().size() - 1);
                    lastOperation.setVisibility(currentVisibility);
                }
            } else {
                visit(child);
            }
        }
    }

    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        System.out.println("process Method with node: " + getNodeText(node));

        ASTUtil.ASTNode functionDeclarator = findChildByType(node, "function_declarator");
        if (functionDeclarator == null) {
            System.out.println("No function declarator found");
            return;
        }

        // Try different paths to find the method name
        String methodName = null;

        // Try constructor (direct identifier)
        methodName = getChildText(functionDeclarator, "identifier");
        if (methodName != null) {
            System.out.println("Found constructor name: " + methodName);
        } else {
            // Try regular method through declarator/field_identifier path
            ASTUtil.ASTNode declarator = findChildByType(functionDeclarator, "declarator");
            if (declarator != null) {
                methodName = getChildText(declarator, "field_identifier");
                System.out.println("Found method name through field_identifier: " + methodName);
            }

            // If still not found, check directly in function_declarator
            if (methodName == null) {
                methodName = getChildText(functionDeclarator, "field_identifier");
                if (methodName != null) {
                    System.out.println("Found method name directly in function_declarator: " + methodName);
                }
            }
        }

        if (methodName == null) {
            System.out.println("Could not find method name in: " + getNodeText(functionDeclarator));
            return;
        }

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.METHOD_DECLARATION
        );

        UMLOperation.Builder builder = UMLOperation.builder(methodName, locationInfo);

        // Process method modifiers
        processMethodModifiers(functionDeclarator, builder);

        // Process parameters
        processParameters(node, builder);

        // Process return type
        processReturnType(node, builder);

        // Process method body
        if (hasType(findChildByType(node, "compound_statement"), "compound_statement")) {
            builder.body(getChildText(node, "compound_statement"));
        }

        // Build and add operation
        UMLOperation operation = builder.build();
        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            System.out.println("Adding method " + methodName + " to class " + currentClass.getName());
            currentClass.addOperation(operation);
            System.out.println("Current operations in class: " + currentClass.getOperations().size());
        } else {
            System.out.println("Adding standalone method " + methodName);
            model.addOperation(operation);
        }
    }
    private void processMethodModifiers(ASTUtil.ASTNode functionDeclarator, UMLOperation.Builder builder) {
        // Get all type qualifiers
        List<ASTUtil.ASTNode> typeQualifiers = findChildrenByType(functionDeclarator, "type_qualifier");
        for (ASTUtil.ASTNode qualifier : typeQualifiers) {
            String qualifierText = getNodeText(qualifier);
            System.out.println("Found qualifier: " + qualifierText);
            if ("const".equals(qualifierText)) {
                builder.setConst(true);
            }
        }

        // Check for noexcept
        if (hasType(findChildByType(functionDeclarator, "noexcept"), "noexcept")) {
            builder.setNoexcept(true);
        }

        // Check for inline
        if (hasType(findChildByType(functionDeclarator, "inline"), "inline")) {
            builder.setInline(true);
        }
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
    protected void processField(ASTUtil.ASTNode node) {
        processField(node, Visibility.DEFAULT);
    }

    protected void processField(ASTUtil.ASTNode node, Visibility visibility) {
        System.out.println("Processing field node: " + node.type);

        if (currentClass == null) {
            processFieldInitialization(node);
            return;
        }

        // Get storage class (static)
        ASTUtil.ASTNode storageClassNode = findChildByType(node, "storage_class_specifier");
        boolean isStatic = storageClassNode != null &&
                storageClassNode.getText(sourceCode).equals("static");

        // Get type
        ASTUtil.ASTNode typeNode = findChildByType(node, "primitive_type");
        if (typeNode == null) {
            typeNode = findChildByType(node, "type_identifier");
        }

        // Get field identifier
        ASTUtil.ASTNode fieldIdNode = findChildByType(node, "field_identifier");

        if (fieldIdNode == null) return;

        String fieldName = fieldIdNode.getText(sourceCode);
        String fieldType = typeNode != null ? typeNode.getText(sourceCode) : "int";

        System.out.println(String.format("Creating field: %s, type: %s, static: %b",
                fieldName, fieldType, isStatic));

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.FIELD_DECLARATION
        );

        UMLAttribute attribute = new UMLAttribute(
                fieldName,
                new UMLType(fieldType),
                locationInfo
        );

        // Set static modifier
        attribute.setStatic(isStatic);
        // Set visibility
        attribute.setVisibility(visibility);

        currentClass.addAttribute(attribute);
        System.out.println(String.format("Added attribute %s to class %s with visibility %s",
                fieldName, currentClass.getName(), visibility));
    }

    private void processFieldInitialization(ASTUtil.ASTNode node) {
        String fullText = node.getText(sourceCode);
        System.out.println("Processing field initialization: " + fullText);

        // Get the init_declarator
        ASTUtil.ASTNode initDeclarator = findChildByType(node, "init_declarator");
        if (initDeclarator == null) {
            System.out.println("No init_declarator found");
            return;
        }

        // Get the qualified identifier (Counter::count)
        ASTUtil.ASTNode qualifiedId = findChildByType(initDeclarator, "qualified_identifier");
        if (qualifiedId == null) {
            System.out.println("No qualified_identifier found");
            return;
        }

        String[] parts = qualifiedId.getText(sourceCode).split("::");
        if (parts.length != 2) {
            System.out.println("Invalid qualified identifier format");
            return;
        }

        String className = parts[0];
        String fieldName = parts[1];

        System.out.println("Looking for class: " + className + ", field: " + fieldName);

        // Find the target class
        UMLClass targetClass = model.getClasses().stream()
                .filter(c -> c.getName().equals(className))
                .findFirst()
                .orElse(null);

        if (targetClass == null) {
            System.out.println("Target class not found: " + className);
            return;
        }

        // Find the attribute
        Optional<UMLAttribute> existingAttr = targetClass.getAttribute(fieldName);
        if (!existingAttr.isPresent()) {
            System.out.println("Attribute not found: " + fieldName);
            return;
        }

        // Get the value from the fullText since we know the format is "type Class::field = value;"
        if (fullText.contains("=")) {
            String value = fullText.substring(fullText.indexOf("=") + 1).trim();
            // Remove semicolon if present
            value = value.replace(";", "").trim();
            existingAttr.get().setInitialValue(value);
            System.out.println("Set initial value for " + fieldName + " to: " + value);
        }
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

    private void processParameters(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        ASTUtil.ASTNode paramList = findChildByType(node, "parameter_list");
        if (paramList == null) return;

        for (ASTUtil.ASTNode param : paramList.children) {
            if (!param.type.equals("parameter_declaration")) continue;

            ASTUtil.ASTNode typeNode = findChildByType(param, "type_identifier");
            ASTUtil.ASTNode nameNode = findChildByType(param, "identifier");

            if (typeNode == null || nameNode == null) continue;

            String paramName = nameNode.getText(sourceCode);
            String paramType = typeNode.getText(sourceCode);

            LocationInfo paramLocation = new LocationInfo(
                    filePath,
                    param.startPoint,
                    param.endPoint,
                    CodeElementType.PARAMETER_DECLARATION
            );

            UMLParameter parameter = new UMLParameter(
                    paramName,
                    new UMLType(paramType),
                    paramLocation
            );

            builder.addParameter(parameter);
        }
    }

    private void processInheritance(ASTUtil.ASTNode node, UMLClass umlClass) {
        ASTUtil.ASTNode baseList = findChildByType(node, "base_class_clause");
        if (baseList == null) return;

        // Debug print
        System.out.println("Found base class clause: " + baseList.getText(sourceCode));

        // Look for type_identifier directly in base_class_clause
        ASTUtil.ASTNode typeIdentifier = findChildByType(baseList, "type_identifier");
        if (typeIdentifier != null) {
            String baseName = typeIdentifier.getText(sourceCode);
            if (!currentNamespace.isEmpty()) {
                baseName = currentNamespace + "::" + baseName;
            }
            System.out.println("Adding superclass: " + baseName);
            umlClass.addSuperclass(baseName);
        }
    }

    private String extractModuleName(String filePath) {
        return filePath.replaceAll("[/\\\\]", "::")
                .replaceAll("\\.cpp$|\\.h$|\\.hpp$", "");
    }
}
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
            case "include_directive":
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
        System.out.println("processing class");
        ASTUtil.ASTNode nameNode = findChildByType(node, "type_identifier");
        if (nameNode == null) return;

        String className = nameNode.getText(sourceCode);
        System.out.println(" class name" + className);
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

        // Process inheritance
        processInheritance(node, currentClass);

        // Process class body
        currentScope.add(className);
        ASTUtil.ASTNode body = findChildByType(node, "field_declaration_list");
        if (body != null) {
            processAccessSpecifiers(body);
        }
        currentScope.remove(currentScope.size() - 1);

        // Finalize class processing
        model.addClass(currentClass);
        currentClass = previousClass;
    }

    private void processAccessSpecifiers(ASTUtil.ASTNode node) {
        Visibility currentVisibility = Visibility.PRIVATE;

        for (ASTUtil.ASTNode child : node.children) {
            if (child.type.equals("access_specifier")) {
                String specifier = child.getText(sourceCode);
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
            } else {
                // Process member with current visibility
                visit(child);
                if (currentClass != null) {
                    Visibility finalCurrentVisibility = currentVisibility;
                    currentClass.getOperations().forEach(op ->
                            op.setVisibility(finalCurrentVisibility));
                    Visibility finalCurrentVisibility1 = currentVisibility;
                    currentClass.getAttributes().forEach(attr ->
                            attr.setVisibility(finalCurrentVisibility1));
                }
            }
        }
    }

    @Override
    protected void processMethod(ASTUtil.ASTNode node) {
        System.out.println("process Method");
        ASTUtil.ASTNode functionDeclarator = findChildByType(node, "function_declarator");
        if (functionDeclarator == null) return;

        // Get method name from identifier
        ASTUtil.ASTNode identifierNode = findChildByType(functionDeclarator, "identifier");
        if (identifierNode == null) return;

        String methodName = identifierNode.getText(sourceCode);
        System.out.println("Method name " + methodName);

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.METHOD_DECLARATION
        );

        UMLOperation.Builder builder = UMLOperation.builder(methodName, locationInfo);

        // Process method modifiers - make sure this is called before building
        processMethodModifiers(functionDeclarator, builder);

        // Process parameters
        processParameters(node, builder);

        // Process return type
        processReturnType(node, builder);

        // Process method body
        ASTUtil.ASTNode body = findChildByType(node, "compound_statement");
        if (body != null) {
            builder.body(body.getText(sourceCode));
        }

        // Build and add operation
        UMLOperation operation = builder.build();
        if (currentClass != null) {
            operation.setClassName(currentClass.getName());
            currentClass.addOperation(operation);
        } else {
            model.addOperation(operation);
        }
    }

    private void processMethodModifiers(ASTUtil.ASTNode functionDeclarator, UMLOperation.Builder builder) {
        // Check for const qualifier at function declaration level
        List<ASTUtil.ASTNode> typeQualifiers = findChildrenByType(functionDeclarator, "type_qualifier");
        for (ASTUtil.ASTNode qualifier : typeQualifiers) {
            String qualifierText = qualifier.getText(sourceCode);
            System.out.println("Found qualifier: " + qualifierText); // Debug print
            if ("const".equals(qualifierText)) {
                builder.setConst(true);
            }
        }

        // Check for noexcept
        ASTUtil.ASTNode noexceptNode = findChildByType(functionDeclarator, "noexcept");
        if (noexceptNode != null) {
            builder.setNoexcept(true);
        }

        // Check for inline
        ASTUtil.ASTNode inlineNode = findChildByType(functionDeclarator, "inline");
        if (inlineNode != null) {
            builder.setInline(true);
        }
    }
    private void processReturnType(ASTUtil.ASTNode node, UMLOperation.Builder builder) {
        // First try to find primitive_type
        ASTUtil.ASTNode typeNode = findChildByType(node, "primitive_type");
        if (typeNode == null) {
            // Then try type_identifier
            typeNode = findChildByType(node, "type_identifier");
        }

        if (typeNode != null) {
            builder.returnType(new UMLType(typeNode.getText(sourceCode)));
        } else {
            builder.returnType(new UMLType("void")); // Default C++ return type
        }
    }
    @Override
    protected void processField(ASTUtil.ASTNode node) {
        if (currentClass == null) return;

        ASTUtil.ASTNode typeNode = findChildByType(node, "type_identifier");
        ASTUtil.ASTNode nameNode = findChildByType(node, "identifier");

        if (typeNode == null || nameNode == null) return;

        String fieldName = nameNode.getText(sourceCode);
        String fieldType = typeNode.getText(sourceCode);

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

        // Process field modifiers
        processFieldModifiers(node, attribute);

        // Get initial value if present
        ASTUtil.ASTNode initNode = findChildByType(node, "initializer");
        if (initNode != null) {
            attribute.setInitialValue(initNode.getText(sourceCode));
        }

        currentClass.addAttribute(attribute);
    }

    @Override
    protected void processImport(ASTUtil.ASTNode node) {
        ASTUtil.ASTNode pathNode = findChildByType(node, "string_literal");
        if (pathNode == null) return;

        String importPath = pathNode.getText(sourceCode)
                .replaceAll("\"", "")
                .replaceAll("<|>", "");

        LocationInfo locationInfo = new LocationInfo(
                filePath,
                node.startPoint,
                node.endPoint,
                CodeElementType.IMPORT_DECLARATION
        );

        UMLImport umlImport = UMLImport.builder(importPath, locationInfo)
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

        for (ASTUtil.ASTNode baseClass : baseList.children) {
            if (baseClass.type.equals("base_class")) {
                ASTUtil.ASTNode nameNode = findChildByType(baseClass, "identifier");
                if (nameNode != null) {
                    String baseName = nameNode.getText(sourceCode);
                    if (!currentNamespace.isEmpty()) {
                        baseName = currentNamespace + "::" + baseName;
                    }
                    umlClass.addSuperclass(baseName);
                }
            }
        }
    }


    private String extractModuleName(String filePath) {
        return filePath.replaceAll("[/\\\\]", "::")
                .replaceAll("\\.cpp$|\\.h$|\\.hpp$", "");
    }
}
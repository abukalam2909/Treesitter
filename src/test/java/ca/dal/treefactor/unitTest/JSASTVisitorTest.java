package ca.dal.treefactor.unitTest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.core.UMLImport;
import ca.dal.treefactor.model.core.UMLParameter;
import ca.dal.treefactor.model.core.Visibility;
import ca.dal.treefactor.model.elements.UMLAttribute;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.util.ASTUtil;
import ca.dal.treefactor.util.JSASTVisitor;
import ca.dal.treefactor.util.TreeSitterUtil;
import io.github.treesitter.jtreesitter.InputEncoding;
import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;

import java.io.IOException;
import java.util.List;

public class JSASTVisitorTest {
    private Language language;
    private UMLModel model;
    private Parser parser;
    private static final String TEST_FILE = "test.js";

    @BeforeEach
    void setUp() throws IOException {
        language = TreeSitterUtil.loadLanguageForFileExtension(TEST_FILE);
        model = new UMLModel("javascript");
        parser = new Parser();
        parser.setLanguage(language);
    }

    @Nested
    class FunctionTests {
        @Test
        void testSimpleFunction() throws Exception {
            String code = """
                function greet(name) {
                    console.log(`Hello, ${name}!`);
                }
                """;

            processCode(code);

            List<UMLOperation> operations = model.getOperations();
            assertEquals(1, operations.size());

            UMLOperation greet = operations.get(0);
            assertEquals("greet", greet.getName());
            assertEquals(1, greet.getParameters().size());
            assertEquals("name", greet.getParameters().get(0).getName());
            assertEquals(Visibility.PUBLIC, greet.getVisibility());
        }

        @Test
        void testFunctionWithDefaultParams() throws Exception {
            String code = """
                function greet(name = "World") {
                    return `Hello ${name}!`;
                }
                """;

            processCode(code);

            UMLOperation operation = model.getOperations().get(0);
            UMLParameter param = operation.getParameters().get(0);
            assertEquals("name", param.getName());
            assertEquals("\"World\"", param.getDefaultValue());
        }
    }

    @Nested
    class ClassTests {
        @Test
        void testSimpleClass() throws Exception {
            String code = """
                class Person {
                    name;
                    constructor(name) {
                        this.name = name;
                    }
                }
                """;

            processCode(code);

            List<UMLClass> classes = model.getClasses();
            assertEquals(1, classes.size());

            UMLClass person = classes.get(0);
            assertEquals("Person", person.getName());
            assertEquals(1, person.getAttributes().size());
            assertTrue(person.getOperations().stream()
                    .anyMatch(op -> op.getName().equals("constructor") && op.isConstructor()));
        }

        @Test
        void testClassWithInheritance() throws Exception {
            String code = """
                class Dog extends Animal {
                    makeSound() {
                        return 'Woof!';
                    }
                }
                """;

            processCode(code);

            UMLClass dog = model.getClasses().get(0);
            assertEquals(1, dog.getSuperclasses().size());
            assertEquals("Animal", dog.getSuperclasses().get(0));
        }
    }

    @Nested
    class AttributeTests {
        @Test
        void testClassAttributes() throws Exception {
            String code = """
                class Counter {
                    count = 0;
                    _total = 0;
                }
                """;

            processCode(code);

            UMLClass counter = model.getClasses().get(0);
            List<UMLAttribute> attributes = counter.getAttributes();
            assertEquals(2, attributes.size());

            assertTrue(attributes.stream()
                    .anyMatch(attr -> attr.getName().equals("count")));
            assertTrue(attributes.stream()
                    .anyMatch(attr -> attr.getName().equals("_total")));
        }

        @Test
        void testPrivateField() throws Exception {
            String code = """
                class Person {
                    #name;
                    #age;
                }
                """;

            processCode(code);

            UMLClass person = model.getClasses().get(0);
            List<UMLAttribute> attributes = person.getAttributes();
            assertEquals(2, attributes.size());
            assertTrue(attributes.stream()
                    .allMatch(attr -> attr.getVisibility() == Visibility.PRIVATE));
        }
    }

    @Nested
    class ImportTests {
        @Test
        void testSimpleImport() throws Exception {
            String code = "import { Component } from '@angular/core';";

            processCode(code);

            List<UMLImport> imports = model.getImports(TEST_FILE);
            assertEquals(1, imports.size());
            assertEquals("@angular/core.Component", imports.get(0).getImportedName());
        }

        @Test
        void testMultipleImports() throws Exception {
            String code = "import { useState, useEffect } from 'react';";

            processCode(code);

            List<UMLImport> imports = model.getImports(TEST_FILE);
            assertEquals(2, imports.size());
            assertTrue(imports.stream()
                    .anyMatch(i -> i.getImportedName().equals("react.useState")));
            assertTrue(imports.stream()
                    .anyMatch(i -> i.getImportedName().equals("react.useEffect")));
        }
    }

    private void processCode(String code) {
        try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
            JSASTVisitor visitor = new JSASTVisitor(model, code, TEST_FILE);
            ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(tree.getRootNode());
            visitor.visit(astNode);
        }
    }
}

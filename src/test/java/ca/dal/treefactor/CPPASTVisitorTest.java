package ca.dal.treefactor;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.elements.UMLAttribute;
import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.util.ASTUtil;
import ca.dal.treefactor.util.CPPASTVisitor;
import ca.dal.treefactor.util.TreeSitterUtil;
import io.github.treesitter.jtreesitter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CPPASTVisitorTest {
    private Language language;
    private UMLModel model;
    private Parser parser;
    private static final String TEST_FILE = "test.cpp";

    @BeforeEach
    void setUp() throws IOException {
        language = TreeSitterUtil.loadLanguageForFileExtension(TEST_FILE);
        model = new UMLModel("cpp");
        parser = new Parser();
        parser.setLanguage(language);
    }

    @Nested
    class ClassTests {
        @Test
        void testSimpleClass() throws Exception {
            String code = """
                class Person {
                public:
                    Person(const std::string& name) : name(name) {}
                    void greet() {
                        std::cout << "Hello, " << name << "!" << std::endl;
                    }
                private:
                    std::string name;
                };
                """;
            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

            processCode(code);

            List<UMLClass> classes = model.getClasses();
            assertEquals(1, classes.size(), "Should have one class");

            UMLClass person = classes.get(0);
            assertEquals("Person", person.getName(), "Class should be named Person");
            System.out.println(person.getOperations().size());
            assertEquals(2, person.getOperations().size(), "Should have two method");
            assertEquals("Person", person.getOperations().get(0).getName(), "The first method's name should be Person");
            assertEquals("greet", person.getOperations().get(1).getName(), "The second method's name should be greet");

            //assertEquals(1, person.getAttributes().size(), "Should have one attribute");
        }

        @Test
        void testClassWithInheritance() throws Exception {
            String code = """
                class Animal {
                public:
                    virtual void makeSound() = 0;
                };
                
                class Dog : public Animal {
                public:
                    void makeSound() override {
                        std::cout << "Woof!" << std::endl;
                    }
                };
                """;

            processCode(code);
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

            assertEquals(2, model.getClasses().size(), "Should have two classes");

            UMLClass dog = model.getClasses().stream()
                    .filter(c -> c.getName().equals("Dog"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(1, dog.getSuperclasses().size(), "Should have one superclass");
            assertEquals("Animal", dog.getSuperclasses().get(0), "Superclass should be Animal");
        }
    }

    @Nested
    class MethodTests {
        @Test
        void testSimpleMethod() throws Exception {
            String code = """
            int add(int a = 1, const int& b = 2) const noexcept {
                int result = a + b;
                return result;
            }
            """;

            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

            processCode(code);

            List<UMLOperation> operations = model.getOperations();
            assertEquals(1, operations.size(), "Should have one method");
            UMLOperation add = operations.get(0);
            assertEquals("add", add.getName(), "Method name should be 'add'");
            assertTrue(add.isConst(), "Should be const");
            assertTrue(add.isNoexcept(), "Should be noexcept");

        }

        @Test
        void testVirtualMethod() throws Exception {
            String code = """
                virtual void makeSound() = 0;
            """;

            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }
            processCode(code);

            UMLOperation makeSound = model.getOperations().get(0);
            assertTrue(makeSound.isVirtual(), "Method should be virtual");
            assertTrue(makeSound.isAbstract(), "Method should be abstract");
        }
    }

    @Nested
    class AttributeTests {
        @Test
        void testClassAttributes() throws Exception {
            String code = """
                class Counter {
                public:
                    static int count;
                private:
                    static int _total;
                };
                
                int Counter::count = 0;
                int Counter::_total = 0;
                """;

            processCode(code);

            UMLClass counter = model.getClasses().get(0);
            List<UMLAttribute> attributes = counter.getAttributes();

            assertEquals(2, attributes.size(), "Should have two attributes");

            UMLAttribute countAttr = attributes.stream()
                    .filter(a -> a.getName().equals("count"))
                    .findFirst()
                    .orElseThrow();

            assertTrue(countAttr.isStatic(), "Should be a static attribute");
            assertEquals(Visibility.PUBLIC, countAttr.getVisibility(), "Should be public");
            assertEquals("0", countAttr.getInitialValue(), "Should have initial value");
        }
    }

    @Nested
    class ImportTests {
        @Test
        void testSimpleImport() throws Exception {
            String code = "#include <iostream>";

            processCode(code);

            List<UMLImport> imports = model.getImports(TEST_FILE);
            assertEquals(1, imports.size(), "Should have one import");
            assertEquals("iostream", imports.get(0).getImportedName(),
                    "Should import iostream");
        }

        @Test
        void testSystemImport() throws Exception {
            String code = "#include <vector>";

            processCode(code);

            List<UMLImport> imports = model.getImports(TEST_FILE);
            assertEquals(1, imports.size(), "Should have one import");
            assertEquals("vector", imports.get(0).getImportedName(),
                    "Should import vector");
        }

        @Test
        void testLocalImport() throws Exception {
            String code = "#include \"Person.h\"";

            processCode(code);

            List<UMLImport> imports = model.getImports(TEST_FILE);
            assertEquals(1, imports.size(), "Should have one import");
            assertEquals("Person.h", imports.get(0).getImportedName(),
                    "Should import Person.h");
        }
    }


    @Nested
    class ParameterTests {
        @Test
        void testMultipleParameters() throws Exception {
            String code = """
            int calculate(int x, int y, int z) {
                return x + y + z;
            }
            """;
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            assertEquals(3, op.getParameters().size(), "Should have three parameters");
            assertEquals("x", op.getParameters().get(0).getName());
            assertEquals("y", op.getParameters().get(1).getName());
            assertEquals("z", op.getParameters().get(2).getName());
        }

        @Test
        void testReferenceParameters() throws Exception {
            String code = """
        void process(const std::vector<int>& data, int& count, std::string& name) {
            count = data.size();
            name = "processed";
        }
        """;

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals(3, params.size(), "Should have three parameters");
            assertTrue(params.get(0).isReference(), "First parameter should be reference");
            assertTrue(params.get(0).isConst(), "First parameter should be const");
            assertTrue(params.get(1).isReference(), "Second parameter should be reference");
            assertTrue(params.get(2).isReference(), "Third parameter should be reference");
        }

        @Test
        void testMethodParameters() throws Exception {
            String code = """
        class Calculator {
        public:
            int add(int x, int y) const {
                return x + y;
            }
        };
        """;

            processCode(code);

            UMLClass calc = model.getClasses().get(0);
            UMLOperation add = calc.getOperations().get(0);
            List<UMLParameter> params = add.getParameters();

            assertEquals(2, params.size());
            assertEquals("int", params.get(0).getType().getTypeName());
            assertEquals("int", params.get(1).getType().getTypeName());
            assertTrue(add.isConst(), "Method should be const");
        }

        @Test
        void testPointerParameters() throws Exception {
            String code = """
        void processData(int* data, const char* name, void* context) {
            // Implementation
        }
        """;

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals(3, params.size());
            assertTrue(params.get(0).isPointer(), "First parameter should be pointer");
            assertEquals("int", params.get(0).getType().getTypeName());

            assertTrue(params.get(1).isPointer(), "Second parameter should be pointer");
            assertTrue(params.get(1).isConst(), "Second parameter should be const");
            assertEquals("char", params.get(1).getType().getTypeName());

            assertTrue(params.get(2).isPointer(), "Third parameter should be pointer");
            assertEquals("void", params.get(2).getType().getTypeName());
        }

        @Test
        void testRValueReferenceParameters() throws Exception {
            String code = """
            void moveData(std::string&& data, std::vector<int>&& numbers) {
                // Implementation
            }
            """;

            processCode(code);
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals(2, params.size());
            assertTrue(params.get(0).isRValueReference(), "First parameter should be rvalue reference");
            assertEquals("std::string", params.get(0).getType().getTypeName());
            assertTrue(params.get(1).isRValueReference(), "Second parameter should be rvalue reference");
            assertEquals("std::vector<int>", params.get(1).getType().getTypeName());
        }
    }

    // Helper method to process code
    private void processCode(String code) {
        try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
            Node rootNode = tree.getRootNode();
            CPPASTVisitor visitor = new CPPASTVisitor(model, code, TEST_FILE);
            ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
            visitor.visit(astNode);
        }
    }
}
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
        model = new UMLModel(new HashSet<>(), "cpp");
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
            assertEquals(2, person.getOperations().size(), "Should have two method");
            assertEquals("Person", person.getOperations().get(0).getName(), "The first method's name should be Person");
            assertEquals("greet", person.getOperations().get(1).getName(), "The second method's name should be greet");

            assertEquals(1, person.getAttributes().size(), "Should have one attribute");
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
package ca.dal.treefactor;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import ca.dal.treefactor.util.PythonASTVisitor;
import ca.dal.treefactor.util.TreeSitterUtil;
import io.github.treesitter.jtreesitter.InputEncoding;
import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;

public class PythonASTVisitorTest {
    private Language language;
    private UMLModel model;
    private Parser parser;
    private static final String TEST_FILE = "test.py";

    @BeforeEach
    void setUp() throws IOException {
        language = TreeSitterUtil.loadLanguageForFileExtension(TEST_FILE);
        model = new UMLModel("python");
        parser = new Parser();
        parser.setLanguage(language);
    }

    @Nested
    class FunctionTests {
        @Test
        void testSimpleFunction() throws Exception {
            String code = """
                def greet(name):
                    print(f"Hello, {name}!")
                """;

            processCode(code);

            List<UMLOperation> operations = model.getOperations();
            assertEquals(1, operations.size(), "Should have one function");

            UMLOperation greet = operations.get(0);
            assertEquals("greet", greet.getName(), "Function name should be 'greet'");
            assertEquals(Visibility.PUBLIC, greet.getVisibility(), "Should be public by default");
            assertEquals(1, greet.getParameters().size(), "Should have one parameter");
            assertEquals("name", greet.getParameters().get(0).getName(), "Parameter should be named 'name'");
        }

        @Test
        void testFunctionWithTypeHints() throws Exception {
            String code = """
                def greet(name: str) -> str:
                    return f"Hello, {name}!"
                """;

            processCode(code);

            // Add debug info
            System.out.println("Number of operations: " + model.getOperations().size());
            model.getOperations().forEach(op ->
                    System.out.println("Operation: " + op.getName()));

            assertFalse(model.getOperations().isEmpty(), "Should have at least one operation");

            UMLOperation greet = model.getOperations().get(0);
            UMLParameter param = greet.getParameters().get(0);

            assertEquals("str", param.getType().getTypeName(), "Parameter should have str type");
            assertEquals("str", greet.getReturnType().getTypeName(), "Return type should be str");
        }

        @Test
        void testPrivateFunction() throws Exception {
            String code = """
                def _helper():
                    pass
                """;

            processCode(code);

            UMLOperation helper = model.getOperations().get(0);
            assertEquals(Visibility.PROTECTED, helper.getVisibility(),
                    "Function with underscore should be protected");
        }
    }

    @Nested
    class ClassTests {
        @Test
        void testSimpleClass() throws Exception {
            String code = """
                class Person:
                    def __init__(self, name):
                        self.name = name
                """;

            processCode(code);

            List<UMLClass> classes = model.getClasses();
            assertEquals(1, classes.size(), "Should have one class");

            UMLClass person = classes.get(0);
            assertEquals("Person", person.getName(), "Class should be named Person");
            assertEquals(1, person.getOperations().size(), "Should have one method");
            assertEquals(1, person.getAttributes().size(), "Should have one attribute");
        }

        @Test
        void testClassWithInheritance() throws Exception {
            String code = """
                class Animal:
                    pass
                    
                class Dog(Animal):
                    pass
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

        @Test
        void testClassWithDecorator() throws Exception {
            String code = """
                @dataclass
                class Person:
                    name: str
                    age: int
                """;

            processCode(code);


            UMLClass person = model.getClasses().get(0);
            assertTrue(person.hasAnnotations(), "Should have annotations");
            assertEquals("dataclass", person.getAnnotations().get(0).getName(),
                    "Should have dataclass annotation");
        }
    }

    @Nested
    class AttributeTests {
        @Test
        void testClassAttributes() throws Exception {
            String code = """
                class Counter:
                    count = 0
                    _total = 0
                """;

            processCode(code);
            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

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

        @Test
        void testInstanceAttributes() throws Exception {
            String code = """
                class Person:
                    def __init__(self):
                        self.name = "John"
                        self._age = 30
                """;

            processCode(code);

            UMLClass person = model.getClasses().get(0);
            List<UMLAttribute> attributes = person.getAttributes();

            assertEquals(2, attributes.size(), "Should have two attributes");
            assertTrue(attributes.stream().anyMatch(a ->
                    a.getName().equals("name") && a.getVisibility() == Visibility.PUBLIC));
            assertTrue(attributes.stream().anyMatch(a ->
                    a.getName().equals("_age") && a.getVisibility() == Visibility.PROTECTED));
        }
    }

    @Nested
    class ImportTests {
        @Test
        void testSimpleImport() throws Exception {
            String code = "import datetime";

            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }
            processCode(code);

            List<UMLImport> imports = model.getImports(TEST_FILE);
            assertEquals(1, imports.size(), "Should have one import");
            assertEquals("datetime", imports.get(0).getImportedName(),
                    "Should import datetime");
        }

        @Test
        void testFromImport() throws Exception {
            String code = "from os.path import join, dirname";

            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }
            processCode(code);

            List<UMLImport> imports = model.getImports(TEST_FILE);
            assertEquals(2, imports.size(), "Should have two imports");
            assertTrue(imports.stream().anyMatch(i ->
                    i.getImportedName().equals("os.path.join")));
            assertTrue(imports.stream().anyMatch(i ->
                    i.getImportedName().equals("os.path.dirname")));
        }
    }

    @Nested
    class ParameterTests {
        @Test
        void testMultipleParameters() throws Exception {
            String code = """
            def calculate(x, y, z):
                return x + y + z
            """;

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            assertEquals(3, op.getParameters().size(), "Should have three parameters");
            assertEquals("x", op.getParameters().get(0).getName());
            assertEquals("y", op.getParameters().get(1).getName());
            assertEquals("z", op.getParameters().get(2).getName());
        }

        @Test
        void testParameterWithDefaultValue() throws Exception {
            String code = """
            def greet(name = "World"):
                print(f"Hello, {name}!")
            """;

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            //System.out.println(model.)
            UMLParameter param = op.getParameters().get(0);
            assertEquals("name", param.getName());
            assertEquals("\"World\"", param.getDefaultValue());
        }

        @Test
        void testTypedParameterWithDefault() throws Exception {
            String code = """
            def greet(name: str = "World", count: int = 1):
                print(f"Hello, {name}!" * count)
            """;

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals(2, params.size());

            assertEquals("name", params.get(0).getName());
            assertEquals("str", params.get(0).getType().getTypeName());
            assertEquals("\"World\"", params.get(0).getDefaultValue());

            assertEquals("count", params.get(1).getName());
            assertEquals("int", params.get(1).getType().getTypeName());
            assertEquals("1", params.get(1).getDefaultValue());
        }

        @Test
        void testMixedTypedAndUntypedParameters() throws Exception {
            String code = """
            def process(data: list, count, name: str = "test"):
                pass
            """;

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals(3, params.size(), "Should have three parameters");
            assertEquals("list", params.get(0).getType().getTypeName());
            assertEquals("object", params.get(1).getType().getTypeName());
            assertEquals("str", params.get(2).getType().getTypeName());
            assertEquals("\"test\"", params.get(2).getDefaultValue());
        }

        @Test
        void testMethodParameters() throws Exception {
            String code = """
            class Calculator:
                def add(self, x: int, y: int) -> int:
                    return x + y
            """;

            processCode(code);

            UMLClass calc = model.getClasses().get(0);
            UMLOperation add = calc.getOperations().get(0);
            List<UMLParameter> params = add.getParameters();

            assertEquals(3, params.size(), "Should include self and two parameters");
            assertEquals("self", params.get(0).getName());
            assertEquals("int", params.get(1).getType().getTypeName());
            assertEquals("int", params.get(2).getType().getTypeName());
        }

        @Test
        void testComplexParameterTypes() throws Exception {
            String code = """
            def process_data(items: List[str], 
                           config: Dict[str, Any] = None) -> Optional[List[int]]:
                pass
            """;

            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals("List[str]", params.get(0).getType().getTypeName());
            assertEquals("Dict[str, Any]", params.get(1).getType().getTypeName());
            assertEquals("None", params.get(1).getDefaultValue());
        }

        @Test
        void testKeywordOnlyParameters() throws Exception {
            String code = """
            def configure(*, host: str, port: int = 8080):
                pass
            """;
            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals(2, params.size());
            assertTrue(params.get(0).isKeywordOnly());
            assertTrue(params.get(1).isKeywordOnly());
            assertEquals("8080", params.get(1).getDefaultValue());
        }

        @Test
        void testMixedParameters() throws Exception {
            String code = """
            def process(arg1, arg2, *, kw1: str, kw2: int = 42):
                pass
            """;

            // Print AST for debugging
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
                System.out.println("AST Structure:");
                System.out.println(ASTUtil.printAST(astNode, 0));
            }
            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            List<UMLParameter> params = op.getParameters();

            assertEquals(4, params.size(), "Should have four parameters");

            // Regular parameters
            assertFalse(params.get(0).isKeywordOnly(), "arg1 should not be keyword-only");
            assertFalse(params.get(1).isKeywordOnly(), "arg2 should not be keyword-only");

            // Keyword-only parameters
            assertTrue(params.get(2).isKeywordOnly(), "kw1 should be keyword-only");
            assertTrue(params.get(3).isKeywordOnly(), "kw2 should be keyword-only");
            assertEquals("42", params.get(3).getDefaultValue(), "kw2 should have default value");
        }

        @Test
        void testOnlySeparator() throws Exception {
            String code = """
            def func(*):
                pass
            """;

            processCode(code);

            UMLOperation op = model.getOperations().get(0);
            assertTrue(op.getParameters().isEmpty(),
                    "Should have no parameters with only separator");
        }
    }

    // Helper method to process code
    private void processCode(String code) {
        try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
            Node rootNode = tree.getRootNode();
            PythonASTVisitor visitor = new PythonASTVisitor(model, code, TEST_FILE);
            ASTUtil.ASTNode astNode = ASTUtil.buildASTWithCursor(rootNode);
            visitor.visit(astNode);
        }
    }
}
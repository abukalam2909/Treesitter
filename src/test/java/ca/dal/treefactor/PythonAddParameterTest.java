package ca.dal.treefactor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.AddParameterRefactoring;
import ca.dal.treefactor.util.UMLModelReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisplayName("Python Add Parameter Detection Tests")
class PythonAddParameterTest {

    private UMLModel parentModel;
    private UMLModel currentModel;
    private List<Refactoring> refactorings;

    @BeforeEach
    void setup() {
        parentModel = null;
        currentModel = null;
        refactorings = null;
    }

    private void createModelsFromCode(String beforeCode, String afterCode) {
        Map<String, String> beforeContents = new HashMap<>();
        beforeContents.put("example.py", beforeCode);
        parentModel = new UMLModelReader(beforeContents).getUmlModel();

        Map<String, String> afterContents = new HashMap<>();
        afterContents.put("example.py", afterCode);
        currentModel = new UMLModelReader(afterContents).getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentModel, currentModel);
        refactorings = modelDiff.detectRefactorings();
    }

    private void assertSingleParameterAddition(String expectedName, String expectedType, String expectedDefault) {
        assertNotNull(refactorings, "Refactorings should not be null");
        assertEquals(1, refactorings.size(), "Should detect exactly one refactoring");

        Refactoring refactoring = refactorings.get(0);
        assertTrue(refactoring instanceof AddParameterRefactoring,
                "Refactoring should be parameter addition");

        AddParameterRefactoring add = (AddParameterRefactoring) refactoring;
        assertEquals(expectedName, add.getAddedParameter().getName(),
                "Added parameter name mismatch");

        if (expectedType != null) {
            assertEquals(expectedType, add.getAddedParameter().getType().getTypeName(),
                    "Added parameter type mismatch");
        }

        if (expectedDefault != null) {
            assertEquals(expectedDefault, add.getDefaultValue(),
                    "Default value mismatch");
        }
    }

    @Nested
    @DisplayName("Simple Parameter Addition Tests")
    class SimpleParameterAdditionTests {

        @Test
        @DisplayName("Basic parameter addition")
        void testSimpleAddition() {
            createModelsFromCode(
                    """
                    def greet():
                        print("Hello!")
                    """,
                    """
                    def greet(name):
                        print(f"Hello, {name}!")
                    """
            );
            assertSingleParameterAddition("name", null, null);
        }

        @Test
        @DisplayName("Add parameter with type annotation")
        void testAddTypedParameter() {
            createModelsFromCode(
                    """
                    def calculate(x: int):
                        return x + 5
                    """,
                    """
                    def calculate(x: int, y: int):
                        return x + y
                    """
            );
            assertSingleParameterAddition("y", "int", null);
        }
    }

    @Nested
    @DisplayName("Default Value Parameter Tests")
    class DefaultValueParameterTests {

        @Test
        @DisplayName("Add parameter with default value")
        void testAddParameterWithDefault() {
            createModelsFromCode(
                    """
                    def greet(name: str):
                        print(f"Hello, {name}!")
                    """,
                    """
                    def greet(name: str, greeting: str = "Hello"):
                        print(f"{greeting}, {name}!")
                    """
            );
            assertSingleParameterAddition("greeting", "str", "\"Hello\"");
        }
    }

    @Nested
    @DisplayName("Multiple Parameter Addition Tests")
    class MultipleParameterTests {

        @Test
        @DisplayName("Add multiple parameters")
        void testAddMultipleParameters() {
            createModelsFromCode(
                    """
                    def calculate(x: int):
                        return x
                    """,
                    """
                    def calculate(x: int, y: int, z: int = 0):
                        return x + y + z
                    """
            );

            assertEquals(2, refactorings.size(), "Should detect two parameter additions");

            var add1 = (AddParameterRefactoring) refactorings.get(0);
            assertEquals("y", add1.getAddedParameter().getName(), "First parameter name mismatch");
            assertEquals("int", add1.getAddedParameter().getType().getTypeName(), "First parameter type mismatch");

            var add2 = (AddParameterRefactoring) refactorings.get(1);
            assertEquals("z", add2.getAddedParameter().getName(), "Second parameter name mismatch");
            assertEquals("int", add2.getAddedParameter().getType().getTypeName(), "Second parameter type mismatch");
            assertEquals("0", add2.getDefaultValue(), "Second parameter default value mismatch");
        }
    }

    @Nested
    @DisplayName("Class Method Parameter Tests")
    class ClassMethodParameterTests {

        @Test
        @DisplayName("Add parameter to class method")
        void testAddParameterToMethod() {
            createModelsFromCode(
                    """
                    class Calculator:
                        def add(self, x: int):
                            return x
                    """,
                    """
                    class Calculator:
                        def add(self, x: int, y: int = 0):
                            return x + y
                    """
            );
            assertSingleParameterAddition("y", "int", "0");
        }
    }

    @Nested
    @DisplayName("Complex Type Parameter Tests")
    class ComplexTypeParameterTests {

        @Test
        @DisplayName("Add parameter with complex type annotation")
        void testAddParameterWithComplexType() {
            createModelsFromCode(
                    """
                    def process():
                        print("Processing...")
                    """,
                    """
                    def process(data: List[Dict[str, Any]] = None):
                        if data is None:
                            print("Processing...")
                        else:
                            print(f"Processing {len(data)} items...")
                    """
            );
            assertSingleParameterAddition("data", "List[Dict[str, Any]]", "None");
        }
    }
}
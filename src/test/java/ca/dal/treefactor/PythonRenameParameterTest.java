package ca.dal.treefactor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameParameterRefactoring;
import ca.dal.treefactor.util.UMLModelReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisplayName("Python Parameter Rename Detection Tests")
class PythonRenameParameterTest {

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

    private void assertSingleParameterRename(String expectedOriginal, String expectedRenamed) {
        assertNotNull(refactorings, "Refactorings should not be null");
        assertEquals(1, refactorings.size(), "Should detect exactly one refactoring");

        Refactoring refactoring = refactorings.get(0);
        assertTrue(refactoring instanceof RenameParameterRefactoring,
                "Refactoring should be parameter rename");

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactoring;
        assertEquals(expectedOriginal, rename.getOriginalParameter().getName(),
                "Original parameter name mismatch");
        assertEquals(expectedRenamed, rename.getRenamedParameter().getName(),
                "Renamed parameter name mismatch");
    }

    @Nested
    @DisplayName("Simple Parameter Rename Tests")
    class SimpleParameterRenameTests {

        @Test
        @DisplayName("Basic parameter rename")
        void testSimpleRename() {
            createModelsFromCode(
                    "def greet(n): print(f\"Hello, {n}!\")",
                    "def greet(name): print(f\"Hello, {name}!\")"
            );
            assertSingleParameterRename("n", "name");
        }

        @Test
        @DisplayName("Parameter rename with default value")
        void testRenameWithDefault() {
            createModelsFromCode(
                    "def calculator(n = 1):\n    return n + 5",
                    "def calculator(num):\n    return num + 5"
            );
            assertSingleParameterRename("n", "num");
        }
    }

    @Nested
    @DisplayName("Typed Parameter Rename Tests")
    class TypedParameterRenameTests {

        @Test
        @DisplayName("Rename typed parameter")
        void testTypedParameterRename() {
            createModelsFromCode(
                    "def calculator(n: int):\n    return n + 5",
                    "def calculator(num: int):\n    return num + 5"
            );
            assertSingleParameterRename("n", "num");
        }

        @Test
        @DisplayName("Rename typed parameter with default")
        void testTypedParameterWithDefault() {
            createModelsFromCode(
                    "def calculator(n: int = 4):\n    return n + 5",
                    "def calculator(num: int = 5):\n    return num + 5"
            );
            assertSingleParameterRename("n", "num");
        }
    }

    @Nested
    @DisplayName("Multiple Parameter Rename Tests")
    class MultipleParameterRenameTests {

        @Test
        @DisplayName("Rename two parameters")
        void testTwoParameterRename() {
            createModelsFromCode(
                    "def fullname(fname, lname):\n    return fname + lname",
                    "def fullname(firstname, lastname):\n    return firstname + lastname"
            );

            assertEquals(2, refactorings.size(), "Should detect two parameter renames");

            var rename1 = (RenameParameterRefactoring) refactorings.get(0);
            assertEquals("fname", rename1.getOriginalParameter().getName(),
                    "First parameter original name mismatch");
            assertEquals("firstname", rename1.getRenamedParameter().getName(),
                    "First parameter new name mismatch");

            var rename2 = (RenameParameterRefactoring) refactorings.get(1);
            assertEquals("lname", rename2.getOriginalParameter().getName(),
                    "Second parameter original name mismatch");
            assertEquals("lastname", rename2.getRenamedParameter().getName(),
                    "Second parameter new name mismatch");
        }
    }

    @Nested
    @DisplayName("Class Method Parameter Rename Tests")
    class ClassMethodParameterRenameTests {

        @Test
        @DisplayName("Rename parameter in class method")
        void testClassMethodParameterRename() {
            createModelsFromCode(
                    """
                    class Square:
                        def calculate_perimeter(self, side_length):
                            return 4 * side_length
                    """,
                    """
                    class Square:
                        def calculate_perimeter(self, s):
                            return 4 * s
                    """
            );
            assertSingleParameterRename("side_length", "s");
        }
    }
}
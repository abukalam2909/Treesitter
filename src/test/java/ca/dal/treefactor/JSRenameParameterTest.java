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

@DisplayName("JavaScript Parameter Rename Detection Tests")
class JSRenameParameterTest {

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
        beforeContents.put("example.js", beforeCode);
        parentModel = new UMLModelReader(beforeContents).getUmlModel();

        Map<String, String> afterContents = new HashMap<>();
        afterContents.put("example.js", afterCode);
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
    @DisplayName("Simple Function Parameter Renames")
    class SimpleFunctionTests {

        @Test
        @DisplayName("Basic function parameter rename")
        void testSimpleRename() {
            createModelsFromCode(
                    "function greet(n) { console.log(`Hello, ${n}!`); }",
                    "function greet(name) { console.log(`Hello, ${name}!`); }"
            );
            assertSingleParameterRename("n", "name");
        }

        @Test
        @DisplayName("Parameter rename with default value")
        void testRenameWithDefault() {
            createModelsFromCode(
                    """
                    function calculator(n = 1) {
                        return n + 5;
                    }
                    """,
                    """
                    function calculator(num = 1) {
                        return num + 5;
                    }
                    """
            );
            assertSingleParameterRename("n", "num");
        }

        @Test
        @DisplayName("Arrow function parameter rename")
        void testArrowFunctionRename() {
            createModelsFromCode(
                    "const greet = (n) => console.log(`Hello, ${n}!`);",
                    "const greet = (name) => console.log(`Hello, ${name}!`);"
            );
            assertSingleParameterRename("n", "name");
        }
    }

    @Nested
    @DisplayName("Multiple Parameter Tests")
    class MultipleParameterTests {

        @Test
        @DisplayName("Rename two parameters in function")
        void testTwoParameterRename() {
            createModelsFromCode(
                    """
                    function fullName(fname, lname) {
                        return fname + ' ' + lname;
                    }
                    """,
                    """
                    function fullName(firstName, lastName) {
                        return firstName + ' ' + lastName;
                    }
                    """
            );

            assertEquals(2, refactorings.size(), "Should detect two parameter renames");

            RenameParameterRefactoring rename1 = (RenameParameterRefactoring) refactorings.get(0);
            assertEquals("fname", rename1.getOriginalParameter().getName(),
                    "First parameter original name mismatch");
            assertEquals("firstName", rename1.getRenamedParameter().getName(),
                    "First parameter new name mismatch");

            RenameParameterRefactoring rename2 = (RenameParameterRefactoring) refactorings.get(1);
            assertEquals("lname", rename2.getOriginalParameter().getName(),
                    "Second parameter original name mismatch");
            assertEquals("lastName", rename2.getRenamedParameter().getName(),
                    "Second parameter new name mismatch");
        }

        @Test
        @DisplayName("Multiple function parameter renames")
        void testMultipleFunctionRenames() {
            createModelsFromCode(
                    """
                    function fullName(fname, lname) {
                        return fname + ' ' + lname;
                    }
                    
                    function greet(p) {
                        console.log(`Hello ${p}`);
                    }
                    
                    const calculate = (n) => n * 2;
                    """,
                    """
                    function fullName(firstName, lastName) {
                        return firstName + ' ' + lastName;
                    }
                    
                    function greet(person) {
                        console.log(`Hello ${person}`);
                    }
                    
                    const calculate = (num) => num * 2;
                    """
            );

            assertEquals(4, refactorings.size(), "Should detect four parameter renames");

            Map<String, String> expectedRenames = new HashMap<>();
            expectedRenames.put("fname", "firstName");
            expectedRenames.put("lname", "lastName");
            expectedRenames.put("p", "person");
            expectedRenames.put("n", "num");

            for (Refactoring ref : refactorings) {
                assertTrue(ref instanceof RenameParameterRefactoring,
                        "Each refactoring should be a parameter rename");

                RenameParameterRefactoring rename = (RenameParameterRefactoring) ref;
                String originalName = rename.getOriginalParameter().getName();
                String expectedNewName = expectedRenames.get(originalName);

                assertNotNull(expectedNewName,
                        "Unexpected original parameter name: " + originalName);
                assertEquals(expectedNewName, rename.getRenamedParameter().getName(),
                        "Incorrect rename for parameter: " + originalName);

                expectedRenames.remove(originalName);
            }

            assertTrue(expectedRenames.isEmpty(),
                    "Not all expected renames were found: " + expectedRenames.keySet());
        }
    }

    @Nested
    @DisplayName("Class-related Parameter Tests")
    class ClassParameterTests {

        @Test
        @DisplayName("Rename parameter in class method")
        void testClassMethodRename() {
            createModelsFromCode(
                    """
                    class Square {
                        calculatePerimeter(side_length) {
                            return 4 * side_length;
                        }
                    }
                    """,
                    """
                    class Square {
                        calculatePerimeter(s) {
                            return 4 * s;
                        }
                    }
                    """
            );
            assertSingleParameterRename("side_length", "s");
        }

        @Test
        @DisplayName("Rename parameter in class constructor")
        void testConstructorRename() {
            createModelsFromCode(
                    """
                    class Person {
                        constructor(n) {
                            this.name = n;
                        }
                    }
                    """,
                    """
                    class Person {
                        constructor(name) {
                            this.name = name;
                        }
                    }
                    """
            );
            assertSingleParameterRename("n", "name");
        }
    }
}
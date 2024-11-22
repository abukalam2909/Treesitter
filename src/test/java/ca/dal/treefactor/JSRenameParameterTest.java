package ca.dal.treefactor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameParameterRefactoring;
import ca.dal.treefactor.util.UMLModelReader;

public class JSRenameParameterTest {

    @Test
    public void testRenameParameterSimple() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = "function greet(n) { console.log(`Hello, ${n}!`); }";
        fileContentsBefore.put("example.js", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = "function greet(name) { console.log(`Hello, ${name}!`); }";
        fileContentsAfter.put("example.js", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("name", rename.getRenamedParameter().getName());
    }

    @Test
    public void testRenameParameterWithDefaultValue() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            function calculator(n = 1) {
                return n + 5;
            }
            """;
        fileContentsBefore.put("example.js", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            function calculator(num = 1) {
                return num + 5;
            }
            """;
        fileContentsAfter.put("example.js", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("num", rename.getRenamedParameter().getName());
    }

    @Test
    public void testRenameParameterArrowFunction() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = "const greet = (n) => console.log(`Hello, ${n}!`);";
        fileContentsBefore.put("example.js", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = "const greet = (name) => console.log(`Hello, ${name}!`);";
        fileContentsAfter.put("example.js", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("name", rename.getRenamedParameter().getName());
    }

    @Test
    public void testRenameParameterTwoParams() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            function fullName(fname, lname) {
                return fname + ' ' + lname;
            }
            """;
        fileContentsBefore.put("example.js", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            function fullName(firstName, lastName) {
                return firstName + ' ' + lastName;
            }
            """;
        fileContentsAfter.put("example.js", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(2, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);
        assertTrue(refactorings.get(1) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename1 = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("fname", rename1.getOriginalParameter().getName());
        assertEquals("firstName", rename1.getRenamedParameter().getName());

        RenameParameterRefactoring rename2 = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("lname", rename2.getOriginalParameter().getName());
        assertEquals("lastName", rename2.getRenamedParameter().getName());
    }

    @Test
    public void testRenameParameterThreeFunctions() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            function fullName(fname, lname) {
                return fname + ' ' + lname;
            }
            
            function greet(p) {
                console.log(`Hello ${p}`);
            }
            
            const calculate = (n) => n * 2;
            """;
        fileContentsBefore.put("example.js", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            function fullName(firstName, lastName) {
                return firstName + ' ' + lastName;
            }
            
            function greet(person) {
                console.log(`Hello ${person}`);
            }
            
            const calculate = (num) => num * 2;
            """;
        fileContentsAfter.put("example.js", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify we found the correct number of refactorings
        assertEquals(4, refactorings.size(), "Should detect four parameter renames");

        // Check that we find all expected renames regardless of order
        boolean foundFname = false;
        boolean foundLname = false;
        boolean foundP = false;
        boolean foundN = false;

        for (Refactoring ref : refactorings) {
            assertTrue(ref instanceof RenameParameterRefactoring, "Should be a parameter rename refactoring");
            RenameParameterRefactoring rename = (RenameParameterRefactoring) ref;

            switch (rename.getOriginalParameter().getName()) {
                case "fname":
                    assertEquals("firstName", rename.getRenamedParameter().getName(), "fname should be renamed to firstName");
                    foundFname = true;
                    break;
                case "lname":
                    assertEquals("lastName", rename.getRenamedParameter().getName(), "lname should be renamed to lastName");
                    foundLname = true;
                    break;
                case "p":
                    assertEquals("person", rename.getRenamedParameter().getName(), "p should be renamed to person");
                    foundP = true;
                    break;
                case "n":
                    assertEquals("num", rename.getRenamedParameter().getName(), "n should be renamed to num");
                    foundN = true;
                    break;
                default:
                    fail("Unexpected parameter rename found: " + rename.getOriginalParameter().getName() +
                            " -> " + rename.getRenamedParameter().getName());
            }
        }

        // Verify we found all expected renames
        assertTrue(foundFname, "Should find fname -> firstName rename");
        assertTrue(foundLname, "Should find lname -> lastName rename");
        assertTrue(foundP, "Should find p -> person rename");
        assertTrue(foundN, "Should find n -> num rename");
    }

    @Test
    public void testRenameParameterInsideClass() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            class Square {
                calculatePerimeter(side_length) {
                    return 4 * side_length;
                }
            }
            """;
        fileContentsBefore.put("example.js", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            class Square {
                calculatePerimeter(s) {
                    return 4 * s;
                }
            }
            """;
        fileContentsAfter.put("example.js", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size(), "Should detect exactly one parameter rename");
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring,
                "Should be a parameter rename refactoring");

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("side_length", rename.getOriginalParameter().getName(),
                "Original parameter should be 'side_length'");
        assertEquals("s", rename.getRenamedParameter().getName(),
                "Renamed parameter should be 's'");
    }

    @Test
    public void testRenameParameterInConstructor() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            class Person {
                constructor(n) {
                    this.name = n;
                }
            }
            """;
        fileContentsBefore.put("example.js", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            class Person {
                constructor(name) {
                    this.name = name;
                }
            }
            """;
        fileContentsAfter.put("example.js", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size(), "Should detect exactly one parameter rename");
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring,
                "Should be a parameter rename refactoring");

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("name", rename.getRenamedParameter().getName());
    }



}

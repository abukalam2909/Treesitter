package ca.dal.treefactor;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.AddParameterRefactoring;
import ca.dal.treefactor.util.UMLModelReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PythonAddParameterTest {

    @Test
    public void testAddParameterSimple() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            def greet():
                print("Hello!")
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            def greet(name):
                print(f"Hello, {name}!")
            """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof AddParameterRefactoring);

        AddParameterRefactoring add = (AddParameterRefactoring) refactorings.get(0);
        assertEquals("name", add.getAddedParameter().getName());
    }

    @Test
    public void testAddParameterWithType() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            def calculate(x: int):
                return x + 5
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            def calculate(x: int, y: int):
                return x + y
            """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof AddParameterRefactoring);

        AddParameterRefactoring add = (AddParameterRefactoring) refactorings.get(0);
        assertEquals("y", add.getAddedParameter().getName());
        assertEquals("int", add.getAddedParameter().getType().getTypeName());
    }

    @Test
    public void testAddParameterWithDefaultValue() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            def greet(name: str):
                print(f"Hello, {name}!")
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            def greet(name: str, greeting: str = "Hello"):
                print(f"{greeting}, {name}!")
            """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof AddParameterRefactoring);

        AddParameterRefactoring add = (AddParameterRefactoring) refactorings.get(0);
        assertEquals("greeting", add.getAddedParameter().getName());
        assertEquals("str", add.getAddedParameter().getType().getTypeName());
        assertEquals("\"Hello\"", add.getDefaultValue());
    }

    @Test
    public void testAddMultipleParameters() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            def calculate(x: int):
                return x
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            def calculate(x: int, y: int, z: int = 0):
                return x + y + z
            """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(2, refactorings.size());
        assertTrue(refactorings.get(0) instanceof AddParameterRefactoring);
        assertTrue(refactorings.get(1) instanceof AddParameterRefactoring);

        AddParameterRefactoring add1 = (AddParameterRefactoring) refactorings.get(0);
        assertEquals("y", add1.getAddedParameter().getName());
        assertEquals("int", add1.getAddedParameter().getType().getTypeName());

        AddParameterRefactoring add2 = (AddParameterRefactoring) refactorings.get(1);
        assertEquals("z", add2.getAddedParameter().getName());
        assertEquals("int", add2.getAddedParameter().getType().getTypeName());
        assertEquals("0", add2.getDefaultValue());
    }

    @Test
    public void testAddParameterToMethod() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            class Calculator:
                def add(self, x: int):
                    return x
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            class Calculator:
                def add(self, x: int, y: int = 0):
                    return x + y
            """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof AddParameterRefactoring);

        AddParameterRefactoring add = (AddParameterRefactoring) refactorings.get(0);
        assertEquals("y", add.getAddedParameter().getName());
        assertEquals("int", add.getAddedParameter().getType().getTypeName());
        assertEquals("0", add.getDefaultValue());
        assertEquals("Calculator", add.getOperation().getClassName());
    }

    @Test
    public void testAddParameterWithComplexType() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            def process():
                print("Processing...")
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            def process(data: List[Dict[str, Any]] = None):
                if data is None:
                    print("Processing...")
                else:
                    print(f"Processing {len(data)} items...")
            """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof AddParameterRefactoring);

        AddParameterRefactoring add = (AddParameterRefactoring) refactorings.get(0);
        assertEquals("data", add.getAddedParameter().getName());
        assertEquals("List[Dict[str, Any]]", add.getAddedParameter().getType().getTypeName());
        assertEquals("None", add.getDefaultValue());
    }
}
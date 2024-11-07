package ca.dal.treefactor;

import ca.dal.treefactor.model.CodeElementType;
import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameParameterRefactoring;
import ca.dal.treefactor.model.elements.*;
import ca.dal.treefactor.model.diff.*;
import ca.dal.treefactor.model.diff.refactoring.*;
import ca.dal.treefactor.util.UMLModelReader;
import io.github.treesitter.jtreesitter.Point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class PythonRenameParameterTest {
    @Test
    public void RenameParameterSimple() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = "def greet(n): print(f\"Hello, {n}!\")";
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore, new HashSet<>());
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = "def greet(name): print(f\"Hello, {name}!\")";
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter, new HashSet<>());
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("name", rename.getRenamedParameter().getName());
    }

    @Test
    public void RenameParameterWithDefaultValue() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            def calculator(n = 1):
                return n + 5
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore, new HashSet<>());
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
                def calculator(num):
                	return num + 5
                """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter, new HashSet<>());
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("num", rename.getRenamedParameter().getName());
    }

    @Test
    public void RenameTypedParameter() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
                def calculator(n: int):
                	return n + 5
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore, new HashSet<>());
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
                def calculator(num: int):
                	return num + 5
                """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter, new HashSet<>());
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("num", rename.getRenamedParameter().getName());
    }

    @Test
    public void RenameTypedParameterWithDefaultValue() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
                def calculator(n: int = 4):
                	return n + 5
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore, new HashSet<>());
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
                def calculator(num: int = 5):
                	return num + 5
                """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter, new HashSet<>());
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("num", rename.getRenamedParameter().getName());
    }

    @Test
    public void RenameParameterTwoParams() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
                def calculator(n: int = 4):
                	return n + 5
            """;
        fileContentsBefore.put("example.py", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore, new HashSet<>());
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
                def calculator(num: int = 5):
                	return num + 5
                """;
        fileContentsAfter.put("example.py", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter, new HashSet<>());
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
        assertEquals("num", rename.getRenamedParameter().getName());
    }
}

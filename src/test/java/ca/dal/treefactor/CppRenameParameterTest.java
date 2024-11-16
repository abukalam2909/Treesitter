package ca.dal.treefactor;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameParameterRefactoring;
import ca.dal.treefactor.util.UMLModelReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CppRenameParameterTest {
    @Test
    public void RenameParameterSimple() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
            """;
        fileContentsBefore.put("example.cpp", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
                int calculate(int n, int y, int z) {
                return n + y + z;}
                """;
        fileContentsAfter.put("example.cpp", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("x", rename.getOriginalParameter().getName());
        assertEquals("n", rename.getRenamedParameter().getName());
    }

    @Test
    public void RenameParameterTwoParams() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
        """;
        fileContentsBefore.put("example.cpp", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
               int calculate(int a, int b, int z) {
                return a + b + z;}
        """;
        fileContentsAfter.put("example.cpp", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        // first parameter
        assertEquals(2, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename1 = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("x", rename1.getOriginalParameter().getName());
        assertEquals("a", rename1.getRenamedParameter().getName());

        // second parameter
        assertTrue(refactorings.get(1) instanceof RenameParameterRefactoring);
        RenameParameterRefactoring rename2 = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("y", rename2.getOriginalParameter().getName());
        assertEquals("b", rename2.getRenamedParameter().getName());
    }

    @Test
    public void RenameParameterThreeParams() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
        """;
        fileContentsBefore.put("example.cpp", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();


        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
               int calculate(int a, int b, int c) {
                return a + b + c;}
        """;
        fileContentsAfter.put("example.cpp", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        // Verify refactoring detection
        // first parameter
        assertEquals(3, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename1 = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("x", rename1.getOriginalParameter().getName());
        assertEquals("a", rename1.getRenamedParameter().getName());

        // second parameter
        assertTrue(refactorings.get(1) instanceof RenameParameterRefactoring);
        RenameParameterRefactoring rename2 = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("y", rename2.getOriginalParameter().getName());
        assertEquals("b", rename2.getRenamedParameter().getName());

        // third parameter
        assertTrue(refactorings.get(2) instanceof RenameParameterRefactoring);
        RenameParameterRefactoring rename3 = (RenameParameterRefactoring) refactorings.get(2);
        assertEquals("z", rename3.getOriginalParameter().getName());
        assertEquals("c", rename3.getRenamedParameter().getName());
    }

    @Test
    public void RenameParameterWithDefaultValue() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            int calculator(int n = 1) {
                return n + 5;
            }
        """;
        fileContentsBefore.put("example.cpp", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            int calculator(int num = 1) {
                return num + 5;
            }
        """;
        fileContentsAfter.put("example.cpp", fileContentsAfterString);
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
    public void RenameParameterWithConstReference() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            void process(const std::string& str) {
                std::cout << str << std::endl;
            }
        """;
        fileContentsBefore.put("example.cpp", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            void process(const std::string& input) {
                std::cout << input << std::endl;
            }
        """;
        fileContentsAfter.put("example.cpp", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(1, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("str", rename.getOriginalParameter().getName());
        assertEquals("input", rename.getRenamedParameter().getName());
    }

    @Test
    public void RenameMultipleParametersWithTypes() {
        Map<String, String> fileContentsBefore = new HashMap<>();
        String fileContentsBeforeString = """
            void fullname(const std::string& fname, const std::string& lname) {
                std::cout << fname << " " << lname << std::endl;
            }
        """;
        fileContentsBefore.put("example.cpp", fileContentsBeforeString);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        String fileContentsAfterString = """
            void fullname(const std::string& firstname, const std::string& lastname) {
                std::cout << firstname << " " << lastname << std::endl;
            }
        """;
        fileContentsAfter.put("example.cpp", fileContentsAfterString);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        List<Refactoring> refactorings = modelDiff.detectRefactorings();

        assertEquals(2, refactorings.size());
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);
        assertTrue(refactorings.get(1) instanceof RenameParameterRefactoring);

        RenameParameterRefactoring rename1 = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("fname", rename1.getOriginalParameter().getName());
        assertEquals("firstname", rename1.getRenamedParameter().getName());

        RenameParameterRefactoring rename2 = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("lname", rename2.getOriginalParameter().getName());
        assertEquals("lastname", rename2.getRenamedParameter().getName());
    }
}

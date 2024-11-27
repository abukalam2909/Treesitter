package ca.dal.treefactor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameMethodRefactoring;
import ca.dal.treefactor.util.UMLModelReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisplayName("Python Rename Method Tests")
public class PythonRenameMethodTest {
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

    @Test
    @DisplayName("Simple function rename")
    void testSimpleFunctionRename() {
        createModelsFromCode(
                """
                def calc_sum(x: int, y: int) -> int:
                    return x + y
                """,
                """
                def calculate_sum(x: int, y: int) -> int:
                    return x + y
                """
        );

        assertEquals(1, refactorings.size(), "Should detect exactly one refactoring");
        assertTrue(refactorings.get(0) instanceof RenameMethodRefactoring,
                "Should be a rename method refactoring");

        RenameMethodRefactoring rename = (RenameMethodRefactoring) refactorings.get(0);
        assertEquals("calc_sum", rename.getOriginalOperation().getName(),
                "Original method name should be 'calc_sum'");
        assertEquals("calculate_sum", rename.getRenamedOperation().getName(),
                "New method name should be 'calculate_sum'");
    }
}
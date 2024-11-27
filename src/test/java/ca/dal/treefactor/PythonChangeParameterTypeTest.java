package ca.dal.treefactor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.ChangeParameterTypeRefactoring;
import ca.dal.treefactor.util.UMLModelReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisplayName("Python Change Parameter Type Tests")
public class PythonChangeParameterTypeTest {
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
    @DisplayName("Simple parameter type change")
    void testSimpleParameterTypeChange() {
        createModelsFromCode(
                """
                def process_value(x: int) -> str:
                    return str(x)
                """,
                """
                def process_value(x: float) -> str:
                    return str(x)
                """
        );

        assertEquals(1, refactorings.size(), "Should detect exactly one refactoring");
        assertTrue(refactorings.get(0) instanceof ChangeParameterTypeRefactoring,
                "Should be a parameter type change refactoring");

        ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
        assertEquals("int", typeChange.getOriginalParameter().getType().getTypeName(),
                "Original parameter type should be 'int'");
        assertEquals("float", typeChange.getChangedParameter().getType().getTypeName(),
                "New parameter type should be 'float'");
    }

    @Test
    @DisplayName("Complex parameter type change")
    void testComplexParameterTypeChange() {
        createModelsFromCode(
                """
                def process_list(items: List[int]) -> int:
                    return sum(items)
                """,
                """
                def process_list(items: List[float]) -> int:
                    return int(sum(items))
                """
        );

        assertEquals(1, refactorings.size(), "Should detect exactly one refactoring");
        assertTrue(refactorings.get(0) instanceof ChangeParameterTypeRefactoring,
                "Should be a parameter type change refactoring");

        ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
        assertEquals("List[int]", typeChange.getOriginalParameter().getType().getTypeName(),
                "Original parameter type should be 'List[int]'");
        assertEquals("List[float]", typeChange.getChangedParameter().getType().getTypeName(),
                "New parameter type should be 'List[float]'");
    }

    @Test
    @DisplayName("Multiple parameter type changes")
    void testMultipleParameterTypeChanges() {
        createModelsFromCode(
                """
                def calculate(x: int, y: int) -> float:
                    return x + y
                """,
                """
                def calculate(x: float, y: float) -> float:
                    return x + y
                """
        );

        assertEquals(2, refactorings.size(), "Should detect two refactorings");
        assertTrue(refactorings.stream().allMatch(r -> r instanceof ChangeParameterTypeRefactoring),
                "All refactorings should be parameter type changes");

        // Verify first parameter change
        ChangeParameterTypeRefactoring firstChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
        assertEquals("int", firstChange.getOriginalParameter().getType().getTypeName(),
                "First parameter's original type should be 'int'");
        assertEquals("float", firstChange.getChangedParameter().getType().getTypeName(),
                "First parameter's new type should be 'float'");

        // Verify second parameter change
        ChangeParameterTypeRefactoring secondChange = (ChangeParameterTypeRefactoring) refactorings.get(1);
        assertEquals("int", secondChange.getOriginalParameter().getType().getTypeName(),
                "Second parameter's original type should be 'int'");
        assertEquals("float", secondChange.getChangedParameter().getType().getTypeName(),
                "Second parameter's new type should be 'float'");
    }

    @Test
    @DisplayName("Class method parameter type change")
    void testClassMethodParameterTypeChange() {
        createModelsFromCode(
                """
                class DataProcessor:
                    def process_value(self, data: Dict[str, int]) -> List[int]:
                        return list(data.values())
                """,
                """
                class DataProcessor:
                    def process_value(self, data: Dict[str, float]) -> List[float]:
                        return list(data.values())
                """
        );

        assertEquals(1, refactorings.size(), "Should detect exactly one refactoring");
        assertTrue(refactorings.get(0) instanceof ChangeParameterTypeRefactoring,
                "Should be a parameter type change refactoring");

        ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
        assertEquals("Dict[str, int]", typeChange.getOriginalParameter().getType().getTypeName(),
                "Original parameter type should be 'Dict[str, int]'");
        assertEquals("Dict[str, float]", typeChange.getChangedParameter().getType().getTypeName(),
                "New parameter type should be 'Dict[str, float]'");
    }
}
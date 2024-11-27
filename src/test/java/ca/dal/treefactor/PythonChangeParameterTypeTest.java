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

    @Nested
    @DisplayName("Simple Parameter Type Change Tests")
    class SimpleParameterTypeChangeTests {
        @BeforeEach
        void setup() {
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
        }

        @Test
        @DisplayName("Should detect single refactoring")
        void testRefactoringCount() {
            assertEquals(1, refactorings.size(),
                    "Should detect exactly one refactoring");
        }

        @Test
        @DisplayName("Should detect correct refactoring type")
        void testRefactoringType() {
            assertTrue(refactorings.get(0) instanceof ChangeParameterTypeRefactoring,
                    "Should be a parameter type change refactoring");
        }

        @Test
        @DisplayName("Should have correct original parameter type")
        void testOriginalParameterType() {
            ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
            assertEquals("int", typeChange.getOriginalParameter().getType().getTypeName(),
                    "Original parameter type should be 'int'");
        }

        @Test
        @DisplayName("Should have correct changed parameter type")
        void testChangedParameterType() {
            ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
            assertEquals("float", typeChange.getChangedParameter().getType().getTypeName(),
                    "New parameter type should be 'float'");
        }
    }

    @Nested
    @DisplayName("Complex Parameter Type Change Tests")
    class ComplexParameterTypeChangeTests {
        @BeforeEach
        void setup() {
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
        }

        @Test
        @DisplayName("Should detect single complex type refactoring")
        void testRefactoringCount() {
            assertEquals(1, refactorings.size(),
                    "Should detect exactly one refactoring");
        }

        @Test
        @DisplayName("Should detect correct complex refactoring type")
        void testRefactoringType() {
            assertTrue(refactorings.get(0) instanceof ChangeParameterTypeRefactoring,
                    "Should be a parameter type change refactoring");
        }

        @Test
        @DisplayName("Should have correct original complex parameter type")
        void testOriginalParameterType() {
            ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
            assertEquals("List[int]", typeChange.getOriginalParameter().getType().getTypeName(),
                    "Original parameter type should be 'List[int]'");
        }

        @Test
        @DisplayName("Should have correct changed complex parameter type")
        void testChangedParameterType() {
            ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
            assertEquals("List[float]", typeChange.getChangedParameter().getType().getTypeName(),
                    "New parameter type should be 'List[float]'");
        }
    }

    @Nested
    @DisplayName("Multiple Parameter Type Change Tests")
    class MultipleParameterTypeChangeTests {
        @BeforeEach
        void setup() {
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
        }

        @Test
        @DisplayName("Should detect two refactorings")
        void testRefactoringCount() {
            assertEquals(2, refactorings.size(),
                    "Should detect two refactorings");
        }

        @Test
        @DisplayName("Should detect correct refactoring types")
        void testRefactoringTypes() {
            assertTrue(refactorings.stream().allMatch(r -> r instanceof ChangeParameterTypeRefactoring),
                    "All refactorings should be parameter type changes");
        }

        @Test
        @DisplayName("First parameter should have correct types")
        void testFirstParameterTypes() {
            ChangeParameterTypeRefactoring firstChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
            assertEquals("int", firstChange.getOriginalParameter().getType().getTypeName(),
                    "First parameter's original type should be 'int'");
            assertEquals("float", firstChange.getChangedParameter().getType().getTypeName(),
                    "First parameter's new type should be 'float'");
        }

        @Test
        @DisplayName("Second parameter should have correct types")
        void testSecondParameterTypes() {
            ChangeParameterTypeRefactoring secondChange = (ChangeParameterTypeRefactoring) refactorings.get(1);
            assertEquals("int", secondChange.getOriginalParameter().getType().getTypeName(),
                    "Second parameter's original type should be 'int'");
            assertEquals("float", secondChange.getChangedParameter().getType().getTypeName(),
                    "Second parameter's new type should be 'float'");
        }
    }

    @Nested
    @DisplayName("Class Method Parameter Type Change Tests")
    class ClassMethodParameterTypeChangeTests {
        @BeforeEach
        void setup() {
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
        }

        @Test
        @DisplayName("Should detect single class method refactoring")
        void testRefactoringCount() {
            assertEquals(1, refactorings.size(),
                    "Should detect exactly one refactoring");
        }

        @Test
        @DisplayName("Should detect correct class method refactoring type")
        void testRefactoringType() {
            assertTrue(refactorings.get(0) instanceof ChangeParameterTypeRefactoring,
                    "Should be a parameter type change refactoring");
        }

        @Test
        @DisplayName("Should have correct original class method parameter type")
        void testOriginalParameterType() {
            ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
            assertEquals("Dict[str, int]", typeChange.getOriginalParameter().getType().getTypeName(),
                    "Original parameter type should be 'Dict[str, int]'");
        }

        @Test
        @DisplayName("Should have correct changed class method parameter type")
        void testChangedParameterType() {
            ChangeParameterTypeRefactoring typeChange = (ChangeParameterTypeRefactoring) refactorings.get(0);
            assertEquals("Dict[str, float]", typeChange.getChangedParameter().getType().getTypeName(),
                    "New parameter type should be 'Dict[str, float]'");
        }
    }
}
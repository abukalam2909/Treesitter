package ca.dal.treefactor;

import ca.dal.treefactor.model.CodeElementType;
import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.diff.mappers.UMLOperationBodyMapper;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameParameterRefactoring;
import ca.dal.treefactor.model.elements.*;
import io.github.treesitter.jtreesitter.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UMLOperationBodyMapperTest {
    private static final String TEST_FILE = "test.py";

    @Test
    void identicalStatementsShouldHavePerfectSimilarityScore() {
        UMLOperation op1 = createOperationWithBody(
                "print('hello')\nx = 5",
                new Point(1, 0), new Point(2, 5)
        );
        UMLOperation op2 = createOperationWithBody(
                "print('hello')\nx = 5",
                new Point(1, 0), new Point(2, 5)
        );

        UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(op1, op2);
        assertEquals(1.0, mapper.bodyComparatorScore(),
                "Identical statements should have perfect similarity score");
    }

    @Nested
    class ParameterRenameTests {
        @Test
        void shouldDetectSingleParameterRename() {
            UMLOperation op1 = createOperationWithBody(
                    "print(f'Hello, {name}!')",
                    new Point(1, 0), new Point(1, 23)
            );
            op1.addParameter(createParameter("name", op1));

            UMLOperation op2 = createOperationWithBody(
                    "print(f'Hello, {person}!')",
                    new Point(1, 0), new Point(1, 25)
            );
            op2.addParameter(createParameter("person", op2));

            UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(op1, op2);
            List<Refactoring> refactorings = mapper.getRefactorings();

            assertEquals(1, refactorings.size(),
                    "Should detect exactly one refactoring");
        }

        @Test
        void shouldCorrectlyIdentifyOriginalAndRenamedParameters() {
            UMLOperation op1 = createOperationWithBody(
                    "print(f'Hello, {name}!')",
                    new Point(1, 0), new Point(1, 23)
            );
            op1.addParameter(createParameter("name", op1));

            UMLOperation op2 = createOperationWithBody(
                    "print(f'Hello, {person}!')",
                    new Point(1, 0), new Point(1, 25)
            );
            op2.addParameter(createParameter("person", op2));

            UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(op1, op2);
            RenameParameterRefactoring rename = (RenameParameterRefactoring) mapper.getRefactorings().get(0);

            assertEquals("name", rename.getOriginalParameter().getName(),
                    "Original parameter should be 'name'");
            assertEquals("person", rename.getRenamedParameter().getName(),
                    "Renamed parameter should be 'person'");
        }
    }

    @Nested
    class PythonSelfParameterTests {
        @Test
        void shouldDetectSingleParameterRenameIgnoringSelf() {
            UMLOperation op1 = createOperationWithBody(
                    "self.value = x",
                    new Point(1, 0), new Point(1, 14)
            );
            op1.addParameter(createParameter("self", op1));
            op1.addParameter(createParameter("x", op1));

            UMLOperation op2 = createOperationWithBody(
                    "self.value = val",
                    new Point(1, 0), new Point(1, 16)
            );
            op2.addParameter(createParameter("self", op2));
            op2.addParameter(createParameter("val", op2));

            UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(op1, op2);
            List<Refactoring> refactorings = mapper.getRefactorings();

            assertEquals(1, refactorings.size(),
                    "Should detect exactly one refactoring when ignoring self parameter");
        }

        @Test
        void shouldCorrectlyIdentifyParameterRenameIgnoringSelf() {
            UMLOperation op1 = createOperationWithBody(
                    "self.value = x",
                    new Point(1, 0), new Point(1, 14)
            );
            op1.addParameter(createParameter("self", op1));
            op1.addParameter(createParameter("x", op1));

            UMLOperation op2 = createOperationWithBody(
                    "self.value = val",
                    new Point(1, 0), new Point(1, 16)
            );
            op2.addParameter(createParameter("self", op2));
            op2.addParameter(createParameter("val", op2));

            UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(op1, op2);
            RenameParameterRefactoring rename = (RenameParameterRefactoring) mapper.getRefactorings().get(0);

            assertEquals("x", rename.getOriginalParameter().getName(),
                    "Original parameter should be 'x'");
            assertEquals("val", rename.getRenamedParameter().getName(),
                    "Renamed parameter should be 'val'");
        }
    }

    private UMLOperation createOperationWithBody(String body, Point start, Point end) {
        LocationInfo location = new LocationInfo(TEST_FILE, start, end,
                CodeElementType.METHOD_DECLARATION);
        UMLOperation operation = UMLOperation.builder("test", location)
                .returnType(new UMLType("None"))
                .body(body)
                .build();
        return operation;
    }

    private UMLParameter createParameter(String name, UMLOperation operation) {
        LocationInfo location = new LocationInfo(TEST_FILE,
                operation.getLocationInfo().getStartPoint(),
                operation.getLocationInfo().getEndPoint(),
                CodeElementType.PARAMETER_DECLARATION
        );
        return new UMLParameter(name, new UMLType("object"), location);
    }
}
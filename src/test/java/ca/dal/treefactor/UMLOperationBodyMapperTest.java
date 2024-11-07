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
    void testExactStatementMatch() {
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

    @Test
    void testParameterRenameInStatements() {
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

        assertEquals(1, refactorings.size(), "Should detect one parameter rename");
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("name", rename.getOriginalParameter().getName());
        assertEquals("person", rename.getRenamedParameter().getName());
    }

    @Test
    void testPythonSelfParameterHandling() {
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

        assertEquals(1, refactorings.size(), "Should detect parameter rename ignoring self");
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("x", rename.getOriginalParameter().getName());
        assertEquals("val", rename.getRenamedParameter().getName());
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
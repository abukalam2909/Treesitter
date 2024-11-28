package ca.dal.treefactor.unitTest;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.util.UMLModelReader;

public class UMLModelReaderTest {
    private UMLModel model;
    private UMLClass umlClass;

    @BeforeEach
    void setUp() {
        // Prepare test data
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("example.py", "class Example:\n    def method(self):\n        pass");

        // Create UMLModelReader and generate UMLModel
        UMLModelReader umlReader = new UMLModelReader(fileContents);
        model = umlReader.getUmlModel();
        if (!model.getClasses().isEmpty()) {
            umlClass = model.getClasses().get(0);
        }
    }

    @Nested
    class ClassLevelTests {
        @Test
        void shouldCreateCorrectNumberOfClasses() {
            assertEquals(1, model.getClasses().size(),
                    "Model should contain exactly one class");
        }

        @Test
        void shouldHaveCorrectClassName() {
            assertEquals("Example", umlClass.getName(),
                    "Class name should be 'Example'");
        }

        @Test
        void shouldHaveNoAttributes() {
            assertEquals(0, umlClass.getAttributes().size(),
                    "Class should have no attributes");
        }
    }

    @Nested
    class MethodLevelTests {
        @Test
        void shouldHaveCorrectNumberOfMethods() {
            assertEquals(1, umlClass.getOperations().size(),
                    "Class should have exactly one method");
        }

        @Test
        void shouldHaveCorrectMethodName() {
            UMLOperation operation = umlClass.getOperations().get(0);
            assertEquals("method", operation.getName(),
                    "Method name should be 'method'");
        }

        @Test
        void shouldHaveCorrectReturnType() {
            UMLOperation operation = umlClass.getOperations().get(0);
            assertEquals("None", operation.getReturnType().getTypeName(),
                    "Method return type should be 'None'");
        }
    }
}
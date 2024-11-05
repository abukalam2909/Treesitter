package ca.dal.treefactor;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.elements.UMLAttribute;
import ca.dal.treefactor.util.UMLModelReader;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class UMLModelReaderTest {

    @Test
    public void testUMLModelGeneration() {
        // Prepare test data
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("example.py", "class Example:\n    def method(self):\n        pass");

        // Create UMLModelReader and generate UMLModel
        UMLModelReader umlReader = new UMLModelReader(fileContents, new HashSet<>());
        UMLModel model = umlReader.getUmlModel();

        // Verify the UML model
        assertEquals(1, model.getClasses().size(), "There should be one class in the model");

        UMLClass umlClass = model.getClasses().get(0);
        assertEquals("Example", umlClass.getName(), "The class name should be 'Example'");

        assertEquals(1, umlClass.getOperations().size(), "There should be one method in the class");
        UMLOperation operation = umlClass.getOperations().get(0);
        assertEquals("method", operation.getName(), "The method name should be 'method'");
        assertEquals("void", operation.getReturnType().getTypeName(), "The return type should be 'void'");

        assertEquals(0, operation.getParameters().size(), "The method should have no parameters");
        assertEquals(0, umlClass.getAttributes().size(), "The class should have no attributes");
    }
}

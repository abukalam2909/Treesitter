package ca.dal.treefactor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.dal.treefactor.model.CodeElementType;
import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.core.LocationInfo;
import ca.dal.treefactor.model.core.UMLParameter;
import ca.dal.treefactor.model.core.UMLType;
import ca.dal.treefactor.model.diff.UMLModelDiff;
import ca.dal.treefactor.model.diff.refactoring.Refactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameParameterRefactoring;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;
import io.github.treesitter.jtreesitter.Point;

class UMLModelDiffTest {
    private static final String TEST_FILE = "test.py";
    private static final Set<String> REPO_DIR = Set.of("/test/repo");

    @Nested
    class PythonRefactoringTests {
        private UMLModel oldModel;
        private UMLModel newModel;
        private UMLModelDiff modelDiff;

        @BeforeEach
        void setUp() {
            oldModel = new UMLModel("python");
            newModel = new UMLModel("python");
        }

        @Test
        void testParameterRename() {
            // Test case for:
            // Old: def greet(n): print(f"Hello, {n}!")
            // New: def greet(name): print(f"Hello, {name}!")

            // Create locations using Point objects
            LocationInfo oldLoc = new LocationInfo(TEST_FILE,
                    new Point(1, 0), new Point(1, 30),
                    CodeElementType.METHOD_DECLARATION);

            // Create old operation
            UMLOperation oldOperation = UMLOperation.builder("greet", oldLoc)
                    .returnType(new UMLType("None"))
                    .body("print(f\"Hello, {n}!\")")
                    .build();

            // Add parameter to old operation
            LocationInfo oldParamLoc = new LocationInfo(TEST_FILE,
                    new Point(1, 8), new Point(1, 9),
                    CodeElementType.PARAMETER_DECLARATION);
            UMLParameter oldParam = new UMLParameter("n", new UMLType("object"), oldParamLoc);
            oldOperation.addParameter(oldParam);

            // Add operation to old model
            oldModel.addOperation(oldOperation);
            oldModel.addSourceFileContent(TEST_FILE,
                    "def greet(n):\n    print(f\"Hello, {n}!\")\n");

            // Create new operation
            LocationInfo newLoc = new LocationInfo(TEST_FILE,
                    new Point(1, 0), new Point(1, 32),
                    CodeElementType.METHOD_DECLARATION);

            UMLOperation newOperation = UMLOperation.builder("greet", newLoc)
                    .returnType(new UMLType("None"))
                    .body("print(f\"Hello, {name}!\")")
                    .build();

            // Add parameter to new operation
            LocationInfo newParamLoc = new LocationInfo(TEST_FILE,
                    new Point(1, 8), new Point(1, 12),
                    CodeElementType.PARAMETER_DECLARATION);
            UMLParameter newParam = new UMLParameter("name", new UMLType("object"), newParamLoc);
            newOperation.addParameter(newParam);

            // Add operation to new model
            newModel.addOperation(newOperation);
            newModel.addSourceFileContent(TEST_FILE,
                    "def greet(name):\n    print(f\"Hello, {name}!\")\n");

            // Perform diff
            modelDiff = new UMLModelDiff(oldModel, newModel);
            List<Refactoring> refactorings = modelDiff.detectRefactorings();

            // Verify refactoring detection
            assertEquals(1, refactorings.size());
            assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

            RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
            assertEquals("n", rename.getOriginalParameter().getName());
            assertEquals("name", rename.getRenamedParameter().getName());
            assertEquals(TEST_FILE, rename.getOriginalParameter().getLocationInfo().getFilePath());
        }

        @Test
        void testParameterRenameInClass() {
            // Test case for:
            // class Greeter:
            //     def greet(self, n): print(f"Hello, {n}!")
            // class Greeter:
            //     def greet(self, name): print(f"Hello, {name}!")

            // Create class locations
            LocationInfo oldClassLoc = new LocationInfo(TEST_FILE,
                    new Point(1, 0), new Point(2, 40),
                    CodeElementType.CLASS_DECLARATION);
            LocationInfo newClassLoc = new LocationInfo(TEST_FILE,
                    new Point(1, 0), new Point(2, 44),
                    CodeElementType.CLASS_DECLARATION);

            // Create old class
            UMLClass oldClass = UMLClass.builder("Greeter", oldClassLoc)
                    .packageName("")
                    .build();

            // Create old method
            LocationInfo oldMethodLoc = new LocationInfo(TEST_FILE,
                    new Point(2, 4), new Point(2, 39),
                    CodeElementType.METHOD_DECLARATION);

            UMLOperation oldOperation = UMLOperation.builder("greet", oldMethodLoc)
                    .className("Greeter")
                    .returnType(new UMLType("None"))
                    .body("print(f\"Hello, {n}!\")")
                    .build();

            // Add parameters to old method
            LocationInfo oldSelfLoc = new LocationInfo(TEST_FILE,
                    new Point(2, 9), new Point(2, 13),
                    CodeElementType.PARAMETER_DECLARATION);
            LocationInfo oldParamLoc = new LocationInfo(TEST_FILE,
                    new Point(2, 15), new Point(2, 16),
                    CodeElementType.PARAMETER_DECLARATION);

            oldOperation.addParameter(new UMLParameter("self", new UMLType("object"), oldSelfLoc));
            oldOperation.addParameter(new UMLParameter("n", new UMLType("object"), oldParamLoc));

            oldClass.addOperation(oldOperation);
            oldModel.addClass(oldClass);

            // Create new class
            UMLClass newClass = UMLClass.builder("Greeter", newClassLoc)
                    .packageName("")
                    .build();

            // Create new method
            LocationInfo newMethodLoc = new LocationInfo(TEST_FILE,
                    new Point(2, 4), new Point(2, 43),
                    CodeElementType.METHOD_DECLARATION);

            UMLOperation newOperation = UMLOperation.builder("greet", newMethodLoc)
                    .className("Greeter")
                    .returnType(new UMLType("None"))
                    .body("print(f\"Hello, {name}!\")")
                    .build();

            // Add parameters to new method
            LocationInfo newSelfLoc = new LocationInfo(TEST_FILE,
                    new Point(2, 9), new Point(2, 13),
                    CodeElementType.PARAMETER_DECLARATION);
            LocationInfo newParamLoc = new LocationInfo(TEST_FILE,
                    new Point(2, 15), new Point(2, 19),
                    CodeElementType.PARAMETER_DECLARATION);

            newOperation.addParameter(new UMLParameter("self", new UMLType("object"), newSelfLoc));
            newOperation.addParameter(new UMLParameter("name", new UMLType("object"), newParamLoc));

            newClass.addOperation(newOperation);
            newModel.addClass(newClass);

            // Add source content
            oldModel.addSourceFileContent(TEST_FILE, """
                class Greeter:
                    def greet(self, n):
                        print(f"Hello, {n}!")
                """);
            newModel.addSourceFileContent(TEST_FILE, """
                class Greeter:
                    def greet(self, name):
                        print(f"Hello, {name}!")
                """);

            // Perform diff
            modelDiff = new UMLModelDiff(oldModel, newModel);
            List<Refactoring> refactorings = modelDiff.detectRefactorings();

            // Verify refactoring detection
            assertEquals(1, refactorings.size());
            assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);

            RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
            assertEquals("n", rename.getOriginalParameter().getName());
            assertEquals("name", rename.getRenamedParameter().getName());
            assertEquals("Greeter", rename.getOperation().getClassName());
        }


    }
}
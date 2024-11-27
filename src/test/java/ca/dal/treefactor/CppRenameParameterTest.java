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

    private List<Refactoring> setupAndGetRefactorings(String beforeCode, String afterCode) {
        Map<String, String> fileContentsBefore = new HashMap<>();
        fileContentsBefore.put("example.cpp", beforeCode);
        UMLModelReader parentUmlReader = new UMLModelReader(fileContentsBefore);
        UMLModel parentUMLModel = parentUmlReader.getUmlModel();

        Map<String, String> fileContentsAfter = new HashMap<>();
        fileContentsAfter.put("example.cpp", afterCode);
        UMLModelReader currentUmlReader = new UMLModelReader(fileContentsAfter);
        UMLModel currentUMLModel = currentUmlReader.getUmlModel();

        UMLModelDiff modelDiff = new UMLModelDiff(parentUMLModel, currentUMLModel);
        return modelDiff.detectRefactorings();
    }

    // Simple Parameter Tests
    @Test
    public void testSimpleRenameRefactoringCount() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
            """;
        String afterCode = """
                int calculate(int n, int y, int z) {
                return n + y + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertEquals(1, refactorings.size());
    }

    @Test
    public void testSimpleRenameRefactoringType() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
            """;
        String afterCode = """
                int calculate(int n, int y, int z) {
                return n + y + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertTrue(refactorings.get(0) instanceof RenameParameterRefactoring);
    }

    @Test
    public void testSimpleRenameOriginalName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
            """;
        String afterCode = """
                int calculate(int n, int y, int z) {
                return n + y + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("x", rename.getOriginalParameter().getName());
    }

    @Test
    public void testSimpleRenameNewName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
            """;
        String afterCode = """
                int calculate(int n, int y, int z) {
                return n + y + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getRenamedParameter().getName());
    }

    // Two Parameters Tests
    @Test
    public void testTwoParamsRefactoringCount() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int z) {
                return a + b + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertEquals(2, refactorings.size());
    }

    @Test
    public void testTwoParamsFirstParameterOriginalName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int z) {
                return a + b + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("x", rename.getOriginalParameter().getName());
    }

    @Test
    public void testTwoParamsFirstParameterNewName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int z) {
                return a + b + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("a", rename.getRenamedParameter().getName());
    }

    @Test
    public void testTwoParamsSecondParameterOriginalName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int z) {
                return a + b + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("y", rename.getOriginalParameter().getName());
    }

    @Test
    public void testTwoParamsSecondParameterNewName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int z) {
                return a + b + z;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("b", rename.getRenamedParameter().getName());
    }

    // Three Parameters Tests
    @Test
    public void testThreeParamsRefactoringCount() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int c) {
                return a + b + c;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertEquals(3, refactorings.size());
    }

    @Test
    public void testThreeParamsFirstParameterNames() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int c) {
                return a + b + c;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("x", rename.getOriginalParameter().getName());
    }

    @Test
    public void testThreeParamsFirstParameterNewName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int c) {
                return a + b + c;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("a", rename.getRenamedParameter().getName());
    }

    @Test
    public void testThreeParamsSecondParameterNames() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int c) {
                return a + b + c;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("y", rename.getOriginalParameter().getName());
    }

    @Test
    public void testThreeParamsSecondParameterNewName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int c) {
                return a + b + c;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("b", rename.getRenamedParameter().getName());
    }

    @Test
    public void testThreeParamsThirdParameterNames() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int c) {
                return a + b + c;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(2);
        assertEquals("z", rename.getOriginalParameter().getName());
    }

    @Test
    public void testThreeParamsThirdParameterNewName() {
        String beforeCode = """
                int calculate(int x, int y, int z) {
                return x + y + z;}
                """;
        String afterCode = """
                int calculate(int a, int b, int c) {
                return a + b + c;}
                """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(2);
        assertEquals("c", rename.getRenamedParameter().getName());
    }

    // Parameter with Default Value Tests
    @Test
    public void testDefaultValueRefactoringCount() {
        String beforeCode = """
            int calculator(int n = 1) {
                return n + 5;
            }
            """;
        String afterCode = """
            int calculator(int num = 1) {
                return num + 5;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertEquals(1, refactorings.size());
    }

    @Test
    public void testDefaultValueOriginalName() {
        String beforeCode = """
            int calculator(int n = 1) {
                return n + 5;
            }
            """;
        String afterCode = """
            int calculator(int num = 1) {
                return num + 5;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("n", rename.getOriginalParameter().getName());
    }

    @Test
    public void testDefaultValueNewName() {
        String beforeCode = """
            int calculator(int n = 1) {
                return n + 5;
            }
            """;
        String afterCode = """
            int calculator(int num = 1) {
                return num + 5;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("num", rename.getRenamedParameter().getName());
    }

    // Const Reference Parameter Tests
    @Test
    public void testConstReferenceRefactoringCount() {
        String beforeCode = """
            void process(const std::string& str) {
                std::cout << str << std::endl;
            }
            """;
        String afterCode = """
            void process(const std::string& input) {
                std::cout << input << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertEquals(1, refactorings.size());
    }

    @Test
    public void testConstReferenceOriginalName() {
        String beforeCode = """
            void process(const std::string& str) {
                std::cout << str << std::endl;
            }
            """;
        String afterCode = """
            void process(const std::string& input) {
                std::cout << input << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("str", rename.getOriginalParameter().getName());
    }

    @Test
    public void testConstReferenceNewName() {
        String beforeCode = """
            void process(const std::string& str) {
                std::cout << str << std::endl;
            }
            """;
        String afterCode = """
            void process(const std::string& input) {
                std::cout << input << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("input", rename.getRenamedParameter().getName());
    }

    // Multiple Parameters with Types Tests
    @Test
    public void testMultipleTypedParamsRefactoringCount() {
        String beforeCode = """
            void fullname(const std::string& fname, const std::string& lname) {
                std::cout << fname << " " << lname << std::endl;
            }
            """;
        String afterCode = """
            void fullname(const std::string& firstname, const std::string& lastname) {
                std::cout << firstname << " " << lastname << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertEquals(2, refactorings.size());
    }

    @Test
    public void testMultipleTypedParamsFirstOriginalName() {
        String beforeCode = """
            void fullname(const std::string& fname, const std::string& lname) {
                std::cout << fname << " " << lname << std::endl;
            }
            """;
        String afterCode = """
            void fullname(const std::string& firstname, const std::string& lastname) {
                std::cout << firstname << " " << lastname << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("fname", rename.getOriginalParameter().getName());
    }

    @Test
    public void testMultipleTypedParamsFirstNewName() {
        String beforeCode = """
            void fullname(const std::string& fname, const std::string& lname) {
                std::cout << fname << " " << lname << std::endl;
            }
            """;
        String afterCode = """
            void fullname(const std::string& firstname, const std::string& lastname) {
                std::cout << firstname << " " << lastname << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("firstname", rename.getRenamedParameter().getName());
    }

    @Test
    public void testMultipleTypedParamsSecondOriginalName() {
        String beforeCode = """
            void fullname(const std::string& fname, const std::string& lname) {
                std::cout << fname << " " << lname << std::endl;
            }
            """;
        String afterCode = """
            void fullname(const std::string& firstname, const std::string& lastname) {
                std::cout << firstname << " " << lastname << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("lname", rename.getOriginalParameter().getName());
    }

    @Test
    public void testMultipleTypedParamsSecondNewName() {
        String beforeCode = """
            void fullname(const std::string& fname, const std::string& lname) {
                std::cout << fname << " " << lname << std::endl;
            }
            """;
        String afterCode = """
            void fullname(const std::string& firstname, const std::string& lastname) {
                std::cout << firstname << " " << lastname << std::endl;
            }
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(1);
        assertEquals("lastname", rename.getRenamedParameter().getName());
    }

    // Parameters Inside Class Tests
    @Test
    public void testClassMethodParameterRefactoringCount() {
        String beforeCode = """
            class Square {
                public:
                int calculatePerimeter(int s) {
                    return 4 * s;
                }
            };
            """;
        String afterCode = """
            class Square {
                public:
                int calculatePerimeter(int sideLength) {
                    return 4 * sideLength;
                }
            };
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        assertEquals(1, refactorings.size());
    }

    @Test
    public void testClassMethodParameterOriginalName() {
        String beforeCode = """
            class Square {
                public:
                int calculatePerimeter(int s) {
                    return 4 * s;
                }
            };
            """;
        String afterCode = """
            class Square {
                public:
                int calculatePerimeter(int sideLength) {
                    return 4 * sideLength;
                }
            };
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("s", rename.getOriginalParameter().getName());
    }

    @Test
    public void testClassMethodParameterNewName() {
        String beforeCode = """
            class Square {
                public:
                int calculatePerimeter(int s) {
                    return 4 * s;
                }
            };
            """;
        String afterCode = """
            class Square {
                public:
                int calculatePerimeter(int sideLength) {
                    return 4 * sideLength;
                }
            };
            """;

        List<Refactoring> refactorings = setupAndGetRefactorings(beforeCode, afterCode);
        RenameParameterRefactoring rename = (RenameParameterRefactoring) refactorings.get(0);
        assertEquals("sideLength", rename.getRenamedParameter().getName());
    }
}

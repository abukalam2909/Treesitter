package ca.dal.treefactor;

import ca.dal.treefactor.util.ASTUtil;
import ca.dal.treefactor.util.TreeSitterUtil;
import io.github.treesitter.jtreesitter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class ASTUtilTest {
    private Parser parser;
    private Language pythonLanguage;
    private String sampleCode;
    private ASTUtil.ASTNode astRoot;

    @BeforeEach
    void setUp() throws IOException {
        sampleCode = "print(\"Hello World!\")";
        pythonLanguage = TreeSitterUtil.loadLanguageForFileExtension("example.py");
        parser = new Parser();
        parser.setLanguage(pythonLanguage);

        try (Tree tree = parser.parse(sampleCode, InputEncoding.UTF_8).orElseThrow()) {
            Node rootNode = tree.getRootNode();
            astRoot = ASTUtil.buildASTWithCursor(rootNode);
        }
    }

    @Test
    public void testASTRootShouldNotBeNull() {
        assertNotNull(astRoot, "AST root should not be null");
    }

    @Test
    public void testASTRootShouldBeModule() {
        assertEquals("module", astRoot.getType(), "Root node should be a module");
    }

    @Test
    public void testModuleShouldHaveOneChild() {
        assertEquals(1, astRoot.getChildren().size(), "Module should have one child");
    }

    @Test
    public void testFirstNodeShouldBeExpressionStatement() {
        ASTUtil.ASTNode statementNode = astRoot.getChildren().get(0);
        assertEquals("expression_statement", statementNode.getType(),
                "First node should be an expression statement");
    }

    @Test
    public void testExpressionStatementShouldHaveOneChild() {
        ASTUtil.ASTNode statementNode = astRoot.getChildren().get(0);
        assertEquals(1, statementNode.getChildren().size(),
                "Expression statement should have one child");
    }

    @Test
    public void testCallNodeType() {
        ASTUtil.ASTNode callNode = astRoot.getChildren().get(0).getChildren().get(0);
        assertEquals("call", callNode.getType(), "Node should be a function call");
    }

    @Test
    public void testCallNodeChildCount() {
        ASTUtil.ASTNode callNode = astRoot.getChildren().get(0).getChildren().get(0);
        assertEquals(2, callNode.getChildren().size(), "Call node should have two children");
    }

    @Test
    public void testFunctionIdentifierType() {
        ASTUtil.ASTNode functionNode = astRoot.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0);
        assertEquals("identifier", functionNode.getType(), "First child should be an identifier");
    }

    @Test
    public void testFunctionIdentifierValue() {
        ASTUtil.ASTNode functionNode = astRoot.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0);
        assertEquals("print", functionNode.getText(sampleCode), "Function name should be 'print'");
    }

    @Test
    public void testArgumentListNodeType() {
        ASTUtil.ASTNode argumentListNode = astRoot.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(1);
        assertEquals("argument_list", argumentListNode.getType(),
                "Node should be an argument list");
    }

    @Test
    public void testArgumentListChildCount() {
        ASTUtil.ASTNode argumentListNode = astRoot.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(1);
        assertEquals(1, argumentListNode.getChildren().size(),
                "Argument list should have one child");
    }

    @Test
    public void testStringArgumentType() {
        ASTUtil.ASTNode stringNode = astRoot.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(1)
                .getChildren().get(0);
        assertEquals("string", stringNode.getType(), "Argument should be a string");
    }

    @Test
    public void testStringArgumentValue() {
        ASTUtil.ASTNode stringNode = astRoot.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(1)
                .getChildren().get(0);
        assertEquals("\"Hello World!\"", stringNode.getText(sampleCode),
                "String content should match");
    }

    @Test
    public void testPrintedASTNotNull() {
        String printedAST = ASTUtil.printAST(astRoot, 0);
        assertNotNull(printedAST, "Printed AST should not be null");
    }

    @Test
    public void testPrintedASTContent() {
        String printedAST = ASTUtil.printAST(astRoot, 0);
        assertTrue(printedAST.contains("module"),
                "Printed AST should contain essential nodes");
    }

    private ASTUtil.ASTNode getNodeAtPath(int... indices) {
        ASTUtil.ASTNode current = astRoot;
        for (int index : indices) {
            current = current.getChildren().get(index);
        }
        return current;
    }
}
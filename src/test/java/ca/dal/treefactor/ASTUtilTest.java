package ca.dal.treefactor;

import ca.dal.treefactor.util.ASTUtil;
import ca.dal.treefactor.util.TreeSitterUtil;
import io.github.treesitter.jtreesitter.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class ASTUtilTest {
    @Test
    public void testPrintHelloWorldAST() throws IOException {
        String code = "print(\"Hello World!\")";
        Language language = TreeSitterUtil.loadLanguageForFileExtension("example.py");

        try (Parser parser = new Parser()) {
            parser.setLanguage(language);
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astRoot = ASTUtil.buildASTWithCursor(rootNode);

                assertNotNull(astRoot);
                assertEquals("module", astRoot.getType());
                assertEquals(1, astRoot.getChildren().size());

                ASTUtil.ASTNode statementNode = astRoot.getChildren().get(0);
                assertEquals("expression_statement", statementNode.getType());
                assertEquals(1, statementNode.getChildren().size());

                ASTUtil.ASTNode callNode = statementNode.getChildren().get(0);
                assertEquals("call", callNode.getType());
                assertEquals(2, callNode.getChildren().size());

                ASTUtil.ASTNode functionNode = callNode.getChildren().get(0);
                assertEquals("identifier", functionNode.getType());
                assertEquals("print", functionNode.getText(code));

                ASTUtil.ASTNode argumentListNode = callNode.getChildren().get(1);
                assertEquals("argument_list", argumentListNode.getType());
                assertEquals(1, argumentListNode.getChildren().size());

                ASTUtil.ASTNode stringNode = argumentListNode.getChildren().get(0);
                assertEquals("string", stringNode.getType());
                assertEquals("\"Hello World!\"", stringNode.getText(code));

                // Print the AST for visualization
                System.out.println(ASTUtil.printAST(astRoot, 0));
            }
        }
    }


}
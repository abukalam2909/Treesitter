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
                assertEquals("module", astRoot.type);
                assertEquals(1, astRoot.children.size());

                ASTUtil.ASTNode statementNode = astRoot.children.get(0);
                assertEquals("expression_statement", statementNode.type);
                assertEquals(1, statementNode.children.size());

                ASTUtil.ASTNode callNode = statementNode.children.get(0);
                assertEquals("call", callNode.type);
                assertEquals(2, callNode.children.size());

                ASTUtil.ASTNode functionNode = callNode.children.get(0);
                assertEquals("identifier", functionNode.type);
                assertEquals("print", functionNode.getText(code));

                ASTUtil.ASTNode argumentListNode = callNode.children.get(1);
                assertEquals("argument_list", argumentListNode.type);
                assertEquals(1, argumentListNode.children.size());

                ASTUtil.ASTNode stringNode = argumentListNode.children.get(0);
                assertEquals("string", stringNode.type);
                assertEquals("\"Hello World!\"", stringNode.getText(code));

                // Print the AST for visualization
                System.out.println(ASTUtil.printAST(astRoot, 0));
            }
        }
    }


}
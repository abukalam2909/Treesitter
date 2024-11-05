package ca.dal.treefactor.util;

import ca.dal.treefactor.model.UMLModel;


import io.github.treesitter.jtreesitter.*;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class UMLModelReader {
    private UMLModel umlModel;

    public UMLModelReader(Map<String, String> fileContents, Set<String> repositoryDirectories) {
        this.umlModel = new UMLModel(repositoryDirectories);
        processFileContents(fileContents);
    }

    private void processFileContents(Map<String, String> fileContents) {
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();

            try {
                // Use your existing TreeSitterUtil to get language and generate AST
                Language language = TreeSitterUtil.loadLanguageForFileExtension(filePath);
                String astString = TreeSitterUtil.generateAST(language, content);

                // Process the AST using appropriate visitor based on file type
                processAST(filePath, content, language);

            } catch (Exception e) {
                System.err.println("Error processing file: " + filePath);
                e.printStackTrace();
            }
        }
    }

    private void processAST(String filePath, String content, Language language) throws Exception {
        try (Parser parser = new Parser()) {
            parser.setLanguage(language);
            try (Tree tree = parser.parse(content, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astRoot = ASTUtil.buildASTWithCursor(rootNode);

                // Create and use appropriate visitor based on file extension
                ASTVisitor visitor = createVisitor(filePath, content);
                if (visitor != null) {
                    visitor.visit(astRoot);
                }
            }
        }
    }

    private ASTVisitor createVisitor(String filePath, String content) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "py":
                return new PythonASTVisitor(umlModel, content, filePath);
            // case "cpp":
            //     return new CPPASTVisitor(umlModel);
            // case "js":
            //     return new JavaScriptASTVisitor(umlModel);
            default:
                return null;
        }
    }


    public UMLModel getUmlModel() {
        return this.umlModel;
    }
}
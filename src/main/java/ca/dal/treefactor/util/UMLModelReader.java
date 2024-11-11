package ca.dal.treefactor.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.dal.treefactor.model.UMLModel;
import io.github.treesitter.jtreesitter.InputEncoding;
import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;

public class UMLModelReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(UMLModelReader.class);
    private final UMLModel umlModel;
    private static final String PYTHON_EXT = "py";
    private static final String CPP_EXT = "cpp";
    private static final String JS_EXT = "js";

    public UMLModelReader(Map<String, String> fileContents, Set<String> repositoryDirectories) {
        // Initialize UMLModel with language detection
        String primaryLanguage = detectPrimaryLanguage(fileContents);
        this.umlModel = new UMLModel(repositoryDirectories, primaryLanguage);
        processFileContents(fileContents);
    }

    private String detectPrimaryLanguage(Map<String, String> fileContents) {
        // Simple language detection based on file extensions
        Map<String, Integer> langCount = new HashMap<>();

        for (String filePath : fileContents.keySet()) {
            String ext = getFileExtension(filePath);
            langCount.merge(ext, 1, Integer::sum);
        }

        return langCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> mapExtensionToLanguage(entry.getKey()))
                .orElse("unknown");
    }

    private void processFileContents(Map<String, String> fileContents) {
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();

            try {
                Language language = TreeSitterUtil.loadLanguageForFileExtension(filePath);
                processAST(filePath, content, language);
            } catch (IOException e) {
                LOGGER.error("Error loading language for file: " + filePath, e);
            } catch (Exception e) {
                LOGGER.error("Error processing file: " + filePath, e);
            }
        }
    }

    private void processAST(String filePath, String content, Language language) throws Exception {
        try (Parser parser = new Parser()) {
            parser.setLanguage(language);
            try (Tree tree = parser.parse(content, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astRoot = ASTUtil.buildASTWithCursor(rootNode);

                ASTVisitor visitor = createVisitor(filePath, content);
                if (visitor != null) {
                    visitor.visit(astRoot);
                } else {
                    LOGGER.warn("Unsupported file type: " + filePath);
                }
            }
        }
    }

    private ASTVisitor createVisitor(String filePath, String content) {
        String extension = getFileExtension(filePath);

        return switch (extension) {
            case PYTHON_EXT -> new PythonASTVisitor(umlModel, content, filePath);
            case CPP_EXT -> throw new UnsupportedOperationException("C++ support not yet implemented");
            case JS_EXT -> throw new UnsupportedOperationException("JavaScript support not yet implemented");
            default -> null;
        };
    }

    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1).toLowerCase() : "";
    }

    private String mapExtensionToLanguage(String extension) {
        return switch (extension) {
            case PYTHON_EXT -> "python";
            case CPP_EXT -> "cpp";
            case JS_EXT -> "javascript";
            default -> "unknown";
        };
    }

    public UMLModel getUmlModel() {
        return this.umlModel;
    }
}
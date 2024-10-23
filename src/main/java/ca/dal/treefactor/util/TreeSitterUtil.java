package ca.dal.treefactor.util;

import io.github.treesitter.jtreesitter.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;

public class TreeSitterUtil {

    public static Language loadLanguageForFileExtension(String filePath) throws IOException {
        String extension = getFileExtension(filePath).toLowerCase();
        String libExtension = OSUtil.getLibExtension();
        String osFolder = OSUtil.getOSFolder();

        Path tempDir = Files.createTempDirectory("tree-sitter-lib");
        Path libPath;
        Language language;

        switch (extension) {
            case "cpp":
                libPath = copyLibrary(tempDir, "libtree-sitter-cpp" + libExtension, osFolder);
                language = loadLanguage(libPath, "tree_sitter_cpp");
                break;
            case "js":
                libPath = copyLibrary(tempDir, "libtree-sitter-javascript" + libExtension, osFolder);
                language = loadLanguage(libPath, "tree_sitter_javascript");
                break;
            case "py":
                libPath = copyLibrary(tempDir, "libtree-sitter-python" + libExtension, osFolder);
                language = loadLanguage(libPath, "tree_sitter_python");
                break;
            default:
                throw new IllegalArgumentException("Unsupported file extension: " + extension);
        }
        return language;
    }

    public static String getFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }

    public static String generateAST(Language language, String code) {
        try (Parser parser = new Parser()) {
            parser.setLanguage(language);
            try (Tree tree = parser.parse(code, InputEncoding.UTF_8).orElseThrow()) {
                Node rootNode = tree.getRootNode();
                ASTUtil.ASTNode astRoot = ASTUtil.buildASTWithCursor(rootNode);
                String astString = ASTUtil.printAST(astRoot, 0);
                return astString;
            }
        }
        catch (Exception e) {
            System.err.println("Error generating AST:");
            e.printStackTrace();
        }
        return null;
    }

    private static Path copyLibrary(Path tempDir, String libraryName, String osFolder) throws IOException {
        Path libraryPath = tempDir.resolve(libraryName);
        try (InputStream is = TreeSitterUtil.class.getResourceAsStream("/native/" + osFolder + "/" + libraryName)) {
            if (is == null) {
                throw new RuntimeException("Could not find " + libraryName + " in resources for " + osFolder);
            }
            Files.copy(is, libraryPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return libraryPath;
    }

    private static Language loadLanguage(Path libPath, String treeSitterName) throws IOException {
        SymbolLookup symbols = SymbolLookup.libraryLookup(libPath, Arena.global());
        return Language.load(symbols, treeSitterName);
    }

    private static String getFileExtension(String filePath) {
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }


}

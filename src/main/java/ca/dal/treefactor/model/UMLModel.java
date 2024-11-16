package ca.dal.treefactor.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import ca.dal.treefactor.model.core.UMLComment;
import ca.dal.treefactor.model.core.UMLImport;
import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.elements.UMLOperation;

public class UMLModel {
    // Core data structures
    private final List<UMLClass> classes;
    private final List<UMLOperation> operations;  // For standalone functions/methods
    private final Map<String, List<UMLComment>> commentMap;
    private final Map<String, List<UMLImport>> importMap;
    private final Map<String, String> sourceFileContents;
    private final Map<String, String> packageMap;  // For modules/namespaces
    private final String language;  // "python", "javascript", or "cpp"

    public UMLModel(String language) {
        this.language = language.toLowerCase();
        this.classes = new ArrayList<>();
        this.operations = new ArrayList<>();
        this.commentMap = new HashMap<>();
        this.importMap = new HashMap<>();
        this.sourceFileContents = new HashMap<>();
        this.packageMap = new HashMap<>();
    }

    // Language-specific operations
    public String getLanguage() {
        return language;
    }

    public boolean isPythonModel() {
        return "python".equals(language);
    }

    public boolean isJavaScriptModel() {
        return "javascript".equals(language);
    }

    public boolean isCPPModel() {
        return "cpp".equals(language);
    }

    // Class operations
    public void addClass(UMLClass umlClass) {
        if (!classes.contains(umlClass)) {
            classes.add(umlClass);
        }
    }

    public void removeClass(UMLClass umlClass) {
        classes.remove(umlClass);
    }

    public List<UMLClass> getClasses() {
        return new ArrayList<>(classes);
    }

    public Optional<UMLClass> getClass(String className) {
        return classes.stream()
                .filter(c -> c.getName().equals(className))
                .findFirst();
    }

    // Function/Method operations
    public void addOperation(UMLOperation operation) {
        if (!operations.contains(operation)) {
            operations.add(operation);
        }
    }

    public void removeOperation(UMLOperation operation) {
        operations.remove(operation);
    }

    public List<UMLOperation> getOperations() {
        return new ArrayList<>(operations);
    }

    public Optional<UMLOperation> getOperation(String operationName) {
        return operations.stream()
                .filter(o -> o.getName().equals(operationName))
                .findFirst();
    }

    // Import operations
    public void addImport(String filePath, UMLImport umlImport) {
        importMap.computeIfAbsent(filePath, k -> new ArrayList<>()).add(umlImport);
    }

    public void addImports(String filePath, List<UMLImport> imports) {
        importMap.put(filePath, new ArrayList<>(imports));
    }

    public List<UMLImport> getImports(String filePath) {
        return importMap.getOrDefault(filePath, new ArrayList<>());
    }

    public void removeImports(String filePath) {
        importMap.remove(filePath);
    }

    // Comment operations
    public void addComment(String filePath, UMLComment comment) {
        commentMap.computeIfAbsent(filePath, k -> new ArrayList<>()).add(comment);
    }

    public void addComments(String filePath, List<UMLComment> comments) {
        commentMap.put(filePath, new ArrayList<>(comments));
    }

    public List<UMLComment> getComments(String filePath) {
        return commentMap.getOrDefault(filePath, new ArrayList<>());
    }

    public void removeComments(String filePath) {
        commentMap.remove(filePath);
    }

    // Package/Module/Namespace operations
    public void addPackage(String filePath, String packageName) {
        packageMap.put(filePath, packageName);
    }

    public String getPackage(String filePath) {
        return packageMap.getOrDefault(filePath, "");
    }

    // Source file operations
    public void addSourceFileContent(String filePath, String content) {
        sourceFileContents.put(filePath, content);
    }

    public String getSourceFileContent(String filePath) {
        return sourceFileContents.get(filePath);
    }

    public Set<String> getSourceFilePaths() {
        return new HashSet<>(sourceFileContents.keySet());
    }

    // Query operations
    public List<UMLClass> getClassesInFile(String filePath) {
        return classes.stream()
                .filter(c -> c.getLocationInfo().getFilePath().equals(filePath))
                .toList();
    }

    public List<UMLOperation> getOperationsInFile(String filePath) {
        List<UMLOperation> result = new ArrayList<>();
        // Add standalone operations
        result.addAll(operations.stream()
                .filter(op -> op.getLocationInfo().getFilePath().equals(filePath))
                .toList());
        // Add class methods
        classes.stream()
                .filter(c -> c.getLocationInfo().getFilePath().equals(filePath))
                .forEach(c -> result.addAll(c.getOperations()));
        return result;
    }

    public List<UMLClass> getClassesByPackage(String packageName) {
        return classes.stream()
                .filter(c -> c.getPackageName().equals(packageName))
                .toList();
    }

    // Statistics operations
    public int getNumberOfClasses() {
        return classes.size();
    }

    public int getNumberOfOperations() {
        int total = operations.size();  // Standalone operations
        total += classes.stream()
                .mapToInt(c -> c.getOperations().size())
                .sum();  // Class methods
        return total;
    }

    public int getNumberOfFiles() {
        return sourceFileContents.size();
    }

    public Map<String, Integer> getClassesPerFile() {
        Map<String, Integer> classCount = new HashMap<>();
        for (UMLClass umlClass : classes) {
            String filePath = umlClass.getLocationInfo().getFilePath();
            classCount.merge(filePath, 1, Integer::sum);
        }
        return classCount;
    }

    public Map<String, Integer> getOperationsPerFile() {
        Map<String, Integer> opCount = new HashMap<>();
        // Count standalone operations
        for (UMLOperation op : operations) {
            String filePath = op.getLocationInfo().getFilePath();
            opCount.merge(filePath, 1, Integer::sum);
        }
        // Count class methods
        for (UMLClass cls : classes) {
            String filePath = cls.getLocationInfo().getFilePath();
            opCount.merge(filePath, cls.getOperations().size(), Integer::sum);
        }
        return opCount;
    }

    // Clear operations
    public void clearImports() {
        importMap.clear();
    }

    public void clearComments() {
        commentMap.clear();
    }

    public void clearClasses() {
        classes.clear();
    }

    public void clearOperations() {
        operations.clear();
    }

    public void clearAll() {
        classes.clear();
        operations.clear();
        commentMap.clear();
        importMap.clear();
        sourceFileContents.clear();
        packageMap.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UML Model Summary (").append(language).append("):\n");
        sb.append("Number of classes: ").append(getNumberOfClasses()).append("\n");
        sb.append("Number of operations: ").append(getNumberOfOperations()).append("\n");
        sb.append("Number of files: ").append(getNumberOfFiles()).append("\n");

        if (!operations.isEmpty()) {
            sb.append("\nStandalone Operations:\n");
            for (UMLOperation op : operations) {
                sb.append("- ").append(op.getName())
                        .append(" (").append(op.getLocationInfo().getFilePath()).append(")\n");
            }
        }

        if (!classes.isEmpty()) {
            sb.append("\nClasses:\n");
            for (UMLClass cls : classes) {
                sb.append("- ").append(cls.getName())
                        .append(" (").append(cls.getLocationInfo().getFilePath()).append(")\n");
                cls.getOperations().forEach(op ->
                        sb.append("  * ").append(op.getName()).append("\n"));
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLModel other = (UMLModel) o;
        return Objects.equals(language, other.language) &&
                Objects.equals(classes, other.classes) &&
                Objects.equals(operations, other.operations) &&
                Objects.equals(commentMap, other.commentMap) &&
                Objects.equals(importMap, other.importMap) &&
                Objects.equals(sourceFileContents, other.sourceFileContents) &&
                Objects.equals(packageMap, other.packageMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, classes, operations, commentMap, importMap, sourceFileContents, packageMap);
    }
}
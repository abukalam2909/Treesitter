package ca.dal.treefactor.model;

import ca.dal.treefactor.model.elements.UMLClass;
import ca.dal.treefactor.model.core.UMLComment;
import ca.dal.treefactor.model.core.UMLImport;

import java.util.*;

public class UMLModel {
    private Set<String> repositoryDirectories;
    private List<UMLClass> classes;
    private Map<String, List<UMLComment>> commentMap;
    private Map<String, List<UMLImport>> importMap;
    private Map<String, String> sourceFileContents;

    public UMLModel(Set<String> repositoryDirectories) {
        this.repositoryDirectories = repositoryDirectories;
        this.classes = new ArrayList<>();
        this.commentMap = new HashMap<>();
        this.importMap = new HashMap<>();
        this.sourceFileContents = new HashMap<>();
    }

    // Basic operations for classes
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

    public UMLClass getClass(String className) {
        for (UMLClass umlClass : classes) {
            if (umlClass.getName().equals(className)) {
                return umlClass;
            }
        }
        return null;
    }

    // Operations for imports
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

    // Operations for comments
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

    // Source file content operations
    public void addSourceFileContent(String filePath, String content) {
        sourceFileContents.put(filePath, content);
    }

    public String getSourceFileContent(String filePath) {
        return sourceFileContents.get(filePath);
    }

    public Set<String> getSourceFilePaths() {
        return new HashSet<>(sourceFileContents.keySet());
    }

    // Repository operations
    public void addRepositoryDirectory(String directory) {
        repositoryDirectories.add(directory);
    }

    public void removeRepositoryDirectory(String directory) {
        repositoryDirectories.remove(directory);
    }

    public Set<String> getRepositoryDirectories() {
        return new HashSet<>(repositoryDirectories);
    }

    // Query methods
    public List<UMLClass> getClassesInFile(String filePath) {
        List<UMLClass> classesInFile = new ArrayList<>();
        for (UMLClass umlClass : classes) {
            if (umlClass.getLocationInfo().getFilePath().equals(filePath)) {
                classesInFile.add(umlClass);
            }
        }
        return classesInFile;
    }

    public List<UMLClass> getClassesByPackage(String packageName) {
        List<UMLClass> packageClasses = new ArrayList<>();
        for (UMLClass umlClass : classes) {
            if (umlClass.getPackageName().equals(packageName)) {
                packageClasses.add(umlClass);
            }
        }
        return packageClasses;
    }

    // Statistics methods
    public int getNumberOfClasses() {
        return classes.size();
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

    // Clear methods
    public void clearImports() {
        importMap.clear();
    }

    public void clearComments() {
        commentMap.clear();
    }

    public void clearClasses() {
        classes.clear();
    }

    public void clearAll() {
        classes.clear();
        commentMap.clear();
        importMap.clear();
        sourceFileContents.clear();
    }

    // Override methods
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UML Model Summary:\n");
        sb.append("Number of classes: ").append(getNumberOfClasses()).append("\n");
        sb.append("Number of files: ").append(getNumberOfFiles()).append("\n");
        sb.append("Repository directories: ").append(repositoryDirectories).append("\n");

        sb.append("\nClasses:\n");
        for (UMLClass umlClass : classes) {
            sb.append("- ").append(umlClass.getName())
                    .append(" (").append(umlClass.getLocationInfo().getFilePath()).append(")\n");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLModel other = (UMLModel) o;
        return Objects.equals(classes, other.classes) &&
                Objects.equals(commentMap, other.commentMap) &&
                Objects.equals(importMap, other.importMap) &&
                Objects.equals(repositoryDirectories, other.repositoryDirectories) &&
                Objects.equals(sourceFileContents, other.sourceFileContents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classes, commentMap, importMap, repositoryDirectories, sourceFileContents);
    }
}
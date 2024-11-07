// UMLModelDiff.java
package ca.dal.treefactor.model.diff;

import ca.dal.treefactor.model.UMLModel;
import ca.dal.treefactor.model.elements.*;
import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.diff.refactoring.*;
import ca.dal.treefactor.model.diff.mappers.*;

import java.util.*;

public class UMLModelDiff {
    private final UMLModel oldModel;
    private final UMLModel newModel;
    private final List<UMLOperationBodyMapper> operationBodyMappers;
    private final Map<String, UMLOperation> oldOperations;
    private final Map<String, UMLOperation> newOperations;
    private final Map<String, UMLClass> oldClasses;
    private final Map<String, UMLClass> newClasses;

    public UMLModelDiff(UMLModel oldModel, UMLModel newModel) {
        this.oldModel = oldModel;
        this.newModel = newModel;
        this.operationBodyMappers = new ArrayList<>();
        this.oldOperations = new HashMap<>();
        this.newOperations = new HashMap<>();
        this.oldClasses = new HashMap<>();
        this.newClasses = new HashMap<>();

        // Initialize maps
        mapOperations();
        mapClasses();
    }

    private void mapOperations() {
        // Map all operations from old model
        for (UMLOperation op : oldModel.getOperations()) {
            oldOperations.put(getOperationKey(op), op);
        }
        for (UMLClass cls : oldModel.getClasses()) {
            for (UMLOperation op : cls.getOperations()) {
                oldOperations.put(getOperationKey(op), op);
            }
        }

        // Map all operations from new model
        for (UMLOperation op : newModel.getOperations()) {
            newOperations.put(getOperationKey(op), op);
        }
        for (UMLClass cls : newModel.getClasses()) {
            for (UMLOperation op : cls.getOperations()) {
                newOperations.put(getOperationKey(op), op);
            }
        }
    }

    private void mapClasses() {
        for (UMLClass cls : oldModel.getClasses()) {
            oldClasses.put(cls.getFullyQualifiedName(), cls);
        }
        for (UMLClass cls : newModel.getClasses()) {
            newClasses.put(cls.getFullyQualifiedName(), cls);
        }
    }

    private String getOperationKey(UMLOperation operation) {
        StringBuilder key = new StringBuilder();
        if (operation.getClassName() != null) {
            key.append(operation.getClassName()).append(".");
        }
        key.append(operation.getName());
        key.append("(");
        // Add parameter types for statically typed languages
        if (!oldModel.isPythonModel()) {
            List<UMLParameter> params = operation.getParameters();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) key.append(",");
                key.append(params.get(i).getType().getTypeName());
            }
        }
        key.append(")");
        return key.toString();
    }

    public List<Refactoring> detectRefactorings() {
        List<Refactoring> refactorings = new ArrayList<>();

        // First match operations with identical signatures
        matchOperationsWithIdenticalSignatures();

        // Then try to match remaining operations based on body similarity
        matchOperationsBasedOnBodySimilarity();

        // Detect refactorings from matched operations
        detectRefactoringsFromMatches(refactorings);

        return refactorings;
    }

    private void matchOperationsWithIdenticalSignatures() {
        Set<String> commonKeys = new HashSet<>(oldOperations.keySet());
        commonKeys.retainAll(newOperations.keySet());

        for (String key : commonKeys) {
            UMLOperation oldOp = oldOperations.get(key);
            UMLOperation newOp = newOperations.get(key);

            UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(oldOp, newOp);
            operationBodyMappers.add(mapper);
        }
    }

    private void matchOperationsBasedOnBodySimilarity() {
        Set<String> unmatchedOldKeys = new HashSet<>(oldOperations.keySet());
        unmatchedOldKeys.removeAll(newOperations.keySet());

        Set<String> unmatchedNewKeys = new HashSet<>(newOperations.keySet());
        unmatchedNewKeys.removeAll(oldOperations.keySet());

        for (String oldKey : unmatchedOldKeys) {
            UMLOperation oldOp = oldOperations.get(oldKey);
            UMLOperation bestMatch = null;
            UMLOperationBodyMapper bestMapper = null;
            double maxSimilarity = 0.0;

            for (String newKey : unmatchedNewKeys) {
                UMLOperation newOp = newOperations.get(newKey);

                // Skip if method names are too different
                if (!areMethodNamesRelated(oldOp.getName(), newOp.getName())) {
                    continue;
                }

                UMLOperationBodyMapper mapper = new UMLOperationBodyMapper(oldOp, newOp);
                double similarity = mapper.bodyComparatorScore();

                if (similarity > maxSimilarity && similarity >= 0.7) {
                    maxSimilarity = similarity;
                    bestMatch = newOp;
                    bestMapper = mapper;
                }
            }

            if (bestMatch != null) {
                operationBodyMappers.add(bestMapper);
                unmatchedNewKeys.remove(getOperationKey(bestMatch));
            }
        }
    }

    private void detectRefactoringsFromMatches(List<Refactoring> refactorings) {
        for (UMLOperationBodyMapper mapper : operationBodyMappers) {
            List<Refactoring> operationRefactorings = mapper.getRefactorings();
            refactorings.addAll(operationRefactorings);
        }
    }

    private boolean areMethodNamesRelated(String oldName, String newName) {
        if (oldName.equals(newName)) return true;

        // Handle common rename patterns
        String normalizedOld = normalizeMethodName(oldName);
        String normalizedNew = normalizeMethodName(newName);

        return normalizedOld.equals(normalizedNew) ||
                normalizedOld.contains(normalizedNew) ||
                normalizedNew.contains(normalizedOld) ||
                getLevenshteinDistance(normalizedOld, normalizedNew) <= 2;
    }

    private String normalizeMethodName(String name) {
        return name.replaceAll("^(get|set|is|has|do|make|create|build|compute|calculate|find|search|fetch)", "")
                .replaceAll("(Async|Impl|Internal|Helper)$", "")
                .toLowerCase();
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] +
                                    (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
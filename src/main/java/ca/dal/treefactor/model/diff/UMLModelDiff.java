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

    /**
     * Constants used for determining operation similarity and matching thresholds.
     * These thresholds are used to determine when operations should be considered related
     * or matching based on various criteria.
     */
    public class OperationThresholds {
        /**
         * Minimum similarity score required to consider two operation bodies as matching.
         * Operations with body similarity below this threshold will not be considered matches.
         * The value of 0.7 (70%) was chosen based on empirical testing to balance between:
         * - Being high enough to avoid false positives
         * - Being low enough to catch operations that have been significantly modified
         * - Accommodating common refactoring patterns that modify method bodies
         */
        public static final double BODY_SIMILARITY_THRESHOLD = 0.7;

        /**
         * Minimum ratio of matched statements required to consider operations as related.
         * This is used when comparing operation bodies statement by statement.
         */
        public static final double STATEMENT_MATCH_THRESHOLD = 0.5;

        /**
         * Maximum Levenshtein distance allowed between method names to consider them related.
         * This helps identify renamed methods that maintain similar functionality.
         */
        public static final int MAX_NAME_EDIT_DISTANCE = 2;

        private OperationThresholds() {
            // Prevent instantiation of this constants class
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

                if (similarity > maxSimilarity && similarity >= OperationThresholds.BODY_SIMILARITY_THRESHOLD) {
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
        // Quick exact match check
        if (oldName.equals(newName)) {
            return true;
        }

        // Normalize names
        String normalizedOld = normalizeMethodName(oldName);
        String normalizedNew = normalizeMethodName(newName);

        // Check various similarity conditions
        boolean exactNormalizedMatch = normalizedOld.equals(normalizedNew);
        boolean oldContainsNew = normalizedOld.contains(normalizedNew);
        boolean newContainsOld = normalizedNew.contains(normalizedOld);
        boolean closeEditDistance = getLevenshteinDistance(normalizedOld, normalizedNew)
                <= OperationThresholds.MAX_NAME_EDIT_DISTANCE;

        return exactNormalizedMatch ||
                oldContainsNew ||
                newContainsOld ||
                closeEditDistance;
    }

    /**
     * Constants for method name normalization patterns.
     * These patterns are used to standardize method names by removing common prefixes and suffixes.
     */
    public class MethodNamePatterns {
        // Common method name prefixes that can be removed during normalization
        private static final String[] PREFIX_PATTERNS = {
                "get",
                "set",
                "is",
                "has",
                "do",
                "make",
                "create",
                "build",
                "compute",
                "calculate",
                "find",
                "search",
                "fetch"
        };

        // Common method name suffixes that can be removed during normalization
        private static final String[] SUFFIX_PATTERNS = {
                "Async",
                "Impl",
                "Internal",
                "Helper"
        };

        // Compiled patterns for better performance
        public static final String PREFIX_REGEX = "^(" + String.join("|", PREFIX_PATTERNS) + ")";
        public static final String SUFFIX_REGEX = "(" + String.join("|", SUFFIX_PATTERNS) + ")$";

        private MethodNamePatterns() {
            // Prevent instantiation
        }
    }

    /**
     * Normalizes a method name by removing common prefixes and suffixes
     * and converting to lowercase for case-insensitive comparison.
     *
     * @param name The original method name
     * @return The normalized method name
     */
    private String normalizeMethodName(String name) {
        return name
                .replaceAll(MethodNamePatterns.PREFIX_REGEX, "")
                .replaceAll(MethodNamePatterns.SUFFIX_REGEX, "")
                .toLowerCase();
    }


    /**
     * Calculates the Levenshtein distance between two strings.
     * The Levenshtein distance is the minimum number of single-character edits
     * required to change one string into another.
     */
    private int getLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        initializeFirstRow(dp, s2.length());
        computeLevenshteinMatrix(dp, s1, s2);
        return dp[s1.length()][s2.length()];
    }

    /**
     * Initializes the first row and column of the distance matrix.
     */
    private void initializeFirstRow(int[][] dp, int length) {
        // Initialize first row
        for (int j = 0; j <= length; j++) {
            dp[0][j] = j;
        }
    }

    /**
     * Computes the Levenshtein distance matrix.
     */
    private void computeLevenshteinMatrix(int[][] dp, String s1, String s2) {
        for (int i = 1; i <= s1.length(); i++) {
            // Initialize first column
            dp[i][0] = i;

            for (int j = 1; j <= s2.length(); j++) {
                dp[i][j] = getMinimumEditDistance(
                        dp, i, j,
                        s1.charAt(i - 1) == s2.charAt(j - 1)
                );
            }
        }
    }

    /**
     * Calculates the minimum edit distance for a position in the matrix.
     */
    private int getMinimumEditDistance(int[][] dp, int i, int j, boolean charsMatch) {
        // Cost of substitution
        int substitutionCost = dp[i - 1][j - 1] + (charsMatch ? 0 : 1);

        // Cost of insertion and deletion
        int insertionCost = dp[i][j - 1] + 1;
        int deletionCost = dp[i - 1][j] + 1;

        // Return minimum of all operations
        return Math.min(substitutionCost, Math.min(insertionCost, deletionCost));
    }

}
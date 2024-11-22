package ca.dal.treefactor.model.diff.mappers;

import ca.dal.treefactor.model.diff.refactoring.operations.RenameMethodRefactoring;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.core.UMLParameter;
import ca.dal.treefactor.model.diff.mapping.*;
import ca.dal.treefactor.model.diff.refactoring.*;
import java.util.*;

public class UMLOperationBodyMapper {
    private final UMLOperation operation1;
    private final UMLOperation operation2;
    private final List<StatementMapping> mappings;
    private final ParameterMapper parameterMapper;
    private List<Refactoring> refactorings;

    public UMLOperationBodyMapper(UMLOperation operation1, UMLOperation operation2) {
        this.operation1 = operation1;
        this.operation2 = operation2;
        this.mappings = new ArrayList<>();
        this.parameterMapper = new ParameterMapper(operation1, operation2);
        this.refactorings = new ArrayList<>();
        mapOperations();
    }

    private void mapOperations() {
        // First map parameters and collect parameter-related refactorings
        refactorings.addAll(parameterMapper.getRefactorings());

        // Then map statements using parameter mapping information
        if (operation1.getBody() != null && operation2.getBody() != null) {
            mapStatements();
        }

        // Finally, look for method-level refactorings
        detectMethodLevelRefactorings();
    }

    private void mapStatements() {
        String[] statements1 = operation1.getBody().split("\n");
        String[] statements2 = operation2.getBody().split("\n");

        // Using LCS (Longest Common Subsequence) to match statements in order
        int[][] lcsMatrix = computeLCSMatrix(statements1, statements2);
        List<StatementMapping> orderedMappings = extractMappingsFromLCS(lcsMatrix, statements1, statements2);
        mappings.addAll(orderedMappings);

        // Try to match remaining statements
        matchRemainingStatements(statements1, statements2);
    }

    private int[][] computeLCSMatrix(String[] statements1, String[] statements2) {
        int[][] matrix = new int[statements1.length + 1][statements2.length + 1];

        for (int i = 1; i <= statements1.length; i++) {
            for (int j = 1; j <= statements2.length; j++) {
                StatementMapper mapper = new StatementMapper(
                        statements1[i-1].trim(),
                        statements2[j-1].trim(),
                        parameterMapper.getParameterReplacements()
                );

                if (mapper.match()) {
                    matrix[i][j] = matrix[i-1][j-1] + 1;
                } else {
                    matrix[i][j] = Math.max(matrix[i-1][j], matrix[i][j-1]);
                }
            }
        }
        return matrix;
    }

    private List<StatementMapping> extractMappingsFromLCS(int[][] matrix, String[] statements1, String[] statements2) {
        List<StatementMapping> result = new ArrayList<>();
        int i = statements1.length;
        int j = statements2.length;

        while (i > 0 && j > 0) {
            if (matrix[i][j] > Math.max(matrix[i-1][j], matrix[i][j-1])) {
                StatementMapper mapper = new StatementMapper(
                        statements1[i-1].trim(),
                        statements2[j-1].trim(),
                        parameterMapper.getParameterReplacements()
                );

                mapper.createMapping().ifPresent(result::add);
                i--;
                j--;
            } else if (matrix[i-1][j] > matrix[i][j-1]) {
                i--;
            } else {
                j--;
            }
        }

        Collections.reverse(result);
        return result;
    }

    // Inside the matchRemainingStatements method, change:
    private void matchRemainingStatements(String[] statements1, String[] statements2) {
        final Set<Integer> mapped1 = getMappedIndices(statements1, true);
        final Set<Integer> mapped2 = getMappedIndices(statements2, false);

        for (int i = 0; i < statements1.length; i++) {
            if (mapped1.contains(i)) continue;

            final int iCopy = i;  // Create a final copy for the lambda

            for (int j = 0; j < statements2.length; j++) {
                if (mapped2.contains(j)) continue;

                final int jCopy = j;  // Create a final copy for the lambda

                StatementMapper mapper = new StatementMapper(
                        statements1[iCopy].trim(),
                        statements2[jCopy].trim(),
                        parameterMapper.getParameterReplacements()
                );

                mapper.createMapping().ifPresent(mapping -> {
                    mappings.add(mapping);
                    mapped1.add(iCopy);
                    mapped2.add(jCopy);
                });
            }
        }
    }

    // And in the getMappedIndices method:
    private Set<Integer> getMappedIndices(final String[] statements, final boolean isFromOperation1) {
        final Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < statements.length; i++) {
            final int iCopy = i;  // Create a final copy for the lambda
            mappings.forEach(mapping -> {
                String statement = isFromOperation1 ? mapping.getStatement1() : mapping.getStatement2();
                if (statements[iCopy].trim().equals(statement)) {
                    indices.add(iCopy);
                }
            });
        }
        return indices;
    }

    /**
     * Threshold for determining if two methods with different names are a rename refactoring.
     * Value represents minimum ratio of mapped statements between methods.
     * Range is 0.0 to 1.0, where:
     * - 1.0 means perfect body similarity (all statements match)
     * - 0.0 means completely different bodies
     * Default 0.7 requires 70% of statements to match.
     */
    private static final double RENAME_METHOD_SIMILARITY_THRESHOLD = 0.7;

    private void detectMethodLevelRefactorings() {
        // Compare method signatures for potential rename
        if (!operation1.getName().equals(operation2.getName())) {
            double bodySimilarity = bodyComparatorScore();
            if (bodySimilarity >= RENAME_METHOD_SIMILARITY_THRESHOLD) {
                refactorings.add(new RenameMethodRefactoring(operation1, operation2));
            }
        }
    }

    public double bodyComparatorScore() {
        if (operation1.getBody() == null || operation2.getBody() == null) {
            return 0.0;
        }

        String[] statements1 = operation1.getBody().split("\n");
        String[] statements2 = operation2.getBody().split("\n");
        int maxStatements = Math.max(statements1.length, statements2.length);

        return maxStatements == 0 ? 0.0 : (double) mappings.size() / maxStatements;
    }

    public List<Refactoring> getRefactorings() {
        return new ArrayList<>(refactorings);
    }

    public List<StatementMapping> getMappings() {
        return new ArrayList<>(mappings);
    }

    public UMLOperation getOperation1() {
        return operation1;
    }

    public UMLOperation getOperation2() {
        return operation2;
    }
}
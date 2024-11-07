// StatementMapper.java
package ca.dal.treefactor.model.diff.mappers;

import ca.dal.treefactor.model.diff.mapping.*;
import java.util.*;
import java.util.regex.Pattern;

public class StatementMapper {
    private final String statement1;
    private final String statement2;
    private final Map<String, String> parameterReplacements;
    private final Set<Replacement> replacements;

    public StatementMapper(String statement1, String statement2, Map<String, String> parameterReplacements) {
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.parameterReplacements = parameterReplacements;
        this.replacements = new HashSet<>();
        mapStatements();
    }

    private void mapStatements() {
        // First apply known parameter replacements
        String mappedStatement1 = statement1;
        for (Map.Entry<String, String> replacement : parameterReplacements.entrySet()) {
            String pattern = "\\b" + Pattern.quote(replacement.getKey()) + "\\b";
            String before = mappedStatement1;
            mappedStatement1 = mappedStatement1.replaceAll(pattern, replacement.getValue());

            if (!before.equals(mappedStatement1)) {
                replacements.add(new Replacement(
                        replacement.getKey(),
                        replacement.getValue(),
                        ReplacementType.PARAMETER_NAME
                ));
            }
        }

        // If statements match after parameter replacements, we're done
        if (mappedStatement1.equals(statement2)) {
            return;
        }

        // Otherwise look for other types of replacements
        findMethodInvocationReplacements(mappedStatement1, statement2);
        findVariableNameReplacements(mappedStatement1, statement2);
        findLiteralReplacements(mappedStatement1, statement2);
        findArgumentReplacements(mappedStatement1, statement2);
    }

    private void findMethodInvocationReplacements(String stmt1, String stmt2) {
        // Match method calls: name(args)
        Pattern methodPattern = Pattern.compile("\\b(\\w+)\\s*\\(([^)]*)\\)");
        // Implementation would look for method name changes and argument changes
    }

    private void findVariableNameReplacements(String stmt1, String stmt2) {
        // Match variable names: look for identifiers that differ
        Pattern varPattern = Pattern.compile("\\b[a-zA-Z_]\\w*\\b");
        // Implementation would look for variable name changes
    }

    private void findLiteralReplacements(String stmt1, String stmt2) {
        // Match string literals: "..." or '...'
        Pattern stringPattern = Pattern.compile("([\"'])(?:(?!\\1).)*\\1");
        // Match number literals: digits with optional decimal point
        Pattern numberPattern = Pattern.compile("\\b\\d*\\.?\\d+\\b");
        // Implementation would look for literal value changes
    }

    private void findArgumentReplacements(String stmt1, String stmt2) {
        // Look for method calls with different argument counts or ordering
        Pattern argListPattern = Pattern.compile("\\((.*?)\\)");
        // Implementation would analyze argument differences
    }

    public boolean match() {
        // After applying all possible replacements, do statements match?
        String mappedStmt1 = applyReplacements(statement1);
        return mappedStmt1.equals(statement2);
    }

    private String applyReplacements(String statement) {
        String result = statement;
        for (Replacement replacement : replacements) {
            String pattern = "\\b" + Pattern.quote(replacement.getBefore()) + "\\b";
            result = result.replaceAll(pattern, replacement.getAfter());
        }
        return result;
    }

    public Set<Replacement> getReplacements() {
        return new HashSet<>(replacements);
    }

    /**
     * Normalize a statement for comparison
     */
    private String normalizeStatement(String statement) {
        return statement.replaceAll("\\s+", " ")  // Normalize whitespace
                .replaceAll("\\(\\s+", "(")  // Remove space after opening parenthesis
                .replaceAll("\\s+\\)", ")")  // Remove space before closing parenthesis
                .trim();
    }

    /**
     * Create a StatementMapping if statements match
     */
    public Optional<StatementMapping> createMapping() {
        if (match()) {
            StatementMapping mapping = new StatementMapping(statement1, statement2);
            replacements.forEach(mapping::addReplacement);
            return Optional.of(mapping);
        }
        return Optional.empty();
    }
}
// StatementMapping.java
package ca.dal.treefactor.model.diff.mapping;

import java.util.*;

public class StatementMapping {
    private final String statement1;
    private final String statement2;
    private final Set<Replacement> replacements;

    public StatementMapping(String statement1, String statement2) {
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.replacements = new HashSet<>();
    }

    public void addReplacement(Replacement replacement) {
        replacements.add(replacement);
    }

    public String getStatement1() {
        return statement1;
    }

    public String getStatement2() {
        return statement2;
    }

    public Set<Replacement> getReplacements() {
        return new HashSet<>(replacements);
    }

    @Override
    public String toString() {
        return String.format("StatementMapping[\nStatement1: %s\nStatement2: %s\nReplacements: %s\n]",
                statement1, statement2, replacements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementMapping that = (StatementMapping) o;
        return Objects.equals(statement1, that.statement1) &&
                Objects.equals(statement2, that.statement2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statement1, statement2);
    }
}
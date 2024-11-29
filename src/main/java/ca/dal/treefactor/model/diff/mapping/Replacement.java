// Replacement.java
package ca.dal.treefactor.model.diff.mapping;

import java.util.Objects;

public class Replacement {
    private final String before;
    private final String after;
    private final ReplacementType type;

    public Replacement(String before, String after, ReplacementType type) {
        this.before = before;
        this.after = after;
        this.type = type;
    }

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    public ReplacementType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s: %s → %s", type, before, after);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Replacement that = (Replacement) o;
        return Objects.equals(before, that.before) &&
                Objects.equals(after, that.after) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(before, after, type);
    }
}
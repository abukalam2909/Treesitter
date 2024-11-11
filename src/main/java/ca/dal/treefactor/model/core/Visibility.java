package ca.dal.treefactor.model.core;

/**
 * Represents the visibility/access level modifiers in different programming languages.
 * Supports common visibility levels across multiple languages:
 * - Java: public, protected, private, package-private
 * - Python: public (_), protected (_), private (__)
 * - C++: public, protected, private
 * - JavaScript: no strict visibility (simulated through conventions)
 */
public enum Visibility {
    PUBLIC("public"),
    PROTECTED("protected"),
    PRIVATE("private"),
    DEFAULT(""),  // package-private in Java, default in other languages
    PACKAGE("package");  // Explicit package visibility

    private final String representation;

    Visibility(String representation) {
        this.representation = representation;
    }

    /**
     * Returns the string representation of the visibility level
     */
    public String getRepresentation() {
        return representation;
    }

    /**
     * Parse visibility from a string, with language-specific handling
     */
    public static Visibility fromString(String visibility, String language) {
        if (visibility == null || visibility.trim().isEmpty()) {
            return DEFAULT;
        }

        switch (language.toLowerCase()) {
            case "java":
                return parseJavaVisibility(visibility);
            case "python":
                return parsePythonVisibility(visibility);
            case "cpp":
                return parseCppVisibility(visibility);
            case "javascript":
                return parseJavaScriptVisibility(visibility);
            default:
                return parseDefaultVisibility(visibility);
        }
    }

    /**
     * Parse Java-specific visibility
     */
    private static Visibility parseJavaVisibility(String visibility) {
        switch (visibility.toLowerCase()) {
            case "public":
                return PUBLIC;
            case "protected":
                return PROTECTED;
            case "private":
                return PRIVATE;
            case "package":
                return PACKAGE;
            default:
                return DEFAULT;
        }
    }

    /**
     * Parse Python-specific visibility based on naming conventions
     */
    private static Visibility parsePythonVisibility(String memberName) {
        if (memberName.startsWith("__")) {
            return PRIVATE;
        } else if (memberName.startsWith("_")) {
            return PROTECTED;
        } else {
            return PUBLIC;
        }
    }

    /**
     * Parse C++-specific visibility
     */
    private static Visibility parseCppVisibility(String visibility) {
        switch (visibility.toLowerCase()) {
            case "public":
                return PUBLIC;
            case "protected":
                return PROTECTED;
            case "private":
                return PRIVATE;
            default:
                return PRIVATE; // C++ members are private by default
        }
    }

    /**
     * Parse JavaScript-specific visibility (mostly convention-based)
     */
    private static Visibility parseJavaScriptVisibility(String memberName) {
        if (memberName.startsWith("#")) {
            return PRIVATE; // Private fields in modern JavaScript
        } else if (memberName.startsWith("_")) {
            return PROTECTED; // Convention for "protected" members
        } else {
            return PUBLIC;
        }
    }

    /**
     * Default visibility parsing for unknown languages
     */
    private static Visibility parseDefaultVisibility(String visibility) {
        switch (visibility.toLowerCase()) {
            case "public":
                return PUBLIC;
            case "protected":
                return PROTECTED;
            case "private":
                return PRIVATE;
            default:
                return DEFAULT;
        }
    }

    /**
     * Check if this visibility is more restrictive than another visibility
     */
    public boolean isMoreRestrictiveThan(Visibility other) {
        if (this == other) return false;

        switch (this) {
            case PRIVATE:
                return true;
            case PROTECTED:
                return other == PUBLIC;
            case DEFAULT:
                return other == PUBLIC;
            case PUBLIC:
                return false;
            default:
                return false;
        }
    }

    /**
     * Returns true if this visibility is accessible from the specified visibility scope
     */
    public boolean isAccessibleFrom(Visibility scope) {
        if (this == PUBLIC) return true;
        if (this == PRIVATE) return scope == this;
        if (this == PROTECTED) return scope != PRIVATE;
        if (this == DEFAULT) return scope != PRIVATE;
        return false;
    }

    @Override
    public String toString() {
        return representation;
    }
}
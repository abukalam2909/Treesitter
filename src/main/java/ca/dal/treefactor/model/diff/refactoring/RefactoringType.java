package ca.dal.treefactor.model.diff.refactoring;

public enum RefactoringType {
    // Parameter Refactorings
    RENAME_PARAMETER("Rename Parameter"),
    ADD_PARAMETER("Add Parameter"),
    CHANGE_PARAMETER_TYPE("Change Parameter Type"),

    // Method Refactorings
    RENAME_METHOD("Rename Method"),

    // Future refactorings to be added
    EXTRACT_METHOD("Extract Method"),
    INLINE_METHOD("Inline Method"),
    MOVE_METHOD("Move Method");

    private final String displayName;

    RefactoringType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
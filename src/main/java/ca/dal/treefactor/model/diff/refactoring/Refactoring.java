package ca.dal.treefactor.model.diff.refactoring;

import ca.dal.treefactor.model.core.LocationInfo;
import java.util.List;

public abstract class Refactoring {
    private final RefactoringType type;
    private final String description;
    private final String language;  // to track which language this refactoring is from

    protected Refactoring(RefactoringType type, String description, String language) {
        this.type = type;
        this.description = description;
        this.language = language;
    }

    public RefactoringType getRefactoringType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * Get locations in the source code for the left-hand side (before) of the refactoring
     */
    public abstract List<LocationInfo> getLeftSideLocations();

    /**
     * Get locations in the source code for the right-hand side (after) of the refactoring
     */
    public abstract List<LocationInfo> getRightSideLocations();

    /**
     * Get the file(s) involved in this refactoring
     */
    public abstract List<String> getInvolvedFiles();

    @Override
    public String toString() {
        return String.format("%s in %s: %s", type.getDisplayName(), language, description);
    }
}
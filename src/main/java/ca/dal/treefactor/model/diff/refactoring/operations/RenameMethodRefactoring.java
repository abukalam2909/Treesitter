package ca.dal.treefactor.model.diff.refactoring.operations;

import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.diff.refactoring.*;
import java.util.List;
import java.util.ArrayList;

public class RenameMethodRefactoring extends Refactoring {
    private final UMLOperation originalOperation;
    private final UMLOperation renamedOperation;

    public RenameMethodRefactoring(UMLOperation originalOperation, UMLOperation renamedOperation) {
        super(RefactoringType.RENAME_METHOD,
                createDescription(originalOperation, renamedOperation),
                determineLanguage(originalOperation));

        this.originalOperation = originalOperation;
        this.renamedOperation = renamedOperation;
    }

    private static String createDescription(
            UMLOperation originalOperation,
            UMLOperation renamedOperation) {

        StringBuilder desc = new StringBuilder();
        if (originalOperation.getClassName() != null) {
            desc.append("Method '").append(originalOperation.getClassName())
                    .append(".")
                    .append(originalOperation.getName())
                    .append("' renamed to '")
                    .append(renamedOperation.getName()).append("'");
        } else {
            desc.append("Function '")
                    .append(originalOperation.getName())
                    .append("' renamed to '")
                    .append(renamedOperation.getName()).append("'");
        }
        return desc.toString();
    }

    private static String determineLanguage(UMLOperation operation) {
        String filePath = operation.getLocationInfo().getFilePath();
        return filePath.endsWith(".py") ? "Python" :
                filePath.endsWith(".js") ? "JavaScript" :
                        filePath.endsWith(".cpp") ? "C++" :
                                "Unknown";
    }

    public UMLOperation getOriginalOperation() {
        return originalOperation;
    }

    public UMLOperation getRenamedOperation() {
        return renamedOperation;
    }

    @Override
    public List<LocationInfo> getLeftSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        locations.add(originalOperation.getLocationInfo());
        return locations;
    }

    @Override
    public List<LocationInfo> getRightSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        locations.add(renamedOperation.getLocationInfo());
        return locations;
    }

    @Override
    public List<String> getInvolvedFiles() {
        List<String> files = new ArrayList<>();
        files.add(originalOperation.getLocationInfo().getFilePath());
        if (!originalOperation.getLocationInfo().getFilePath()
                .equals(renamedOperation.getLocationInfo().getFilePath())) {
            files.add(renamedOperation.getLocationInfo().getFilePath());
        }
        return files;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RenameMethodRefactoring)) {
            return false;
        }
        RenameMethodRefactoring other = (RenameMethodRefactoring) obj;
        return this.originalOperation.equals(other.originalOperation) &&
                this.renamedOperation.equals(other.renamedOperation);
    }

    @Override
    public int hashCode() {
        return originalOperation.hashCode() * 31 + renamedOperation.hashCode();
    }
}

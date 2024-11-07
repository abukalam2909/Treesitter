// operations/RenameParameterRefactoring.java
package ca.dal.treefactor.model.diff.refactoring.operations;

import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.diff.refactoring.*;
import java.util.List;
import java.util.ArrayList;

public class RenameParameterRefactoring extends Refactoring {
    private final UMLParameter originalParameter;
    private final UMLParameter renamedParameter;
    private final UMLOperation operation;

    public RenameParameterRefactoring(
            UMLParameter originalParameter,
            UMLParameter renamedParameter,
            UMLOperation operation) {

        super(RefactoringType.RENAME_PARAMETER,
                createDescription(originalParameter, renamedParameter, operation),
                determineLanguage(operation));

        this.originalParameter = originalParameter;
        this.renamedParameter = renamedParameter;
        this.operation = operation;
    }

    private static String createDescription(
            UMLParameter originalParameter,
            UMLParameter renamedParameter,
            UMLOperation operation) {

        StringBuilder desc = new StringBuilder();
        desc.append("Parameter '").append(originalParameter.getName())
                .append("' renamed to '").append(renamedParameter.getName())
                .append("' in ");

        if (operation.getClassName() != null) {
            desc.append("method '").append(operation.getClassName())
                    .append(".").append(operation.getName()).append("'");
        } else {
            desc.append("function '").append(operation.getName()).append("'");
        }

        return desc.toString();
    }

    private static String determineLanguage(UMLOperation operation) {
        // You might want to make this more sophisticated based on your needs
        return operation.getLocationInfo().getFilePath().endsWith(".py") ? "Python" :
                operation.getLocationInfo().getFilePath().endsWith(".js") ? "JavaScript" :
                        operation.getLocationInfo().getFilePath().endsWith(".cpp") ? "C++" :
                                "Unknown";
    }

    public UMLParameter getOriginalParameter() {
        return originalParameter;
    }

    public UMLParameter getRenamedParameter() {
        return renamedParameter;
    }

    public UMLOperation getOperation() {
        return operation;
    }

    @Override
    public List<LocationInfo> getLeftSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        locations.add(originalParameter.getLocationInfo());
        // Also include locations where this parameter is used in the method body
        if (operation.getBody() != null) {
            // Note: You might want to add actual parameter usage locations
            // from your AST analysis here
        }
        return locations;
    }

    @Override
    public List<LocationInfo> getRightSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        locations.add(renamedParameter.getLocationInfo());
        // Also include locations where this parameter is used in the method body
        if (operation.getBody() != null) {
            // Note: You might want to add actual parameter usage locations
            // from your AST analysis here
        }
        return locations;
    }

    @Override
    public List<String> getInvolvedFiles() {
        return List.of(operation.getLocationInfo().getFilePath());
    }

    @Override
    public String toString() {
        return String.format("%s: %s renamed to %s in %s",
                getRefactoringType().getDisplayName(),
                originalParameter.getName(),
                renamedParameter.getName(),
                operation.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RenameParameterRefactoring)) {
            return false;
        }
        RenameParameterRefactoring other = (RenameParameterRefactoring) obj;
        return this.originalParameter.equals(other.originalParameter) &&
                this.renamedParameter.equals(other.renamedParameter) &&
                this.operation.equals(other.operation);
    }

    @Override
    public int hashCode() {
        return originalParameter.hashCode() * 31 +
                renamedParameter.hashCode() * 17 +
                operation.hashCode();
    }
}
package ca.dal.treefactor.model.diff.refactoring.operations;

import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.diff.refactoring.*;
import java.util.List;
import java.util.ArrayList;

public class ChangeParameterTypeRefactoring extends Refactoring {
    private final UMLParameter originalParameter;
    private final UMLParameter changedParameter;
    private final UMLOperation operation;

    public ChangeParameterTypeRefactoring(
            UMLParameter originalParameter,
            UMLParameter changedParameter,
            UMLOperation operation) {

        super(RefactoringType.CHANGE_PARAMETER_TYPE,
                createDescription(originalParameter, changedParameter, operation),
                determineLanguage(operation));

        this.originalParameter = originalParameter;
        this.changedParameter = changedParameter;
        this.operation = operation;
    }

    private static String createDescription(
            UMLParameter originalParameter,
            UMLParameter changedParameter,
            UMLOperation operation) {

        StringBuilder desc = new StringBuilder();
        desc.append("Parameter '").append(originalParameter.getName())
                .append("' type changed from '").append(originalParameter.getType().getTypeName())
                .append("' to '").append(changedParameter.getType().getTypeName())
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
        String filePath = operation.getLocationInfo().getFilePath();
        return filePath.endsWith(".py") ? "Python" :
                filePath.endsWith(".js") ? "JavaScript" :
                        filePath.endsWith(".cpp") ? "C++" :
                                "Unknown";
    }

    public UMLParameter getOriginalParameter() {
        return originalParameter;
    }

    public UMLParameter getChangedParameter() {
        return changedParameter;
    }

    public UMLOperation getOperation() {
        return operation;
    }

    @Override
    public List<LocationInfo> getLeftSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        locations.add(originalParameter.getLocationInfo());
        return locations;
    }

    @Override
    public List<LocationInfo> getRightSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        locations.add(changedParameter.getLocationInfo());
        return locations;
    }

    @Override
    public List<String> getInvolvedFiles() {
        return List.of(operation.getLocationInfo().getFilePath());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ChangeParameterTypeRefactoring)) {
            return false;
        }
        ChangeParameterTypeRefactoring other = (ChangeParameterTypeRefactoring) obj;
        return this.originalParameter.equals(other.originalParameter) &&
                this.changedParameter.equals(other.changedParameter) &&
                this.operation.equals(other.operation);
    }

    @Override
    public int hashCode() {
        return originalParameter.hashCode() * 31 +
                changedParameter.hashCode() * 17 +
                operation.hashCode();
    }
}


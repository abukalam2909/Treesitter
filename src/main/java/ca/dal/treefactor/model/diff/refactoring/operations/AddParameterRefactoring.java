package ca.dal.treefactor.model.diff.refactoring.operations;

import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.diff.refactoring.*;
import java.util.List;
import java.util.ArrayList;

public class AddParameterRefactoring extends Refactoring {
    private final UMLParameter addedParameter;
    private final UMLOperation operation;
    private final String defaultValue;

    public AddParameterRefactoring(
            UMLParameter addedParameter,
            UMLOperation operation,
            String defaultValue) {

        super(RefactoringType.ADD_PARAMETER,
                createDescription(addedParameter, operation, defaultValue),
                determineLanguage(operation));

        this.addedParameter = addedParameter;
        this.operation = operation;
        this.defaultValue = defaultValue;
    }

    private static String createDescription(
            UMLParameter addedParameter,
            UMLOperation operation,
            String defaultValue) {

        StringBuilder desc = new StringBuilder();
        desc.append("Parameter '").append(addedParameter.getName())
                .append("' of type '").append(addedParameter.getType().getTypeName())
                .append("' added to ");

        if (operation.getClassName() != null) {
            desc.append("method '").append(operation.getClassName())
                    .append(".").append(operation.getName()).append("'");
        } else {
            desc.append("function '").append(operation.getName()).append("'");
        }

        if (defaultValue != null) {
            desc.append(" with default value '").append(defaultValue).append("'");
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

    public UMLParameter getAddedParameter() {
        return addedParameter;
    }

    public UMLOperation getOperation() {
        return operation;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public List<LocationInfo> getLeftSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        // Only includes the operation location since parameter didn't exist before
        locations.add(operation.getLocationInfo());
        return locations;
    }

    @Override
    public List<LocationInfo> getRightSideLocations() {
        List<LocationInfo> locations = new ArrayList<>();
        locations.add(operation.getLocationInfo());
        locations.add(addedParameter.getLocationInfo());
        return locations;
    }

    @Override
    public List<String> getInvolvedFiles() {
        return List.of(operation.getLocationInfo().getFilePath());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AddParameterRefactoring)) {
            return false;
        }
        AddParameterRefactoring other = (AddParameterRefactoring) obj;
        return this.addedParameter.equals(other.addedParameter) &&
                this.operation.equals(other.operation);
    }

    @Override
    public int hashCode() {
        return addedParameter.hashCode() * 31 + operation.hashCode();
    }
}
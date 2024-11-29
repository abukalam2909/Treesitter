// ParameterMapper.java
package ca.dal.treefactor.model.diff.mappers;

import ca.dal.treefactor.model.core.*;
import ca.dal.treefactor.model.diff.refactoring.operations.AddParameterRefactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.ChangeParameterTypeRefactoring;
import ca.dal.treefactor.model.diff.refactoring.operations.RenameParameterRefactoring;
import ca.dal.treefactor.model.elements.UMLOperation;
import ca.dal.treefactor.model.diff.refactoring.*;
import java.util.*;

public class ParameterMapper {
    private final UMLOperation operation1;
    private final UMLOperation operation2;
    private final Map<String, String> parameterReplacements;
    private final List<Refactoring> refactorings;

    public ParameterMapper(UMLOperation operation1, UMLOperation operation2) {
        this.operation1 = operation1;
        this.operation2 = operation2;
        this.parameterReplacements = new HashMap<>();
        this.refactorings = new ArrayList<>();
        mapParameters();
    }

    private void mapParameters() {
        List<UMLParameter> params1 = operation1.getParameters();
        List<UMLParameter> params2 = operation2.getParameters();

        // Keep track of parameter renames to avoid duplicates
        Set<String> processedParams = new HashSet<>();

        // Skip 'self' parameter in Python methods, but not for C++
        boolean isPython = operation1.getLocationInfo().getFilePath().endsWith(".py");
        int startIndex = isPython && shouldSkipSelfParameter(params1, params2) ? 1 : 0;

        // First pass: Match parameters by position
        for (int i = startIndex; i < params1.size() && i < params2.size(); i++) {
            UMLParameter param1 = params1.get(i);
            UMLParameter param2 = params2.get(i);

            String paramKey = param1.getName() + "->" + param2.getName();
            if (!param1.getName().equals(param2.getName()) && !processedParams.contains(paramKey)) {
                // Parameter rename detected
                parameterReplacements.put(param1.getName(), param2.getName());
                refactorings.add(new RenameParameterRefactoring(param1, param2, operation2));
                processedParams.add(paramKey); // Mark this rename as processed
            } else if (!param1.getType().equals(param2.getType())) {
                // Parameter type change detected
                refactorings.add(new ChangeParameterTypeRefactoring(param1, param2, operation2));
            }
        }

        // Second pass: Detect added/removed parameters
        if (params1.size() < params2.size()) {
            // Parameters were added
            for (int i = params1.size(); i < params2.size(); i++) {
                UMLParameter addedParam = params2.get(i);
                String defaultValue = addedParam.hasDefaultValue() ? addedParam.getDefaultValue() : null;
                refactorings.add(new AddParameterRefactoring(addedParam, operation2, defaultValue));
            }
        }
    }

    private boolean shouldSkipSelfParameter(List<UMLParameter> params1, List<UMLParameter> params2) {
        return params1.size() > 0 && params2.size() > 0 &&
                params1.get(0).getName().equals("self") &&
                params2.get(0).getName().equals("self");
    }

    public Map<String, String> getParameterReplacements() {
        return new HashMap<>(parameterReplacements);
    }

    public List<Refactoring> getRefactorings() {
        return new ArrayList<>(refactorings);
    }

    /**
     * Check if a parameter name was changed
     */
    public boolean isParameterRenamed(String oldName, String newName) {
        return parameterReplacements.containsKey(oldName) &&
                parameterReplacements.get(oldName).equals(newName);
    }

    /**
     * Get the mapped name for a parameter
     */
    public String getMappedName(String originalName) {
        return parameterReplacements.getOrDefault(originalName, originalName);
    }
}
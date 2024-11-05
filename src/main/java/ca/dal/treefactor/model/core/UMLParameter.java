package ca.dal.treefactor.model.core;

import java.util.*;

public class UMLParameter {
    private final String name;
    private final UMLType type;
    private final List<UMLAnnotation> annotations;
    private final LocationInfo locationInfo;

    // Parameter properties
    private String kind; // "in", "out", or "inout" for languages that support it
    private boolean isVarargs;
    private boolean isFinal;
    private boolean isDefaultValuePresent;
    private String defaultValue;

    public UMLParameter(String name, UMLType type, LocationInfo locationInfo) {
        this.name = name;
        this.type = type;
        this.locationInfo = locationInfo;
        this.annotations = new ArrayList<>();
        this.kind = "in"; // default to "in" parameter
    }

    // Annotation management
    public void addAnnotation(UMLAnnotation annotation) {
        if (!annotations.contains(annotation)) {
            annotations.add(annotation);
        }
    }

    public void removeAnnotation(UMLAnnotation annotation) {
        annotations.remove(annotation);
    }

    public List<UMLAnnotation> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    // Basic getters
    public String getName() {
        return name;
    }

    public UMLType getType() {
        return type;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    // Property getters and setters
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        if (kind != null && (kind.equals("in") || kind.equals("out") || kind.equals("inout"))) {
            this.kind = kind;
        } else {
            throw new IllegalArgumentException("Parameter kind must be 'in', 'out', or 'inout'");
        }
    }

    public boolean isVarargs() {
        return isVarargs;
    }

    public void setVarargs(boolean isVarargs) {
        this.isVarargs = isVarargs;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean hasDefaultValue() {
        return isDefaultValuePresent;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        this.isDefaultValuePresent = defaultValue != null;
    }

    // Utility methods
    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    /**
     * Returns the parameter's signature as it would appear in code
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();

        // Add annotations
        for (UMLAnnotation annotation : annotations) {
            sb.append(annotation.toString()).append(" ");
        }

        // Add final modifier if present
        if (isFinal) {
            sb.append("final ");
        }

        // Add type
        sb.append(type.toString());

        // Add varargs if applicable
        if (isVarargs) {
            sb.append("...");
        }

        // Add name
        sb.append(" ").append(name);

        // Add default value if present
        if (isDefaultValuePresent) {
            sb.append(" = ").append(defaultValue);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getSignature();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLParameter parameter = (UMLParameter) o;
        return Objects.equals(name, parameter.name) &&
                Objects.equals(type, parameter.type) &&
                Objects.equals(locationInfo, parameter.locationInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, locationInfo);
    }

    // Builder pattern for fluent API
    public static class Builder {
        private final String name;
        private final UMLType type;
        private final LocationInfo locationInfo;
        private final List<UMLAnnotation> annotations = new ArrayList<>();
        private String kind = "in";
        private boolean isVarargs;
        private boolean isFinal;
        private String defaultValue;

        public Builder(String name, UMLType type, LocationInfo locationInfo) {
            this.name = name;
            this.type = type;
            this.locationInfo = locationInfo;
        }

        public Builder addAnnotation(UMLAnnotation annotation) {
            annotations.add(annotation);
            return this;
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder setVarargs(boolean isVarargs) {
            this.isVarargs = isVarargs;
            return this;
        }

        public Builder setFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public UMLParameter build() {
            UMLParameter parameter = new UMLParameter(name, type, locationInfo);
            parameter.setKind(kind);
            parameter.setVarargs(isVarargs);
            parameter.setFinal(isFinal);
            if (defaultValue != null) {
                parameter.setDefaultValue(defaultValue);
            }
            annotations.forEach(parameter::addAnnotation);
            return parameter;
        }
    }

    public static Builder builder(String name, UMLType type, LocationInfo locationInfo) {
        return new Builder(name, type, locationInfo);
    }
}
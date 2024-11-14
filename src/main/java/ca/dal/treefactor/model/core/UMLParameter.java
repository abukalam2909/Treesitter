package ca.dal.treefactor.model.core;

import java.util.*;

public class UMLParameter {
    private final String name;
    private final UMLType type;
    private final List<UMLAnnotation> annotations;
    private final LocationInfo locationInfo;

    // Common parameter properties
    private String kind;
    private boolean isVarargs;
    private boolean isFinal;
    private boolean isDefaultValuePresent;
    private String defaultValue;

    // Python-specific properties
    private boolean isKeywordOnly;

    // C++-specific properties
    private boolean isReference;
    private boolean isRValueReference;
    private boolean isConst;
    private boolean isPointer;
    private boolean isVolatile; // Additional C++ qualifier that might be useful

    public UMLParameter(String name, UMLType type, LocationInfo locationInfo) {
        this.name = name;
        this.type = type;
        this.locationInfo = locationInfo;
        this.annotations = new ArrayList<>();
        this.kind = "in"; // default to "in" parameter
        this.isKeywordOnly = false;
        this.isReference = false;
        this.isRValueReference = false;
        this.isConst = false;
        this.isPointer = false;
        this.isVolatile = false;
    }

    // Existing annotation methods remain the same
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

    // Existing basic getters remain the same
    public String getName() {
        return name;
    }

    public UMLType getType() {
        return type;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    // Existing property getters and setters remain the same
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

    // Python-specific getters and setters
    public boolean isKeywordOnly() {
        return isKeywordOnly;
    }

    public void setKeywordOnly(boolean keywordOnly) {
        this.isKeywordOnly = keywordOnly;
    }

    // C++-specific getters and setters
    public boolean isReference() {
        return isReference;
    }

    public void setReference(boolean reference) {
        // Cannot be both reference and rvalue reference
        if (reference && this.isRValueReference) {
            this.isRValueReference = false;
        }
        this.isReference = reference;
    }

    public boolean isRValueReference() {
        return isRValueReference;
    }

    public void setRValueReference(boolean rValueReference) {
        // Cannot be both reference and rvalue reference
        if (rValueReference && this.isReference) {
            this.isReference = false;
        }
        this.isRValueReference = rValueReference;
    }

    public boolean isConst() {
        return isConst;
    }

    public void setConst(boolean isConst) {
        this.isConst = isConst;
    }

    public boolean isPointer() {
        return isPointer;
    }

    public void setPointer(boolean pointer) {
        this.isPointer = pointer;
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    public void setVolatile(boolean volatile_) {
        this.isVolatile = volatile_;
    }

    // Updated getSignature() to include C++ qualifiers
    public String getSignature() {
        StringBuilder sb = new StringBuilder();

        // Add annotations
        for (UMLAnnotation annotation : annotations) {
            sb.append(annotation.toString()).append(" ");
        }

        // Add const qualifier if present
        if (isConst) {
            sb.append("const ");
        }

        // Add volatile qualifier if present
        if (isVolatile) {
            sb.append("volatile ");
        }

        // Add final modifier if present
        if (isFinal) {
            sb.append("final ");
        }

        // Add type
        sb.append(type.toString());

        // Add pointer or reference symbols
        if (isPointer) {
            sb.append("*");
        }
        if (isReference) {
            sb.append("&");
        }
        if (isRValueReference) {
            sb.append("&&");
        }

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

    // Existing toString, equals, and hashCode methods remain the same
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

    // Updated Builder pattern to include C++ features
    public static class Builder {
        private final String name;
        private final UMLType type;
        private final LocationInfo locationInfo;
        private final List<UMLAnnotation> annotations = new ArrayList<>();
        private String kind = "in";
        private boolean isVarargs;
        private boolean isFinal;
        private String defaultValue;
        private boolean isKeywordOnly;
        private boolean isReference;
        private boolean isRValueReference;
        private boolean isConst;
        private boolean isPointer;
        private boolean isVolatile;

        public Builder(String name, UMLType type, LocationInfo locationInfo) {
            this.name = name;
            this.type = type;
            this.locationInfo = locationInfo;
        }

        // Existing builder methods
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

        public Builder setKeywordOnly(boolean keywordOnly) {
            this.isKeywordOnly = keywordOnly;
            return this;
        }

        // New C++-specific builder methods
        public Builder setReference(boolean reference) {
            this.isReference = reference;
            if (reference) this.isRValueReference = false; // Mutually exclusive
            return this;
        }

        public Builder setRValueReference(boolean rValueReference) {
            this.isRValueReference = rValueReference;
            if (rValueReference) this.isReference = false; // Mutually exclusive
            return this;
        }

        public Builder setConst(boolean isConst) {
            this.isConst = isConst;
            return this;
        }

        public Builder setPointer(boolean pointer) {
            this.isPointer = pointer;
            return this;
        }

        public Builder setVolatile(boolean volatile_) {
            this.isVolatile = volatile_;
            return this;
        }

        public UMLParameter build() {
            UMLParameter parameter = new UMLParameter(name, type, locationInfo);
            parameter.setKind(kind);
            parameter.setVarargs(isVarargs);
            parameter.setFinal(isFinal);
            parameter.setKeywordOnly(isKeywordOnly);
            parameter.setReference(isReference);
            parameter.setRValueReference(isRValueReference);
            parameter.setConst(isConst);
            parameter.setPointer(isPointer);
            parameter.setVolatile(isVolatile);
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
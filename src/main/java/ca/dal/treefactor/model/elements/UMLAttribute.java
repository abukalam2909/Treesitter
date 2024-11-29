package ca.dal.treefactor.model.elements;

import ca.dal.treefactor.model.core.*;
import java.util.*;

public class UMLAttribute {
    // Core attributes
    private final String name;
    private final LocationInfo locationInfo;
    private UMLType type;
    private String className;  // The class this attribute belongs to

    // Documentation and metadata
    private final List<UMLComment> comments;
    private final List<UMLAnnotation> annotations;

    // Modifiers
    private Visibility visibility;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isVolatile;  // For Java/C++
    private boolean isTransient; // For Java
    private boolean isConst;     // For C++
    private boolean isReadOnly;  // For TypeScript/C#

    // Value
    private String initialValue;

    public UMLAttribute(String name, UMLType type, LocationInfo locationInfo) {
        this.name = name;
        this.type = type;
        this.locationInfo = locationInfo;
        this.comments = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.visibility = Visibility.DEFAULT;
    }

    // Comment management
    public void addComment(UMLComment comment) {
        if (!comments.contains(comment)) {
            comments.add(comment);
        }
    }

    public void removeComment(UMLComment comment) {
        comments.remove(comment);
    }

    public List<UMLComment> getComments() {
        return new ArrayList<>(comments);
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

    // Basic getters and setters
    public String getName() {
        return name;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public UMLType getType() {
        return type;
    }

    public void setType(UMLType type) {
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    // Modifier getters and setters
    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    public void setVolatile(boolean isVolatile) {
        this.isVolatile = isVolatile;
    }

    public boolean isTransient() {
        return isTransient;
    }

    public void setTransient(boolean isTransient) {
        this.isTransient = isTransient;
    }

    public boolean isConst() {
        return isConst;
    }

    public void setConst(boolean isConst) {
        this.isConst = isConst;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public String getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }

    // Utility methods
    public boolean hasInitialValue() {
        return initialValue != null && !initialValue.isEmpty();
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    public boolean hasComments() {
        return !comments.isEmpty();
    }

    public String getSignature() {
        StringBuilder sb = new StringBuilder();

        // Add annotations
        for (UMLAnnotation annotation : annotations) {
            sb.append(annotation.toString()).append("\n");
        }

        // Add modifiers
        if (visibility != Visibility.DEFAULT) {
            sb.append(visibility.toString().toLowerCase()).append(" ");
        }
        if (isStatic) sb.append("static ");
        if (isFinal) sb.append("final ");
        if (isVolatile) sb.append("volatile ");
        if (isTransient) sb.append("transient ");
        if (isConst) sb.append("const ");
        if (isReadOnly) sb.append("readonly ");

        // Add type and name
        sb.append(type.toString()).append(" ").append(name);

        // Add initial value if present
        if (hasInitialValue()) {
            sb.append(" = ").append(initialValue);
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

        UMLAttribute attribute = (UMLAttribute) o;
        return Objects.equals(name, attribute.name) &&
                Objects.equals(className, attribute.className) &&
                Objects.equals(type, attribute.type) &&
                Objects.equals(locationInfo, attribute.locationInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, className, type, locationInfo);
    }

    // Builder pattern
    public static class Builder {
        private final String name;
        private final UMLType type;
        private final LocationInfo locationInfo;
        private String className;
        private Visibility visibility = Visibility.DEFAULT;
        private boolean isStatic;
        private boolean isFinal;
        private boolean isVolatile;
        private boolean isTransient;
        private boolean isConst;
        private boolean isReadOnly;
        private String initialValue;

        public Builder(String name, UMLType type, LocationInfo locationInfo) {
            this.name = name;
            this.type = type;
            this.locationInfo = locationInfo;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder setStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public Builder setFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public Builder setVolatile(boolean isVolatile) {
            this.isVolatile = isVolatile;
            return this;
        }

        public Builder setTransient(boolean isTransient) {
            this.isTransient = isTransient;
            return this;
        }

        public Builder setConst(boolean isConst) {
            this.isConst = isConst;
            return this;
        }

        public Builder setReadOnly(boolean isReadOnly) {
            this.isReadOnly = isReadOnly;
            return this;
        }

        public Builder initialValue(String initialValue) {
            this.initialValue = initialValue;
            return this;
        }

        public UMLAttribute build() {
            UMLAttribute attribute = new UMLAttribute(name, type, locationInfo);
            attribute.setClassName(className);
            attribute.setVisibility(visibility);
            attribute.setStatic(isStatic);
            attribute.setFinal(isFinal);
            attribute.setVolatile(isVolatile);
            attribute.setTransient(isTransient);
            attribute.setConst(isConst);
            attribute.setReadOnly(isReadOnly);
            attribute.setInitialValue(initialValue);
            return attribute;
        }
    }

    public static Builder builder(String name, UMLType type, LocationInfo locationInfo) {
        return new Builder(name, type, locationInfo);
    }
}
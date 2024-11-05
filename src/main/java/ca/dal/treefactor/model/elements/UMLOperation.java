package ca.dal.treefactor.model.elements;

import ca.dal.treefactor.model.core.*;
import java.util.*;

public class UMLOperation {
    private final String name;
    private final LocationInfo locationInfo;
    private final List<UMLParameter> parameters;
    private final List<UMLComment> comments;
    private final List<UMLAnnotation> annotations;
    private final List<UMLType> thrownExceptions;
    private UMLType returnType;
    private String className; // The class this operation belongs to

    // Operation properties
    private Visibility visibility;
    private boolean isAbstract;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isConstructor;
    private boolean isSynchronized;
    private boolean isNative;
    private boolean isDefault;
    private String body; // The operation's body/implementation

    public UMLOperation(String name, LocationInfo locationInfo) {
        this.name = name;
        this.locationInfo = locationInfo;
        this.parameters = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.thrownExceptions = new ArrayList<>();
        this.visibility = Visibility.DEFAULT;
    }

    // Parameter management
    public void addParameter(UMLParameter parameter) {
        parameters.add(parameter);
    }

    public void removeParameter(UMLParameter parameter) {
        parameters.remove(parameter);
    }

    public List<UMLParameter> getParameters() {
        return new ArrayList<>(parameters);
    }

    public UMLParameter getParameter(String parameterName) {
        for (UMLParameter parameter : parameters) {
            if (parameter.getName().equals(parameterName)) {
                return parameter;
            }
        }
        return null;
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

    // Exception management
    public void addThrownException(UMLType exceptionType) {
        if (!thrownExceptions.contains(exceptionType)) {
            thrownExceptions.add(exceptionType);
        }
    }

    public void removeThrownException(UMLType exceptionType) {
        thrownExceptions.remove(exceptionType);
    }

    public List<UMLType> getThrownExceptions() {
        return new ArrayList<>(thrownExceptions);
    }

    // Basic getters and setters
    public String getName() {
        return name;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public UMLType getReturnType() {
        return returnType;
    }

    public void setReturnType(UMLType returnType) {
        this.returnType = returnType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    // Modifiers getters and setters
    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
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

    public boolean isConstructor() {
        return isConstructor;
    }

    public void setConstructor(boolean isConstructor) {
        this.isConstructor = isConstructor;
    }

    public boolean isSynchronized() {
        return isSynchronized;
    }

    public void setSynchronized(boolean isSynchronized) {
        this.isSynchronized = isSynchronized;
    }

    public boolean isNative() {
        return isNative;
    }

    public void setNative(boolean isNative) {
        this.isNative = isNative;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    // Utility methods
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
        if (isAbstract) sb.append("abstract ");
        if (isSynchronized) sb.append("synchronized ");
        if (isNative) sb.append("native ");
        if (isDefault) sb.append("default ");

        // Add return type for non-constructors
        if (!isConstructor && returnType != null) {
            sb.append(returnType.toString()).append(" ");
        }

        // Add name and parameters
        sb.append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).toString());
        }
        sb.append(")");

        // Add thrown exceptions
        if (!thrownExceptions.isEmpty()) {
            sb.append(" throws ");
            for (int i = 0; i < thrownExceptions.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(thrownExceptions.get(i).toString());
            }
        }

        return sb.toString();
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    public boolean throwsExceptions() {
        return !thrownExceptions.isEmpty();
    }

    @Override
    public String toString() {
        return getSignature();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLOperation operation = (UMLOperation) o;
        return Objects.equals(name, operation.name) &&
                Objects.equals(className, operation.className) &&
                Objects.equals(parameters, operation.parameters) &&
                Objects.equals(locationInfo, operation.locationInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, className, parameters, locationInfo);
    }

    // Builder pattern for fluent API
    public static class Builder {
        private final String name;
        private final LocationInfo locationInfo;
        private final List<UMLParameter> parameters = new ArrayList<>();
        private final List<UMLAnnotation> annotations = new ArrayList<>();
        private final List<UMLType> thrownExceptions = new ArrayList<>();
        private UMLType returnType;
        private String className;
        private Visibility visibility = Visibility.DEFAULT;
        private boolean isAbstract;
        private boolean isStatic;
        private boolean isFinal;
        private boolean isConstructor;
        private boolean isSynchronized;
        private boolean isNative;
        private boolean isDefault;
        private String body;

        public Builder(String name, LocationInfo locationInfo) {
            this.name = name;
            this.locationInfo = locationInfo;
        }

        public Builder addParameter(UMLParameter parameter) {
            parameters.add(parameter);
            return this;
        }

        public Builder addAnnotation(UMLAnnotation annotation) {
            annotations.add(annotation);
            return this;
        }

        public Builder addThrownException(UMLType exceptionType) {
            thrownExceptions.add(exceptionType);
            return this;
        }

        public Builder returnType(UMLType returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder setAbstract(boolean isAbstract) {
            this.isAbstract = isAbstract;
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

        public Builder setConstructor(boolean isConstructor) {
            this.isConstructor = isConstructor;
            return this;
        }

        public Builder setSynchronized(boolean isSynchronized) {
            this.isSynchronized = isSynchronized;
            return this;
        }

        public Builder setNative(boolean isNative) {
            this.isNative = isNative;
            return this;
        }

        public Builder setDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public UMLOperation build() {
            UMLOperation operation = new UMLOperation(name, locationInfo);
            operation.setReturnType(returnType);
            operation.setClassName(className);
            operation.setVisibility(visibility);
            operation.setAbstract(isAbstract);
            operation.setStatic(isStatic);
            operation.setFinal(isFinal);
            operation.setConstructor(isConstructor);
            operation.setSynchronized(isSynchronized);
            operation.setNative(isNative);
            operation.setDefault(isDefault);
            operation.setBody(body);

            parameters.forEach(operation::addParameter);
            annotations.forEach(operation::addAnnotation);
            thrownExceptions.forEach(operation::addThrownException);

            return operation;
        }
    }

    public static Builder builder(String name, LocationInfo locationInfo) {
        return new Builder(name, locationInfo);
    }
}
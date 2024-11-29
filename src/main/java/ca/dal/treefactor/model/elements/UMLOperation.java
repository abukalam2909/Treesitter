package ca.dal.treefactor.model.elements;

import ca.dal.treefactor.model.core.*;
import java.util.*;

public class UMLOperation {
    // Core attributes
    private final String name;
    private final LocationInfo locationInfo;
    private String className;  // Optional: for methods belonging to classes

    // Type information
    private UMLType returnType;
    private final List<UMLParameter> parameters;

    // Documentation and metadata
    private final List<UMLComment> comments;
    private final List<UMLAnnotation> annotations;  // For decorators/attributes

    // Modifiers
    private Visibility visibility;
    private boolean isStatic;
    private boolean isAbstract;
    private boolean isFinal;
    private boolean isConstructor;
    private boolean isDestructor;  // For C++
    private boolean isAsync;       // For JavaScript/Python
    private boolean isGenerator;   // For JavaScript/Python
    private boolean isVirtual;     // For C++
    private boolean isConst;       // For C++
    private boolean isInline;      // For C++
    private boolean isNoexcept;    // For C++

    // Method body
    private String body;

    public UMLOperation(String name, LocationInfo locationInfo) {
        this.name = name;
        this.locationInfo = locationInfo;
        this.parameters = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.visibility = Visibility.PUBLIC;  // Default visibility
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

    public Optional<UMLParameter> getParameter(String paramName) {
        return parameters.stream()
                .filter(p -> p.getName().equals(paramName))
                .findFirst();
    }

    // Documentation management
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

    // Annotation/Decorator management
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

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
                .anyMatch(a -> a.getName().equals(annotationName));
    }

    // Getters and setters
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

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
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

    public boolean isDestructor() {
        return isDestructor;
    }

    public void setDestructor(boolean isDestructor) {
        this.isDestructor = isDestructor;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    public boolean isGenerator() {
        return isGenerator;
    }

    public void setGenerator(boolean isGenerator) {
        this.isGenerator = isGenerator;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean isVirtual) {
        this.isVirtual = isVirtual;
    }

    public boolean isConst() {
        return isConst;
    }

    public void setConst(boolean isConst) {
        this.isConst = isConst;
    }

    public boolean isInline() {
        return isInline;
    }

    public void setInline(boolean isInline) {
        this.isInline = isInline;
    }

    public boolean isNoexcept() {
        return isNoexcept;
    }

    public void setNoexcept(boolean isNoexcept) {
        this.isNoexcept = isNoexcept;
    }

    // Utility methods
    public boolean isInstanceMethod() {
        return !isStatic && className != null;
    }

    public boolean isClassMethod() {
        return isStatic && className != null;
    }

    public boolean isStandaloneFunction() {
        return className == null;
    }

    public String getSignature() {
        StringBuilder sb = new StringBuilder();

        // Add annotations/decorators
        for (UMLAnnotation annotation : annotations) {
            sb.append(annotation.toString()).append("\n");
        }

        // Add modifiers
        if (visibility != Visibility.DEFAULT) {
            sb.append(visibility.toString().toLowerCase()).append(" ");
        }
        if (isStatic) sb.append("static ");
        if (isAbstract) sb.append("abstract ");
        if (isFinal) sb.append("final ");
        if (isVirtual) sb.append("virtual ");
        if (isAsync) sb.append("async ");

        // Add return type if not constructor
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

        if (isInline) sb.append("inline ");

        // Add const qualifier for C++
        if (isConst) sb.append(" const");
        if (isNoexcept) sb.append(" noexcept");

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

        UMLOperation that = (UMLOperation) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(className, that.className) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(locationInfo, that.locationInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, className, parameters, locationInfo);
    }

    // Builder pattern for easier construction
    public static class Builder {
        private final String name;
        private final LocationInfo locationInfo;
        private String className;
        private UMLType returnType;
        private final List<UMLParameter> parameters = new ArrayList<>();
        private final List<UMLAnnotation> annotations = new ArrayList<>();
        private Visibility visibility = Visibility.PUBLIC;
        private boolean isStatic;
        private boolean isAbstract;
        private boolean isFinal;
        private boolean isConstructor;
        private boolean isAsync;
        private boolean isVirtual;
        private boolean isConst;
        private boolean isInline;
        private boolean isNoexcept;
        private String body;

        public Builder(String name, LocationInfo locationInfo) {
            this.name = name;
            this.locationInfo = locationInfo;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder returnType(UMLType returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder addParameter(UMLParameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Builder addAnnotation(UMLAnnotation annotation) {
            this.annotations.add(annotation);
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

        public Builder setAbstract(boolean isAbstract) {
            this.isAbstract = isAbstract;
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

        public Builder setAsync(boolean isAsync) {
            this.isAsync = isAsync;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder setVirtual(boolean isVirtual) {
            this.isVirtual = isVirtual;
            return this;
        }

        public Builder setConst(boolean isConst) {
            this.isConst = isConst;
            return this;
        }

        public Builder setInline(boolean isInline) {
            this.isInline = isInline;
            return this;
        }

        public Builder setNoexcept(boolean isNoexcept) {
            this.isNoexcept = isNoexcept;
            return this;
        }

        public UMLOperation build() {
            UMLOperation operation = new UMLOperation(name, locationInfo);
            operation.setClassName(className);
            operation.setReturnType(returnType);
            parameters.forEach(operation::addParameter);
            annotations.forEach(operation::addAnnotation);
            operation.setVisibility(visibility);
            operation.setStatic(isStatic);
            operation.setAbstract(isAbstract);
            operation.setFinal(isFinal);
            operation.setConstructor(isConstructor);
            operation.setAsync(isAsync);
            operation.setVirtual(isVirtual);
            operation.setConst(isConst);      // Make sure this is being set
            operation.setInline(isInline);    // Make sure this is being set
            operation.setNoexcept(isNoexcept);// Make sure this is being set
            operation.setBody(body);
            return operation;
        }
    }

    public static Builder builder(String name, LocationInfo locationInfo) {
        return new Builder(name, locationInfo);
    }
}
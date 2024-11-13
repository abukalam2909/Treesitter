package ca.dal.treefactor.model.elements;

import ca.dal.treefactor.model.core.*;
import java.util.*;

public class UMLClass {
    // Core attributes
    private final String packageName;
    private final String name;
    private final LocationInfo locationInfo;
    private final List<UMLOperation> operations;
    private final List<UMLAttribute> attributes;
    private final List<UMLComment> comments;
    private final List<UMLAnnotation> annotations;
    private final List<String> superclasses;
    private final List<String> interfaces;

    // Class properties
    private Visibility visibility;
    private boolean isAbstract;
    private boolean isInterface;
    private boolean isFinal;
    private boolean isStatic;
    private boolean isEnum;
    private boolean isRecord;
    private boolean isInnerClass;
    private boolean isTemplate;  // For C++ templates

    // Constructor
    public UMLClass(String packageName, String name, LocationInfo locationInfo) {
        this.packageName = packageName;
        this.name = name;
        this.locationInfo = locationInfo;
        this.operations = new ArrayList<>();
        this.attributes = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.superclasses = new ArrayList<>();
        this.interfaces = new ArrayList<>();
        this.visibility = Visibility.PUBLIC;
    }

    // Operation management
    public void addOperation(UMLOperation operation) {
        if (!operations.contains(operation)) {
            operation.setClassName(name);
            System.out.println("Adding operation " + operation.getName() + " to class " + getName());
            operations.add(operation);
            System.out.println("Current operations in class: " + operations.size());

        }
    }

    public void removeOperation(UMLOperation operation) {
        operations.remove(operation);
    }

    public List<UMLOperation> getOperations() {
        return new ArrayList<>(operations);
    }

    public Optional<UMLOperation> getOperation(String operationName) {
        return operations.stream()
                .filter(op -> op.getName().equals(operationName))
                .findFirst();
    }

    // Attribute management
    public void addAttribute(UMLAttribute attribute) {
        if (!attributes.contains(attribute)) {
            attribute.setClassName(name);
            attributes.add(attribute);
        }
    }

    public void removeAttribute(UMLAttribute attribute) {
        attributes.remove(attribute);
    }

    public List<UMLAttribute> getAttributes() {
        return new ArrayList<>(attributes);
    }

    public Optional<UMLAttribute> getAttribute(String attributeName) {
        return attributes.stream()
                .filter(attr -> attr.getName().equals(attributeName))
                .findFirst();
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

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
                .anyMatch(a -> a.getName().equals(annotationName));
    }

    // Inheritance management
    public void addSuperclass(String superclass) {
        if (!superclasses.contains(superclass)) {
            superclasses.add(superclass);
        }
    }

    public void removeSuperclass(String superclass) {
        superclasses.remove(superclass);
    }

    public List<String> getSuperclasses() {
        return new ArrayList<>(superclasses);
    }

    public void addInterface(String interfaceName) {
        if (!interfaces.contains(interfaceName)) {
            interfaces.add(interfaceName);
        }
    }

    public void removeInterface(String interfaceName) {
        interfaces.remove(interfaceName);
    }

    public List<String> getInterfaces() {
        return new ArrayList<>(interfaces);
    }

    // Getters and setters
    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

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

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    public boolean isRecord() {
        return isRecord;
    }

    public void setRecord(boolean isRecord) {
        this.isRecord = isRecord;
    }

    public boolean isInnerClass() {
        return isInnerClass;
    }

    public void setInnerClass(boolean isInnerClass) {
        this.isInnerClass = isInnerClass;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    // Utility methods
    public String getFullyQualifiedName() {
        return packageName.isEmpty() ? name : packageName + "." + name;
    }

    public boolean hasOperations() {
        return !operations.isEmpty();
    }

    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    public boolean hasInheritance() {
        return !superclasses.isEmpty() || !interfaces.isEmpty();
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    public List<UMLOperation> getConstructors() {
        return operations.stream()
                .filter(UMLOperation::isConstructor)
                .toList();
    }

    public List<UMLOperation> getInstanceMethods() {
        return operations.stream()
                .filter(UMLOperation::isInstanceMethod)
                .toList();
    }

    public List<UMLOperation> getClassMethods() {
        return operations.stream()
                .filter(UMLOperation::isClassMethod)
                .toList();
    }

    // Class signature generation
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

        // Add type
        if (isInterface) {
            sb.append("interface ");
        } else if (isEnum) {
            sb.append("enum ");
        } else if (isRecord) {
            sb.append("record ");
        } else {
            sb.append("class ");
        }

        // Add name
        sb.append(name);

        // Add superclasses
        if (!superclasses.isEmpty()) {
            sb.append(" extends ").append(String.join(", ", superclasses));
        }

        // Add interfaces
        if (!interfaces.isEmpty()) {
            sb.append(" implements ").append(String.join(", ", interfaces));
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

        UMLClass umlClass = (UMLClass) o;
        return Objects.equals(packageName, umlClass.packageName) &&
                Objects.equals(name, umlClass.name) &&
                Objects.equals(locationInfo, umlClass.locationInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, name, locationInfo);
    }

    // Builder pattern
    public static class Builder {
        private final String name;
        private final LocationInfo locationInfo;
        private String packageName = "";
        private Visibility visibility = Visibility.PUBLIC;
        private boolean isAbstract = false;
        private boolean isInterface = false;
        private boolean isFinal = false;
        private boolean isStatic = false;
        private boolean isEnum = false;
        private boolean isRecord = false;
        private boolean isInnerClass = false;
        private boolean isTemplate = false;

        public Builder(String name, LocationInfo locationInfo) {
            this.name = name;
            this.locationInfo = locationInfo;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
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

        public Builder setInterface(boolean isInterface) {
            this.isInterface = isInterface;
            return this;
        }

        public Builder setFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public Builder setStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public Builder setEnum(boolean isEnum) {
            this.isEnum = isEnum;
            return this;
        }

        public Builder setRecord(boolean isRecord) {
            this.isRecord = isRecord;
            return this;
        }

        public Builder setInnerClass(boolean isInnerClass) {
            this.isInnerClass = isInnerClass;
            return this;
        }

        public Builder setTemplate(boolean isTemplate) {
            this.isTemplate = isTemplate;
            return this;
        }

        public UMLClass build() {
            UMLClass umlClass = new UMLClass(packageName, name, locationInfo);
            umlClass.setVisibility(visibility);
            umlClass.setAbstract(isAbstract);
            umlClass.setInterface(isInterface);
            umlClass.setFinal(isFinal);
            umlClass.setStatic(isStatic);
            umlClass.setEnum(isEnum);
            umlClass.setRecord(isRecord);
            umlClass.setInnerClass(isInnerClass);
            umlClass.setTemplate(isTemplate);
            return umlClass;
        }
    }

    public static Builder builder(String name, LocationInfo locationInfo) {
        return new Builder(name, locationInfo);
    }
}
package ca.dal.treefactor.model.elements;

import ca.dal.treefactor.model.core.*;
import java.util.*;

public class UMLClass {
    private final String packageName;
    private final String name;
    private final LocationInfo locationInfo;
    private final List<UMLOperation> operations;
    private final List<UMLAttribute> attributes;
    private final List<UMLComment> comments;
    private final List<String> superclasses;
    private final List<String> interfaces;
    private final List<UMLAnnotation> annotations;

    // Class properties
    private boolean isInterface;
    private boolean isAbstract;
    private boolean isEnum;
    private boolean isRecord;
    private boolean isStatic;
    private boolean isFinal;
    private Visibility visibility;

    // Constructor
    public UMLClass(String packageName, String name, LocationInfo locationInfo) {
        this.packageName = packageName;
        this.name = name;
        this.locationInfo = locationInfo;
        this.operations = new ArrayList<>();
        this.attributes = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.superclasses = new ArrayList<>();
        this.interfaces = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.visibility = Visibility.DEFAULT;
    }

    // Operations management
    public void addOperation(UMLOperation operation) {
        if (!operations.contains(operation)) {
            operations.add(operation);
        }
    }

    public void removeOperation(UMLOperation operation) {
        operations.remove(operation);
    }

    public List<UMLOperation> getOperations() {
        return new ArrayList<>(operations);
    }

    public UMLOperation getOperation(String operationName) {
        for (UMLOperation operation : operations) {
            if (operation.getName().equals(operationName)) {
                return operation;
            }
        }
        return null;
    }

    // Attributes management
    public void addAttribute(UMLAttribute attribute) {
        if (!attributes.contains(attribute)) {
            attributes.add(attribute);
        }
    }

    public void removeAttribute(UMLAttribute attribute) {
        attributes.remove(attribute);
    }

    public List<UMLAttribute> getAttributes() {
        return new ArrayList<>(attributes);
    }

    public UMLAttribute getAttribute(String attributeName) {
        for (UMLAttribute attribute : attributes) {
            if (attribute.getName().equals(attributeName)) {
                return attribute;
            }
        }
        return null;
    }

    // Comments management
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

    // Annotations management
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

    // Getters for immutable fields
    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    // Class type setters and getters
    public void setInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setRecord(boolean isRecord) {
        this.isRecord = isRecord;
    }

    public boolean isRecord() {
        return isRecord;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Visibility getVisibility() {
        return visibility;
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

    public boolean hasSuperclass() {
        return !superclasses.isEmpty();
    }

    public boolean implementsInterfaces() {
        return !interfaces.isEmpty();
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    // Override methods
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Add annotations
        for (UMLAnnotation annotation : annotations) {
            sb.append(annotation.toString()).append("\n");
        }

        // Add visibility and modifiers
        sb.append(visibility.toString().toLowerCase()).append(" ");
        if (isAbstract && !isInterface) sb.append("abstract ");
        if (isStatic) sb.append("static ");
        if (isFinal) sb.append("final ");

        // Add class type and name
        if (isInterface) sb.append("interface ");
        else if (isEnum) sb.append("enum ");
        else if (isRecord) sb.append("record ");
        else sb.append("class ");
        sb.append(name);

        // Add inheritance
        if (!superclasses.isEmpty()) {
            sb.append(" extends ").append(String.join(", ", superclasses));
        }
        if (!interfaces.isEmpty()) {
            sb.append(" implements ").append(String.join(", ", interfaces));
        }

        return sb.toString();
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
}
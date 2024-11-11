package ca.dal.treefactor.model.core;

import java.util.*;

public class UMLType {
    private final String typeName;
    private final List<UMLType> typeParameters; // For generics/templates
    private final List<UMLAnnotation> annotations;

    // Array and collection properties
    private boolean isArray;
    private int arrayDimensions;
    private boolean isCollection;
    private boolean isMap;

    // Type properties
    private boolean isPrimitive;
    private boolean isVoid;
    private boolean isVarargs;
    private String packageName; // For non-primitive types

    public UMLType(String typeName) {
        this.typeName = typeName;
        this.typeParameters = new ArrayList<>();
        this.annotations = new ArrayList<>();
        initializeType();
    }

    private void initializeType() {
        // Determine if it's a primitive type
        Set<String> primitiveTypes = new HashSet<>(Arrays.asList(
                "byte", "short", "int", "long", "float", "double", "boolean", "char",
                // Python numeric types
                "int", "float", "complex",
                // C++ primitive types
                "size_t", "wchar_t"
        ));
        this.isPrimitive = primitiveTypes.contains(typeName.toLowerCase());

        // Check if it's void
        this.isVoid = typeName.equals("void");

        // Extract package name for non-primitive types
        if (!isPrimitive && typeName.contains(".")) {
            int lastDot = typeName.lastIndexOf('.');
            this.packageName = typeName.substring(0, lastDot);
        }
    }

    // Type parameters management
    public void addTypeParameter(UMLType typeParameter) {
        typeParameters.add(typeParameter);
    }

    public void removeTypeParameter(UMLType typeParameter) {
        typeParameters.remove(typeParameter);
    }

    public List<UMLType> getTypeParameters() {
        return new ArrayList<>(typeParameters);
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
    public String getTypeName() {
        return typeName;
    }

    public String getPackageName() {
        return packageName;
    }

    // Array properties
    public void setArray(boolean isArray, int dimensions) {
        this.isArray = isArray;
        this.arrayDimensions = dimensions;
    }

    public boolean isArray() {
        return isArray;
    }

    public int getArrayDimensions() {
        return arrayDimensions;
    }

    // Collection properties
    public void setCollection(boolean isCollection) {
        this.isCollection = isCollection;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setMap(boolean isMap) {
        this.isMap = isMap;
    }

    public boolean isMap() {
        return isMap;
    }

    // Other properties
    public boolean isPrimitive() {
        return isPrimitive;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public void setVarargs(boolean isVarargs) {
        this.isVarargs = isVarargs;
    }

    public boolean isVarargs() {
        return isVarargs;
    }

    // Utility methods
    public String getSimpleName() {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot == -1 ? typeName : typeName.substring(lastDot + 1);
    }

    public boolean hasTypeParameters() {
        return !typeParameters.isEmpty();
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    /**
     * Returns the type's full representation as it would appear in code
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Add annotations
        for (UMLAnnotation annotation : annotations) {
            sb.append(annotation.toString()).append(" ");
        }

        // Add type name
        sb.append(typeName);

        // Add type parameters if present
        if (!typeParameters.isEmpty()) {
            sb.append("<");
            for (int i = 0; i < typeParameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeParameters.get(i).toString());
            }
            sb.append(">");
        }

        // Add array brackets if it's an array
        if (isArray) {
            sb.append("[]".repeat(arrayDimensions));
        }

        // Add varargs if applicable
        if (isVarargs) {
            sb.append("...");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLType that = (UMLType) o;
        return Objects.equals(typeName, that.typeName) &&
                Objects.equals(typeParameters, that.typeParameters) &&
                isArray == that.isArray &&
                arrayDimensions == that.arrayDimensions &&
                isVarargs == that.isVarargs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, typeParameters, isArray, arrayDimensions, isVarargs);
    }

    // Builder pattern for fluent API
    public static class Builder {
        private final String typeName;
        private final List<UMLType> typeParameters = new ArrayList<>();
        private final List<UMLAnnotation> annotations = new ArrayList<>();
        private boolean isArray;
        private int arrayDimensions;
        private boolean isCollection;
        private boolean isMap;
        private boolean isVarargs;

        public Builder(String typeName) {
            this.typeName = typeName;
        }

        public Builder addTypeParameter(UMLType typeParameter) {
            typeParameters.add(typeParameter);
            return this;
        }

        public Builder addAnnotation(UMLAnnotation annotation) {
            annotations.add(annotation);
            return this;
        }

        public Builder setArray(boolean isArray, int dimensions) {
            this.isArray = isArray;
            this.arrayDimensions = dimensions;
            return this;
        }

        public Builder setCollection(boolean isCollection) {
            this.isCollection = isCollection;
            return this;
        }

        public Builder setMap(boolean isMap) {
            this.isMap = isMap;
            return this;
        }

        public Builder setVarargs(boolean isVarargs) {
            this.isVarargs = isVarargs;
            return this;
        }

        public UMLType build() {
            UMLType type = new UMLType(typeName);
            typeParameters.forEach(type::addTypeParameter);
            annotations.forEach(type::addAnnotation);
            type.setArray(isArray, arrayDimensions);
            type.setCollection(isCollection);
            type.setMap(isMap);
            type.setVarargs(isVarargs);
            return type;
        }
    }

    public static Builder builder(String typeName) {
        return new Builder(typeName);
    }

    // Static factory methods for common types
    public static UMLType createVoidType() {
        return new UMLType("void");
    }

    public static UMLType createPrimitiveType(String typeName) {
        return new UMLType(typeName);
    }

    public static UMLType createArrayType(String elementTypeName, int dimensions) {
        UMLType type = new UMLType(elementTypeName);
        type.setArray(true, dimensions);
        return type;
    }

    public static UMLType createCollectionType(String collectionType, UMLType elementType) {
        UMLType type = new UMLType(collectionType);
        type.setCollection(true);
        type.addTypeParameter(elementType);
        return type;
    }

    public static UMLType createMapType(String mapType, UMLType keyType, UMLType valueType) {
        UMLType type = new UMLType(mapType);
        type.setMap(true);
        type.addTypeParameter(keyType);
        type.addTypeParameter(valueType);
        return type;
    }
}
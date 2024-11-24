package ca.dal.treefactor.model.core;

import java.util.*;

public class UMLAnnotation {
    private final String name;
    private final LocationInfo locationInfo;
    private final Map<String, String> values; // Stores annotation key-value pairs
    private final List<UMLAnnotation> nestedAnnotations; // For nested annotations

    public UMLAnnotation(String name, LocationInfo locationInfo) {
        this.name = name;
        this.locationInfo = locationInfo;
        this.values = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order
        this.nestedAnnotations = new ArrayList<>();
    }

    // Value management
    public void addValue(String key, String value) {
        values.put(key, value);
    }

    public void removeValue(String key) {
        values.remove(key);
    }

    public String getValue(String key) {
        return values.get(key);
    }

    public Map<String, String> getValues() {
        return new LinkedHashMap<>(values);
    }

    public boolean hasValue(String key) {
        return values.containsKey(key);
    }

    // Nested annotation management
    public void addNestedAnnotation(UMLAnnotation annotation) {
        nestedAnnotations.add(annotation);
    }

    public void removeNestedAnnotation(UMLAnnotation annotation) {
        nestedAnnotations.remove(annotation);
    }

    public List<UMLAnnotation> getNestedAnnotations() {
        return new ArrayList<>(nestedAnnotations);
    }

    // Basic getters
    public String getName() {
        return name;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    // Utility methods
    public boolean hasValues() {
        return !values.isEmpty();
    }

    public boolean hasNestedAnnotations() {
        return !nestedAnnotations.isEmpty();
    }

    /**
     * Returns the simple name of the annotation (without package)
     */
    public String getSimpleName() {
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? name : name.substring(lastDot + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendAnnotationStart(sb);
        appendAnnotationContent(sb);
        return sb.toString();
    }

    private void appendAnnotationStart(StringBuilder sb) {
        sb.append('@').append(name);
        if (hasContent()) {
            sb.append('(');
        }
    }

    private boolean hasContent() {
        return !values.isEmpty() || !nestedAnnotations.isEmpty();
    }

    private void appendAnnotationContent(StringBuilder sb) {
        if (!hasContent()) {
            return;
        }

        appendValues(sb);
        appendNestedAnnotations(sb);
        sb.append(')');
    }

    private void appendValues(StringBuilder sb) {
        if (values.isEmpty()) {
            return;
        }

        if (isSimpleValueAnnotation()) {
            sb.append(values.get("value"));
        } else {
            appendKeyValuePairs(sb);
        }
    }

    private boolean isSimpleValueAnnotation() {
        return values.size() == 1 && values.containsKey("value");
    }

    private void appendKeyValuePairs(StringBuilder sb) {
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(" = ").append(entry.getValue());
            first = false;
        }
    }

    private void appendNestedAnnotations(StringBuilder sb) {
        if (nestedAnnotations.isEmpty()) {
            return;
        }

        if (!values.isEmpty()) {
            sb.append(", ");
        }

        boolean first = true;
        for (UMLAnnotation nested : nestedAnnotations) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(nested.toString());
            first = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLAnnotation that = (UMLAnnotation) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(locationInfo, that.locationInfo) &&
                Objects.equals(values, that.values) &&
                Objects.equals(nestedAnnotations, that.nestedAnnotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, locationInfo, values, nestedAnnotations);
    }

    // Builder pattern for fluent API
    public static class Builder {
        private final String name;
        private final LocationInfo locationInfo;
        private final Map<String, String> values = new LinkedHashMap<>();
        private final List<UMLAnnotation> nestedAnnotations = new ArrayList<>();

        public Builder(String name, LocationInfo locationInfo) {
            this.name = name;
            this.locationInfo = locationInfo;
        }

        public Builder addValue(String key, String value) {
            values.put(key, value);
            return this;
        }

        public Builder addNestedAnnotation(UMLAnnotation annotation) {
            nestedAnnotations.add(annotation);
            return this;
        }

        public UMLAnnotation build() {
            UMLAnnotation annotation = new UMLAnnotation(name, locationInfo);
            values.forEach(annotation::addValue);
            nestedAnnotations.forEach(annotation::addNestedAnnotation);
            return annotation;
        }
    }

    /**
     * Creates a new builder instance for fluent construction of UMLAnnotation
     */
    public static Builder builder(String name, LocationInfo locationInfo) {
        return new Builder(name, locationInfo);
    }
}
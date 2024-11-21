package ca.dal.treefactor.model.core;

import java.util.Objects;

public class UMLImport {
    private final String importedName;
    private final LocationInfo locationInfo;
    private final ImportType type;
    private final boolean isStatic;
    private final String alias;  // For Python's 'as' keyword and JavaScript's 'as'/'default as'

    public enum ImportType {
        SINGLE,         // import specific item
        WILDCARD,      // import all (*, from x import *)
        DESTRUCTURING,  // JavaScript destructuring imports
        NAMESPACE,      // JavaScript namespace imports
        DEFAULT,        // JavaScript default imports
        RELATIVE,       // Python relative imports (.., .)
        DIRECT         // Direct file imports (require, include)
    }

    public UMLImport(String importedName, LocationInfo locationInfo, ImportType type,
                     boolean isStatic, String alias) {
        this.importedName = importedName;
        this.locationInfo = locationInfo;
        this.type = type;
        this.isStatic = isStatic;
        this.alias = alias;
    }

    // Getters
    public String getImportedName() {
        return importedName;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public ImportType getType() {
        return type;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getAlias() {
        return alias;
    }

    public boolean hasAlias() {
        return alias != null && !alias.isEmpty();
    }

    // Utility methods
    public String getSimpleName() {
        int lastDot = importedName.lastIndexOf('.');
        return lastDot == -1 ? importedName : importedName.substring(lastDot + 1);
    }

    public String getPackageName() {
        int lastDot = importedName.lastIndexOf('.');
        return lastDot == -1 ? "" : importedName.substring(0, lastDot);
    }

    public String getEffectiveName() {
        return hasAlias() ? alias : getSimpleName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case SINGLE -> buildSingleImport(sb);
            case WILDCARD -> buildWildcardImport(sb);
            case DESTRUCTURING -> buildDestructuringImport(sb);
            case NAMESPACE -> buildNamespaceImport(sb);
            case DEFAULT -> buildDefaultImport(sb);
            case RELATIVE -> buildRelativeImport(sb);
            case DIRECT -> buildDirectImport(sb);
        }

        return sb.toString();
    }

    private void buildSingleImport(StringBuilder sb) {
        sb.append("import ").append(importedName);
        appendAliasIfPresent(sb);
    }

    private void buildWildcardImport(StringBuilder sb) {
        sb.append("import * from ").append(importedName);
    }

    private void buildDestructuringImport(StringBuilder sb) {
        sb.append("import { ").append(importedName).append(" }");
        appendAliasIfPresent(sb);
    }

    private void buildNamespaceImport(StringBuilder sb) {
        sb.append("import * as ").append(alias)
                .append(" from ").append(importedName);
    }

    private void buildDefaultImport(StringBuilder sb) {
        sb.append("import ")
                .append(hasAlias() ? alias : "default")
                .append(" from ").append(importedName);
    }

    private void buildRelativeImport(StringBuilder sb) {
        sb.append("from ").append(importedName).append(" import ");
        if (hasAlias()) {
            sb.append(getSimpleName()).append(" as ").append(alias);
        } else {
            sb.append(getSimpleName());
        }
    }

    private void buildDirectImport(StringBuilder sb) {
        sb.append("require('").append(importedName).append("')");
    }

    private void appendAliasIfPresent(StringBuilder sb) {
        if (hasAlias()) {
            sb.append(" as ").append(alias);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UMLImport umlImport = (UMLImport) o;
        return isStatic == umlImport.isStatic &&
                Objects.equals(importedName, umlImport.importedName) &&
                Objects.equals(locationInfo, umlImport.locationInfo) &&
                type == umlImport.type &&
                Objects.equals(alias, umlImport.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(importedName, locationInfo, type, isStatic, alias);
    }

    // Builder pattern
    public static class Builder {
        private final String importedName;
        private final LocationInfo locationInfo;
        private ImportType type = ImportType.SINGLE;
        private boolean isStatic = false;
        private String alias;

        public Builder(String importedName, LocationInfo locationInfo) {
            this.importedName = importedName;
            this.locationInfo = locationInfo;
        }

        public Builder type(ImportType type) {
            this.type = type;
            return this;
        }

        public Builder setStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public UMLImport build() {
            return new UMLImport(importedName, locationInfo, type, isStatic, alias);
        }
    }

    public static Builder builder(String importedName, LocationInfo locationInfo) {
        return new Builder(importedName, locationInfo);
    }
}
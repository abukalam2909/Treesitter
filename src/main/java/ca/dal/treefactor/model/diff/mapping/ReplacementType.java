// ReplacementType.java
package ca.dal.treefactor.model.diff.mapping;

public enum ReplacementType {
    VARIABLE_NAME("Variable name replacement"),
    PARAMETER_NAME("Parameter name replacement"),
    METHOD_NAME("Method name replacement"),
    TYPE_NAME("Type name replacement"),
    LITERAL("Literal value replacement"),
    METHOD_INVOCATION("Method invocation replacement"),
    CLASS_INSTANCE_CREATION("Class instantiation replacement"),
    ARRAY_CREATION("Array creation replacement"),
    ARRAY_ACCESS("Array access replacement"),
    FIELD_ACCESS("Field access replacement"),
    ARGUMENT_REPLACEMENT("Method argument replacement"),
    ARGUMENT_ADDITION("Method argument addition"),
    ARGUMENT_DELETION("Method argument deletion"),
    ARGUMENT_MERGE("Method arguments merge"),
    ARGUMENT_SPLIT("Method arguments split");

    private final String description;

    ReplacementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
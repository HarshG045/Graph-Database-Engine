package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the schema (metadata) for a node type.
 * Stores the type name and a list of required property keys.
 * Equivalent to a DDL table definition in a relational system.
 */
public class NodeType {

    private final String name;
    private final List<String> requiredProperties;
    private final List<String> optionalProperties;

    public NodeType(String name) {
        this.name = name;
        this.requiredProperties = new ArrayList<>();
        this.optionalProperties = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<String> getRequiredProperties() {
        return requiredProperties;
    }

    public List<String> getOptionalProperties() {
        return optionalProperties;
    }

    public void addRequiredProperty(String propertyKey) {
        if (!requiredProperties.contains(propertyKey)) {
            requiredProperties.add(propertyKey);
        }
    }

    public void addOptionalProperty(String propertyKey) {
        if (!optionalProperties.contains(propertyKey)) {
            optionalProperties.add(propertyKey);
        }
    }

    public boolean isRequiredProperty(String key) {
        return requiredProperties.contains(key);
    }

    public List<String> getAllProperties() {
        List<String> all = new ArrayList<>(requiredProperties);
        all.addAll(optionalProperties);
        return all;
    }

    @Override
    public String toString() {
        return "NodeType{name='" + name
                + "', required=" + requiredProperties
                + ", optional=" + optionalProperties + "}";
    }
}

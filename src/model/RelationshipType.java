package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the schema (metadata) for a relationship (edge) type.
 * Stores the type name, allowed source node type, allowed destination node type,
 * and required/optional properties.
 */
public class RelationshipType {

    private final String name;
    private final String sourceNodeType;
    private final String destinationNodeType;
    private final List<String> requiredProperties;
    private final List<String> optionalProperties;

    /**
     * @param name                  Relationship type name (e.g., FRIENDS, WORKS_AT)
     * @param sourceNodeType        Allowed source node type (null = any)
     * @param destinationNodeType   Allowed destination node type (null = any)
     */
    public RelationshipType(String name, String sourceNodeType, String destinationNodeType) {
        this.name = name;
        this.sourceNodeType = sourceNodeType;
        this.destinationNodeType = destinationNodeType;
        this.requiredProperties = new ArrayList<>();
        this.optionalProperties = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getSourceNodeType() {
        return sourceNodeType;
    }

    public String getDestinationNodeType() {
        return destinationNodeType;
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

    /**
     * Checks whether a given source and destination node type combination
     * is valid for this relationship type.
     */
    public boolean allowsTypes(String srcType, String dstType) {
        boolean srcOk = (sourceNodeType == null || sourceNodeType.equals(srcType));
        boolean dstOk = (destinationNodeType == null || destinationNodeType.equals(dstType));
        return srcOk && dstOk;
    }

    @Override
    public String toString() {
        return "RelationshipType{name='" + name
                + "', from='" + sourceNodeType
                + "', to='" + destinationNodeType
                + "', required=" + requiredProperties
                + ", optional=" + optionalProperties + "}";
    }
}

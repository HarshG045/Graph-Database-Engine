package model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a directed edge (relationship) between two nodes.
 * Contains source node ID, destination node ID, relationship type, and properties.
 */
public class Edge {

    private final String sourceId;
    private final String destinationId;
    private final String relationshipType;
    private final Map<String, String> properties;

    public Edge(String sourceId, String destinationId, String relationshipType) {
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.relationshipType = relationshipType;
        this.properties = new HashMap<>();
    }

    public Edge(String sourceId, String destinationId, String relationshipType,
                Map<String, String> properties) {
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.relationshipType = relationshipType;
        this.properties = new HashMap<>(properties);
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public String toString() {
        return "Edge{" + sourceId + " -[" + relationshipType + "]-> " + destinationId
                + ", properties=" + properties + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Edge)) return false;
        Edge other = (Edge) obj;
        return this.sourceId.equals(other.sourceId)
                && this.destinationId.equals(other.destinationId)
                && this.relationshipType.equals(other.relationshipType);
    }

    @Override
    public int hashCode() {
        return (sourceId + destinationId + relationshipType).hashCode();
    }
}

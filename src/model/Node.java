package model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single node (vertex) in the graph.
 * Each node has a unique ID, a type (defined in schema), and a property map.
 */
public class Node {

    private final String id;
    private final String type;
    private final Map<String, String> properties;

    public Node(String id, String type) {
        this.id = id;
        this.type = type;
        this.properties = new HashMap<>();
    }

    public Node(String id, String type, Map<String, String> properties) {
        this.id = id;
        this.type = type;
        this.properties = new HashMap<>(properties);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
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

    public void removeProperty(String key) {
        properties.remove(key);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public String toString() {
        return "Node{id='" + id + "', type='" + type + "', properties=" + properties + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Node)) return false;
        Node other = (Node) obj;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

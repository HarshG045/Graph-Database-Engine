package query;

import java.util.HashMap;
import java.util.Map;

/**
 * Query Object
 *
 * Represents a structured, parsed query after the QueryParser has processed
 * the raw input string. Contains all fields needed by the QueryExecutor.
 *
 * This is the intermediate representation between parsing and execution,
 * equivalent to a parsed AST/plan in a real database engine.
 */
public class Query {

    /**
     * All supported query/command types.
     */
    public enum Type {
        // DDL
        CREATE_NODE_TYPE,
        CREATE_RELATIONSHIP_TYPE,
        DROP_NODE_TYPE,
        DROP_RELATIONSHIP_TYPE,
        SHOW_SCHEMA,

        // DML — Node
        ADD_NODE,
        DELETE_NODE,
        UPDATE_NODE,

        // DML — Edge
        ADD_EDGE,
        DELETE_EDGE,
        UPDATE_EDGE,

        // Query / Retrieval
        FIND,               // FIND <type> [WHERE key=value]
        NEIGHBORS,          // NEIGHBORS <nodeId>

        // Traversal
        BFS,
        DFS,
        SHORTEST_PATH,

        // Persistence
        SAVE,
        LOAD,

        // Utility
        SHOW_GRAPH,
        SHOW_INDEX,
        COUNT,              // COUNT NODES [TYPE <type>] / COUNT EDGES [TYPE <relType>]
        CLEAR,              // CLEAR / CLEAR ALL
        DESCRIBE,           // DESCRIBE <nodeId>
        UNKNOWN
    }

    private final Type type;

    /*
     * Generic fields used across command types.
     * Not all fields are populated for every type.
     */

    private String nodeId;           // Used for ADD/DELETE/UPDATE node, BFS, DFS, NEIGHBORS
    private String nodeType;         // Node type label (schema type or filter type)
    private String edgeSourceId;     // Source node for edges
    private String edgeDestId;       // Destination node for edges
    private String relationshipType; // Edge/relationship type

    private String secondNodeId;     // Used for SHORTEST_PATH destination

    private String conditionKey;     // WHERE clause key
    private String conditionValue;   // WHERE clause value

    // Properties for ADD NODE / ADD EDGE / UPDATE
    private final Map<String, String> properties;

    // Required/Optional property lists for CREATE TYPE commands (stored as comma-separated)
    private String requiredProps;
    private String optionalProps;

    // For SAVE/LOAD — optional file path
    private String filePath;

    // For CLEAR ALL — flag indicating wipe of schema too
    private boolean clearAll;

    public Query(Type type) {
        this.type = type;
        this.properties = new HashMap<>();
    }

    // Getters and setters

    public Type getType() { return type; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getEdgeSourceId() { return edgeSourceId; }
    public void setEdgeSourceId(String id) { this.edgeSourceId = id; }

    public String getEdgeDestId() { return edgeDestId; }
    public void setEdgeDestId(String id) { this.edgeDestId = id; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String rt) { this.relationshipType = rt; }

    public String getSecondNodeId() { return secondNodeId; }
    public void setSecondNodeId(String id) { this.secondNodeId = id; }

    public String getConditionKey() { return conditionKey; }
    public void setConditionKey(String key) { this.conditionKey = key; }

    public String getConditionValue() { return conditionValue; }
    public void setConditionValue(String value) { this.conditionValue = value; }

    public Map<String, String> getProperties() { return properties; }
    public void addProperty(String key, String value) { properties.put(key, value); }

    public String getRequiredProps() { return requiredProps; }
    public void setRequiredProps(String rp) { this.requiredProps = rp; }

    public String getOptionalProps() { return optionalProps; }
    public void setOptionalProps(String op) { this.optionalProps = op; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String fp) { this.filePath = fp; }

    public boolean isClearAll() { return clearAll; }
    public void setClearAll(boolean v) { this.clearAll = v; }

    @Override
    public String toString() {
        return "Query{type=" + type
                + (nodeId != null ? ", nodeId='" + nodeId + "'" : "")
                + (nodeType != null ? ", nodeType='" + nodeType + "'" : "")
                + (edgeSourceId != null ? ", src='" + edgeSourceId + "'" : "")
                + (edgeDestId != null ? ", dst='" + edgeDestId + "'" : "")
                + (relationshipType != null ? ", relType='" + relationshipType + "'" : "")
                + (secondNodeId != null ? ", to='" + secondNodeId + "'" : "")
                + (conditionKey != null ? ", where=" + conditionKey + "=" + conditionValue : "")
                + (!properties.isEmpty() ? ", props=" + properties : "")
                + "}";
    }
}

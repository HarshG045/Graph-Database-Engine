package schema;

import model.NodeType;
import model.RelationshipType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Schema Manager — DDL Layer
 *
 * Maintains the catalog of all defined node types and relationship types.
 * Equivalent to the data dictionary / system catalog in a relational DBMS.
 *
 * Supported DDL operations:
 *   CREATE NODE TYPE <name> PROPERTIES (req: [...], opt: [...])
 *   CREATE RELATIONSHIP TYPE <name> FROM <src> TO <dst> PROPERTIES (...)
 *   DROP NODE TYPE <name>
 *   DROP RELATIONSHIP TYPE <name>
 *   SHOW SCHEMA
 */
public class SchemaManager {

    // Catalog: node type name → NodeType definition
    private final Map<String, NodeType> nodeTypes;

    // Catalog: relationship type name → RelationshipType definition
    private final Map<String, RelationshipType> relationshipTypes;

    public SchemaManager() {
        this.nodeTypes = new HashMap<>();
        this.relationshipTypes = new HashMap<>();
    }

    // ─────────────────────────────────────────────
    //  Node Type DDL
    // ─────────────────────────────────────────────

    /**
     * Registers a new node type with no required properties.
     *
     * @throws IllegalArgumentException if type already exists
     */
    public void createNodeType(String typeName) {
        if (nodeTypes.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "Node type already exists: " + typeName);
        }
        nodeTypes.put(typeName, new NodeType(typeName));
        System.out.println("[SCHEMA] Node type created: " + typeName);
    }

    /**
     * Registers a new node type with pre-specified required and optional properties.
     */
    public void createNodeType(String typeName, String[] required, String[] optional) {
        if (nodeTypes.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "Node type already exists: " + typeName);
        }
        NodeType nt = new NodeType(typeName);
        if (required != null) {
            for (String p : required) nt.addRequiredProperty(p.trim());
        }
        if (optional != null) {
            for (String p : optional) nt.addOptionalProperty(p.trim());
        }
        nodeTypes.put(typeName, nt);
        System.out.println("[SCHEMA] Node type created: " + nt);
    }

    /**
     * Removes a node type from the catalog.
     *
     * @throws IllegalArgumentException if type does not exist
     */
    public void dropNodeType(String typeName) {
        if (!nodeTypes.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "Node type not found: " + typeName);
        }
        nodeTypes.remove(typeName);
        System.out.println("[SCHEMA] Node type dropped: " + typeName);
    }

    public NodeType getNodeType(String typeName) {
        return nodeTypes.get(typeName);
    }

    public boolean nodeTypeExists(String typeName) {
        return nodeTypes.containsKey(typeName);
    }

    public Collection<NodeType> getAllNodeTypes() {
        return nodeTypes.values();
    }

    // ─────────────────────────────────────────────
    //  Relationship Type DDL
    // ─────────────────────────────────────────────

    /**
     * Registers a new relationship type.
     *
     * @param typeName              Name of the relationship (e.g. FRIENDS)
     * @param sourceNodeType        Expected source node type (null = unconstrained)
     * @param destinationNodeType   Expected destination node type (null = unconstrained)
     */
    public void createRelationshipType(String typeName,
                                       String sourceNodeType,
                                       String destinationNodeType) {
        if (relationshipTypes.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "Relationship type already exists: " + typeName);
        }
        RelationshipType rt = new RelationshipType(typeName, sourceNodeType, destinationNodeType);
        relationshipTypes.put(typeName, rt);
        System.out.println("[SCHEMA] Relationship type created: " + rt);
    }

    /**
     * Registers a relationship type with required and optional properties.
     */
    public void createRelationshipType(String typeName,
                                       String sourceNodeType,
                                       String destinationNodeType,
                                       String[] required,
                                       String[] optional) {
        if (relationshipTypes.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "Relationship type already exists: " + typeName);
        }
        RelationshipType rt = new RelationshipType(typeName, sourceNodeType, destinationNodeType);
        if (required != null) {
            for (String p : required) rt.addRequiredProperty(p.trim());
        }
        if (optional != null) {
            for (String p : optional) rt.addOptionalProperty(p.trim());
        }
        relationshipTypes.put(typeName, rt);
        System.out.println("[SCHEMA] Relationship type created: " + rt);
    }

    /**
     * Removes a relationship type from the catalog.
     */
    public void dropRelationshipType(String typeName) {
        if (!relationshipTypes.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "Relationship type not found: " + typeName);
        }
        relationshipTypes.remove(typeName);
        System.out.println("[SCHEMA] Relationship type dropped: " + typeName);
    }

    public RelationshipType getRelationshipType(String typeName) {
        return relationshipTypes.get(typeName);
    }

    public boolean relationshipTypeExists(String typeName) {
        return relationshipTypes.containsKey(typeName);
    }

    public Collection<RelationshipType> getAllRelationshipTypes() {
        return relationshipTypes.values();
    }

    // ─────────────────────────────────────────────
    //  Display
    // ─────────────────────────────────────────────

    public void printSchema() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║           SCHEMA CATALOG             ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  NODE TYPES                          ║");
        if (nodeTypes.isEmpty()) {
            System.out.println("║    (none defined)                    ║");
        } else {
            for (NodeType nt : nodeTypes.values()) {
                System.out.println("║  • " + nt);
            }
        }
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  RELATIONSHIP TYPES                  ║");
        if (relationshipTypes.isEmpty()) {
            System.out.println("║    (none defined)                    ║");
        } else {
            for (RelationshipType rt : relationshipTypes.values()) {
                System.out.println("║  • " + rt);
            }
        }
        System.out.println("╚══════════════════════════════════════╝");
    }

    // ─────────────────────────────────────────────
    //  Internal access for persistence layer
    // ─────────────────────────────────────────────

    public Map<String, NodeType> getNodeTypeMap() {
        return nodeTypes;
    }

    public Map<String, RelationshipType> getRelationshipTypeMap() {
        return relationshipTypes;
    }
}

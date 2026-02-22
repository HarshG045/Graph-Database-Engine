package constraint;

import model.Edge;
import model.Node;
import model.NodeType;
import model.RelationshipType;
import schema.SchemaManager;
import storage.GraphStorage;

import java.util.List;

/**
 * Constraint Validator
 *
 * Enforces data integrity rules before any node or edge is committed to storage.
 * Equivalent to primary key, foreign key, and domain constraints in a relational DBMS.
 *
 * Rules enforced:
 *   - Node ID must be unique
 *   - Node type must be declared in schema
 *   - All required properties must be present on the node
 *   - Edge source and destination must exist in the graph
 *   - Edge relationship type must be declared in schema
 *   - Edge node types must satisfy the relationship type's source/destination constraints
 *   - All required properties must be present on the edge
 */
public class ConstraintValidator {

    private final SchemaManager schemaManager;
    private final GraphStorage graphStorage;

    public ConstraintValidator(SchemaManager schemaManager, GraphStorage graphStorage) {
        this.schemaManager = schemaManager;
        this.graphStorage = graphStorage;
    }

    // ─────────────────────────────────────────────
    //  Node Validation
    // ─────────────────────────────────────────────

    /**
     * Validates a node before insertion.
     *
     * @throws IllegalArgumentException on any violation
     */
    public void validateNodeForInsert(Node node) {
        // 1. Unique ID constraint
        if (graphStorage.nodeExists(node.getId())) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Node ID already exists: " + node.getId());
        }

        // 2. Type must be registered in schema
        if (!schemaManager.nodeTypeExists(node.getType())) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Unknown node type: '" + node.getType()
                    + "'. Define it first with CREATE NODE TYPE.");
        }

        // 3. Required properties must be present
        NodeType nodeType = schemaManager.getNodeType(node.getType());
        for (String required : nodeType.getRequiredProperties()) {
            if (!node.hasProperty(required)) {
                throw new IllegalArgumentException(
                        "[CONSTRAINT] Missing required property '" + required
                        + "' for node type '" + node.getType() + "'.");
            }
        }
    }

    /**
     * Validates a property update on an existing node.
     * Ensures required properties are not removed / left empty.
     */
    public void validatePropertyUpdate(Node node, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            NodeType nodeType = schemaManager.getNodeType(node.getType());
            if (nodeType != null && nodeType.isRequiredProperty(key)) {
                throw new IllegalArgumentException(
                        "[CONSTRAINT] Cannot set required property '" + key + "' to null/empty.");
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Edge Validation
    // ─────────────────────────────────────────────

    /**
     * Validates an edge before insertion.
     *
     * @throws IllegalArgumentException on any violation
     */
    public void validateEdgeForInsert(Edge edge) {
        String srcId = edge.getSourceId();
        String dstId = edge.getDestinationId();
        String relType = edge.getRelationshipType();

        // 1. Source node must exist
        if (!graphStorage.nodeExists(srcId)) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Source node does not exist: " + srcId);
        }

        // 2. Destination node must exist
        if (!graphStorage.nodeExists(dstId)) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Destination node does not exist: " + dstId);
        }

        // 3. Relationship type must be registered in schema
        if (!schemaManager.relationshipTypeExists(relType)) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Unknown relationship type: '" + relType
                    + "'. Define it first with CREATE RELATIONSHIP TYPE.");
        }

        // 4. Duplicate edge prevention
        if (graphStorage.edgeExists(srcId, dstId, relType)) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Edge already exists: "
                    + srcId + " -[" + relType + "]-> " + dstId);
        }

        // 5. Node type compatibility check
        RelationshipType rt = schemaManager.getRelationshipType(relType);
        Node srcNode = graphStorage.getNode(srcId);
        Node dstNode = graphStorage.getNode(dstId);

        if (!rt.allowsTypes(srcNode.getType(), dstNode.getType())) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Relationship '" + relType
                    + "' expects (" + rt.getSourceNodeType()
                    + " -> " + rt.getDestinationNodeType()
                    + ") but got (" + srcNode.getType()
                    + " -> " + dstNode.getType() + ").");
        }

        // 6. Required edge properties must be present
        List<String> requiredProps = rt.getRequiredProperties();
        for (String required : requiredProps) {
            if (!edge.hasProperty(required)) {
                throw new IllegalArgumentException(
                        "[CONSTRAINT] Missing required property '" + required
                        + "' for relationship type '" + relType + "'.");
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Accessors
    // ─────────────────────────────────────────────

    public SchemaManager getSchemaManager() {
        return schemaManager;
    }
}

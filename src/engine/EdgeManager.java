package engine;

import constraint.ConstraintValidator;
import model.Edge;
import model.RelationshipType;
import schema.SchemaManager;
import storage.GraphStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Edge Manager — DML Layer (Edge Operations)
 *
 * Handles all edge-level Create, Read, Delete operations.
 * Every operation is validated through ConstraintValidator before
 * being committed to GraphStorage.
 */
public class EdgeManager {

    private final GraphStorage storage;
    private final ConstraintValidator validator;

    public EdgeManager(GraphStorage storage, ConstraintValidator validator) {
        this.storage = storage;
        this.validator = validator;
    }

    // ─────────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────────

    /**
     * Adds a directed edge with no properties.
     */
    public void addEdge(String sourceId, String destinationId, String relationshipType) {
        Edge edge = new Edge(sourceId, destinationId, relationshipType);
        validator.validateEdgeForInsert(edge);
        storage.addEdge(edge);
        System.out.println("[EDGE] Added: " + edge);
    }

    /**
     * Adds a directed edge with properties.
     */
    public void addEdge(String sourceId, String destinationId,
                        String relationshipType, Map<String, String> properties) {
        Edge edge = new Edge(sourceId, destinationId, relationshipType, properties);
        validator.validateEdgeForInsert(edge);
        storage.addEdge(edge);
        System.out.println("[EDGE] Added: " + edge);
    }

    // ─────────────────────────────────────────────
    //  READ
    // ─────────────────────────────────────────────

    /**
     * Returns all outgoing edges from a node.
     */
    public List<Edge> getOutgoingEdges(String nodeId) {
        return storage.getOutgoingEdges(nodeId);
    }

    /**
     * Returns all incoming edges to a node.
     */
    public List<Edge> getIncomingEdges(String nodeId) {
        return storage.getIncomingEdges(nodeId);
    }

    /**
     * Returns all edges in the graph.
     */
    public List<Edge> getAllEdges() {
        return storage.getAllEdges();
    }

    /**
     * Returns all edges of a specific relationship type.
     */
    public List<Edge> getEdgesByType(String relationshipType) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : storage.getAllEdges()) {
            if (e.getRelationshipType().equals(relationshipType)) result.add(e);
        }
        return result;
    }

    /**
     * Returns outgoing edges of a specific relationship type from a node.
     */
    public List<Edge> getOutgoingEdgesByType(String nodeId, String relationshipType) {
        List<Edge> result = new ArrayList<>();
        for (Edge e : storage.getOutgoingEdges(nodeId)) {
            if (e.getRelationshipType().equals(relationshipType)) result.add(e);
        }
        return result;
    }

    // ─────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────

    /**
     * Sets or updates a property on an existing edge.
     * Validates that required properties are not cleared.
     */
    public void updateEdgeProperty(String sourceId, String destinationId,
                                   String relationshipType, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            SchemaManager sm = validator.getSchemaManager();
            RelationshipType rt = sm.getRelationshipType(relationshipType);
            if (rt != null && rt.isRequiredProperty(key)) {
                throw new IllegalArgumentException(
                        "[CONSTRAINT] Cannot set required edge property '" + key + "' to null/empty.");
            }
        }
        List<Edge> outgoing = storage.getOutgoingEdges(sourceId);
        for (Edge e : outgoing) {
            if (e.getDestinationId().equals(destinationId)
                    && e.getRelationshipType().equals(relationshipType)) {
                e.setProperty(key, value);
                System.out.println("[EDGE] Updated property '" + key + "' = '" + value
                        + "' on edge " + sourceId + " -[" + relationshipType + "]-> " + destinationId);
                return;
            }
        }
        throw new IllegalArgumentException(
                "[EDGE] Edge not found: " + sourceId + " -[" + relationshipType + "]-> " + destinationId);
    }

    // ─────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────

    /**
     * Removes a directed edge.
     */
    public void deleteEdge(String sourceId, String destinationId, String relationshipType) {
        boolean removed = storage.removeEdge(sourceId, destinationId, relationshipType);
        if (!removed) {
            throw new IllegalArgumentException(
                    "[EDGE] Edge not found: " + sourceId
                    + " -[" + relationshipType + "]-> " + destinationId);
        }
        System.out.println("[EDGE] Deleted: " + sourceId
                + " -[" + relationshipType + "]-> " + destinationId);
    }

    // ─────────────────────────────────────────────
    //  Display
    // ─────────────────────────────────────────────

    public void printAllEdges() {
        List<Edge> all = getAllEdges();
        System.out.println("── All Edges (" + all.size() + ") ──────────────────────");
        if (all.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (Edge e : all) System.out.println("  " + e);
        }
    }
}

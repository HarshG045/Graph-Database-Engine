package engine;

import constraint.ConstraintValidator;
import index.PropertyIndex;
import model.Node;
import storage.GraphStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Node Manager — DML Layer (Node Operations)
 *
 * Handles all node-level Create, Read, Update, Delete operations.
 * Every operation is validated through ConstraintValidator before
 * being committed to GraphStorage, and the PropertyIndex is kept in sync.
 */
public class NodeManager {

    private final GraphStorage storage;
    private final ConstraintValidator validator;
    private final PropertyIndex index;

    public NodeManager(GraphStorage storage, ConstraintValidator validator, PropertyIndex index) {
        this.storage = storage;
        this.validator = validator;
        this.index = index;
    }

    // ─────────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────────

    /**
     * Adds a node with no initial properties.
     */
    public void addNode(String id, String type) {
        Node node = new Node(id, type);
        validator.validateNodeForInsert(node);
        storage.addNode(node);
        index.indexNode(node);
        System.out.println("[NODE] Added: " + node);
    }

    /**
     * Adds a node with an initial set of properties.
     */
    public void addNode(String id, String type, Map<String, String> properties) {
        Node node = new Node(id, type, properties);
        validator.validateNodeForInsert(node);
        storage.addNode(node);
        index.indexNode(node);
        System.out.println("[NODE] Added: " + node);
    }

    // ─────────────────────────────────────────────
    //  READ
    // ─────────────────────────────────────────────

    public Node getNode(String id) {
        Node node = storage.getNode(id);
        if (node == null) {
            System.out.println("[NODE] Not found: " + id);
        }
        return node;
    }

    /**
     * Returns all nodes in the graph.
     */
    public List<Node> getAllNodes() {
        return new ArrayList<>(storage.getAllNodes());
    }

    /**
     * Returns all nodes of a specific type.
     */
    public List<Node> getNodesByType(String type) {
        List<Node> result = new ArrayList<>();
        for (Node n : storage.getAllNodes()) {
            if (n.getType().equals(type)) result.add(n);
        }
        return result;
    }

    /**
     * Returns nodes matching a property key=value condition.
     * Uses the PropertyIndex for fast lookup; falls back to scan if needed.
     */
    public List<Node> getNodesByProperty(String key, String value) {
        List<String> ids = index.lookup(key, value);
        List<Node> result = new ArrayList<>();
        for (String id : ids) {
            Node n = storage.getNode(id);
            if (n != null) result.add(n);
        }
        return result;
    }

    /**
     * Returns nodes matching both a type filter AND a property condition.
     */
    public List<Node> getNodesByTypeAndProperty(String type, String key, String value) {
        List<Node> byProp = getNodesByProperty(key, value);
        List<Node> result = new ArrayList<>();
        for (Node n : byProp) {
            if (n.getType().equals(type)) result.add(n);
        }
        return result;
    }

    // ─────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────

    /**
     * Sets or updates a single property on a node.
     */
    public void updateNodeProperty(String id, String key, String value) {
        Node node = storage.getNode(id);
        if (node == null) {
            throw new IllegalArgumentException("[NODE] Node not found: " + id);
        }
        validator.validatePropertyUpdate(node, key, value);
        String oldValue = node.getProperty(key);
        node.setProperty(key, value);
        index.updateEntry(id, key, oldValue, value);
        System.out.println("[NODE] Updated property '" + key + "' = '" + value
                + "' on node " + id);
    }

    // ─────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────

    /**
     * Removes a node and all its connected edges.
     */
    public void deleteNode(String id) {
        Node node = storage.getNode(id);
        if (node == null) {
            throw new IllegalArgumentException("[NODE] Node not found: " + id);
        }
        index.deindexNode(node);
        boolean removed = storage.removeNode(id);
        if (removed) {
            System.out.println("[NODE] Deleted node: " + id);
        }
    }

    // ─────────────────────────────────────────────
    //  Display
    // ─────────────────────────────────────────────

    public void printAllNodes() {
        List<Node> all = getAllNodes();
        System.out.println("── All Nodes (" + all.size() + ") ──────────────────────");
        if (all.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (Node n : all) System.out.println("  " + n);
        }
    }
}

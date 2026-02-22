package engine;

import constraint.ConstraintValidator;
import index.PropertyIndex;
import model.Edge;
import model.Node;
import schema.SchemaManager;
import storage.GraphStorage;
import storage.StorageManager;
import traversal.TraversalEngine;

import java.io.IOException;

/**
 * Graph Engine — Core Facade
 *
 * Central access point that wires all system components together.
 * Provides a single API used by the QueryExecutor and Main CLI.
 *
 *   Schema layer      → SchemaManager
 *   Storage layer     → GraphStorage
 *   Constraint layer  → ConstraintValidator
 *   Node DML layer    → NodeManager
 *   Edge DML layer    → EdgeManager
 *   Traversal layer   → TraversalEngine
 *   Index layer       → PropertyIndex
 *   Persistence layer → StorageManager
 */
public class GraphEngine {

    private final SchemaManager    schemaManager;
    private final GraphStorage     storage;
    private final ConstraintValidator validator;
    private final PropertyIndex    propertyIndex;
    private final NodeManager      nodeManager;
    private final EdgeManager      edgeManager;
    private final TraversalEngine  traversalEngine;
    private final StorageManager   storageManager;

    public GraphEngine() {
        schemaManager   = new SchemaManager();
        storage         = new GraphStorage();
        validator       = new ConstraintValidator(schemaManager, storage);
        propertyIndex   = new PropertyIndex();
        nodeManager     = new NodeManager(storage, validator, propertyIndex);
        edgeManager     = new EdgeManager(storage, validator);
        traversalEngine = new TraversalEngine(storage);
        storageManager  = new StorageManager();
    }

    // ─────────────────────────────────────────────
    //  Persistence
    // ─────────────────────────────────────────────

    public void save(String filePath) {
        try {
            storageManager.save(schemaManager, storage, filePath);
        } catch (IOException e) {
            System.out.println("[STORAGE] Save failed: " + e.getMessage());
        }
    }

    public void load(String filePath) {
        try {
            storageManager.load(schemaManager, storage, propertyIndex, filePath);
        } catch (IOException e) {
            System.out.println("[STORAGE] Load failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  Safe DROP with referential integrity checks
    // ─────────────────────────────────────────────

    /**
     * Drops a node type only if no existing nodes use it.
     *
     * @throws IllegalArgumentException if nodes of this type still exist
     */
    public void dropNodeType(String typeName) {
        int count = 0;
        for (Node n : storage.getAllNodes()) {
            if (n.getType().equals(typeName)) count++;
        }
        if (count > 0) {
            throw new IllegalArgumentException(
                    "[SCHEMA] Cannot drop node type '" + typeName + "': "
                    + count + " node(s) of this type still exist. Delete them first.");
        }
        schemaManager.dropNodeType(typeName);
    }

    /**
     * Drops a relationship type only if no existing edges use it.
     *
     * @throws IllegalArgumentException if edges of this type still exist
     */
    public void dropRelationshipType(String typeName) {
        int count = 0;
        for (Edge e : storage.getAllEdges()) {
            if (e.getRelationshipType().equals(typeName)) count++;
        }
        if (count > 0) {
            throw new IllegalArgumentException(
                    "[SCHEMA] Cannot drop relationship type '" + typeName + "': "
                    + count + " edge(s) of this type still exist. Delete them first.");
        }
        schemaManager.dropRelationshipType(typeName);
    }

    // ─────────────────────────────────────────────
    //  Clear Operations
    // ─────────────────────────────────────────────

    /**
     * Removes all nodes and edges but preserves the schema definition.
     */
    public void clearGraph() {
        storage.clear();
        propertyIndex.clear();
        System.out.println("[ENGINE] Graph data cleared. Schema is preserved.");
    }

    /**
     * Removes everything: schema, nodes, edges, and index.
     */
    public void clearAll() {
        storage.clear();
        propertyIndex.clear();
        schemaManager.getNodeTypeMap().clear();
        schemaManager.getRelationshipTypeMap().clear();
        System.out.println("[ENGINE] All data and schema cleared.");
    }

    // ─────────────────────────────────────────────
    //  Component accessors (for QueryExecutor)
    // ─────────────────────────────────────────────

    public SchemaManager    getSchemaManager()   { return schemaManager; }
    public GraphStorage     getStorage()         { return storage; }
    public ConstraintValidator getValidator()    { return validator; }
    public PropertyIndex    getPropertyIndex()   { return propertyIndex; }
    public NodeManager      getNodeManager()     { return nodeManager; }
    public EdgeManager      getEdgeManager()     { return edgeManager; }
    public TraversalEngine  getTraversalEngine() { return traversalEngine; }
    public StorageManager   getStorageManager()  { return storageManager; }
}

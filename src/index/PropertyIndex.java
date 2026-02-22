package index;

import model.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Property Index — Lightweight Indexing Layer
 *
 * Maintains a secondary index structure:
 *   Map<PropertyKey, Map<PropertyValue, List<NodeID>>>
 *
 * This allows property-based lookups in O(1) average time
 * instead of a full scan of all nodes (O(V)).
 *
 * The index is updated automatically when nodes are added,
 * updated, or deleted by the engine.
 */
public class PropertyIndex {

    // propertyKey → (propertyValue → list of nodeIds)
    private final Map<String, Map<String, List<String>>> index;

    public PropertyIndex() {
        this.index = new HashMap<>();
    }

    // ─────────────────────────────────────────────
    //  Index Maintenance
    // ─────────────────────────────────────────────

    /**
     * Indexes all properties of a newly inserted node.
     */
    public void indexNode(Node node) {
        for (Map.Entry<String, String> entry : node.getProperties().entrySet()) {
            addEntry(entry.getKey(), entry.getValue(), node.getId());
        }
    }

    /**
     * Removes all index entries belonging to a deleted node.
     */
    public void deindexNode(Node node) {
        for (Map.Entry<String, String> entry : node.getProperties().entrySet()) {
            removeEntry(entry.getKey(), entry.getValue(), node.getId());
        }
    }

    /**
     * Updates the index when a node property is changed.
     *
     * @param nodeId   ID of the node being updated
     * @param key      Property key being changed
     * @param oldValue Previous value (null if new property)
     * @param newValue New value
     */
    public void updateEntry(String nodeId, String key, String oldValue, String newValue) {
        if (oldValue != null) {
            removeEntry(key, oldValue, nodeId);
        }
        if (newValue != null) {
            addEntry(key, newValue, nodeId);
        }
    }

    // ─────────────────────────────────────────────
    //  Index Lookup
    // ─────────────────────────────────────────────

    /**
     * Returns all node IDs where propertyKey = propertyValue.
     *
     * @return list of matching node IDs (empty if none)
     */
    public List<String> lookup(String propertyKey, String propertyValue) {
        Map<String, List<String>> valueMap = index.get(propertyKey);
        if (valueMap == null) return new ArrayList<>();
        List<String> result = valueMap.get(propertyValue);
        return result != null ? new ArrayList<>(result) : new ArrayList<>();
    }

    /**
     * Checks whether any node has a given property key/value pair indexed.
     */
    public boolean hasEntry(String propertyKey, String propertyValue) {
        Map<String, List<String>> valueMap = index.get(propertyKey);
        if (valueMap == null) return false;
        List<String> ids = valueMap.get(propertyValue);
        return ids != null && !ids.isEmpty();
    }

    // ─────────────────────────────────────────────
    //  Internal helpers
    // ─────────────────────────────────────────────

    private void addEntry(String key, String value, String nodeId) {
        index.computeIfAbsent(key, k -> new HashMap<>())
             .computeIfAbsent(value, v -> new ArrayList<>())
             .add(nodeId);
    }

    private void removeEntry(String key, String value, String nodeId) {
        Map<String, List<String>> valueMap = index.get(key);
        if (valueMap == null) return;
        List<String> ids = valueMap.get(value);
        if (ids != null) {
            ids.remove(nodeId);
            if (ids.isEmpty()) valueMap.remove(value);
        }
        if (valueMap.isEmpty()) index.remove(key);
    }

    /**
     * Clears the entire index (used during graph reload).
     */
    public void clear() {
        index.clear();
    }

    public void printIndex() {
        System.out.println("[INDEX] Current property index:");
        if (index.isEmpty()) {
            System.out.println("  (empty)");
            return;
        }
        for (Map.Entry<String, Map<String, List<String>>> propEntry : index.entrySet()) {
            System.out.println("  " + propEntry.getKey() + ":");
            for (Map.Entry<String, List<String>> valEntry : propEntry.getValue().entrySet()) {
                System.out.println("    '" + valEntry.getKey() + "' → " + valEntry.getValue());
            }
        }
    }
}

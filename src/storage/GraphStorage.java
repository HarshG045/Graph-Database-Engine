package storage;

import model.Edge;
import model.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph Storage Layer
 *
 * Core in-memory storage for the graph using:
 *   - HashMap<NodeID, Node>        → O(1) node lookup
 *   - HashMap<NodeID, List<Edge>>  → Adjacency list for outgoing edges
 *   - HashMap<NodeID, List<Edge>>  → Reverse adjacency list for incoming edges
 *
 * Adjacency list representation ensures traversal in O(V + E) time.
 */
public class GraphStorage {

    // Primary node store: nodeId → Node object
    private final Map<String, Node> nodes;

    // Outgoing edges: nodeId → list of edges starting from this node
    private final Map<String, List<Edge>> adjacencyList;

    // Incoming edges: nodeId → list of edges arriving at this node
    private final Map<String, List<Edge>> reverseAdjacencyList;

    public GraphStorage() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.reverseAdjacencyList = new HashMap<>();
    }

    // ─────────────────────────────────────────────
    //  Node Operations
    // ─────────────────────────────────────────────

    /**
     * Inserts a node. Initialises its adjacency list slots.
     * Does NOT validate schema — validation is done by ConstraintValidator.
     */
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
        reverseAdjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    /**
     * Removes a node and all edges connected to it.
     *
     * @return true if the node existed and was removed
     */
    public boolean removeNode(String nodeId) {
        if (!nodes.containsKey(nodeId)) return false;

        // Remove all outgoing edges
        List<Edge> outgoing = adjacencyList.getOrDefault(nodeId, new ArrayList<>());
        for (Edge e : new ArrayList<>(outgoing)) {
            removeEdgeFromReverseList(e);
        }
        adjacencyList.remove(nodeId);

        // Remove all incoming edges
        List<Edge> incoming = reverseAdjacencyList.getOrDefault(nodeId, new ArrayList<>());
        for (Edge e : new ArrayList<>(incoming)) {
            List<Edge> srcList = adjacencyList.get(e.getSourceId());
            if (srcList != null) srcList.remove(e);
        }
        reverseAdjacencyList.remove(nodeId);

        nodes.remove(nodeId);
        return true;
    }

    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public boolean nodeExists(String nodeId) {
        return nodes.containsKey(nodeId);
    }

    public Collection<Node> getAllNodes() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    // ─────────────────────────────────────────────
    //  Edge Operations
    // ─────────────────────────────────────────────

    /**
     * Inserts a directed edge into both adjacency lists.
     */
    public void addEdge(Edge edge) {
        String src = edge.getSourceId();
        String dst = edge.getDestinationId();

        adjacencyList.computeIfAbsent(src, k -> new ArrayList<>()).add(edge);
        reverseAdjacencyList.computeIfAbsent(dst, k -> new ArrayList<>()).add(edge);
    }

    /**
     * Removes a directed edge identified by (sourceId, destinationId, relationshipType).
     *
     * @return true if the edge existed and was removed
     */
    public boolean removeEdge(String sourceId, String destinationId, String relationshipType) {
        List<Edge> outgoing = adjacencyList.get(sourceId);
        if (outgoing == null) return false;

        Edge target = null;
        for (Edge e : outgoing) {
            if (e.getDestinationId().equals(destinationId)
                    && e.getRelationshipType().equals(relationshipType)) {
                target = e;
                break;
            }
        }
        if (target == null) return false;

        outgoing.remove(target);
        removeEdgeFromReverseList(target);
        return true;
    }

    private void removeEdgeFromReverseList(Edge edge) {
        List<Edge> incoming = reverseAdjacencyList.get(edge.getDestinationId());
        if (incoming != null) incoming.remove(edge);
    }

    /**
     * Returns all outgoing edges from a node.
     */
    public List<Edge> getOutgoingEdges(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, new ArrayList<>());
    }

    /**
     * Returns all incoming edges to a node.
     */
    public List<Edge> getIncomingEdges(String nodeId) {
        return reverseAdjacencyList.getOrDefault(nodeId, new ArrayList<>());
    }

    /**
     * Returns all edges in the graph regardless of direction.
     */
    public List<Edge> getAllEdges() {
        List<Edge> all = new ArrayList<>();
        for (List<Edge> edgeList : adjacencyList.values()) {
            all.addAll(edgeList);
        }
        return all;
    }

    public int getEdgeCount() {
        int count = 0;
        for (List<Edge> list : adjacencyList.values()) {
            count += list.size();
        }
        return count;
    }

    /**
     * Checks if a specific directed edge exists.
     */
    public boolean edgeExists(String sourceId, String destinationId, String relationshipType) {
        List<Edge> outgoing = adjacencyList.get(sourceId);
        if (outgoing == null) return false;
        for (Edge e : outgoing) {
            if (e.getDestinationId().equals(destinationId)
                    && e.getRelationshipType().equals(relationshipType)) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────
    //  Debug / Display
    // ─────────────────────────────────────────────

    public void printGraph() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│               GRAPH STATE               │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ Nodes (" + getNodeCount() + ")");
        for (Node n : nodes.values()) {
            System.out.println("│   " + n);
        }
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ Edges (" + getEdgeCount() + ")");
        for (Edge e : getAllEdges()) {
            System.out.println("│   " + e);
        }
        System.out.println("└─────────────────────────────────────────┘");
    }

    /**
     * Clears all data (used during graph reload from persistence).
     */
    public void clear() {
        nodes.clear();
        adjacencyList.clear();
        reverseAdjacencyList.clear();
    }

    /**
     * Raw access to internal node map — used by StorageManager for serialisation.
     */
    public Map<String, Node> getNodeMap() {
        return nodes;
    }

    /**
     * Raw access to adjacency list — used by StorageManager for serialisation.
     */
    public Map<String, List<Edge>> getAdjacencyList() {
        return adjacencyList;
    }
}

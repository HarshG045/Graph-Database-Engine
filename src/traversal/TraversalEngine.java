package traversal;

import model.Edge;
import model.Node;
import storage.GraphStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Traversal Engine
 *
 * Implements graph traversal algorithms that replace JOIN operations
 * in relational systems:
 *
 *   BFS  — Breadth-First Search (level-order traversal)
 *   DFS  — Depth-First Search (recursive / stack-based)
 *   SP   — Shortest Path (unweighted BFS-based)
 *
 * All algorithms work on the adjacency list stored in GraphStorage.
 * Time complexity: O(V + E) for BFS and DFS.
 */
public class TraversalEngine {

    private final GraphStorage storage;

    public TraversalEngine(GraphStorage storage) {
        this.storage = storage;
    }

    // ─────────────────────────────────────────────
    //  BFS — Breadth-First Search
    // ─────────────────────────────────────────────

    /**
     * Performs BFS from the given start node.
     * Returns the list of nodes visited in BFS order.
     *
     * @param startId           ID of the starting node
     * @param relationshipType  If not null, only traverse edges of this type
     * @return ordered list of visited nodes
     */
    public List<Node> bfs(String startId, String relationshipType) {
        validateNodeExists(startId);

        List<Node> visited = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(startId);
        seen.add(startId);

        System.out.println("[BFS] Starting from node: " + startId
                + (relationshipType != null ? ", relationship filter: " + relationshipType : ""));

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            Node current = storage.getNode(currentId);
            if (current != null) {
                visited.add(current);
                System.out.println("[BFS]   Visiting: " + current);
            }

            for (Edge edge : storage.getOutgoingEdges(currentId)) {
                if (relationshipType != null
                        && !edge.getRelationshipType().equals(relationshipType)) {
                    continue;
                }
                String neighborId = edge.getDestinationId();
                if (!seen.contains(neighborId)) {
                    seen.add(neighborId);
                    queue.add(neighborId);
                }
            }
        }
        return visited;
    }

    /**
     * BFS without relationship type filter — traverses all edge types.
     */
    public List<Node> bfs(String startId) {
        return bfs(startId, null);
    }

    // ─────────────────────────────────────────────
    //  DFS — Depth-First Search
    // ─────────────────────────────────────────────

    /**
     * Performs iterative DFS from the given start node.
     * Returns nodes in DFS visit order.
     *
     * @param startId           ID of the starting node
     * @param relationshipType  If not null, only traverse edges of this type
     * @return ordered list of visited nodes
     */
    public List<Node> dfs(String startId, String relationshipType) {
        validateNodeExists(startId);

        List<Node> visited = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        stack.push(startId);

        System.out.println("[DFS] Starting from node: " + startId
                + (relationshipType != null ? ", relationship filter: " + relationshipType : ""));

        while (!stack.isEmpty()) {
            String currentId = stack.pop();
            if (seen.contains(currentId)) continue;
            seen.add(currentId);

            Node current = storage.getNode(currentId);
            if (current != null) {
                visited.add(current);
                System.out.println("[DFS]   Visiting: " + current);
            }

            // Push neighbours in reverse order so left-most is processed first
            List<Edge> edges = storage.getOutgoingEdges(currentId);
            for (int i = edges.size() - 1; i >= 0; i--) {
                Edge edge = edges.get(i);
                if (relationshipType != null
                        && !edge.getRelationshipType().equals(relationshipType)) {
                    continue;
                }
                String neighborId = edge.getDestinationId();
                if (!seen.contains(neighborId)) {
                    stack.push(neighborId);
                }
            }
        }
        return visited;
    }

    /**
     * DFS without relationship type filter.
     */
    public List<Node> dfs(String startId) {
        return dfs(startId, null);
    }

    // ─────────────────────────────────────────────
    //  Shortest Path (unweighted BFS)
    // ─────────────────────────────────────────────

    /**
     * Finds the shortest path (minimum hops) between two nodes using BFS.
     * Traverses all edge types unless a filter is specified.
     *
     * @param startId   Source node ID
     * @param endId     Target node ID
     * @return ordered list of nodes along the path (includes start and end),
     *         or empty list if no path exists
     */
    public List<Node> shortestPath(String startId, String endId) {
        return shortestPath(startId, endId, null);
    }

    /**
     * Finds the shortest path filtered to a specific relationship type.
     */
    public List<Node> shortestPath(String startId, String endId, String relationshipType) {
        validateNodeExists(startId);
        validateNodeExists(endId);

        if (startId.equals(endId)) {
            List<Node> single = new ArrayList<>();
            single.add(storage.getNode(startId));
            return single;
        }

        // BFS with parent tracking
        Map<String, String> parent = new HashMap<>();  // nodeId → parentId
        Set<String> seen = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(startId);
        seen.add(startId);
        parent.put(startId, null);

        System.out.println("[SHORTEST PATH] Searching: " + startId + " → " + endId
                + (relationshipType != null ? " via [" + relationshipType + "]" : ""));

        boolean found = false;
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            if (currentId.equals(endId)) {
                found = true;
                break;
            }
            for (Edge edge : storage.getOutgoingEdges(currentId)) {
                if (relationshipType != null
                        && !edge.getRelationshipType().equals(relationshipType)) {
                    continue;
                }
                String neighborId = edge.getDestinationId();
                if (!seen.contains(neighborId)) {
                    seen.add(neighborId);
                    parent.put(neighborId, currentId);
                    queue.add(neighborId);
                }
            }
        }

        if (!found) {
            System.out.println("[SHORTEST PATH] No path found between " + startId + " and " + endId);
            return new ArrayList<>();
        }

        // Reconstruct path by walking back through parent map
        List<Node> path = new ArrayList<>();
        String current = endId;
        while (current != null) {
            Node node = storage.getNode(current);
            if (node != null) path.add(node);
            current = parent.get(current);
        }
        Collections.reverse(path);

        System.out.println("[SHORTEST PATH] Path length: " + (path.size() - 1) + " hops");
        return path;
    }

    // ─────────────────────────────────────────────
    //  Neighbour queries
    // ─────────────────────────────────────────────

    /**
     * Returns direct neighbours (nodes reachable via a single outgoing edge).
     *
     * @param nodeId            ID of the source node
     * @param relationshipType  Optional filter (null = all types)
     */
    public List<Node> getNeighbors(String nodeId, String relationshipType) {
        validateNodeExists(nodeId);
        List<Node> neighbors = new ArrayList<>();
        for (Edge edge : storage.getOutgoingEdges(nodeId)) {
            if (relationshipType != null
                    && !edge.getRelationshipType().equals(relationshipType)) {
                continue;
            }
            Node n = storage.getNode(edge.getDestinationId());
            if (n != null) neighbors.add(n);
        }
        return neighbors;
    }

    public List<Node> getNeighbors(String nodeId) {
        return getNeighbors(nodeId, null);
    }

    // ─────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────

    private void validateNodeExists(String nodeId) {
        if (!storage.nodeExists(nodeId)) {
            throw new IllegalArgumentException(
                    "[TRAVERSAL] Node does not exist: " + nodeId);
        }
    }

    /**
     * Pretty-prints a path result.
     */
    public static void printPath(List<Node> path) {
        if (path.isEmpty()) {
            System.out.println("  (no path)");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i).getId());
            if (i < path.size() - 1) sb.append(" → ");
        }
        System.out.println("  Path: " + sb);
        System.out.println("  Nodes:");
        for (Node n : path) System.out.println("    " + n);
    }
}

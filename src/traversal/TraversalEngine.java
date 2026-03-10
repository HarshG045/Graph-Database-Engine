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
import java.util.PriorityQueue;
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

    // ─────────────────────────────────────────────
    //  Degree Analysis
    // ─────────────────────────────────────────────

    /**
     * Returns int[3] = {inDegree, outDegree, totalDegree} for a node.
     */
    public int[] degree(String nodeId) {
        validateNodeExists(nodeId);
        int out = storage.getOutgoingEdges(nodeId).size();
        int in  = storage.getIncomingEdges(nodeId).size();
        return new int[]{in, out, in + out};
    }

    // ─────────────────────────────────────────────
    //  Connected Components (undirected interpretation)
    // ─────────────────────────────────────────────

    /**
     * Finds all connected components treating the graph as undirected.
     * Returns a list where each element is a list of node IDs in one component.
     */
    public List<List<String>> connectedComponents() {
        List<List<String>> components = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Node node : storage.getAllNodes()) {
            String id = node.getId();
            if (!visited.contains(id)) {
                List<String> component = new ArrayList<>();
                // BFS treating graph as undirected
                Queue<String> queue = new LinkedList<>();
                queue.add(id);
                visited.add(id);
                while (!queue.isEmpty()) {
                    String current = queue.poll();
                    component.add(current);
                    // Outgoing neighbors
                    for (Edge e : storage.getOutgoingEdges(current)) {
                        if (!visited.contains(e.getDestinationId())) {
                            visited.add(e.getDestinationId());
                            queue.add(e.getDestinationId());
                        }
                    }
                    // Incoming neighbors (treat as undirected)
                    for (Edge e : storage.getIncomingEdges(current)) {
                        if (!visited.contains(e.getSourceId())) {
                            visited.add(e.getSourceId());
                            queue.add(e.getSourceId());
                        }
                    }
                }
                Collections.sort(component);
                components.add(component);
            }
        }
        return components;
    }

    // ─────────────────────────────────────────────
    //  Cycle Detection (directed graph)
    // ─────────────────────────────────────────────

    /**
     * Detects if the directed graph contains at least one cycle.
     * Uses DFS with three-color marking (WHITE/GRAY/BLACK).
     *
     * @return list of node IDs forming the first cycle found, or empty if acyclic
     */
    public List<String> detectCycle() {
        // 0 = white (unvisited), 1 = gray (in current path), 2 = black (done)
        Map<String, Integer> color = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        for (Node n : storage.getAllNodes()) {
            color.put(n.getId(), 0);
        }

        for (Node n : storage.getAllNodes()) {
            if (color.get(n.getId()) == 0) {
                List<String> cycle = dfsCycleVisit(n.getId(), color, parent);
                if (cycle != null) return cycle;
            }
        }
        return new ArrayList<>(); // no cycle
    }

    private List<String> dfsCycleVisit(String nodeId, Map<String, Integer> color,
                                        Map<String, String> parent) {
        color.put(nodeId, 1); // gray
        for (Edge e : storage.getOutgoingEdges(nodeId)) {
            String neighbor = e.getDestinationId();
            if (color.getOrDefault(neighbor, 0) == 1) {
                // Back edge found — reconstruct cycle
                List<String> cycle = new ArrayList<>();
                cycle.add(neighbor);
                String cur = nodeId;
                while (!cur.equals(neighbor)) {
                    cycle.add(cur);
                    cur = parent.get(cur);
                    if (cur == null) break;
                }
                cycle.add(neighbor);
                Collections.reverse(cycle);
                return cycle;
            }
            if (color.getOrDefault(neighbor, 0) == 0) {
                parent.put(neighbor, nodeId);
                List<String> cycle = dfsCycleVisit(neighbor, color, parent);
                if (cycle != null) return cycle;
            }
        }
        color.put(nodeId, 2); // black
        return null;
    }

    // ─────────────────────────────────────────────
    //  Path Exists (quick boolean check)
    // ─────────────────────────────────────────────

    /**
     * Checks if any path exists from startId to endId.
     * Optionally filters by relationship type.
     */
    public boolean pathExists(String startId, String endId, String relationshipType) {
        validateNodeExists(startId);
        validateNodeExists(endId);
        if (startId.equals(endId)) return true;

        Set<String> seen = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        seen.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (Edge e : storage.getOutgoingEdges(current)) {
                if (relationshipType != null && !e.getRelationshipType().equals(relationshipType))
                    continue;
                String neighbor = e.getDestinationId();
                if (neighbor.equals(endId)) return true;
                if (!seen.contains(neighbor)) {
                    seen.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────
    //  Weighted Shortest Path (Dijkstra)
    // ─────────────────────────────────────────────

    /**
     * Finds the shortest weighted path between two nodes using Dijkstra's algorithm.
     * Edge weights are read from the specified property key.
     * Edges without the weight property are assigned a default weight of 1.0.
     *
     * @param startId          Source node ID
     * @param endId            Target node ID
     * @param weightProperty   Edge property key to use as weight (e.g., "distance")
     * @param relationshipType Optional filter — null means all edge types
     * @return ordered list of nodes along the path, or empty if no path exists
     */
    public List<Node> weightedShortestPath(String startId, String endId,
                                            String weightProperty, String relationshipType) {
        validateNodeExists(startId);
        validateNodeExists(endId);

        if (startId.equals(endId)) {
            List<Node> single = new ArrayList<>();
            single.add(storage.getNode(startId));
            return single;
        }

        // dist[nodeId] = best known distance
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // PQ entries: [distance, nodeId]
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));

        // Use a parallel map from index to nodeId for PQ
        Map<String, Integer> nodeIdxMap = new HashMap<>();
        List<String> nodeIdList = new ArrayList<>();

        dist.put(startId, 0.0);
        parent.put(startId, null);
        // Store index in array position [1]
        int startIdx = nodeIdList.size();
        nodeIdList.add(startId);
        nodeIdxMap.put(startId, startIdx);
        pq.add(new double[]{0.0, startIdx});

        System.out.println("[DIJKSTRA] Searching: " + startId + " → " + endId
                + " using weight property '" + weightProperty + "'"
                + (relationshipType != null ? " via [" + relationshipType + "]" : ""));

        boolean found = false;
        while (!pq.isEmpty()) {
            double[] entry = pq.poll();
            double currentDist = entry[0];
            String currentId = nodeIdList.get((int) entry[1]);

            if (visited.contains(currentId)) continue;
            visited.add(currentId);

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
                if (visited.contains(neighborId)) continue;

                // Read weight from edge property, default to 1.0
                double weight = 1.0;
                String weightStr = edge.getProperty(weightProperty);
                if (weightStr != null) {
                    try {
                        weight = Double.parseDouble(weightStr);
                        if (weight < 0) {
                            System.out.println("[DIJKSTRA] Warning: negative weight on edge "
                                    + edge + ", using absolute value.");
                            weight = Math.abs(weight);
                        }
                    } catch (NumberFormatException e) {
                        weight = 1.0;
                    }
                }

                double newDist = currentDist + weight;
                if (newDist < dist.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    dist.put(neighborId, newDist);
                    parent.put(neighborId, currentId);
                    int idx;
                    if (nodeIdxMap.containsKey(neighborId)) {
                        idx = nodeIdxMap.get(neighborId);
                    } else {
                        idx = nodeIdList.size();
                        nodeIdList.add(neighborId);
                        nodeIdxMap.put(neighborId, idx);
                    }
                    pq.add(new double[]{newDist, idx});
                }
            }
        }

        if (!found) {
            System.out.println("[DIJKSTRA] No path found between " + startId + " and " + endId);
            return new ArrayList<>();
        }

        // Reconstruct path
        List<Node> path = new ArrayList<>();
        String current = endId;
        while (current != null) {
            Node node = storage.getNode(current);
            if (node != null) path.add(node);
            current = parent.get(current);
        }
        Collections.reverse(path);

        System.out.println("[DIJKSTRA] Path length: " + (path.size() - 1) + " hops, total weight: "
                + String.format("%.2f", dist.get(endId)));
        return path;
    }

    /**
     * Returns the total weight of a weighted path.
     */
    public double getPathWeight(List<Node> path, String weightProperty, String relationshipType) {
        if (path == null || path.size() < 2) return 0;
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String srcId = path.get(i).getId();
            String dstId = path.get(i + 1).getId();
            for (Edge e : storage.getOutgoingEdges(srcId)) {
                if (e.getDestinationId().equals(dstId)) {
                    if (relationshipType != null && !e.getRelationshipType().equals(relationshipType))
                        continue;
                    String ws = e.getProperty(weightProperty);
                    if (ws != null) {
                        try { total += Double.parseDouble(ws); } catch (NumberFormatException ex) { total += 1; }
                    } else {
                        total += 1;
                    }
                    break;
                }
            }
        }
        return total;
    }
}

package query;

import engine.EdgeManager;
import engine.GraphEngine;
import engine.NodeManager;
import index.PropertyIndex;
import model.Edge;
import model.Node;
import model.NodeType;
import model.RelationshipType;
import schema.SchemaManager;
import storage.GraphStorage;
import traversal.TraversalEngine;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Query Executor
 *
 * Interprets a structured Query object and dispatches to the appropriate
 * engine component (SchemaManager, NodeManager, EdgeManager, TraversalEngine).
 *
 * This layer separates command understanding (QueryParser) from command
 * execution (QueryExecutor), mirroring a real database engine's plan/execute split.
 */
public class QueryExecutor {

    private final GraphEngine engine;
    private final SchemaManager schemaManager;
    private final NodeManager nodeManager;
    private final EdgeManager edgeManager;
    private final TraversalEngine traversalEngine;
    private final PropertyIndex propertyIndex;
    private final GraphStorage storage;
    private final List<String> commandHistory = new ArrayList<>();

    public QueryExecutor(GraphEngine engine) {
        this.engine = engine;
        this.schemaManager = engine.getSchemaManager();
        this.nodeManager   = engine.getNodeManager();
        this.edgeManager   = engine.getEdgeManager();
        this.traversalEngine = engine.getTraversalEngine();
        this.propertyIndex = engine.getPropertyIndex();
        this.storage       = engine.getStorage();
    }

    /**
     * Executes the given Query object and prints results to stdout.
     */
    public void execute(Query q) {
        System.out.println();

        // Track command history (skip HISTORY itself to avoid clutter)
        if (q.getType() != Query.Type.HISTORY && q.getType() != Query.Type.UNKNOWN) {
            commandHistory.add(q.toString());
        }

        try {
            switch (q.getType()) {

                // ── DDL ────────────────────────────────────────────────
                case CREATE_NODE_TYPE:
                    String[] req = splitProps(q.getRequiredProps());
                    String[] opt = splitProps(q.getOptionalProps());
                    schemaManager.createNodeType(q.getNodeType(), req, opt);
                    break;

                case CREATE_RELATIONSHIP_TYPE:
                    String[] rreq = splitProps(q.getRequiredProps());
                    String[] ropt = splitProps(q.getOptionalProps());
                    schemaManager.createRelationshipType(
                            q.getRelationshipType(),
                            q.getEdgeSourceId(),
                            q.getEdgeDestId(),
                            rreq, ropt);
                    break;

                case DROP_NODE_TYPE:
                    engine.dropNodeType(q.getNodeType());
                    break;

                case DROP_RELATIONSHIP_TYPE:
                    engine.dropRelationshipType(q.getRelationshipType());
                    break;

                case SHOW_SCHEMA:
                    schemaManager.printSchema();
                    break;

                // ── DML — Node ─────────────────────────────────────────
                case ADD_NODE:
                    if (q.getProperties().isEmpty()) {
                        nodeManager.addNode(q.getNodeId(), q.getNodeType());
                    } else {
                        nodeManager.addNode(q.getNodeId(), q.getNodeType(), q.getProperties());
                    }
                    break;

                case DELETE_NODE:
                    nodeManager.deleteNode(q.getNodeId());
                    break;

                case UPDATE_NODE:
                    nodeManager.updateNodeProperty(
                            q.getNodeId(), q.getConditionKey(), q.getConditionValue());
                    break;

                // ── DML — Edge ─────────────────────────────────────────
                case ADD_EDGE:
                    if (q.getProperties().isEmpty()) {
                        edgeManager.addEdge(
                                q.getEdgeSourceId(), q.getEdgeDestId(), q.getRelationshipType());
                    } else {
                        edgeManager.addEdge(
                                q.getEdgeSourceId(), q.getEdgeDestId(),
                                q.getRelationshipType(), q.getProperties());
                    }
                    break;

                case DELETE_EDGE:
                    edgeManager.deleteEdge(
                            q.getEdgeSourceId(), q.getEdgeDestId(), q.getRelationshipType());
                    break;

                case UPDATE_EDGE:
                    edgeManager.updateEdgeProperty(
                            q.getEdgeSourceId(), q.getEdgeDestId(),
                            q.getRelationshipType(), q.getConditionKey(), q.getConditionValue());
                    break;

                // ── Retrieval ──────────────────────────────────────────
                case FIND:
                    executeFind(q);
                    break;

                case NEIGHBORS:
                    executeNeighbors(q);
                    break;

                // ── Traversal ──────────────────────────────────────────
                case BFS: {
                    List<Node> result = traversalEngine.bfs(
                            q.getNodeId(), q.getRelationshipType());
                    printNodeList("BFS Result", result);
                    break;
                }

                case DFS: {
                    List<Node> result = traversalEngine.dfs(
                            q.getNodeId(), q.getRelationshipType());
                    printNodeList("DFS Result", result);
                    break;
                }

                case SHORTEST_PATH: {
                    List<Node> path = traversalEngine.shortestPath(
                            q.getNodeId(), q.getSecondNodeId(), q.getRelationshipType());
                    TraversalEngine.printPath(path);
                    break;
                }

                // ── Persistence ────────────────────────────────────────
                case SAVE:
                    engine.save(q.getFilePath());
                    break;

                case LOAD:
                    engine.load(q.getFilePath());
                    break;

                // ── Utility ────────────────────────────────────────────
                case SHOW_GRAPH:
                    storage.printGraph();
                    break;

                case SHOW_INDEX:
                    propertyIndex.printIndex();
                    break;

                case COUNT:
                    executeCount(q);
                    break;

                case CLEAR:
                    if (q.isClearAll()) {
                        engine.clearAll();
                    } else {
                        engine.clearGraph();
                    }
                    break;

                case DESCRIBE:
                    executeDescribe(q);
                    break;

                // ── Graph Analysis ──────────────────────────────────────
                case DEGREE:
                    executeDegree(q);
                    break;

                case STATS:
                    executeStats();
                    break;

                case CONNECTED_COMPONENTS:
                    executeComponents();
                    break;

                case HAS_CYCLE:
                    executeCycleDetection();
                    break;

                case PATH_EXISTS:
                    executePathExists(q);
                    break;

                // ── Export ──────────────────────────────────────────────
                case EXPORT_DOT:
                    executeExportDot(q);
                    break;

                case EXPORT_CSV:
                    executeExportCsv(q);
                    break;

                // ── History ─────────────────────────────────────────────
                case HISTORY:
                    executeHistory();
                    break;

                case UNKNOWN:
                default:
                    System.out.println("[ERROR] Unknown command. Type HELP for usage.");
                    break;
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  FIND execution
    // ─────────────────────────────────────────────

    private void executeFind(Query q) {
        List<Node> results;
        String type  = (q.getNodeType() != null && !q.getNodeType().isEmpty())
                        ? q.getNodeType() : null;
        String key   = q.getConditionKey();
        String value = q.getConditionValue();

        if (type != null && key != null) {
            results = nodeManager.getNodesByTypeAndProperty(type, key, value);
        } else if (type != null) {
            results = nodeManager.getNodesByType(type);
        } else if (key != null) {
            results = nodeManager.getNodesByProperty(key, value);
        } else {
            results = nodeManager.getAllNodes();
        }

        printNodeList("FIND Result", results);
    }

    // ─────────────────────────────────────────────
    //  NEIGHBORS execution
    // ─────────────────────────────────────────────

    private void executeNeighbors(Query q) {
        List<Node> neighbors = traversalEngine.getNeighbors(q.getNodeId(), q.getRelationshipType());
        printNodeList("Neighbors of " + q.getNodeId(), neighbors);

        System.out.println("  Incoming edges:");
        List<Edge> inEdges = storage.getIncomingEdges(q.getNodeId());
        if (inEdges.isEmpty()) System.out.println("    (none)");
        else for (Edge e : inEdges) System.out.println("    " + e);
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private void printNodeList(String label, List<Node> nodes) {
        System.out.println("── " + label + " (" + nodes.size() + " result(s)) ──────────────");
        if (nodes.isEmpty()) {
            System.out.println("  (no results)");
        } else {
            for (Node n : nodes) System.out.println("  " + n);
        }
    }

    private String[] splitProps(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = csv.split(",");
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }

    // ─────────────────────────────────────────────
    //  COUNT execution
    // ─────────────────────────────────────────────

    private void executeCount(Query q) {
        String target = q.getNodeType();        // "NODES", "EDGES", or "ALL"
        String filter = q.getRelationshipType(); // optional type filter

        if ("NODES".equals(target)) {
            int count;
            if (filter != null) {
                count = (int) nodeManager.getAllNodes().stream()
                        .filter(n -> n.getType().equals(filter)).count();
                System.out.println("[COUNT] " + count + " node(s) of type '" + filter + "'");
            } else {
                count = storage.getNodeCount();
                System.out.println("[COUNT] " + count + " node(s) total");
            }
        } else if ("EDGES".equals(target)) {
            int count;
            if (filter != null) {
                count = (int) storage.getAllEdges().stream()
                        .filter(e -> e.getRelationshipType().equals(filter)).count();
                System.out.println("[COUNT] " + count + " edge(s) of type '" + filter + "'");
            } else {
                count = storage.getEdgeCount();
                System.out.println("[COUNT] " + count + " edge(s) total");
            }
        } else {
            System.out.println("[COUNT] Nodes: " + storage.getNodeCount()
                    + "  |  Edges: " + storage.getEdgeCount());
        }
    }

    // ─────────────────────────────────────────────
    //  DESCRIBE execution
    // ─────────────────────────────────────────────

    private void executeDescribe(Query q) {
        Node node = storage.getNode(q.getNodeId());
        if (node == null) {
            throw new IllegalArgumentException("[NODE] Node not found: " + q.getNodeId());
        }

        System.out.println("── DESCRIBE: " + node.getId() + " ──────────────────────────────");
        System.out.println("  Type      : " + node.getType());
        System.out.println("  Properties:");
        if (node.getProperties().isEmpty()) {
            System.out.println("    (none)");
        } else {
            for (java.util.Map.Entry<String, String> e : node.getProperties().entrySet()) {
                System.out.println("    " + e.getKey() + " = " + e.getValue());
            }
        }

        List<Edge> outgoing = storage.getOutgoingEdges(node.getId());
        System.out.println("  Outgoing edges (" + outgoing.size() + "):");
        if (outgoing.isEmpty()) {
            System.out.println("    (none)");
        } else {
            for (Edge e : outgoing) {
                Node dest = storage.getNode(e.getDestinationId());
                String destLabel = dest != null
                        ? dest.getId() + " [" + dest.getType() + "]" : e.getDestinationId();
                System.out.println("    -[" + e.getRelationshipType() + "]-> " + destLabel
                        + (e.getProperties().isEmpty() ? "" : "  " + e.getProperties()));
            }
        }

        List<Edge> incoming = storage.getIncomingEdges(node.getId());
        System.out.println("  Incoming edges (" + incoming.size() + "):");
        if (incoming.isEmpty()) {
            System.out.println("    (none)");
        } else {
            for (Edge e : incoming) {
                Node src = storage.getNode(e.getSourceId());
                String srcLabel = src != null
                        ? src.getId() + " [" + src.getType() + "]" : e.getSourceId();
                System.out.println("    <-[" + e.getRelationshipType() + "]- " + srcLabel
                        + (e.getProperties().isEmpty() ? "" : "  " + e.getProperties()));
            }
        }
    }

    // ─────────────────────────────────────────────
    //  DEGREE execution
    // ─────────────────────────────────────────────

    private void executeDegree(Query q) {
        int[] d = traversalEngine.degree(q.getNodeId());
        System.out.println("── DEGREE: " + q.getNodeId() + " ──────────────────────────────");
        System.out.println("  In-degree  : " + d[0]);
        System.out.println("  Out-degree : " + d[1]);
        System.out.println("  Total      : " + d[2]);
    }

    // ─────────────────────────────────────────────
    //  STATS execution
    // ─────────────────────────────────────────────

    private void executeStats() {
        int nodeCount = storage.getNodeCount();
        int edgeCount = storage.getEdgeCount();
        int schemaNodeTypes = schemaManager.getNodeTypeMap().size();
        int schemaRelTypes = schemaManager.getRelationshipTypeMap().size();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║           GRAPH STATISTICS               ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("  Schema:");
        System.out.println("    Node types         : " + schemaNodeTypes);
        System.out.println("    Relationship types : " + schemaRelTypes);
        System.out.println("  Data:");
        System.out.println("    Total nodes        : " + nodeCount);
        System.out.println("    Total edges        : " + edgeCount);

        if (nodeCount > 0) {
            double avgDegree = (edgeCount * 2.0) / nodeCount;
            // Max possible directed edges = V * (V-1)
            double density = nodeCount > 1
                    ? (double) edgeCount / (nodeCount * (nodeCount - 1)) : 0;
            System.out.println("    Avg degree         : " + String.format("%.2f", avgDegree));
            System.out.println("    Graph density      : " + String.format("%.4f", density));
        }

        // Per-type node counts
        System.out.println("  Nodes by type:");
        for (NodeType nt : schemaManager.getAllNodeTypes()) {
            long count = nodeManager.getAllNodes().stream()
                    .filter(n -> n.getType().equals(nt.getName())).count();
            System.out.println("    " + nt.getName() + " : " + count);
        }

        // Per-type edge counts
        System.out.println("  Edges by type:");
        for (RelationshipType rt : schemaManager.getAllRelationshipTypes()) {
            long count = storage.getAllEdges().stream()
                    .filter(e -> e.getRelationshipType().equals(rt.getName())).count();
            System.out.println("    " + rt.getName() + " : " + count);
        }
        System.out.println("╚══════════════════════════════════════════╝");
    }

    // ─────────────────────────────────────────────
    //  COMPONENTS execution
    // ─────────────────────────────────────────────

    private void executeComponents() {
        List<List<String>> components = traversalEngine.connectedComponents();
        System.out.println("── Connected Components (" + components.size() + ") ──────────────");
        for (int i = 0; i < components.size(); i++) {
            List<String> comp = components.get(i);
            System.out.println("  Component " + (i + 1) + " (" + comp.size() + " nodes): " + comp);
        }
        if (components.size() == 1) {
            System.out.println("  → Graph is fully connected.");
        } else {
            System.out.println("  → Graph has " + components.size() + " disconnected components.");
        }
    }

    // ─────────────────────────────────────────────
    //  HAS CYCLE execution
    // ─────────────────────────────────────────────

    private void executeCycleDetection() {
        List<String> cycle = traversalEngine.detectCycle();
        if (cycle.isEmpty()) {
            System.out.println("[CYCLE] No cycles detected. The graph is a DAG (Directed Acyclic Graph).");
        } else {
            System.out.println("[CYCLE] Cycle detected!");
            StringBuilder sb = new StringBuilder("  ");
            for (int i = 0; i < cycle.size(); i++) {
                sb.append(cycle.get(i));
                if (i < cycle.size() - 1) sb.append(" → ");
            }
            System.out.println(sb);
        }
    }

    // ─────────────────────────────────────────────
    //  PATH EXISTS execution
    // ─────────────────────────────────────────────

    private void executePathExists(Query q) {
        boolean exists = traversalEngine.pathExists(
                q.getNodeId(), q.getSecondNodeId(), q.getRelationshipType());
        if (exists) {
            System.out.println("[PATH] Yes — a path exists from " + q.getNodeId()
                    + " to " + q.getSecondNodeId() + ".");
        } else {
            System.out.println("[PATH] No — no path from " + q.getNodeId()
                    + " to " + q.getSecondNodeId() + ".");
        }
    }

    // ─────────────────────────────────────────────
    //  EXPORT DOT execution (Graphviz format)
    // ─────────────────────────────────────────────

    private void executeExportDot(Query q) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  node [shape=record, style=filled, fillcolor=lightyellow];\n\n");

        // Nodes
        for (Node n : storage.getAllNodes()) {
            sb.append("  \"").append(n.getId()).append("\" [label=\"{");
            sb.append(n.getId()).append(" | ").append(n.getType());
            if (!n.getProperties().isEmpty()) {
                sb.append(" | ");
                boolean first = true;
                for (Map.Entry<String, String> e : n.getProperties().entrySet()) {
                    if (!first) sb.append("\\n");
                    sb.append(e.getKey()).append("=").append(e.getValue());
                    first = false;
                }
            }
            sb.append("}\"];\n");
        }

        sb.append("\n");

        // Edges
        for (Edge e : storage.getAllEdges()) {
            sb.append("  \"").append(e.getSourceId()).append("\" -> \"")
              .append(e.getDestinationId()).append("\" [label=\"")
              .append(e.getRelationshipType()).append("\"];\n");
        }

        sb.append("}\n");

        if (q.getFilePath() != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(q.getFilePath()))) {
                pw.print(sb);
                System.out.println("[EXPORT] DOT file saved to: " + q.getFilePath());
            } catch (IOException ex) {
                System.out.println("[EXPORT] Failed to write file: " + ex.getMessage());
            }
        } else {
            System.out.println(sb);
        }
    }

    // ─────────────────────────────────────────────
    //  EXPORT CSV execution
    // ─────────────────────────────────────────────

    private void executeExportCsv(Query q) {
        StringBuilder sb = new StringBuilder();

        // Nodes CSV
        sb.append("# NODES\n");
        sb.append("node_id,type,properties\n");
        for (Node n : storage.getAllNodes()) {
            sb.append(n.getId()).append(",").append(n.getType()).append(",\"");
            boolean first = true;
            for (Map.Entry<String, String> e : n.getProperties().entrySet()) {
                if (!first) sb.append(";");
                sb.append(e.getKey()).append("=").append(e.getValue());
                first = false;
            }
            sb.append("\"\n");
        }

        sb.append("\n# EDGES\n");
        sb.append("source_id,destination_id,relationship_type,properties\n");
        for (Edge e : storage.getAllEdges()) {
            sb.append(e.getSourceId()).append(",").append(e.getDestinationId())
              .append(",").append(e.getRelationshipType()).append(",\"");
            boolean first = true;
            for (Map.Entry<String, String> p : e.getProperties().entrySet()) {
                if (!first) sb.append(";");
                sb.append(p.getKey()).append("=").append(p.getValue());
                first = false;
            }
            sb.append("\"\n");
        }

        if (q.getFilePath() != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(q.getFilePath()))) {
                pw.print(sb);
                System.out.println("[EXPORT] CSV file saved to: " + q.getFilePath());
            } catch (IOException ex) {
                System.out.println("[EXPORT] Failed to write file: " + ex.getMessage());
            }
        } else {
            System.out.println(sb);
        }
    }

    // ─────────────────────────────────────────────
    //  HISTORY execution
    // ─────────────────────────────────────────────

    private void executeHistory() {
        if (commandHistory.isEmpty()) {
            System.out.println("  (no command history)");
            return;
        }
        System.out.println("── Command History (" + commandHistory.size() + " commands) ──────────────");
        for (int i = 0; i < commandHistory.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + commandHistory.get(i));
        }
    }
}

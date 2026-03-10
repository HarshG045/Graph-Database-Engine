package query;

import engine.EdgeManager;
import engine.GraphEngine;
import engine.NodeManager;
import index.PropertyIndex;
import model.Edge;
import model.Node;
import model.NodeType;
import model.PropertyValue;
import model.RelationshipType;
import schema.SchemaManager;
import storage.GraphStorage;
import storage.StorageManager;
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

                case IMPORT_CSV:
                    executeImportCsv(q);
                    break;

                // ── Schema Evolution ────────────────────────────────────
                case ALTER_NODE_TYPE:
                    executeAlterNodeType(q);
                    break;

                case ALTER_RELATIONSHIP_TYPE:
                    executeAlterRelType(q);
                    break;

                // ── Property Management ─────────────────────────────────
                case REMOVE_PROPERTY:
                    executeRemoveProperty(q);
                    break;

                // ── Weighted Shortest Path ──────────────────────────────
                case WEIGHTED_SHORTEST_PATH:
                    executeWeightedShortestPath(q);
                    break;

                // ── EXPLAIN ──────────────────────────────────────────────
                case EXPLAIN:
                    executeExplain(q);
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

        boolean hasConditions = q.getConditions() != null && !q.getConditions().isEmpty();
        boolean hasSingleCondition = q.getConditionKey() != null && !hasConditions;

        // Start with type-based filtering (uses type index if available)
        if (type != null) {
            results = nodeManager.getNodesByType(type);
        } else {
            results = nodeManager.getAllNodes();
        }

        // Apply compound conditions (AND/OR)
        if (hasConditions) {
            String logic = q.getConditionLogic(); // "AND" or "OR"
            List<Query.Condition> conds = q.getConditions();
            List<Node> filtered = new ArrayList<>();
            for (Node n : results) {
                boolean match;
                if ("OR".equals(logic)) {
                    match = false;
                    for (Query.Condition c : conds) {
                        String propVal = n.getProperty(c.key);
                        if (propVal != null && PropertyValue.evaluate(propVal, c.operator, c.value)) {
                            match = true;
                            break;
                        }
                    }
                } else {
                    // AND (default)
                    match = true;
                    for (Query.Condition c : conds) {
                        String propVal = n.getProperty(c.key);
                        if (propVal == null || !PropertyValue.evaluate(propVal, c.operator, c.value)) {
                            match = false;
                            break;
                        }
                    }
                }
                if (match) filtered.add(n);
            }
            results = filtered;
        } else if (hasSingleCondition) {
            // Single condition with operator support
            String key = q.getConditionKey();
            String op = q.getConditionOp();
            String value = q.getConditionValue();

            if ("=".equals(op) && type != null) {
                // Use the index for exact match
                results = nodeManager.getNodesByTypeAndProperty(type, key, value);
            } else if ("=".equals(op) && type == null) {
                results = nodeManager.getNodesByProperty(key, value);
            } else {
                // Range query — must scan
                List<Node> filtered = new ArrayList<>();
                for (Node n : results) {
                    String propVal = n.getProperty(key);
                    if (propVal != null && PropertyValue.evaluate(propVal, op, value)) {
                        filtered.add(n);
                    }
                }
                results = filtered;
            }
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
            String safePath = StorageManager.validatePath(q.getFilePath());
            try (PrintWriter pw = new PrintWriter(new FileWriter(safePath))) {
                pw.print(sb);
                System.out.println("[EXPORT] DOT file saved to: " + safePath);
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
            String safePath = StorageManager.validatePath(q.getFilePath());
            try (PrintWriter pw = new PrintWriter(new FileWriter(safePath))) {
                pw.print(sb);
                System.out.println("[EXPORT] CSV file saved to: " + safePath);
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

    // ─────────────────────────────────────────────
    //  IMPORT CSV execution
    // ─────────────────────────────────────────────

    private void executeImportCsv(Query q) {
        String filePath = q.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            System.out.println("[IMPORT] Usage: IMPORT CSV <filepath>");
            return;
        }
        String safePath = StorageManager.validatePath(filePath);

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(safePath))) {
            String line;
            String section = null;
            int nodesImported = 0;
            int edgesImported = 0;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("# NODES")) {
                    section = "NODES";
                    continue;
                }
                if (line.startsWith("# EDGES")) {
                    section = "EDGES";
                    continue;
                }
                if (line.startsWith("node_id,") || line.startsWith("source_id,")) {
                    continue; // skip header
                }

                if ("NODES".equals(section)) {
                    // node_id,type,"key1=val1;key2=val2"
                    String[] cols = parseCsvLine(line);
                    if (cols.length >= 2) {
                        String id = cols[0];
                        String type = cols[1];
                        java.util.Map<String, String> props = new java.util.HashMap<>();
                        if (cols.length >= 3) {
                            String propsStr = cols[2].replace("\"", "");
                            if (!propsStr.isEmpty()) {
                                for (String pair : propsStr.split(";")) {
                                    String[] kv = pair.split("=", 2);
                                    if (kv.length == 2) props.put(kv[0].trim(), kv[1].trim());
                                }
                            }
                        }
                        try {
                            if (props.isEmpty()) {
                                nodeManager.addNode(id, type);
                            } else {
                                nodeManager.addNode(id, type, props);
                            }
                            nodesImported++;
                        } catch (Exception e) {
                            System.out.println("[IMPORT] Skipping node " + id + ": " + e.getMessage());
                        }
                    }
                } else if ("EDGES".equals(section)) {
                    // source_id,destination_id,relationship_type,"key1=val1;key2=val2"
                    String[] cols = parseCsvLine(line);
                    if (cols.length >= 3) {
                        String src = cols[0];
                        String dst = cols[1];
                        String relType = cols[2];
                        java.util.Map<String, String> props = new java.util.HashMap<>();
                        if (cols.length >= 4) {
                            String propsStr = cols[3].replace("\"", "");
                            if (!propsStr.isEmpty()) {
                                for (String pair : propsStr.split(";")) {
                                    String[] kv = pair.split("=", 2);
                                    if (kv.length == 2) props.put(kv[0].trim(), kv[1].trim());
                                }
                            }
                        }
                        try {
                            if (props.isEmpty()) {
                                edgeManager.addEdge(src, dst, relType);
                            } else {
                                edgeManager.addEdge(src, dst, relType, props);
                            }
                            edgesImported++;
                        } catch (Exception e) {
                            System.out.println("[IMPORT] Skipping edge " + src + "->" + dst + ": " + e.getMessage());
                        }
                    }
                }
            }
            System.out.println("[IMPORT] CSV imported from: " + safePath
                    + "  (nodes=" + nodesImported + ", edges=" + edgesImported + ")");
        } catch (IOException ex) {
            System.out.println("[IMPORT] Failed to read file: " + ex.getMessage());
        }
    }

    /**
     * Simple CSV line parser that handles quoted fields.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    // ─────────────────────────────────────────────
    //  ALTER TYPE execution
    // ─────────────────────────────────────────────

    private void executeAlterNodeType(Query q) {
        String typeName = q.getNodeType();
        NodeType nt = schemaManager.getNodeType(typeName);
        if (nt == null) {
            throw new IllegalArgumentException("[SCHEMA] Node type not found: " + typeName);
        }

        String op = q.getConditionOp();
        if ("ADD_REQUIRED".equals(op)) {
            String prop = q.getRequiredProps();
            if (prop != null) {
                nt.addRequiredProperty(prop);
                System.out.println("[SCHEMA] Added required property '" + prop
                        + "' to node type '" + typeName + "'");
            }
        } else if ("ADD_OPTIONAL".equals(op)) {
            String prop = q.getOptionalProps();
            if (prop != null) {
                nt.addOptionalProperty(prop);
                System.out.println("[SCHEMA] Added optional property '" + prop
                        + "' to node type '" + typeName + "'");
            }
        } else if ("REMOVE".equals(op)) {
            String prop = q.getConditionKey();
            if (prop != null) {
                nt.removeProperty(prop);
                System.out.println("[SCHEMA] Removed property '" + prop
                        + "' from node type '" + typeName + "'");
            }
        }
    }

    private void executeAlterRelType(Query q) {
        String typeName = q.getRelationshipType();
        RelationshipType rt = schemaManager.getRelationshipType(typeName);
        if (rt == null) {
            throw new IllegalArgumentException("[SCHEMA] Relationship type not found: " + typeName);
        }

        String op = q.getConditionOp();
        if ("ADD_REQUIRED".equals(op)) {
            String prop = q.getRequiredProps();
            if (prop != null) {
                rt.addRequiredProperty(prop);
                System.out.println("[SCHEMA] Added required property '" + prop
                        + "' to relationship type '" + typeName + "'");
            }
        } else if ("ADD_OPTIONAL".equals(op)) {
            String prop = q.getOptionalProps();
            if (prop != null) {
                rt.addOptionalProperty(prop);
                System.out.println("[SCHEMA] Added optional property '" + prop
                        + "' to relationship type '" + typeName + "'");
            }
        } else if ("REMOVE".equals(op)) {
            String prop = q.getConditionKey();
            if (prop != null) {
                rt.removeProperty(prop);
                System.out.println("[SCHEMA] Removed property '" + prop
                        + "' from relationship type '" + typeName + "'");
            }
        }
    }

    // ─────────────────────────────────────────────
    //  REMOVE PROPERTY execution
    // ─────────────────────────────────────────────

    private void executeRemoveProperty(Query q) {
        String key = q.getConditionKey();
        String nodeId = q.getNodeId();
        String typeName = q.getNodeType();

        // ── Schema removal: REMOVE PROPERTY <typeName> <property> ──
        if (typeName != null && nodeId == null) {
            // Check node types first
            if (schemaManager.nodeTypeExists(typeName)) {
                NodeType nt = schemaManager.getNodeType(typeName);
                if (nt.isRequiredProperty(key)) {
                    // Check if any nodes have this property set
                    for (Node n : storage.getNodesByType(typeName)) {
                        if (n.hasProperty(key)) {
                            throw new IllegalArgumentException(
                                    "[CONSTRAINT] Cannot remove required property '" + key
                                    + "' — node '" + n.getId() + "' still has it.");
                        }
                    }
                }
                nt.removeProperty(key);
                System.out.println("[SCHEMA] Removed property '" + key
                        + "' from node type '" + typeName + "'");
                return;
            }
            // Check relationship types
            if (schemaManager.relationshipTypeExists(typeName)) {
                RelationshipType rt = schemaManager.getRelationshipType(typeName);
                rt.removeProperty(key);
                System.out.println("[SCHEMA] Removed property '" + key
                        + "' from relationship type '" + typeName + "'");
                return;
            }
            System.out.println("[ERROR] Type not found: " + typeName);
            return;
        }

        // ── Node-instance removal: REMOVE PROPERTY <key> FROM <nodeId> ──
        if (key == null || nodeId == null) {
            System.out.println("[ERROR] Usage: REMOVE PROPERTY <type> <property>  or  REMOVE PROPERTY <key> FROM <nodeId>");
            return;
        }

        Node node = storage.getNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("[NODE] Node not found: " + nodeId);
        }

        // Prevent removal of required properties
        NodeType nt = schemaManager.getNodeType(node.getType());
        if (nt != null && nt.isRequiredProperty(key)) {
            throw new IllegalArgumentException(
                    "[CONSTRAINT] Cannot remove required property '" + key
                    + "' from node type '" + node.getType() + "'");
        }

        String oldValue = node.getProperty(key);
        if (oldValue == null) {
            System.out.println("[NODE] Property '" + key + "' does not exist on node " + nodeId);
            return;
        }
        node.removeProperty(key);
        propertyIndex.updateEntry(nodeId, key, oldValue, null);
        System.out.println("[NODE] Removed property '" + key + "' from node " + nodeId);
    }

    // ─────────────────────────────────────────────
    //  WEIGHTED SHORTEST PATH execution
    // ─────────────────────────────────────────────

    private void executeWeightedShortestPath(Query q) {
        String weightProp = q.getConditionKey();
        if (weightProp == null) weightProp = "weight";

        List<Node> path = traversalEngine.weightedShortestPath(
                q.getNodeId(), q.getSecondNodeId(), weightProp, q.getRelationshipType());

        if (path.isEmpty()) {
            System.out.println("  (no path)");
            return;
        }

        double totalWeight = traversalEngine.getPathWeight(
                path, weightProp, q.getRelationshipType());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i).getId());
            if (i < path.size() - 1) sb.append(" → ");
        }
        System.out.println("  Path: " + sb);
        System.out.println("  Total weight: " + String.format("%.2f", totalWeight));
        System.out.println("  Nodes:");
        for (Node n : path) System.out.println("    " + n);
    }

    // ─────────────────────────────────────────────
    //  EXPLAIN execution
    // ─────────────────────────────────────────────

    private void executeExplain(Query q) {
        String innerCmd = q.getFilePath();
        if (innerCmd == null || innerCmd.isEmpty()) {
            System.out.println("[EXPLAIN] Usage: EXPLAIN <query>");
            return;
        }

        QueryParser parser = new QueryParser();
        Query inner = parser.parse(innerCmd);

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║                    QUERY PLAN                        ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("  Command type  : " + inner.getType());

        if (inner.getType() == Query.Type.FIND) {
            String type = inner.getNodeType();
            String key = inner.getConditionKey();
            String op = inner.getConditionOp();
            boolean hasCompound = inner.getConditions() != null && !inner.getConditions().isEmpty();

            if (type != null && !type.isEmpty()) {
                System.out.println("  Type filter   : " + type + "  → uses TYPE INDEX (O(k))");
            }

            if (hasCompound) {
                System.out.println("  Logic         : " + inner.getConditionLogic());
                for (Query.Condition c : inner.getConditions()) {
                    boolean canIndex = "=".equals(c.operator);
                    System.out.println("  Condition     : " + c
                            + (canIndex ? "  → INDEX eligible" : "  → SCAN required"));
                }
            } else if (key != null) {
                boolean isExact = "=".equals(op);
                System.out.println("  Condition     : " + key + " " + op + " " + inner.getConditionValue());
                if (isExact) {
                    boolean indexed = propertyIndex.hasEntry(key, inner.getConditionValue());
                    System.out.println("  Strategy      : PROPERTY INDEX lookup"
                            + (indexed ? " (entries exist)" : " (no entries yet)"));
                } else {
                    System.out.println("  Strategy      : FULL SCAN with filter (range operator: " + op + ")");
                }
            } else if (type != null) {
                System.out.println("  Strategy      : TYPE INDEX scan (O(k), k=nodes of type)");
            } else {
                System.out.println("  Strategy      : FULL NODE SCAN (O(V))");
            }
        } else if (inner.getType() == Query.Type.SHORTEST_PATH) {
            System.out.println("  Algorithm     : BFS (unweighted)");
            System.out.println("  Complexity    : O(V + E)");
            System.out.println("  From          : " + inner.getNodeId());
            System.out.println("  To            : " + inner.getSecondNodeId());
            if (inner.getRelationshipType() != null) {
                System.out.println("  Edge filter   : " + inner.getRelationshipType());
            }
        } else if (inner.getType() == Query.Type.WEIGHTED_SHORTEST_PATH) {
            System.out.println("  Algorithm     : Dijkstra (weighted)");
            System.out.println("  Complexity    : O((V + E) log V)");
            System.out.println("  Weight prop   : " + inner.getConditionKey());
        } else if (inner.getType() == Query.Type.BFS || inner.getType() == Query.Type.DFS) {
            System.out.println("  Algorithm     : " + inner.getType());
            System.out.println("  Complexity    : O(V + E)");
            System.out.println("  Start node    : " + inner.getNodeId());
        } else {
            System.out.println("  (no detailed plan available for this command type)");
        }

        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}

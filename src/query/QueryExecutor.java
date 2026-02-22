package query;

import engine.EdgeManager;
import engine.GraphEngine;
import engine.NodeManager;
import index.PropertyIndex;
import model.Edge;
import model.Node;
import schema.SchemaManager;
import storage.GraphStorage;
import traversal.TraversalEngine;

import java.util.List;

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
        System.out.println("» Executing: " + q);

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
}

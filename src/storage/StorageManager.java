package storage;

import index.PropertyIndex;
import model.Edge;
import model.Node;
import model.NodeType;
import model.RelationshipType;
import schema.SchemaManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage Manager — Persistence Layer
 *
 * Handles serialisation and deserialisation of the entire graph state
 * to/from a plain-text file using a simple custom format.
 *
 * File format (sections separated by markers):
 *
 *   [SCHEMA_NODE_TYPES]
 *   TypeName|req:p1,p2|opt:p3
 *   ...
 *
 *   [SCHEMA_RELATIONSHIP_TYPES]
 *   TypeName|srcType|dstType|req:p1|opt:p2
 *   ...
 *
 *   [NODES]
 *   nodeId|type|key1=val1;key2=val2
 *   ...
 *
 *   [EDGES]
 *   srcId|dstId|relType|key1=val1;key2=val2
 *   ...
 *
 * This avoids any external JSON library dependency.
 */
public class StorageManager {

    private static final String DEFAULT_FILE = "graph_data.gdb";

    private static final String SEC_NODE_TYPES = "[SCHEMA_NODE_TYPES]";
    private static final String SEC_REL_TYPES  = "[SCHEMA_RELATIONSHIP_TYPES]";
    private static final String SEC_NODES      = "[NODES]";
    private static final String SEC_EDGES      = "[EDGES]";

    // ─────────────────────────────────────────────
    //  SAVE
    // ─────────────────────────────────────────────

    /**
     * Serialises schema, nodes, and edges to the specified file.
     *
     * @param filePath  path to output file; uses default if null
     */
    public void save(SchemaManager schemaManager,
                     GraphStorage graphStorage,
                     String filePath) throws IOException {

        String path = (filePath != null && !filePath.isEmpty()) ? filePath : DEFAULT_FILE;

        // Create parent directories if they don't exist
        File file = new File(path);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {

            // Schema — Node Types
            writer.write(SEC_NODE_TYPES);
            writer.newLine();
            for (NodeType nt : schemaManager.getAllNodeTypes()) {
                String req = String.join(",", nt.getRequiredProperties());
                String opt = String.join(",", nt.getOptionalProperties());
                writer.write(nt.getName() + "|req:" + req + "|opt:" + opt);
                writer.newLine();
            }

            // Schema — Relationship Types
            writer.write(SEC_REL_TYPES);
            writer.newLine();
            for (RelationshipType rt : schemaManager.getAllRelationshipTypes()) {
                String srcType = rt.getSourceNodeType() != null ? rt.getSourceNodeType() : "";
                String dstType = rt.getDestinationNodeType() != null ? rt.getDestinationNodeType() : "";
                String req = String.join(",", rt.getRequiredProperties());
                String opt = String.join(",", rt.getOptionalProperties());
                writer.write(rt.getName() + "|" + srcType + "|" + dstType
                        + "|req:" + req + "|opt:" + opt);
                writer.newLine();
            }

            // Nodes
            writer.write(SEC_NODES);
            writer.newLine();
            for (Node node : graphStorage.getAllNodes()) {
                writer.write(node.getId() + "|" + node.getType() + "|"
                        + serializeProperties(node.getProperties()));
                writer.newLine();
            }

            // Edges
            writer.write(SEC_EDGES);
            writer.newLine();
            for (Edge edge : graphStorage.getAllEdges()) {
                writer.write(edge.getSourceId() + "|" + edge.getDestinationId()
                        + "|" + edge.getRelationshipType() + "|"
                        + serializeProperties(edge.getProperties()));
                writer.newLine();
            }
        }

        System.out.println("[STORAGE] Graph saved to: " + path
                + "  (nodes=" + graphStorage.getNodeCount()
                + ", edges=" + graphStorage.getEdgeCount() + ")");
    }

    // ─────────────────────────────────────────────
    //  LOAD
    // ─────────────────────────────────────────────

    /**
     * Deserialises graph state from file, rebuilding schema, nodes, edges, and index.
     *
     * @param filePath  path to input file; uses default if null
     */
    public void load(SchemaManager schemaManager,
                     GraphStorage graphStorage,
                     PropertyIndex propertyIndex,
                     String filePath) throws IOException {

        String path = (filePath != null && !filePath.isEmpty()) ? filePath : DEFAULT_FILE;

        // Clear existing state
        graphStorage.clear();
        propertyIndex.clear();
        // Clear schema catalogs
        schemaManager.getNodeTypeMap().clear();
        schemaManager.getRelationshipTypeMap().clear();

        String currentSection = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals(SEC_NODE_TYPES)) { currentSection = SEC_NODE_TYPES; continue; }
                if (line.equals(SEC_REL_TYPES))  { currentSection = SEC_REL_TYPES;  continue; }
                if (line.equals(SEC_NODES))      { currentSection = SEC_NODES;      continue; }
                if (line.equals(SEC_EDGES))      { currentSection = SEC_EDGES;      continue; }

                if (SEC_NODE_TYPES.equals(currentSection)) {
                    loadNodeType(schemaManager, line);
                } else if (SEC_REL_TYPES.equals(currentSection)) {
                    loadRelType(schemaManager, line);
                } else if (SEC_NODES.equals(currentSection)) {
                    loadNode(graphStorage, propertyIndex, line);
                } else if (SEC_EDGES.equals(currentSection)) {
                    loadEdge(graphStorage, line);
                }
            }
        }

        System.out.println("[STORAGE] Graph loaded from: " + path
                + "  (nodes=" + graphStorage.getNodeCount()
                + ", edges=" + graphStorage.getEdgeCount() + ")");
    }

    // ─────────────────────────────────────────────
    //  Row parsers
    // ─────────────────────────────────────────────

    // TypeName|req:p1,p2|opt:p3
    private void loadNodeType(SchemaManager sm, String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 1) return;
        String name = parts[0];
        NodeType nt = new NodeType(name);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("req:") && parts[i].length() > 4) {
                for (String p : parts[i].substring(4).split(",")) {
                    if (!p.isEmpty()) nt.addRequiredProperty(p);
                }
            } else if (parts[i].startsWith("opt:") && parts[i].length() > 4) {
                for (String p : parts[i].substring(4).split(",")) {
                    if (!p.isEmpty()) nt.addOptionalProperty(p);
                }
            }
        }
        sm.getNodeTypeMap().put(name, nt);
    }

    // TypeName|srcType|dstType|req:p1|opt:p2
    private void loadRelType(SchemaManager sm, String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 3) return;
        String name    = parts[0];
        String srcType = parts[1].isEmpty() ? null : parts[1];
        String dstType = parts[2].isEmpty() ? null : parts[2];
        RelationshipType rt = new RelationshipType(name, srcType, dstType);
        for (int i = 3; i < parts.length; i++) {
            if (parts[i].startsWith("req:") && parts[i].length() > 4) {
                for (String p : parts[i].substring(4).split(",")) {
                    if (!p.isEmpty()) rt.addRequiredProperty(p);
                }
            } else if (parts[i].startsWith("opt:") && parts[i].length() > 4) {
                for (String p : parts[i].substring(4).split(",")) {
                    if (!p.isEmpty()) rt.addOptionalProperty(p);
                }
            }
        }
        sm.getRelationshipTypeMap().put(name, rt);
    }

    // nodeId|type|key1=val1;key2=val2
    private void loadNode(GraphStorage gs, PropertyIndex pi, String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length < 2) return;
        String id   = parts[0];
        String type = parts[1];
        Map<String, String> props = new HashMap<>();
        if (parts.length == 3 && !parts[2].isEmpty()) {
            props = deserializeProperties(parts[2]);
        }
        Node node = new Node(id, type, props);
        gs.addNode(node);
        pi.indexNode(node);
    }

    // srcId|dstId|relType|key1=val1;key2=val2
    private void loadEdge(GraphStorage gs, String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 3) return;
        String src     = parts[0];
        String dst     = parts[1];
        String relType = parts[2];
        Map<String, String> props = new HashMap<>();
        if (parts.length == 4 && !parts[3].isEmpty()) {
            props = deserializeProperties(parts[3]);
        }
        Edge edge = new Edge(src, dst, relType, props);
        gs.addEdge(edge);
    }

    // ─────────────────────────────────────────────
    //  Property serialisation helpers
    // ─────────────────────────────────────────────

    private String serializeProperties(Map<String, String> props) {
        if (props == null || props.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : props.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            // Escape | and ; inside values to avoid parser confusion
            sb.append(escape(e.getKey())).append("=").append(escape(e.getValue()));
        }
        return sb.toString();
    }

    private Map<String, String> deserializeProperties(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split(";")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(unescape(kv[0]), unescape(kv[1]));
            }
        }
        return map;
    }

    private String escape(String val) {
        // Escape backslash FIRST so subsequent replacements don't double-escape it.
        return val.replace("\\", "~b").replace("|", "~p").replace(";", "~s").replace("=", "~e");
    }

    private String unescape(String val) {
        // Unescape letter-codes first, THEN restore the tilde escape last
        // so ~b sequences left after prior steps resolve cleanly.
        return val.replace("~p", "|").replace("~s", ";").replace("~e", "=").replace("~b", "\\");
    }
}

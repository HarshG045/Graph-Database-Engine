package query;

/**
 * Query Parser
 *
 * Converts raw user input strings into structured Query objects.
 * Handles syntax recognition, token extraction, and optional parameter parsing.
 *
 * Supported syntax:
 *
 *   DDL:
 *     CREATE NODE TYPE <name> [REQUIRED prop1,prop2] [OPTIONAL prop3,prop4]
 *     CREATE RELATIONSHIP TYPE <name> FROM <srcType> TO <dstType> [REQUIRED p1] [OPTIONAL p2]
 *     DROP NODE TYPE <name>
 *     DROP RELATIONSHIP TYPE <name>
 *     SHOW SCHEMA
 *
 *   DML — Node:
 *     ADD NODE <id> TYPE <type> [PROPERTIES key=value,key2=value2]
 *     DELETE NODE <id>
 *     UPDATE NODE <id> SET <key>=<value>
 *
 *   DML — Edge:
 *     ADD EDGE <srcId> TO <dstId> TYPE <relType> [PROPERTIES key=value]
 *     DELETE EDGE <srcId> TO <dstId> TYPE <relType>
 *     UPDATE EDGE <srcId> TO <dstId> TYPE <relType> SET <key>=<value>
 *
 *   Query:
 *     FIND <type> [WHERE <key>=<value>]
 *     NEIGHBORS <nodeId>
 *
 *   Traversal:
 *     BFS <nodeId> [TYPE <relType>]
 *     DFS <nodeId> [TYPE <relType>]
 *     SHORTEST PATH <startId> TO <endId> [TYPE <relType>]
 *
 *   Persistence:
 *     SAVE [<filepath>]
 *     LOAD [<filepath>]
 *
 *   Utility:
 *     SHOW GRAPH
 *     SHOW INDEX
 */
public class QueryParser {

    /**
     * Parses a single command string into a Query object.
     *
     * @param input raw user input (case-insensitive keywords, case-sensitive values)
     * @return Query object ready for execution
     */
    public Query parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new Query(Query.Type.UNKNOWN);
        }

        String trimmed = input.trim();
        // Normalise leading keyword tokens to uppercase for matching,
        // but preserve original case for values (IDs, property values, file paths).
        String upper = trimmed.toUpperCase();

        // ── DDL ──────────────────────────────────────────────────────
        if (upper.startsWith("CREATE NODE TYPE")) {
            return parseCreateNodeType(trimmed);
        }
        if (upper.startsWith("CREATE RELATIONSHIP TYPE")) {
            return parseCreateRelationshipType(trimmed);
        }
        if (upper.startsWith("DROP NODE TYPE")) {
            return parseDropNodeType(trimmed);
        }
        if (upper.startsWith("DROP RELATIONSHIP TYPE")) {
            return parseDropRelationshipType(trimmed);
        }
        if (upper.equals("SHOW SCHEMA")) {
            return new Query(Query.Type.SHOW_SCHEMA);
        }

        // ── DML — Node ────────────────────────────────────────────────
        if (upper.startsWith("ADD NODE")) {
            return parseAddNode(trimmed);
        }
        if (upper.startsWith("DELETE NODE")) {
            return parseDeleteNode(trimmed);
        }
        if (upper.startsWith("UPDATE NODE")) {
            return parseUpdateNode(trimmed);
        }

        // ── DML — Edge ────────────────────────────────────────────────
        if (upper.startsWith("ADD EDGE")) {
            return parseAddEdge(trimmed);
        }
        if (upper.startsWith("DELETE EDGE")) {
            return parseDeleteEdge(trimmed);
        }
        if (upper.startsWith("UPDATE EDGE")) {
            return parseUpdateEdge(trimmed);
        }

        // ── Query ─────────────────────────────────────────────────────
        if (upper.startsWith("FIND")) {
            return parseFind(trimmed);
        }
        if (upper.startsWith("NEIGHBORS")) {
            return parseNeighbors(trimmed);
        }

        // ── Traversal ─────────────────────────────────────────────────
        if (upper.startsWith("BFS")) {
            return parseBfsOrDfs(trimmed, Query.Type.BFS);
        }
        if (upper.startsWith("DFS")) {
            return parseBfsOrDfs(trimmed, Query.Type.DFS);
        }
        if (upper.startsWith("SHORTEST PATH")) {
            return parseShortestPath(trimmed);
        }

        // ── Persistence ───────────────────────────────────────────────
        if (upper.startsWith("SAVE")) {
            return parseSaveLoad(trimmed, Query.Type.SAVE);
        }
        if (upper.startsWith("LOAD")) {
            return parseSaveLoad(trimmed, Query.Type.LOAD);
        }

        // ── Utility ───────────────────────────────────────────────────
        if (upper.equals("SHOW GRAPH")) {
            return new Query(Query.Type.SHOW_GRAPH);
        }
        if (upper.equals("SHOW INDEX")) {
            return new Query(Query.Type.SHOW_INDEX);
        }
        if (upper.startsWith("COUNT")) {
            return parseCount(trimmed);
        }
        if (upper.equals("CLEAR ALL")) {
            Query q = new Query(Query.Type.CLEAR);
            q.setClearAll(true);
            return q;
        }
        if (upper.equals("CLEAR")) {
            return new Query(Query.Type.CLEAR);
        }
        if (upper.startsWith("DESCRIBE")) {
            Query q = new Query(Query.Type.DESCRIBE);
            q.setNodeId(stripPrefix(trimmed, "DESCRIBE").trim());
            return q;
        }

        return new Query(Query.Type.UNKNOWN);
    }

    // ─────────────────────────────────────────────
    //  DDL parsers
    // ─────────────────────────────────────────────

    // CREATE NODE TYPE User REQUIRED name,age OPTIONAL email
    private Query parseCreateNodeType(String input) {
        Query q = new Query(Query.Type.CREATE_NODE_TYPE);
        // Strip leading keyword
        String rest = stripPrefix(input, "CREATE NODE TYPE").trim();
        String upper = rest.toUpperCase();

        String typeName;
        if (upper.contains("REQUIRED") || upper.contains("OPTIONAL")) {
            int idx = Math.min(
                    upper.contains("REQUIRED") ? upper.indexOf("REQUIRED") : Integer.MAX_VALUE,
                    upper.contains("OPTIONAL") ? upper.indexOf("OPTIONAL") : Integer.MAX_VALUE);
            typeName = rest.substring(0, idx).trim();
        } else {
            typeName = rest.trim();
        }
        q.setNodeType(typeName);
        q.setRequiredProps(extractKeywordValue(rest, "REQUIRED"));
        q.setOptionalProps(extractKeywordValue(rest, "OPTIONAL"));
        return q;
    }

    // CREATE RELATIONSHIP TYPE FRIENDS FROM User TO User REQUIRED since OPTIONAL weight
    private Query parseCreateRelationshipType(String input) {
        Query q = new Query(Query.Type.CREATE_RELATIONSHIP_TYPE);
        String rest = stripPrefix(input, "CREATE RELATIONSHIP TYPE").trim();
        String upper = rest.toUpperCase();

        // Name is the first token before FROM/REQUIRED/OPTIONAL
        String relTypeName = firstToken(rest);
        q.setRelationshipType(relTypeName);

        String fromType = extractKeywordValue(rest, "FROM");
        String toType   = extractKeywordValue(rest, "TO");

        // FROM/TO values might come before REQUIRED — strip trailing keywords
        if (fromType != null) {
            fromType = fromType.split("\\s+")[0]; // only first token
        }
        if (toType != null) {
            toType = toType.split("\\s+")[0];
        }

        q.setEdgeSourceId(fromType);   // repurpose fields for source / dest type
        q.setEdgeDestId(toType);
        q.setRequiredProps(extractKeywordValue(rest, "REQUIRED"));
        q.setOptionalProps(extractKeywordValue(rest, "OPTIONAL"));
        return q;
    }

    // DROP NODE TYPE User
    private Query parseDropNodeType(String input) {
        Query q = new Query(Query.Type.DROP_NODE_TYPE);
        q.setNodeType(stripPrefix(input, "DROP NODE TYPE").trim());
        return q;
    }

    // DROP RELATIONSHIP TYPE FRIENDS
    private Query parseDropRelationshipType(String input) {
        Query q = new Query(Query.Type.DROP_RELATIONSHIP_TYPE);
        q.setRelationshipType(stripPrefix(input, "DROP RELATIONSHIP TYPE").trim());
        return q;
    }

    // ─────────────────────────────────────────────
    //  DML — Node parsers
    // ─────────────────────────────────────────────

    // ADD NODE u1 TYPE User PROPERTIES name=Alice,age=25
    private Query parseAddNode(String input) {
        Query q = new Query(Query.Type.ADD_NODE);
        String rest = stripPrefix(input, "ADD NODE").trim();

        q.setNodeId(firstToken(rest));
        q.setNodeType(extractKeywordValue(rest, "TYPE"));
        parseProperties(q, extractKeywordValue(rest, "PROPERTIES"));
        return q;
    }

    // DELETE NODE u1
    private Query parseDeleteNode(String input) {
        Query q = new Query(Query.Type.DELETE_NODE);
        q.setNodeId(stripPrefix(input, "DELETE NODE").trim());
        return q;
    }

    // UPDATE NODE u1 SET age=26
    private Query parseUpdateNode(String input) {
        Query q = new Query(Query.Type.UPDATE_NODE);
        String rest = stripPrefix(input, "UPDATE NODE").trim();
        q.setNodeId(firstToken(rest));
        String setExpr = extractKeywordValue(rest, "SET");
        if (setExpr != null) {
            String[] parts = setExpr.split("=", 2);
            if (parts.length == 2) {
                q.setConditionKey(parts[0].trim());
                q.setConditionValue(parts[1].trim());
            }
        }
        return q;
    }

    // ─────────────────────────────────────────────
    //  DML — Edge parsers
    // ─────────────────────────────────────────────

    // ADD EDGE u1 TO u2 TYPE FRIENDS PROPERTIES since=2020
    private Query parseAddEdge(String input) {
        Query q = new Query(Query.Type.ADD_EDGE);
        String rest = stripPrefix(input, "ADD EDGE").trim();
        q.setEdgeSourceId(firstToken(rest));
        q.setEdgeDestId(extractKeywordValue(rest, "TO"));
        String toVal = q.getEdgeDestId();
        if (toVal != null) q.setEdgeDestId(toVal.split("\\s+")[0]);
        q.setRelationshipType(extractKeywordValue(rest, "TYPE"));
        parseProperties(q, extractKeywordValue(rest, "PROPERTIES"));
        return q;
    }

    // DELETE EDGE u1 TO u2 TYPE FRIENDS
    private Query parseDeleteEdge(String input) {
        Query q = new Query(Query.Type.DELETE_EDGE);
        String rest = stripPrefix(input, "DELETE EDGE").trim();
        q.setEdgeSourceId(firstToken(rest));
        String toVal = extractKeywordValue(rest, "TO");
        if (toVal != null) q.setEdgeDestId(toVal.split("\\s+")[0]);
        q.setRelationshipType(extractKeywordValue(rest, "TYPE"));
        return q;
    }

    // UPDATE EDGE u1 TO u2 TYPE FRIENDS SET since=2021
    private Query parseUpdateEdge(String input) {
        Query q = new Query(Query.Type.UPDATE_EDGE);
        String rest = stripPrefix(input, "UPDATE EDGE").trim();
        q.setEdgeSourceId(firstToken(rest));
        String toVal = extractKeywordValue(rest, "TO");
        if (toVal != null) q.setEdgeDestId(toVal.split("\\s+")[0]);
        q.setRelationshipType(extractKeywordValue(rest, "TYPE"));
        String setExpr = extractKeywordValue(rest, "SET");
        if (setExpr != null) {
            String[] parts = setExpr.split("=", 2);
            if (parts.length == 2) {
                q.setConditionKey(parts[0].trim());
                q.setConditionValue(parts[1].trim());
            }
        }
        return q;
    }

    // ─────────────────────────────────────────────
    //  Query parsers
    // ─────────────────────────────────────────────

    // FIND User WHERE age=25
    // FIND User
    private Query parseFind(String input) {
        Query q = new Query(Query.Type.FIND);
        String rest = stripPrefix(input, "FIND").trim();
        String upper = rest.toUpperCase();
        String typePart;
        if (upper.contains("WHERE")) {
            typePart = rest.substring(0, upper.indexOf("WHERE")).trim();
            String whereExpr = rest.substring(upper.indexOf("WHERE") + 5).trim();
            String[] parts = whereExpr.split("=", 2);
            if (parts.length == 2) {
                q.setConditionKey(parts[0].trim());
                q.setConditionValue(parts[1].trim());
            }
        } else {
            typePart = rest.trim();
        }
        q.setNodeType(typePart);
        return q;
    }

    // NEIGHBORS u1
    private Query parseNeighbors(String input) {
        Query q = new Query(Query.Type.NEIGHBORS);
        q.setNodeId(stripPrefix(input, "NEIGHBORS").trim());
        return q;
    }

    // ─────────────────────────────────────────────
    //  Traversal parsers
    // ─────────────────────────────────────────────

    // BFS u1 [TYPE FRIENDS]
    // DFS u1 [TYPE FRIENDS]
    private Query parseBfsOrDfs(String input, Query.Type type) {
        Query q = new Query(type);
        String prefix = (type == Query.Type.BFS) ? "BFS" : "DFS";
        String rest = stripPrefix(input, prefix).trim();
        q.setNodeId(firstToken(rest));
        q.setRelationshipType(extractKeywordValue(rest, "TYPE"));
        return q;
    }

    // SHORTEST PATH u1 TO u2 [TYPE FRIENDS]
    private Query parseShortestPath(String input) {
        Query q = new Query(Query.Type.SHORTEST_PATH);
        String rest = stripPrefix(input, "SHORTEST PATH").trim();
        q.setNodeId(firstToken(rest));
        String toVal = extractKeywordValue(rest, "TO");
        if (toVal != null) {
            q.setSecondNodeId(toVal.split("\\s+")[0]);
        }
        q.setRelationshipType(extractKeywordValue(rest, "TYPE"));
        return q;
    }

    // ─────────────────────────────────────────────
    //  Persistence parsers
    // ─────────────────────────────────────────────

    // SAVE [filepath]
    // LOAD [filepath]
    private Query parseSaveLoad(String input, Query.Type type) {
        Query q = new Query(type);
        String prefix = (type == Query.Type.SAVE) ? "SAVE" : "LOAD";
        String rest = stripPrefix(input, prefix).trim();
        if (!rest.isEmpty()) q.setFilePath(rest);
        return q;
    }

    // ─────────────────────────────────────────────
    //  Helper utilities
    // ─────────────────────────────────────────────

    /**
     * Removes a case-insensitive prefix from the string.
     */
    private String stripPrefix(String input, String prefix) {
        if (input.toUpperCase().startsWith(prefix.toUpperCase())) {
            return input.substring(prefix.length());
        }
        return input;
    }

    /**
     * Returns the first whitespace-delimited token.
     */
    private String firstToken(String input) {
        String trimmed = input.trim();
        int space = trimmed.indexOf(' ');
        return space == -1 ? trimmed : trimmed.substring(0, space);
    }

    /**
     * Extracts the value following a keyword token (case-insensitive).
     * Returns null if the keyword is not present.
     *
     * Example: extractKeywordValue("u1 TO u2 TYPE FRIENDS", "TO") → "u2 TYPE FRIENDS"
     * (caller is responsible for extracting only what it needs)
     */
    private String extractKeywordValue(String input, String keyword) {
        String upper = input.toUpperCase();
        String kwUpper = keyword.toUpperCase();
        int idx = upper.indexOf(" " + kwUpper + " ");
        if (idx == -1) {
            // Check if it ends with keyword (trailing)
            if (upper.endsWith(" " + kwUpper)) return "";
            return null;
        }
        String after = input.substring(idx + keyword.length() + 2).trim();
        // Trim at the next keyword boundary
        after = trimAtNextKeyword(after);
        return after.isEmpty() ? null : after;
    }

    /**
     * Cuts a string at the position of the next recognised keyword.
     */
    private String trimAtNextKeyword(String input) {
        String[] keywords = {
            " TYPE ", " FROM ", " TO ", " WHERE ", " PROPERTIES ",
            " REQUIRED ", " OPTIONAL ", " SET "
        };
        String upper = " " + input.toUpperCase() + " ";
        int cutAt = input.length();
        for (String kw : keywords) {
            int pos = upper.indexOf(kw, 1); // skip leading space
            if (pos > 0 && pos - 1 < cutAt) {
                cutAt = pos - 1;
            }
        }
        return input.substring(0, cutAt).trim();
    }

    /**
     * Parses a PROPERTIES string like "name=Alice,age=25" into the Query properties map.
     */
    private void parseProperties(Query q, String propsStr) {
        if (propsStr == null || propsStr.trim().isEmpty()) return;
        String[] pairs = propsStr.split(",");
        for (String pair : pairs) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2) {
                q.addProperty(kv[0].trim(), kv[1].trim());
            }
        }
    }

    // ─────────────────────────────────────────────
    //  COUNT parser
    // ─────────────────────────────────────────────

    // COUNT NODES [TYPE User]
    // COUNT EDGES [TYPE FRIENDS]
    private Query parseCount(String input) {
        Query q = new Query(Query.Type.COUNT);
        String upper = input.toUpperCase();
        if (upper.startsWith("COUNT NODES")) {
            q.setNodeType("NODES");
            String typeVal = extractKeywordValue(input, "TYPE");
            if (typeVal != null) q.setRelationshipType(typeVal.split("\\s+")[0]);
        } else if (upper.startsWith("COUNT EDGES")) {
            q.setNodeType("EDGES");
            String typeVal = extractKeywordValue(input, "TYPE");
            if (typeVal != null) q.setRelationshipType(typeVal.split("\\s+")[0]);
        } else {
            // COUNT alone → count both
            q.setNodeType("ALL");
        }
        return q;
    }
}

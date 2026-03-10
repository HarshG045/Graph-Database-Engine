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
        if (upper.startsWith("WEIGHTED SHORTEST PATH")) {
            return parseWeightedShortestPath(trimmed);
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
            String id = stripPrefix(trimmed, "DESCRIBE").trim();
            if (id.isEmpty()) return new Query(Query.Type.UNKNOWN);
            Query q = new Query(Query.Type.DESCRIBE);
            q.setNodeId(id);
            return q;
        }

        // ── Graph Analysis ────────────────────────────────────────────
        if (upper.startsWith("DEGREE")) {
            String id = stripPrefix(trimmed, "DEGREE").trim();
            if (id.isEmpty()) return new Query(Query.Type.UNKNOWN);
            Query q = new Query(Query.Type.DEGREE);
            q.setNodeId(id);
            return q;
        }
        if (upper.equals("STATS")) {
            return new Query(Query.Type.STATS);
        }
        if (upper.equals("COMPONENTS")) {
            return new Query(Query.Type.CONNECTED_COMPONENTS);
        }
        if (upper.equals("HAS CYCLE")) {
            return new Query(Query.Type.HAS_CYCLE);
        }
        if (upper.startsWith("PATH EXISTS")) {
            return parsePathExists(trimmed);
        }

        // ── Export ────────────────────────────────────────────────────
        if (upper.startsWith("EXPORT DOT")) {
            Query q = new Query(Query.Type.EXPORT_DOT);
            String rest = stripPrefix(trimmed, "EXPORT DOT").trim();
            if (!rest.isEmpty()) q.setFilePath(rest);
            return q;
        }
        if (upper.startsWith("EXPORT CSV")) {
            Query q = new Query(Query.Type.EXPORT_CSV);
            String rest = stripPrefix(trimmed, "EXPORT CSV").trim();
            if (!rest.isEmpty()) q.setFilePath(rest);
            return q;
        }

        // ── Import ─────────────────────────────────────────────────────
        if (upper.startsWith("IMPORT CSV")) {
            Query q = new Query(Query.Type.IMPORT_CSV);
            String rest = stripPrefix(trimmed, "IMPORT CSV").trim();
            if (!rest.isEmpty()) q.setFilePath(rest);
            return q;
        }

        // ── ALTER TYPE ────────────────────────────────────────────────
        if (upper.startsWith("ALTER NODE TYPE")) {
            return parseAlterType(trimmed, true);
        }
        if (upper.startsWith("ALTER RELATIONSHIP TYPE")) {
            return parseAlterType(trimmed, false);
        }

        // ── REMOVE PROPERTY ────────────────────────────────────────────
        if (upper.startsWith("REMOVE PROPERTY")) {
            return parseRemoveProperty(trimmed);
        }

        // ── EXPLAIN ────────────────────────────────────────────────────
        if (upper.startsWith("EXPLAIN")) {
            return parseExplain(trimmed);
        }

        // ── History ───────────────────────────────────────────────────
        if (upper.equals("HISTORY")) {
            return new Query(Query.Type.HISTORY);
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
        if (rest.isEmpty()) return new Query(Query.Type.UNKNOWN);

        q.setNodeId(firstToken(rest));
        String nodeType = extractKeywordValue(rest, "TYPE");
        if (nodeType != null) nodeType = nodeType.split("\\s+")[0];
        q.setNodeType(nodeType);
        parseProperties(q, extractKeywordValue(rest, "PROPERTIES"));
        return q;
    }

    // DELETE NODE u1
    private Query parseDeleteNode(String input) {
        Query q = new Query(Query.Type.DELETE_NODE);
        String id = stripPrefix(input, "DELETE NODE").trim();
        if (id.isEmpty()) return new Query(Query.Type.UNKNOWN);
        q.setNodeId(id);
        return q;
    }

    // UPDATE NODE u1 SET age=26
    private Query parseUpdateNode(String input) {
        Query q = new Query(Query.Type.UPDATE_NODE);
        String rest = stripPrefix(input, "UPDATE NODE").trim();
        if (rest.isEmpty()) return new Query(Query.Type.UNKNOWN);
        q.setNodeId(firstToken(rest));
        String setExpr = extractKeywordValue(rest, "SET");
        if (setExpr != null && !setExpr.isEmpty()) {
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
        if (rest.isEmpty()) return new Query(Query.Type.UNKNOWN);
        q.setEdgeSourceId(firstToken(rest));
        q.setEdgeDestId(extractKeywordValue(rest, "TO"));
        String toVal = q.getEdgeDestId();
        if (toVal != null && !toVal.isEmpty()) q.setEdgeDestId(toVal.split("\\s+")[0]);
        String relType = extractKeywordValue(rest, "TYPE");
        if (relType != null && !relType.isEmpty()) relType = relType.split("\\s+")[0];
        q.setRelationshipType(relType);
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
    // FIND User WHERE age > 25
    // FIND User WHERE age > 25 AND name = Alice
    // FIND User WHERE age > 25 OR name = Bob
    // FIND User
    private Query parseFind(String input) {
        Query q = new Query(Query.Type.FIND);
        String rest = stripPrefix(input, "FIND").trim();
        String upper = rest.toUpperCase();
        String typePart;
        if (upper.contains("WHERE")) {
            typePart = rest.substring(0, upper.indexOf("WHERE")).trim();
            String whereExpr = rest.substring(upper.indexOf("WHERE") + 5).trim();
            parseWhereClause(q, whereExpr);
        } else {
            typePart = rest.trim();
        }
        q.setNodeType(typePart);
        return q;
    }

    /**
     * Parses a WHERE clause that may contain:
     *   - Single condition:   age = 25, age > 25, age != 30
     *   - Compound AND:       age > 25 AND name = Alice
     *   - Compound OR:        age > 25 OR name = Bob
     */
    private void parseWhereClause(Query q, String whereExpr) {
        String upper = whereExpr.toUpperCase();

        // Check for AND / OR
        int andIdx = findLogicOperator(upper, " AND ");
        int orIdx  = findLogicOperator(upper, " OR ");

        if (andIdx > 0 || orIdx > 0) {
            // Compound condition
            boolean isAnd = (andIdx > 0 && (orIdx < 0 || andIdx < orIdx));
            String logic = isAnd ? "AND" : "OR";
            String splitter = isAnd ? " AND " : " OR ";
            int splitIdx = isAnd ? andIdx : orIdx;

            q.setConditionLogic(logic);

            // Split and parse each part
            String[] parts = splitCompound(whereExpr, upper, splitter);
            for (String part : parts) {
                Query.Condition cond = parseSingleCondition(part.trim());
                if (cond != null) {
                    q.addCondition(cond.key, cond.operator, cond.value);
                }
            }

            // Also set the first condition in the legacy fields for backward compat
            if (q.getConditions() != null && !q.getConditions().isEmpty()) {
                Query.Condition first = q.getConditions().get(0);
                q.setConditionKey(first.key);
                q.setConditionOp(first.operator);
                q.setConditionValue(first.value);
            }
        } else {
            // Single condition
            Query.Condition cond = parseSingleCondition(whereExpr);
            if (cond != null) {
                q.setConditionKey(cond.key);
                q.setConditionOp(cond.operator);
                q.setConditionValue(cond.value);
            }
        }
    }

    /**
     * Parses a single condition like "age > 25" or "name=Alice" into key, operator, value.
     * Supports: =, !=, <=, >=, <, >
     */
    private Query.Condition parseSingleCondition(String expr) {
        if (expr == null || expr.trim().isEmpty()) return null;
        expr = expr.trim();

        // Try two-char operators first: !=, <=, >=
        String[] twoCharOps = {"!=", "<=", ">="};
        for (String op : twoCharOps) {
            int idx = expr.indexOf(op);
            if (idx > 0) {
                String key = expr.substring(0, idx).trim();
                String val = expr.substring(idx + 2).trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    return new Query.Condition(key, op, val);
                }
            }
        }

        // Single-char operators: <, >, =
        String[] oneCharOps = {"<", ">", "="};
        for (String op : oneCharOps) {
            int idx = expr.indexOf(op);
            if (idx > 0) {
                String key = expr.substring(0, idx).trim();
                String val = expr.substring(idx + 1).trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    return new Query.Condition(key, op, val);
                }
            }
        }

        return null;
    }

    private int findLogicOperator(String upper, String op) {
        return upper.indexOf(op);
    }

    private String[] splitCompound(String original, String upper, String splitter) {
        // Split preserving original case
        java.util.List<String> parts = new java.util.ArrayList<>();
        int start = 0;
        int idx;
        String upperSplitter = splitter.toUpperCase();
        while ((idx = upper.indexOf(upperSplitter, start)) >= 0) {
            parts.add(original.substring(start, idx));
            start = idx + splitter.length();
        }
        parts.add(original.substring(start));
        return parts.toArray(new String[0]);
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
            " REQUIRED ", " OPTIONAL ", " SET ", " WEIGHT "
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

    // PATH EXISTS u1 TO u4 [TYPE FRIENDS]
    private Query parsePathExists(String input) {
        Query q = new Query(Query.Type.PATH_EXISTS);
        String rest = stripPrefix(input, "PATH EXISTS").trim();
        q.setNodeId(firstToken(rest));
        String toVal = extractKeywordValue(rest, "TO");
        if (toVal != null) {
            q.setSecondNodeId(toVal.split("\\s+")[0]);
        }
        q.setRelationshipType(extractKeywordValue(rest, "TYPE"));
        return q;
    }

    // WEIGHTED SHORTEST PATH u1 TO u2 WEIGHT distance [TYPE ROADS]
    private Query parseWeightedShortestPath(String input) {
        Query q = new Query(Query.Type.WEIGHTED_SHORTEST_PATH);
        String rest = stripPrefix(input, "WEIGHTED SHORTEST PATH").trim();
        if (rest.isEmpty()) return new Query(Query.Type.UNKNOWN);
        q.setNodeId(firstToken(rest));
        String toVal = extractKeywordValue(rest, "TO");
        if (toVal != null) {
            q.setSecondNodeId(toVal.split("\\s+")[0]);
        }
        // WEIGHT property key used for edge weight
        String weightProp = extractKeywordValue(rest, "WEIGHT");
        if (weightProp != null) {
            q.setConditionKey(weightProp.split("\\s+")[0]);
        }
        q.setRelationshipType(extractKeywordValue(rest, "TYPE"));
        return q;
    }

    // ALTER NODE TYPE User ADD REQUIRED email
    // ALTER NODE TYPE User ADD OPTIONAL phone
    // ALTER NODE TYPE User REMOVE email
    // ALTER RELATIONSHIP TYPE FRIENDS ADD OPTIONAL weight
    private Query parseAlterType(String input, boolean isNode) {
        Query.Type qtype = isNode ? Query.Type.ALTER_NODE_TYPE : Query.Type.ALTER_RELATIONSHIP_TYPE;
        Query q = new Query(qtype);
        String prefix = isNode ? "ALTER NODE TYPE" : "ALTER RELATIONSHIP TYPE";
        String rest = stripPrefix(input, prefix).trim();
        if (rest.isEmpty()) return new Query(Query.Type.UNKNOWN);

        // First token is the type name
        String typeName = firstToken(rest);
        if (isNode) {
            q.setNodeType(typeName);
        } else {
            q.setRelationshipType(typeName);
        }

        String upper = rest.toUpperCase();
        // Look for ADD REQUIRED, ADD OPTIONAL, REMOVE
        if (upper.contains("ADD REQUIRED")) {
            String val = extractKeywordValueAfter(rest, "ADD REQUIRED");
            q.setRequiredProps(val);
            q.setConditionOp("ADD_REQUIRED");
        } else if (upper.contains("ADD OPTIONAL")) {
            String val = extractKeywordValueAfter(rest, "ADD OPTIONAL");
            q.setOptionalProps(val);
            q.setConditionOp("ADD_OPTIONAL");
        } else if (upper.contains("REMOVE")) {
            String val = extractKeywordValueAfter(rest, "REMOVE");
            q.setConditionKey(val);
            q.setConditionOp("REMOVE");
        }
        return q;
    }

    private String extractKeywordValueAfter(String input, String keyword) {
        String upper = input.toUpperCase();
        String kwUpper = keyword.toUpperCase();
        int idx = upper.indexOf(kwUpper);
        if (idx < 0) return null;
        String after = input.substring(idx + keyword.length()).trim();
        return after.isEmpty() ? null : after.split("\\s+")[0];
    }

    // REMOVE PROPERTY <typeName> <property>   (schema removal)
    // REMOVE PROPERTY <key> FROM <nodeId>      (node instance removal)
    private Query parseRemoveProperty(String input) {
        Query q = new Query(Query.Type.REMOVE_PROPERTY);
        String rest = stripPrefix(input, "REMOVE PROPERTY").trim();
        if (rest.isEmpty()) return new Query(Query.Type.UNKNOWN);

        // Check for FROM keyword → node-instance removal
        String fromVal = extractKeywordValue(rest, "FROM");
        if (fromVal != null) {
            q.setConditionKey(firstToken(rest));
            q.setNodeId(fromVal.split("\\s+")[0]);
        } else {
            // Schema removal: REMOVE PROPERTY <typeName> <property>
            String[] parts = rest.split("\\s+", 2);
            if (parts.length < 2) return new Query(Query.Type.UNKNOWN);
            q.setNodeType(parts[0]);           // type name
            q.setConditionKey(parts[1].trim()); // property name
        }
        return q;
    }

    // EXPLAIN FIND User WHERE age > 25
    private Query parseExplain(String input) {
        Query q = new Query(Query.Type.EXPLAIN);
        String rest = stripPrefix(input, "EXPLAIN").trim();
        // Store the inner query text in filePath (repurposed field)
        if (!rest.isEmpty()) {
            q.setFilePath(rest);
        }
        return q;
    }
}

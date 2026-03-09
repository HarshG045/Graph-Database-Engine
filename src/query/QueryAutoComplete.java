package query;

import engine.GraphEngine;
import model.Node;
import schema.SchemaManager;

import java.util.*;

/**
 * Query AutoComplete Engine
 *
 * Provides context-aware suggestions for the GDB query language.
 * Given a partial input string, returns a list of possible completions
 * based on the current position in the command syntax and live schema/data.
 */
public class QueryAutoComplete {

    private final GraphEngine engine;

    private static final String[] TOP_LEVEL = {
        "CREATE", "DROP", "ADD", "DELETE", "UPDATE",
        "FIND", "NEIGHBORS", "DESCRIBE",
        "BFS", "DFS", "SHORTEST",
        "DEGREE", "STATS", "COMPONENTS",
        "HAS", "PATH", "EXPORT", "HISTORY",
        "SAVE", "LOAD",
        "SHOW", "COUNT", "CLEAR",
        "DEMO", "HELP", "EXIT", "QUIT"
    };

    public QueryAutoComplete(GraphEngine engine) {
        this.engine = engine;
    }

    /**
     * Returns a list of suggested completions for the given partial input.
     */
    public List<String> suggest(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Arrays.asList(TOP_LEVEL);
        }

        String trimmed = input.trim();
        String[] tokens = trimmed.split("\\s+");
        boolean endsWithSpace = input.endsWith(" ");
        // The token being typed (empty string if user ended with space)
        String partial = endsWithSpace ? "" : tokens[tokens.length - 1];
        // Number of complete tokens (not counting the one being typed)
        int complete = endsWithSpace ? tokens.length : tokens.length - 1;

        // ── First token (top-level command) ──
        if (complete == 0) {
            return filterPrefix(TOP_LEVEL, partial);
        }

        String cmd = tokens[0].toUpperCase();

        switch (cmd) {
            case "CREATE":   return suggestCreate(tokens, complete, partial);
            case "DROP":     return suggestDrop(tokens, complete, partial);
            case "ADD":      return suggestAdd(tokens, complete, partial);
            case "DELETE":   return suggestDelete(tokens, complete, partial);
            case "UPDATE":   return suggestUpdate(tokens, complete, partial);
            case "FIND":     return suggestFind(tokens, complete, partial);
            case "NEIGHBORS":return suggestSingleNodeId(complete, partial);
            case "DESCRIBE": return suggestSingleNodeId(complete, partial);
            case "BFS":
            case "DFS":      return suggestBfsDfs(complete, partial);
            case "SHORTEST": return suggestShortestPath(complete, partial);
            case "SHOW":     return suggestShow(complete, partial);
            case "COUNT":    return suggestCount(tokens, complete, partial);
            case "CLEAR":    return suggestClear(complete, partial);
            case "DEGREE":  return suggestSingleNodeId(complete, partial);
            case "HAS":     return suggestHas(complete, partial);
            case "PATH":    return suggestPathExists(tokens, complete, partial);
            case "EXPORT":  return suggestExport(tokens, complete, partial);
            default:         return Collections.emptyList();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CREATE NODE TYPE <name> [REQUIRED p1,p2] [OPTIONAL p3]
    //  CREATE RELATIONSHIP TYPE <name> FROM <src> TO <dst> [REQUIRED] [OPTIONAL]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestCreate(String[] tokens, int complete, String partial) {
        // CREATE _
        if (complete == 1) return filterPrefix(asList("NODE", "RELATIONSHIP"), partial);

        String kind = tokens[1].toUpperCase();

        // CREATE NODE _ | CREATE RELATIONSHIP _
        if (complete == 2) return filterPrefix(asList("TYPE"), partial);

        // CREATE NODE TYPE _ | CREATE RELATIONSHIP TYPE _
        if (complete == 3) return Collections.emptyList(); // user types the name

        if (kind.equals("NODE")) {
            // CREATE NODE TYPE <name> ...
            // After name: REQUIRED, OPTIONAL
            return filterPrefix(asList("REQUIRED", "OPTIONAL"), partial);
        }

        if (kind.equals("RELATIONSHIP")) {
            // CREATE RELATIONSHIP TYPE <name> FROM <src> TO <dst> [REQUIRED] [OPTIONAL]
            // tokens: 0=CREATE 1=RELATIONSHIP 2=TYPE 3=<name> 4=? ...
            // Find where we are by tracking expected keywords
            String joined = joinUpper(tokens, complete);

            if (!joined.contains("FROM")) {
                return filterPrefix(asList("FROM"), partial);
            }
            if (!joined.contains(" TO ") && !lastCompleteTokenIs(tokens, complete, "TO")) {
                // After FROM we need a node type, then TO
                String afterFrom = getTokenAfterKeyword(tokens, complete, "FROM");
                if (afterFrom == null) {
                    // We're right after FROM — suggest node types
                    return filterPrefix(getNodeTypeNames(), partial);
                }
                // Node type was given, now expect TO
                return filterPrefix(asList("TO"), partial);
            }

            // TO exists. After TO we need a node type, then REQUIRED/OPTIONAL
            String afterTo = getTokenAfterKeyword(tokens, complete, "TO");
            if (afterTo == null) {
                return filterPrefix(getNodeTypeNames(), partial);
            }

            return filterPrefix(asList("REQUIRED", "OPTIONAL"), partial);
        }

        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  DROP NODE TYPE <name> | DROP RELATIONSHIP TYPE <name>
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestDrop(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("NODE", "RELATIONSHIP"), partial);
        if (complete == 2) return filterPrefix(asList("TYPE"), partial);
        if (complete == 3) {
            String kind = tokens[1].toUpperCase();
            if (kind.equals("NODE")) return filterPrefix(getNodeTypeNames(), partial);
            if (kind.equals("RELATIONSHIP")) return filterPrefix(getRelTypeNames(), partial);
        }
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ADD NODE <id> TYPE <type> [PROPERTIES k=v]
    //  ADD EDGE <src> TO <dst> TYPE <rel> [PROPERTIES k=v]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestAdd(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("NODE", "EDGE"), partial);

        String kind = tokens[1].toUpperCase();

        if (kind.equals("NODE")) {
            // ADD NODE <id> TYPE <type> PROPERTIES ...
            // complete: 2=<id>, 3=TYPE, 4=<type>, 5=PROPERTIES
            if (complete == 2) return Collections.emptyList(); // user types node id
            if (complete == 3) return filterPrefix(asList("TYPE"), partial);
            if (complete == 4) return filterPrefix(getNodeTypeNames(), partial);
            if (complete >= 5) return filterPrefix(asList("PROPERTIES"), partial);
        }

        if (kind.equals("EDGE")) {
            // ADD EDGE <src> TO <dst> TYPE <rel> PROPERTIES ...
            // complete: 2=<src>, 3=TO, 4=<dst>, 5=TYPE, 6=<rel>, 7=PROPERTIES
            if (complete == 2) return filterPrefix(getNodeIds(), partial);
            if (complete == 3) return filterPrefix(asList("TO"), partial);
            if (complete == 4) return filterPrefix(getNodeIds(), partial);
            if (complete == 5) return filterPrefix(asList("TYPE"), partial);
            if (complete == 6) return filterPrefix(getRelTypeNames(), partial);
            if (complete >= 7) return filterPrefix(asList("PROPERTIES"), partial);
        }

        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  DELETE NODE <id> | DELETE EDGE <src> TO <dst> TYPE <rel>
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestDelete(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("NODE", "EDGE"), partial);

        String kind = tokens[1].toUpperCase();

        if (kind.equals("NODE")) {
            if (complete == 2) return filterPrefix(getNodeIds(), partial);
        }

        if (kind.equals("EDGE")) {
            if (complete == 2) return filterPrefix(getNodeIds(), partial);
            if (complete == 3) return filterPrefix(asList("TO"), partial);
            if (complete == 4) return filterPrefix(getNodeIds(), partial);
            if (complete == 5) return filterPrefix(asList("TYPE"), partial);
            if (complete == 6) return filterPrefix(getRelTypeNames(), partial);
        }

        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UPDATE NODE <id> SET k=v | UPDATE EDGE <src> TO <dst> TYPE <rel> SET k=v
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestUpdate(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("NODE", "EDGE"), partial);

        String kind = tokens[1].toUpperCase();

        if (kind.equals("NODE")) {
            if (complete == 2) return filterPrefix(getNodeIds(), partial);
            if (complete == 3) return filterPrefix(asList("SET"), partial);
        }

        if (kind.equals("EDGE")) {
            if (complete == 2) return filterPrefix(getNodeIds(), partial);
            if (complete == 3) return filterPrefix(asList("TO"), partial);
            if (complete == 4) return filterPrefix(getNodeIds(), partial);
            if (complete == 5) return filterPrefix(asList("TYPE"), partial);
            if (complete == 6) return filterPrefix(getRelTypeNames(), partial);
            if (complete == 7) return filterPrefix(asList("SET"), partial);
        }

        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  FIND <type> [WHERE key=value]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestFind(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(getNodeTypeNames(), partial);
        if (complete == 2) return filterPrefix(asList("WHERE"), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  NEIGHBORS <id> | DESCRIBE <id>
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestSingleNodeId(int complete, String partial) {
        if (complete == 1) return filterPrefix(getNodeIds(), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  BFS <id> [TYPE <rel>] | DFS <id> [TYPE <rel>]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestBfsDfs(int complete, String partial) {
        if (complete == 1) return filterPrefix(getNodeIds(), partial);
        if (complete == 2) return filterPrefix(asList("TYPE"), partial);
        if (complete == 3) return filterPrefix(getRelTypeNames(), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  SHORTEST PATH <start> TO <end> [TYPE <rel>]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestShortestPath(int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("PATH"), partial);
        if (complete == 2) return filterPrefix(getNodeIds(), partial);
        if (complete == 3) return filterPrefix(asList("TO"), partial);
        if (complete == 4) return filterPrefix(getNodeIds(), partial);
        if (complete == 5) return filterPrefix(asList("TYPE"), partial);
        if (complete == 6) return filterPrefix(getRelTypeNames(), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  SHOW SCHEMA | SHOW GRAPH | SHOW INDEX
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestShow(int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("SCHEMA", "GRAPH", "INDEX"), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  COUNT [NODES|EDGES] [TYPE <type>]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestCount(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("NODES", "EDGES"), partial);
        if (complete == 2) return filterPrefix(asList("TYPE"), partial);
        if (complete == 3) {
            if (tokens[1].equalsIgnoreCase("NODES")) return filterPrefix(getNodeTypeNames(), partial);
            if (tokens[1].equalsIgnoreCase("EDGES")) return filterPrefix(getRelTypeNames(), partial);
        }
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CLEAR [ALL]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestClear(int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("ALL"), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  HAS CYCLE
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestHas(int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("CYCLE"), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  PATH EXISTS <src> TO <dst> [TYPE <rel>]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestPathExists(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("EXISTS"), partial);
        if (complete == 2) return filterPrefix(getNodeIds(), partial);
        if (complete == 3) return filterPrefix(asList("TO"), partial);
        if (complete == 4) return filterPrefix(getNodeIds(), partial);
        if (complete == 5) return filterPrefix(asList("TYPE"), partial);
        if (complete == 6) return filterPrefix(getRelTypeNames(), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPORT DOT [filepath] | EXPORT CSV [filepath]
    // ═══════════════════════════════════════════════════════════════

    private List<String> suggestExport(String[] tokens, int complete, String partial) {
        if (complete == 1) return filterPrefix(asList("DOT", "CSV"), partial);
        return Collections.emptyList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Data helpers — fetch live schema / node data
    // ═══════════════════════════════════════════════════════════════

    private List<String> getNodeTypeNames() {
        SchemaManager sm = engine.getSchemaManager();
        List<String> names = new ArrayList<>(sm.getNodeTypeMap().keySet());
        Collections.sort(names);
        return names;
    }

    private List<String> getRelTypeNames() {
        SchemaManager sm = engine.getSchemaManager();
        List<String> names = new ArrayList<>(sm.getRelationshipTypeMap().keySet());
        Collections.sort(names);
        return names;
    }

    private List<String> getNodeIds() {
        List<String> ids = new ArrayList<>();
        for (Node n : engine.getStorage().getAllNodes()) {
            ids.add(n.getId());
        }
        Collections.sort(ids);
        return ids;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Utility helpers
    // ═══════════════════════════════════════════════════════════════

    private List<String> filterPrefix(List<String> candidates, String prefix) {
        if (prefix == null || prefix.isEmpty()) return candidates;
        List<String> result = new ArrayList<>();
        String upper = prefix.toUpperCase();
        for (String c : candidates) {
            if (c.toUpperCase().startsWith(upper)) {
                result.add(c);
            }
        }
        return result;
    }

    private List<String> filterPrefix(String[] candidates, String prefix) {
        return filterPrefix(Arrays.asList(candidates), prefix);
    }

    private List<String> asList(String... items) {
        return Arrays.asList(items);
    }

    /** Join tokens[0..upTo-1] in uppercase with spaces. */
    private String joinUpper(String[] tokens, int upTo) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(upTo, tokens.length);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(' ');
            sb.append(tokens[i].toUpperCase());
        }
        return sb.toString();
    }

    /** Check if the last complete token matches a keyword. */
    private boolean lastCompleteTokenIs(String[] tokens, int complete, String keyword) {
        if (complete < 1 || complete > tokens.length) return false;
        return tokens[complete - 1].equalsIgnoreCase(keyword);
    }

    /** Get the token immediately after a keyword, or null if not found / nothing after it. */
    private String getTokenAfterKeyword(String[] tokens, int complete, String keyword) {
        for (int i = 0; i < complete - 1; i++) {
            if (tokens[i].equalsIgnoreCase(keyword) && i + 1 < complete) {
                return tokens[i + 1];
            }
        }
        return null;
    }
}

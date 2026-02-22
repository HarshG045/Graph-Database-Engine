import engine.GraphEngine;
import query.Query;
import query.QueryExecutor;
import query.QueryParser;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Main Entry Point — Interactive CLI
 *
 * Provides a read-eval-print loop (REPL) for the Graph Database Engine.
 * Users type commands which are parsed and executed in real time.
 *
 * Usage:
 *   java -cp out Main
 *
 * Type HELP to see the command reference.
 * Type EXIT or QUIT to terminate.
 */
public class Main {

    private static final String BANNER =
        "╔═══════════════════════════════════════════════════════════╗\n" +
        "║         GRAPH-ORIENTED DATABASE ENGINE v1.0               ║\n" +
        "║  Schema · Storage · Traversal · Query · Persistence       ║\n" +
        "╚═══════════════════════════════════════════════════════════╝\n" +
        "  Type HELP for command reference.  Type EXIT to quit.\n";

    private static final String HELP =
        "\n╔════════════════════════════════════════════════════════════╗\n" +
        "║                    COMMAND REFERENCE                       ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ DDL — Schema Definition                                    ║\n" +
        "║  CREATE NODE TYPE <name> [REQUIRED p1,p2] [OPTIONAL p3]   ║\n" +
        "║  CREATE RELATIONSHIP TYPE <name> FROM <src> TO <dst>       ║\n" +
        "║         [REQUIRED p1] [OPTIONAL p2]                        ║\n" +
        "║  DROP NODE TYPE <name>       (blocked if nodes exist)      ║\n" +
        "║  DROP RELATIONSHIP TYPE <name>  (blocked if edges exist)   ║\n" +
        "║  SHOW SCHEMA                                               ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ DML — Node Operations                                      ║\n" +
        "║  ADD NODE <id> TYPE <type> [PROPERTIES k=v,k2=v2]         ║\n" +
        "║  DELETE NODE <id>                                          ║\n" +
        "║  UPDATE NODE <id> SET <key>=<value>                        ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ DML — Edge Operations                                      ║\n" +
        "║  ADD EDGE <srcId> TO <dstId> TYPE <relType>               ║\n" +
        "║         [PROPERTIES k=v]                                   ║\n" +
        "║  DELETE EDGE <srcId> TO <dstId> TYPE <relType>            ║\n" +
        "║  UPDATE EDGE <srcId> TO <dstId> TYPE <relType> SET k=v    ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ Query / Retrieval                                          ║\n" +
        "║  FIND <type> [WHERE <key>=<value>]                        ║\n" +
        "║  NEIGHBORS <nodeId>                                        ║\n" +
        "║  DESCRIBE <nodeId>    (full node detail + all edges)       ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ Traversal                                                  ║\n" +
        "║  BFS <nodeId> [TYPE <relType>]                            ║\n" +
        "║  DFS <nodeId> [TYPE <relType>]                            ║\n" +
        "║  SHORTEST PATH <startId> TO <endId> [TYPE <relType>]      ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ Aggregation / Counting                                     ║\n" +
        "║  COUNT                        (total nodes and edges)      ║\n" +
        "║  COUNT NODES [TYPE <type>]                                ║\n" +
        "║  COUNT EDGES [TYPE <relType>]                             ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ Persistence                                                ║\n" +
        "║  SAVE [<filepath>]                                        ║\n" +
        "║  LOAD [<filepath>]                                        ║\n" +
        "╠════════════════════════════════════════════════════════════╣\n" +
        "║ Utility                                                    ║\n" +
        "║  SHOW GRAPH                                               ║\n" +
        "║  SHOW INDEX                                               ║\n" +
        "║  CLEAR             (wipe nodes+edges, keep schema)         ║\n" +
        "║  CLEAR ALL         (wipe everything including schema)      ║\n" +
        "║  DEMO                                                     ║\n" +
        "║  HELP                                                     ║\n" +
        "║  EXIT / QUIT                                              ║\n" +
        "╚════════════════════════════════════════════════════════════╝\n";

    public static void main(String[] args) {
        System.out.println(BANNER);

        GraphEngine   engine   = new GraphEngine();
        QueryParser   parser   = new QueryParser();
        QueryExecutor executor = new QueryExecutor(engine);
        Scanner       scanner  = new Scanner(System.in);

        while (true) {
            System.out.print("GDB> ");
            String input;
            try {
                input = scanner.nextLine().trim();
            } catch (NoSuchElementException e) {
                // stdin closed (e.g. piped input ended)
                System.out.println("Goodbye.");
                break;
            }

            if (input.isEmpty()) continue;

            String upper = input.toUpperCase();
            if (upper.equals("EXIT") || upper.equals("QUIT")) {
                System.out.println("Goodbye.");
                break;
            }
            if (upper.equals("HELP")) {
                System.out.println(HELP);
                continue;
            }
            if (upper.equals("DEMO")) {
                runDemo(executor, parser);
                continue;
            }

            Query query = parser.parse(input);
            executor.execute(query);
            System.out.println();
        }

        scanner.close();
    }

    /**
     * Pre-built demonstration that exercises all engine layers.
     * Runs automatically when the user types DEMO.
     */
    private static void runDemo(QueryExecutor executor, QueryParser parser) {
        System.out.println("\n══ DEMO: Social Network Graph ══════════════════════════\n");

        String[] commands = {
            // ── DDL
            "CREATE NODE TYPE User REQUIRED name,age OPTIONAL email",
            "CREATE NODE TYPE Company REQUIRED name OPTIONAL industry",
            "CREATE RELATIONSHIP TYPE FRIENDS FROM User TO User OPTIONAL since",
            "CREATE RELATIONSHIP TYPE WORKS_AT FROM User TO Company REQUIRED role",
            "SHOW SCHEMA",

            // ── Add nodes
            "ADD NODE u1 TYPE User PROPERTIES name=Alice,age=30,email=alice@example.com",
            "ADD NODE u2 TYPE User PROPERTIES name=Bob,age=25",
            "ADD NODE u3 TYPE User PROPERTIES name=Carol,age=28",
            "ADD NODE u4 TYPE User PROPERTIES name=Dave,age=35",
            "ADD NODE c1 TYPE Company PROPERTIES name=TechCorp,industry=Software",
            "ADD NODE c2 TYPE Company PROPERTIES name=DataInc,industry=Analytics",

            // ── Add edges
            "ADD EDGE u1 TO u2 TYPE FRIENDS PROPERTIES since=2020",
            "ADD EDGE u2 TO u3 TYPE FRIENDS PROPERTIES since=2021",
            "ADD EDGE u3 TO u4 TYPE FRIENDS",
            "ADD EDGE u1 TO c1 TYPE WORKS_AT PROPERTIES role=Engineer",
            "ADD EDGE u2 TO c1 TYPE WORKS_AT PROPERTIES role=Manager",
            "ADD EDGE u4 TO c2 TYPE WORKS_AT PROPERTIES role=Analyst",

            // ── Show graph
            "SHOW GRAPH",

            // ── Queries
            "FIND User",
            "FIND User WHERE age=25",
            "FIND Company WHERE industry=Software",

            // ── Neighbors
            "NEIGHBORS u1",

            // ── Traversal
            "BFS u1",
            "DFS u1",
            "SHORTEST PATH u1 TO u4",
            "SHORTEST PATH u1 TO u4 TYPE FRIENDS",

            // ── Update
            "UPDATE NODE u2 SET age=26",
            "FIND User WHERE age=26",

            // ── New: COUNT commands
            "COUNT",
            "COUNT NODES",
            "COUNT NODES TYPE User",
            "COUNT EDGES TYPE FRIENDS",

            // ── New: DESCRIBE
            "DESCRIBE u1",

            // ── Index
            "SHOW INDEX",

            // ── Persistence
            "SAVE demo_graph.gdb",
        };

        for (String cmd : commands) {
            System.out.println("\n  [INPUT] " + cmd);
            executor.execute(parser.parse(cmd));
        }

        System.out.println("\n══ DEMO COMPLETE ═══════════════════════════════════════\n");
    }
}

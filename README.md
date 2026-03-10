# Graph-Oriented Database Engine

A fully in-memory, schema-enforced graph database engine built in Java with a custom query language, interactive REPL CLI, BFS/DFS/shortest-path traversal, property indexing, and file persistence.

---

## Table of Contents

1. [Features](#features)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Requirements](#requirements)
5. [Build and Run](#build-and-run)
6. [Command Reference](#command-reference)
   - [DDL — Schema Definition](#ddl--schema-definition)
   - [DML — Node Operations](#dml--node-operations)
   - [DML — Edge Operations](#dml--edge-operations)
   - [Query and Retrieval](#query-and-retrieval)
   - [Traversal](#traversal)
   - [Aggregation and Counting](#aggregation-and-counting)
   - [Persistence](#persistence)
   - [Utility](#utility)
7. [Complete Examples](#complete-examples)
8. [Persistence File Format](#persistence-file-format)
9. [Design Decisions](#design-decisions)
10. [Known Limitations](#known-limitations)

---

## Features

- **Schema-first design** — define node types and relationship types with required and optional properties before inserting data
- **Constraint enforcement** — unique node IDs, required property validation, type compatibility checks on edges, referential integrity on schema DROP
- **Schema evolution** — `ALTER TYPE` to add properties, `REMOVE PROPERTY` to drop them
- **Graph traversal** — BFS, DFS, unweighted shortest-path, and **Dijkstra weighted shortest-path** with optional relationship-type filters
- **Property index** — O(1) average lookup for `WHERE` property queries; O(k) type index for fast node-type lookups
- **Range queries** — `WHERE age > 25`, `WHERE price <= 100` with operators `=`, `!=`, `<`, `<=`, `>`, `>=`
- **Compound conditions** — `WHERE age > 25 AND city = NYC` or `WHERE role = Eng OR role = Mgr`
- **Property data types** — automatic type detection (integer, float, boolean, string) for correct numeric/lexicographic comparisons
- **Import CSV** — bulk-load nodes and edges from CSV files
- **EXPLAIN** — display the query execution plan before running a command
- **Validated LOAD** — post-load validation reports missing types, required properties, and dangling references
- **Rich query language** — custom command syntax parsed via `QueryParser` into `Query` objects executed by `QueryExecutor`
- **File persistence** — save and load the full graph state (schema + nodes + edges) to a `.gdb` text file with path sandboxing
- **Interactive REPL** — live `GDB> ` prompt with `HELP`, `DEMO`, tab-autocomplete, and `EXIT` built in

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Main  (REPL CLI)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
              ┌──────────────▼──────────────┐
              │        QueryParser           │  raw string → Query
              └──────────────┬──────────────┘
                             │
              ┌──────────────▼──────────────┐
              │       QueryExecutor          │  dispatches to engine
              └──────────────┬──────────────┘
                             │
              ┌──────────────▼──────────────┐
              │         GraphEngine          │  facade / coordinator
              └──┬──────┬──────┬────────────┘
                 │      │      │
       ┌─────────▼─┐  ┌─▼────────┐  ┌───────────────┐
       │NodeManager│  │EdgeManager│  │TraversalEngine│
       └─────────┬─┘  └──────────┘  └───────────────┘
                 │
       ┌─────────▼──────────┐   ┌──────────────────┐
       │   GraphStorage      │   │  PropertyIndex   │
       │  (HashMap + Adj.   │   │  (nested Maps)   │
       │   List)             │   └──────────────────┘
       └─────────────────────┘
                 │
       ┌─────────▼──────────┐   ┌───────────────────┐
       │   SchemaManager    │   │ConstraintValidator │
       └────────────────────┘   └───────────────────┘
                 │
       ┌─────────▼──────────┐
       │   StorageManager   │   .gdb file I/O
       └────────────────────┘
```

**Component responsibilities:**

| Component | Package | Responsibility |
|---|---|---|
| `Node` | `model` | Graph node entity with id, type, properties |
| `Edge` | `model` | Directed edge with src, dst, relationship type, properties |
| `NodeType` | `model` | Schema metadata for node types |
| `RelationshipType` | `model` | Schema metadata for edge types (src type, dst type, required/optional props) |
| `PropertyValue` | `model` | Type-aware property value comparison (int, float, boolean, string) |
| `SchemaManager` | `schema` | Catalog of node types and relationship types |
| `GraphStorage` | `storage` | In-memory store: HashMap for nodes, adjacency list for edges |
| `StorageManager` | `storage` | File serialisation/deserialisation of graph state |
| `ConstraintValidator` | `constraint` | Validates inserts and updates against schema rules |
| `PropertyIndex` | `index` | Inverted index for O(1) property-value lookups |
| `NodeManager` | `engine` | Node CRUD + index maintenance |
| `EdgeManager` | `engine` | Edge CRUD with constraint checking |
| `GraphEngine` | `engine` | Facade wiring all subsystems; safe DROP and CLEAR operations |
| `TraversalEngine` | `traversal` | BFS, DFS, Shortest Path, Dijkstra Weighted Shortest Path |
| `QueryParser` | `query` | Parses command strings into `Query` objects |
| `QueryExecutor` | `query` | Executes `Query` objects against `GraphEngine` |
| `QueryAutoComplete` | `query` | Context-aware tab-completion suggestions |
| `ConsoleReader` | (default) | Line reader with autocomplete support |
| `Main` | (default) | Entry point; REPL loop |

---

## Project Structure

```
Graph Database Engine/
├── src/
│   ├── Main.java
│   ├── ConsoleReader.java
│   ├── model/
│   │   ├── Node.java
│   │   ├── Edge.java
│   │   ├── NodeType.java
│   │   ├── RelationshipType.java
│   │   └── PropertyValue.java
│   ├── schema/
│   │   └── SchemaManager.java
│   ├── storage/
│   │   ├── GraphStorage.java
│   │   └── StorageManager.java
│   ├── constraint/
│   │   └── ConstraintValidator.java
│   ├── index/
│   │   └── PropertyIndex.java
│   ├── engine/
│   │   ├── NodeManager.java
│   │   ├── EdgeManager.java
│   │   └── GraphEngine.java
│   ├── traversal/
│   │   └── TraversalEngine.java
│   └── query/
│       ├── Query.java
│       ├── QueryParser.java
│       ├── QueryExecutor.java
│       └── QueryAutoComplete.java
├── out/               (compiled .class files — created by build)
├── compile.bat        (Windows compile script)
├── run.bat            (Windows compile-and-run script)
├── compile.sh         (Linux / macOS compile script)
├── run.sh             (Linux / macOS compile-and-run script)
└── README.md
```

---

## Requirements

- **Java 8 or later** (uses only the standard library — no external dependencies)
- **Windows** — use the provided `.bat` scripts or run `javac`/`java` directly
- **Linux / macOS** — use the provided `.sh` scripts (requires `bash`)

---

## Build and Run

### Option 1: Use the provided scripts (Windows)

**Compile only:**
```bat
compile.bat
```

**Compile and run:**
```bat
run.bat
```

`run.bat` will recompile automatically if needed, then launch the interactive REPL.

### Option 2: Use the provided scripts (Linux / macOS)

Make the scripts executable once:
```bash
chmod +x compile.sh run.sh
```

**Compile only:**
```bash
./compile.sh
```

**Compile and run:**
```bash
./run.sh
```

`run.sh` will recompile automatically if needed, then launch the interactive REPL.

### Option 3: Manual commands

**Compile:**
```bat
javac -d out src\Main.java src\model\*.java src\schema\*.java src\storage\*.java src\constraint\*.java src\index\*.java src\engine\*.java src\traversal\*.java src\query\*.java
```

**Run:**
```bat
java -cp out Main
```

### Option 4: Run the built-in demo

After starting the REPL, type:
```
GDB> DEMO
```

This creates a social-network graph (Users, Companies, FRIENDS and WORKS_AT relationships), runs queries, traversals, COUNT, DESCRIBE, and saves to `demo_graph.gdb`.

---

## Command Reference

Keywords are **case-insensitive**. Node IDs, type names, and property values are **case-sensitive**.

---

### DDL — Schema Definition

#### CREATE NODE TYPE
```
CREATE NODE TYPE <name> [REQUIRED prop1,prop2,...] [OPTIONAL prop3,prop4,...]
```
Defines a node type with optional required and optional property lists.

```
GDB> CREATE NODE TYPE User REQUIRED name,age OPTIONAL email
GDB> CREATE NODE TYPE Company REQUIRED name OPTIONAL industry
```

#### CREATE RELATIONSHIP TYPE
```
CREATE RELATIONSHIP TYPE <name> FROM <srcNodeType> TO <dstNodeType> [REQUIRED p1] [OPTIONAL p2]
```
Defines a directed relationship type. Edges of this type are validated against the declared source and destination node types.

```
GDB> CREATE RELATIONSHIP TYPE FRIENDS FROM User TO User OPTIONAL since
GDB> CREATE RELATIONSHIP TYPE WORKS_AT FROM User TO Company REQUIRED role
```

#### DROP NODE TYPE
```
DROP NODE TYPE <name>
```
Removes the node type from the schema. **Blocked if any nodes of this type still exist.**

```
GDB> DROP NODE TYPE TempType
```

#### DROP RELATIONSHIP TYPE
```
DROP RELATIONSHIP TYPE <name>
```
Removes the relationship type from the schema. **Blocked if any edges of this type still exist.**

```
GDB> DROP RELATIONSHIP TYPE OLD_LINK
```

#### ALTER NODE TYPE / ALTER RELATIONSHIP TYPE
```
ALTER NODE TYPE <name> ADD REQUIRED|OPTIONAL <property>
ALTER RELATIONSHIP TYPE <name> ADD REQUIRED|OPTIONAL <property>
```
Adds a new required or optional property to an existing type definition.

```
GDB> ALTER NODE TYPE User ADD OPTIONAL phone
GDB> ALTER RELATIONSHIP TYPE FRIENDS ADD OPTIONAL strength
```

#### REMOVE PROPERTY
```
REMOVE PROPERTY <typeName> <propertyName>
```
Removes a property from a node type or relationship type schema. **Blocked if the property is required and nodes/edges still depend on it.**

```
GDB> REMOVE PROPERTY User phone
GDB> REMOVE PROPERTY FRIENDS strength
```

#### SHOW SCHEMA
```
SHOW SCHEMA
```
Displays all defined node types and relationship types with their property constraints.

---

### DML — Node Operations

#### ADD NODE
```
ADD NODE <id> TYPE <type> [PROPERTIES key=value,key2=value2,...]
```
Inserts a new node. The ID must be unique. The type must exist in the schema. All required properties must be supplied.

```
GDB> ADD NODE u1 TYPE User PROPERTIES name=Alice,age=30,email=alice@example.com
GDB> ADD NODE u2 TYPE User PROPERTIES name=Bob,age=25
GDB> ADD NODE c1 TYPE Company PROPERTIES name=TechCorp,industry=Software
```

#### DELETE NODE
```
DELETE NODE <id>
```
Removes a node and all its associated edges (cascading delete).

```
GDB> DELETE NODE u2
```

#### UPDATE NODE
```
UPDATE NODE <id> SET <key>=<value>
```
Updates a single property on an existing node. Cannot clear a required property.

```
GDB> UPDATE NODE u1 SET age=31
GDB> UPDATE NODE u1 SET email=newalice@example.com
```

---

### DML — Edge Operations

#### ADD EDGE
```
ADD EDGE <srcId> TO <dstId> TYPE <relType> [PROPERTIES key=value,...]
```
Creates a directed edge. Both nodes must exist, the relationship type must exist and be compatible with the node types, and all required edge properties must be provided.

```
GDB> ADD EDGE u1 TO u2 TYPE FRIENDS PROPERTIES since=2020
GDB> ADD EDGE u1 TO c1 TYPE WORKS_AT PROPERTIES role=Engineer
GDB> ADD EDGE u2 TO u3 TYPE FRIENDS
```

#### DELETE EDGE
```
DELETE EDGE <srcId> TO <dstId> TYPE <relType>
```
Removes the specified directed edge.

```
GDB> DELETE EDGE u1 TO u2 TYPE FRIENDS
```

#### UPDATE EDGE
```
UPDATE EDGE <srcId> TO <dstId> TYPE <relType> SET <key>=<value>
```
Updates a property on an existing edge. Cannot clear a required property.

```
GDB> UPDATE EDGE u1 TO u2 TYPE FRIENDS SET since=2019
```

---

### Query and Retrieval

#### FIND
```
FIND <nodeType> [WHERE <key> <operator> <value>]
```
Returns all nodes of the given type, optionally filtered by property conditions.

**Operators:** `=`, `!=`, `<`, `<=`, `>`, `>=`

Uses the property index for O(1) exact-match lookups. Range operators trigger a filtered scan with type-aware comparison (integers and floats are compared numerically).

```
GDB> FIND User
GDB> FIND User WHERE age=25
GDB> FIND User WHERE age > 25
GDB> FIND User WHERE age <= 30
GDB> FIND Company WHERE name != TechCorp
```

**Compound conditions** — combine two conditions with `AND` or `OR`:
```
GDB> FIND User WHERE age >= 28 AND age <= 35
GDB> FIND User WHERE name = Alice OR name = Dave
```

#### NEIGHBORS
```
NEIGHBORS <nodeId>
```
Lists all direct outgoing neighbors of the node (regardless of edge type).

```
GDB> NEIGHBORS u1
```

#### DESCRIBE
```
DESCRIBE <nodeId>
```
Displays the node's type, all properties, all outgoing edges, and all incoming edges.

```
GDB> DESCRIBE u1
```

---

### Traversal

All traversal commands accept an optional `TYPE <relType>` filter. Without it, all edge types are traversed.

#### BFS
```
BFS <nodeId> [TYPE <relType>]
```
Breadth-first traversal from the given node. Prints nodes in BFS order.

```
GDB> BFS u1
GDB> BFS u1 TYPE FRIENDS
```

#### DFS
```
DFS <nodeId> [TYPE <relType>]
```
Depth-first traversal from the given node (iterative, stack-based).

```
GDB> DFS u1
GDB> DFS u1 TYPE FRIENDS
```

#### SHORTEST PATH
```
SHORTEST PATH <startId> TO <endId> [TYPE <relType>]
```
Finds the shortest unweighted path between two nodes using BFS. Prints the path as a sequence of nodes and edges.

```
GDB> SHORTEST PATH u1 TO u4
GDB> SHORTEST PATH u1 TO u4 TYPE FRIENDS
```

If no path exists, the engine reports accordingly.

#### WEIGHTED SHORTEST PATH
```
WEIGHTED SHORTEST PATH <startId> TO <endId> WEIGHT <property> [TYPE <relType>]
```
Finds the shortest weighted path using **Dijkstra's algorithm**. The specified edge property is used as the weight (non-numeric or missing properties default to 1.0).

```
GDB> WEIGHTED SHORTEST PATH u1 TO u4 WEIGHT distance
GDB> WEIGHTED SHORTEST PATH u1 TO u4 WEIGHT since TYPE FRIENDS
```

---

### Aggregation and Counting

#### COUNT (all)
```
COUNT
```
Prints total node count and total edge count.

#### COUNT NODES
```
COUNT NODES [TYPE <nodeType>]
```
Counts all nodes, or only nodes of the specified type.

```
GDB> COUNT NODES
GDB> COUNT NODES TYPE User
```

#### COUNT EDGES
```
COUNT EDGES [TYPE <relType>]
```
Counts all edges, or only edges of the specified relationship type.

```
GDB> COUNT EDGES
GDB> COUNT EDGES TYPE FRIENDS
```

---

### Import / Export / Persistence

#### SAVE
```
SAVE [<filepath>]
```
Serialises the full graph state (schema + nodes + edges) to a file. Default filename: `graph_data.gdb`. Parent directories are created automatically. Path sandboxing blocks absolute paths and `..` traversal.

```
GDB> SAVE
GDB> SAVE my_graph.gdb
```

#### LOAD
```
LOAD [<filepath>]
```
Deserialises graph state from file, replacing all current data. Default filename: `graph_data.gdb`. After loading, the engine **validates all data** against the loaded schema and reports any warnings (missing required properties, unknown types, dangling edge references).

```
GDB> LOAD
GDB> LOAD my_graph.gdb
```

#### IMPORT CSV
```
IMPORT CSV <filepath>
```
Bulk-loads nodes and edges from a CSV file. The file must contain `# NODES` and `# EDGES` section headers.

CSV format:
```csv
# NODES
id,type,name,age,email
u5,User,Eve,29,eve@example.com
u6,User,Frank,32,

# EDGES
source,destination,type,since
u5,u6,FRIENDS,2023
```
The first row under each header is treated as the column header. Node types and relationship types must already exist in the schema.

```
GDB> IMPORT CSV people.csv
```

#### EXPORT DOT
```
EXPORT DOT [<filepath>]
```
Exports the graph in Graphviz DOT format. Prints to console if no file is given.

#### EXPORT CSV
```
EXPORT CSV [<filepath>]
```
Exports nodes and edges in CSV format. Prints to console if no file is given.

---

### Utility

#### SHOW GRAPH
```
SHOW GRAPH
```
Prints a summary of all nodes and all edges currently in the graph.

#### SHOW INDEX
```
SHOW INDEX
```
Dumps the contents of the property index (property key → value → list of node IDs).

#### CLEAR
```
CLEAR
```
Removes all nodes and edges but **preserves the schema** (type definitions remain).

#### CLEAR ALL
```
CLEAR ALL
```
Removes everything: schema, nodes, edges, and the index. Returns the engine to a blank state.

#### EXPLAIN
```
EXPLAIN <command>
```
Displays the query execution plan for a command **without executing it**. Shows the command type, filter strategy, index usage, and estimated complexity.

```
GDB> EXPLAIN FIND User WHERE age > 25
GDB> EXPLAIN FIND User WHERE name = Alice
GDB> EXPLAIN SHORTEST PATH u1 TO u4
```

#### HELP
```
HELP
```
Prints the in-app command reference summary.

#### EXIT / QUIT
```
EXIT
QUIT
```
Terminates the REPL.

#### DEMO
```
DEMO
```
Runs the built-in social-network demonstration script and saves the result to `demo_graph.gdb`.

---

## Complete Examples

### Social Network Example

```
GDB> CREATE NODE TYPE User REQUIRED name,age OPTIONAL email
GDB> CREATE NODE TYPE Company REQUIRED name OPTIONAL industry
GDB> CREATE RELATIONSHIP TYPE FRIENDS FROM User TO User OPTIONAL since
GDB> CREATE RELATIONSHIP TYPE WORKS_AT FROM User TO Company REQUIRED role

GDB> ADD NODE u1 TYPE User PROPERTIES name=Alice,age=30,email=alice@example.com
GDB> ADD NODE u2 TYPE User PROPERTIES name=Bob,age=25
GDB> ADD NODE u3 TYPE User PROPERTIES name=Carol,age=28
GDB> ADD NODE u4 TYPE User PROPERTIES name=Dave,age=35
GDB> ADD NODE c1 TYPE Company PROPERTIES name=TechCorp,industry=Software

GDB> ADD EDGE u1 TO u2 TYPE FRIENDS PROPERTIES since=2020
GDB> ADD EDGE u2 TO u3 TYPE FRIENDS PROPERTIES since=2021
GDB> ADD EDGE u3 TO u4 TYPE FRIENDS
GDB> ADD EDGE u1 TO c1 TYPE WORKS_AT PROPERTIES role=Engineer
GDB> ADD EDGE u2 TO c1 TYPE WORKS_AT PROPERTIES role=Manager

GDB> FIND User WHERE age=25
GDB> FIND User WHERE age > 25
GDB> FIND User WHERE age >= 28 AND age <= 35
GDB> NEIGHBORS u1
GDB> BFS u1 TYPE FRIENDS
GDB> SHORTEST PATH u1 TO u4 TYPE FRIENDS
GDB> WEIGHTED SHORTEST PATH u1 TO u4 WEIGHT since TYPE FRIENDS
GDB> DESCRIBE u1

GDB> COUNT NODES TYPE User
GDB> COUNT EDGES TYPE FRIENDS

GDB> ALTER NODE TYPE User ADD OPTIONAL phone
GDB> EXPLAIN FIND User WHERE age > 25

GDB> SAVE social_network.gdb
```

### Reload and Continue

```
GDB> LOAD social_network.gdb
GDB> SHOW SCHEMA
GDB> SHOW GRAPH
GDB> UPDATE NODE u2 SET age=26
GDB> FIND User WHERE age=26
GDB> REMOVE PROPERTY User phone
```

### Schema Drop with Referential Integrity

```
GDB> CREATE NODE TYPE Temp REQUIRED label
GDB> ADD NODE t1 TYPE Temp PROPERTIES label=test
GDB> DROP NODE TYPE Temp
[SCHEMA] Cannot drop node type 'Temp': 1 node(s) of this type still exist. Delete them first.

GDB> DELETE NODE t1
GDB> DROP NODE TYPE Temp
[SCHEMA] Node type 'Temp' dropped.
```

---

## Persistence File Format

The `.gdb` file is a plain-text format with four labelled sections:

```
[SCHEMA_NODE_TYPES]
User|req:name,age|opt:email
Company|req:name|opt:industry

[SCHEMA_RELATIONSHIP_TYPES]
FRIENDS|User|User|req:|opt:since
WORKS_AT|User|Company|req:role|opt:

[NODES]
u1|User|name=Alice;age=30;email=alice@example.com
u2|User|name=Bob;age=25
c1|Company|name=TechCorp;industry=Software

[EDGES]
u1|u2|FRIENDS|since=2020
u1|c1|WORKS_AT|role=Engineer
```

- Fields within a record are separated by `|`
- Properties are separated by `;`; key-value pairs use `=`
- Special characters in values are escaped with `~`:
  - `~t` → `~` (tilde)
  - `~b` → `\` (backslash)
  - `~p` → `|` (pipe)
  - `~s` → `;` (semicolon)
  - `~e` → `=` (equals)
- Tildes are safely round-tripped (`~` → `~t` on save, `~t` → `~` on load)

---

## Design Decisions

1. **Facade pattern for `GraphEngine`** — a single entry point wires all subsystems. `QueryExecutor` interacts only with `GraphEngine`, keeping coupling minimal.

2. **Referential integrity lives in `GraphEngine`** — `SchemaManager` has no access to `GraphStorage` to avoid circular dependencies. `GraphEngine` wrapper methods (`dropNodeType`, `dropRelationshipType`) check live data before delegating to `SchemaManager`.

3. **Property index is separate from storage** — `PropertyIndex` maintains an inverted map independent of `GraphStorage`, making index rebuilding on `LOAD` straightforward.

4. **Tilde escape in persistence** — a backslash escape scheme has ambiguous round-trip behaviour for values that contain literal escape sequences. Switching to tilde provides unambiguous, single-pass encoding.

5. **Iterative DFS** — implemented with an explicit stack rather than recursion to avoid `StackOverflowError` on deep graphs.

6. **BFS for shortest path** — the graph is unweighted; BFS guarantees the minimum hop-count path. A parent map tracks each node's predecessor for path reconstruction.

7. **Dijkstra for weighted shortest path** — when edge properties carry numeric weights, Dijkstra's algorithm with a priority queue finds the minimum-cost path. Missing or non-numeric weights default to 1.0.

8. **Type index for O(k) lookups** — `GraphStorage` maintains a secondary `Map<String, Set<String>>` mapping type names to node IDs. `FIND <type>` leverages this index instead of scanning all nodes.

9. **PropertyValue type detection** — property values are auto-detected as integer, float, boolean, or string. Range comparisons (`<`, `>`, etc.) use numeric ordering for numbers and lexicographic ordering for strings.

10. **Post-load validation** — after `LOAD`, the `ConstraintValidator` scans all nodes and edges against the schema and reports warnings for missing required properties, unknown types, or dangling edge references.

11. **Path sandboxing** — `SAVE`, `LOAD`, `IMPORT CSV`, `EXPORT DOT`, and `EXPORT CSV` all validate paths to block `..` traversal, absolute paths, and unsafe file extensions.

12. **Sentinel strings in COUNT queries** — the `nodeType` field carries `"NODES"`, `"EDGES"`, or `"ALL"` as sentinels, and `relType` carries the optional type filter, avoiding COUNT-specific fields in the `Query` model.

---

## Known Limitations

- **In-memory only** — the entire graph is held in RAM; very large graphs are constrained by available heap.
- **No transactions** — there is no rollback mechanism; a failed command sequence may leave partial state.
- **Two-condition limit** — compound `WHERE` supports at most two conditions joined by `AND` or `OR`.
- **Single-threaded** — there is no locking or thread-safety; the engine is designed for single-user interactive use.
- **Case-sensitive values** — node IDs, type names, and property values are case-sensitive; query keywords are case-insensitive.
- **CSV import requires schema** — node types and relationship types must already be defined before `IMPORT CSV`.

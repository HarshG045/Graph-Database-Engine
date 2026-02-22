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
- **Graph traversal** — BFS, DFS, and unweighted shortest-path with optional relationship-type filters
- **Property index** — O(1) average lookup for `WHERE` property queries
- **Rich query language** — custom command syntax parsed via `QueryParser` into `Query` objects executed by `QueryExecutor`
- **File persistence** — save and load the full graph state (schema + nodes + edges) to a `.gdb` text file
- **Interactive REPL** — live `GDB> ` prompt with `HELP`, `DEMO`, and `EXIT` built in

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
| `SchemaManager` | `schema` | Catalog of node types and relationship types |
| `GraphStorage` | `storage` | In-memory store: HashMap for nodes, adjacency list for edges |
| `StorageManager` | `storage` | File serialisation/deserialisation of graph state |
| `ConstraintValidator` | `constraint` | Validates inserts and updates against schema rules |
| `PropertyIndex` | `index` | Inverted index for O(1) property-value lookups |
| `NodeManager` | `engine` | Node CRUD + index maintenance |
| `EdgeManager` | `engine` | Edge CRUD with constraint checking |
| `GraphEngine` | `engine` | Facade wiring all subsystems; safe DROP and CLEAR operations |
| `TraversalEngine` | `traversal` | BFS, DFS, Shortest Path algorithms |
| `QueryParser` | `query` | Parses command strings into `Query` objects |
| `QueryExecutor` | `query` | Executes `Query` objects against `GraphEngine` |
| `Main` | (default) | Entry point; REPL loop |

---

## Project Structure

```
Graph Database Engine/
├── src/
│   ├── Main.java
│   ├── model/
│   │   ├── Node.java
│   │   ├── Edge.java
│   │   ├── NodeType.java
│   │   └── RelationshipType.java
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
│       └── QueryExecutor.java
├── out/               (compiled .class files — created by build)
├── compile.bat        (Windows compile script)
├── run.bat            (Windows compile-and-run script)
└── README.md
```

---

## Requirements

- **Java 8 or later** (uses only the standard library — no external dependencies)
- Windows command prompt for the `.bat` scripts, or any terminal capable of running `javac`/`java`

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

### Option 2: Manual commands

**Compile:**
```bat
javac -d out src\Main.java src\model\*.java src\schema\*.java src\storage\*.java src\constraint\*.java src\index\*.java src\engine\*.java src\traversal\*.java src\query\*.java
```

**Run:**
```bat
java -cp out Main
```

### Option 3: Run the built-in demo

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
FIND <nodeType> [WHERE <key>=<value>]
```
Returns all nodes of the given type, optionally filtered by a property value. Uses the property index for efficient lookup when `WHERE` is specified.

```
GDB> FIND User
GDB> FIND User WHERE age=25
GDB> FIND Company WHERE industry=Software
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

### Persistence

#### SAVE
```
SAVE [<filepath>]
```
Serialises the full graph state (schema + nodes + edges) to a file. Default filename: `graph_data.gdb`. Parent directories are created automatically.

```
GDB> SAVE
GDB> SAVE my_graph.gdb
GDB> SAVE backups\snapshot.gdb
```

#### LOAD
```
LOAD [<filepath>]
```
Deserialises graph state from file, replacing all current data. Default filename: `graph_data.gdb`.

```
GDB> LOAD
GDB> LOAD my_graph.gdb
```

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
GDB> NEIGHBORS u1
GDB> BFS u1 TYPE FRIENDS
GDB> SHORTEST PATH u1 TO u4 TYPE FRIENDS
GDB> DESCRIBE u1

GDB> COUNT NODES TYPE User
GDB> COUNT EDGES TYPE FRIENDS

GDB> SAVE social_network.gdb
```

### Reload and Continue

```
GDB> LOAD social_network.gdb
GDB> SHOW SCHEMA
GDB> SHOW GRAPH
GDB> UPDATE NODE u2 SET age=26
GDB> FIND User WHERE age=26
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
  - `~b` → `\` (backslash)
  - `~p` → `|` (pipe)
  - `~s` → `;` (semicolon)
  - `~e` → `=` (equals)
- **The tilde character `~` cannot appear in property values**

---

## Design Decisions

1. **Facade pattern for `GraphEngine`** — a single entry point wires all subsystems. `QueryExecutor` interacts only with `GraphEngine`, keeping coupling minimal.

2. **Referential integrity lives in `GraphEngine`** — `SchemaManager` has no access to `GraphStorage` to avoid circular dependencies. `GraphEngine` wrapper methods (`dropNodeType`, `dropRelationshipType`) check live data before delegating to `SchemaManager`.

3. **Property index is separate from storage** — `PropertyIndex` maintains an inverted map independent of `GraphStorage`, making index rebuilding on `LOAD` straightforward.

4. **Tilde escape in persistence** — a backslash escape scheme has ambiguous round-trip behaviour for values that contain literal escape sequences. Switching to tilde provides unambiguous, single-pass encoding.

5. **Iterative DFS** — implemented with an explicit stack rather than recursion to avoid `StackOverflowError` on deep graphs.

6. **BFS for shortest path** — the graph is unweighted; BFS guarantees the minimum hop-count path. A parent map tracks each node's predecessor for path reconstruction.

7. **Sentinel strings in COUNT queries** — the `nodeType` field carries `"NODES"`, `"EDGES"`, or `"ALL"` as sentinels, and `relType` carries the optional type filter, avoiding COUNT-specific fields in the `Query` model.

---

## Known Limitations

- **In-memory only** — the entire graph is held in RAM; very large graphs are constrained by available heap.
- **No transactions** — there is no rollback mechanism; a failed command sequence may leave partial state.
- **Single-property WHERE** — `FIND` supports only one `WHERE key=value` filter per query.
- **Unweighted shortest path** — `SHORTEST PATH` uses BFS (hop count). Edge weights stored as properties are not used by the algorithm.
- **Tilde reserved in values** — the `~` character is the escape prefix in `.gdb` files and cannot appear in property values.
- **Single-threaded** — there is no locking or thread-safety; the engine is designed for single-user interactive use.
- **Case-sensitive values** — node IDs, type names, and property values are case-sensitive; query keywords are case-insensitive.

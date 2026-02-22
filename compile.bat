@echo off
REM ─────────────────────────────────────────────────────────────────────
REM  compile.bat  —  Compiles the Graph Database Engine
REM  Run this from:  "Graph Database Engine\" directory
REM ─────────────────────────────────────────────────────────────────────

echo Cleaning previous build...
if exist out (
    rmdir /s /q out
)
mkdir out

echo Compiling source files...
javac -d out ^
    src\model\Node.java ^
    src\model\Edge.java ^
    src\model\NodeType.java ^
    src\model\RelationshipType.java ^
    src\schema\SchemaManager.java ^
    src\storage\GraphStorage.java ^
    src\index\PropertyIndex.java ^
    src\constraint\ConstraintValidator.java ^
    src\engine\NodeManager.java ^
    src\engine\EdgeManager.java ^
    src\engine\GraphEngine.java ^
    src\traversal\TraversalEngine.java ^
    src\query\Query.java ^
    src\query\QueryParser.java ^
    src\query\QueryExecutor.java ^
    src\storage\StorageManager.java ^
    src\Main.java

if %ERRORLEVEL% == 0 (
    echo.
    echo  Build SUCCESSFUL. Run the engine with:  run.bat
) else (
    echo.
    echo  Build FAILED. Check error messages above.
)

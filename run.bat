@echo off
REM ─────────────────────────────────────────────────────────────────────
REM  run.bat  —  Starts the Graph Database Engine interactive CLI
REM  Compile first with:  compile.bat
REM ─────────────────────────────────────────────────────────────────────

if not exist out\Main.class (
    echo  Engine not compiled. Running compile.bat first...
    call compile.bat
    if %ERRORLEVEL% NEQ 0 (
        echo  Compilation failed. Cannot run.
        exit /b 1
    )
)

echo.
java -cp out Main

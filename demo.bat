@echo off
REM ─────────────────────────────────────────────────────────────────────
REM  demo.bat  —  Runs the Graph Database Engine demo automatically
REM  Uses demo_script.txt as the input command file.
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

if not exist demo_script.txt (
    echo  demo_script.txt not found!
    exit /b 1
)

echo.
REM Strip REM comments and blank lines, then pipe into the engine
findstr /v /i /b "REM " demo_script.txt | findstr /r /v "^$" | java -cp out Main

#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────
#  run.sh  —  Starts the Graph Database Engine interactive CLI
#  Compile first with:  ./compile.sh
# ─────────────────────────────────────────────────────────────────────

if [ ! -f out/Main.class ]; then
    echo " Engine not compiled. Running compile.sh first..."
    bash "$(dirname "$0")/compile.sh"
    if [ $? -ne 0 ]; then
        echo " Compilation failed. Cannot run."
        exit 1
    fi
fi

echo ""
java -cp out Main

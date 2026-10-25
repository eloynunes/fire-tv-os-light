#!/usr/bin/env bash
# Testes locais: unit tests JVM + build release (+ lint com --lint). Log completo em arquivo; resumo curto no terminal.
set -uo pipefail
source "$(dirname "$0")/env.sh"
cd "$ROOT"
TASKS=(testDebugUnitTest assembleRelease)
[[ "${1:-}" == "--lint" ]] && TASKS+=(lintDebug)
LOG="$RESULTS/local-$TS.log"
START=$(date +%s)
./gradlew --console=plain "${TASKS[@]}" >"$LOG" 2>&1
RC=$?
DUR=$(( $(date +%s) - START ))
python3 -I "$ROOT/scripts/summarize.py" local "$LOG" "$RC" "$DUR" "$RESULTS/history.jsonl" "$ROOT"
exit $RC

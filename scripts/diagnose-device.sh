#!/usr/bin/env bash
# Coleta diagnóstico somente-leitura do Fire Stick. Dados brutos em testing/results/device/<ts>/, resumo curto no terminal.
set -uo pipefail
source "$(dirname "$0")/env.sh"
OUT="$RESULTS/device/$TS"; mkdir -p "$OUT"
adbd shell getprop > "$OUT/getprop.txt"
adbd shell cat /proc/cpuinfo > "$OUT/cpuinfo.txt"
adbd shell cat /proc/meminfo > "$OUT/meminfo.txt"
adbd shell 'for c in /sys/devices/system/cpu/cpu[0-9]*; do echo "$c $(cat $c/cpufreq/cpuinfo_max_freq 2>/dev/null) $(cat $c/cpufreq/scaling_cur_freq 2>/dev/null)"; done' > "$OUT/cpufreq.txt"
adbd shell df -h > "$OUT/df.txt"
adbd shell wm size > "$OUT/wm.txt"; adbd shell wm density >> "$OUT/wm.txt"
adbd shell pm list packages -f > "$OUT/packages.txt"
adbd shell pm list packages -3 > "$OUT/packages3.txt"
adbd shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER > "$OUT/leanback.txt" 2>&1
adbd shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME > "$OUT/home.txt" 2>&1
adbd shell dumpsys meminfo > "$OUT/dumpsys_meminfo.txt"
adbd shell dumpsys activity processes > "$OUT/processes.txt"
adbd shell top -b -n 1 -m 15 > "$OUT/top.txt" 2>&1
adbd shell dumpsys SurfaceFlinger > "$OUT/surfaceflinger.txt" 2>&1
adbd shell dumpsys diskstats > "$OUT/diskstats.txt" 2>&1
python3 -I "$ROOT/scripts/device_summary.py" "$OUT" | tee "$OUT/summary.md"
cp "$OUT/summary.md" "$RESULTS/device/latest-summary.md"

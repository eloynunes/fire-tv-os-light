#!/usr/bin/env bash
# Compara fluidez de navegação (mesma sequência de teclas) entre a Home da Amazon e o TV Light.
source "$(dirname "$0")/env.sh"
OUT="$RESULTS/smoothness/$TS"; mkdir -p "$OUT"
SEQ="DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT DPAD_DOWN DPAD_RIGHT DPAD_RIGHT DPAD_DOWN DPAD_LEFT DPAD_LEFT DPAD_UP DPAD_UP DPAD_LEFT"
run() { # $1 nome  $2 pacote  $3 comando para abrir
  eval "$3"; sleep 3
  adbd shell dumpsys gfxinfo "$2" reset >/dev/null
  for k in $SEQ; do adbd shell input keyevent KEYCODE_$k; sleep 0.5; done
  adbd shell dumpsys gfxinfo "$2" > "$OUT/$1.txt"
  printf "%-10s %s | %s\n" "$1" "$(grep -E 'Total frames|Janky' "$OUT/$1.txt" | tr -s ' ' | tr '\n' ' ')" "$(grep -E '(50th|90th|95th) percentile' "$OUT/$1.txt" | sed 's/ percentile//' | tr '\n' ' ')"
}
run amazon com.amazon.tv.launcher "adbd shell input keyevent KEYCODE_HOME"
run tvlight "$PKG" "adbd shell am start -n $PKG/.MainActivity >/dev/null"
adbd shell input keyevent KEYCODE_HOME

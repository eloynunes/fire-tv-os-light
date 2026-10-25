#!/usr/bin/env bash
# Testes reais no Fire Stick: instala, mede abertura/memória, navega com o controle, verifica crash e persistência.
set -uo pipefail
source "$(dirname "$0")/env.sh"
OUT="$RESULTS/device-test/$TS"; mkdir -p "$OUT"
"$ROOT/scripts/install.sh" > "$OUT/install.txt" 2>&1 || { cat "$OUT/install.txt"; exit 1; }
adbd logcat -c
ACT="$PKG/.MainActivity"
key() { adbd shell input keyevent "$@"; sleep 0.6; }
dump() { adbd shell uiautomator dump /sdcard/tvl.xml >/dev/null 2>&1; adbd shell cat /sdcard/tvl.xml > "$OUT/$1.xml"; }
# 3 aberturas a frio
for i in 1 2 3; do
  adbd shell am force-stop "$PKG"; sleep 1
  adbd shell am start -W -n "$ACT" > "$OUT/start-cold-$i.txt"
done
sleep 2
# abertura a quente (volta para Home Amazon e retorna)
key KEYCODE_HOME; adbd shell am start -W -n "$ACT" > "$OUT/start-warm.txt"; sleep 2
adbd shell dumpsys meminfo "$PKG" > "$OUT/meminfo.txt"
dump before
# navegação: direita x2, baixo, esquerda, cima (não abre apps)
for k in DPAD_RIGHT DPAD_RIGHT DPAD_DOWN DPAD_LEFT DPAD_UP; do key KEYCODE_$k; done
adbd shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo.txt"
# persistência (estado lido do log do app, não da tela): alterna favorito, reinicia, confere, desfaz.
state() { sleep 1; adbd logcat -d -s TVLight:I | grep "state fav=" | tail -1 | sed 's/.*state //' > "$OUT/$1.txt"; }
restart() { adbd shell am force-stop "$PKG"; adbd shell am start -W -n "$ACT" >/dev/null; sleep 2; }
restart; state s0
key KEYCODE_DPAD_DOWN; key KEYCODE_MENU; key KEYCODE_DPAD_CENTER; state s1
restart; state s2
# foco inicial = 1º favorito; o favorito novo é o último da fileira.
N=$(sed -E 's/^fav=([0-9]+).*/\1/' "$OUT/s2.txt")
for ((i=1; i<N; i++)); do key KEYCODE_DPAD_RIGHT; done
key KEYCODE_MENU; key KEYCODE_DPAD_CENTER; state s3
adbd shell screencap -p /sdcard/tvl.png && adbd pull /sdcard/tvl.png "$OUT/screen.png" >/dev/null
adbd shell rm -f /sdcard/tvl.png /sdcard/tvl.xml
adbd logcat -d -v brief > "$OUT/logcat.txt"
python3 -I "$ROOT/scripts/device_test_summary.py" "$OUT" "$RESULTS/history.jsonl"
RC=$?
echo "  dados: $OUT"
exit $RC

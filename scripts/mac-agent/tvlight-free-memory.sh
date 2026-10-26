#!/bin/bash
# Legacy utility: not validated for unattended use or playback safety.
if [ "${TVLIGHT_ENABLE_LEGACY_TOOLS:-}" != "1" ]; then
  echo "Disabled legacy utility. Read SECURITY.md before explicitly enabling TVLIGHT_ENABLE_LEGACY_TOOLS=1." >&2
  exit 2
fi
# Roda via launchd a cada 2 min. Uma vez por boot do Fire TV (90 s após ligar), encerra Prime Video/Netflix
# que a Amazon abre sozinha em segundo plano. Não mexe no app que estiver na tela. Silencioso se a TV não estiver acessível.
ADB=/opt/homebrew/bin/adb
HOST=192.0.2.1; DEV="$HOST:5555"
DIR="$HOME/Library/Application Support/TVLight"; STATE="$DIR/last_boot"; LOG="$HOME/Library/Logs/tvlight-free-memory.log"
mkdir -p "$DIR"
nc -z -G 2 "$HOST" 5555 2>/dev/null || exit 0
"$ADB" connect "$DEV" >/dev/null 2>&1
[ "$("$ADB" -s "$DEV" get-state 2>/dev/null)" = device ] || exit 0
a() { "$ADB" -s "$DEV" shell "$@" 2>/dev/null | tr -d '\r'; }
boot=$(a cat /proc/sys/kernel/random/boot_id); [ -n "$boot" ] || exit 0
[ "$boot" = "$(cat "$STATE" 2>/dev/null)" ] && exit 0
up=$(a cut -d. -f1 /proc/uptime); [ "${up:-0}" -lt 90 ] && exit 0
before=$(a grep MemAvailable /proc/meminfo | awk '{print int($2/1024)}')
top=$(a dumpsys window windows | grep mCurrentFocus)
for p in com.amazon.firebat com.netflix.ninja; do
  [[ "$top" == *"$p"* ]] || a am force-stop "$p"
done
sleep 3
after=$(a grep MemAvailable /proc/meminfo | awk '{print int($2/1024)}')
echo "$boot" > "$STATE"
echo "$(date '+%F %T') boot=${boot:0:8} RAM ${before}MB -> ${after}MB" >> "$LOG"

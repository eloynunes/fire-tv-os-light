#!/bin/bash
# Legacy utility: not validated for unattended use or playback safety.
if [ "${TVLIGHT_ENABLE_LEGACY_TOOLS:-}" != "1" ]; then
  echo "Disabled legacy utility. Read SECURITY.md before explicitly enabling TVLIGHT_ENABLE_LEGACY_TOOLS=1." >&2
  exit 2
fi
# Vigia contínuo (launchd KeepAlive): quando o Fire TV abre a Home da Amazon (botão Home),
# o Mac manda abrir o TV Light pelo ADB — comandos do ADB não sofrem a espera de ~5 s do Android.
ADB=/opt/homebrew/bin/adb
HOST="${TVLIGHT_DEVICE_HOST:?Set TVLIGHT_DEVICE_HOST explicitly; no default device is selected}"; DEV="$HOST:5555"; ACT=com.firetvoslight.launcher/.MainActivity
LOG="$HOME/Library/Logs/tvlight-home-watch.log"
while true; do
  if nc -z -G 2 "$HOST" 5555 2>/dev/null; then
    "$ADB" connect "$DEV" >/dev/null 2>&1
    if [ "$("$ADB" -s "$DEV" get-state 2>/dev/null)" = device ]; then
      echo "$(date '+%F %T') conectado" >> "$LOG"
      "$ADB" -s "$DEV" logcat -T 1 -v brief ActivityManager:I '*:S' 2>/dev/null | while IFS= read -r line; do
        case "$line" in
          *"START u0 {act=android.intent.action.MAIN cat=[android.intent.category.HOME]"*)
            "$ADB" -s "$DEV" shell am start -n "$ACT" >/dev/null 2>&1 &
            echo "$(date '+%F %T') home -> TV Light" >> "$LOG" ;;
        esac
      done
    fi
  fi
  sleep 10
done

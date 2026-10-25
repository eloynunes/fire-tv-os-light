#!/bin/bash
# Instala (ou --uninstall) o agente do Mac que limpa a memória do Fire TV a cada boot.
LABEL=com.firetvoslight.freememory; LABEL2=com.firetvoslight.homewatch
DIR="$HOME/Library/Application Support/TVLight"; PLIST="$HOME/Library/LaunchAgents/$LABEL.plist"; PLIST2="$HOME/Library/LaunchAgents/$LABEL2.plist"
launchctl bootout "gui/$(id -u)/$LABEL" 2>/dev/null; launchctl bootout "gui/$(id -u)/$LABEL2" 2>/dev/null
if [ "$1" = "--uninstall" ]; then rm -f "$PLIST" "$PLIST2" "$DIR"/tvlight-*.sh "$DIR/last_boot"; echo "Agentes removidos."; exit 0; fi
mkdir -p "$DIR" "$HOME/Library/LaunchAgents"
cp "$(dirname "$0")"/tvlight-*.sh "$DIR/"; chmod +x "$DIR"/tvlight-*.sh
cat > "$PLIST" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>Label</key><string>$LABEL</string>
  <key>ProgramArguments</key><array><string>/bin/bash</string><string>$DIR/tvlight-free-memory.sh</string></array>
  <key>StartInterval</key><integer>120</integer>
  <key>RunAtLoad</key><true/>
  <key>LowPriorityIO</key><true/>
  <key>Nice</key><integer>10</integer>
</dict></plist>
PL
cat > "$PLIST2" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>Label</key><string>$LABEL2</string>
  <key>ProgramArguments</key><array><string>/bin/bash</string><string>$DIR/tvlight-home-watch.sh</string></array>
  <key>KeepAlive</key><true/>
  <key>RunAtLoad</key><true/>
</dict></plist>
PL
launchctl bootstrap "gui/$(id -u)" "$PLIST" && launchctl bootstrap "gui/$(id -u)" "$PLIST2" && echo "Agentes instalados: limpeza a cada boot + vigia do Home."

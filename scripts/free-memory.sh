#!/usr/bin/env bash
# Legacy utility: not validated for unattended use or playback safety.
if [ "${TVLIGHT_ENABLE_LEGACY_TOOLS:-}" != "1" ]; then
  echo "Disabled legacy utility. Read SECURITY.md before explicitly enabling TVLIGHT_ENABLE_LEGACY_TOOLS=1." >&2
  exit 2
fi
# Libera RAM encerrando apps que a Amazon mantém vivos em segundo plano (vale até o próximo reinício).
# Não encerra o app que estiver na tela.
source "$(dirname "$0")/env.sh"
before=$(adbd shell grep MemAvailable /proc/meminfo | awk '{print int($2/1024)}')
top=$(adbd shell dumpsys window windows | grep mCurrentFocus)
for p in com.amazon.firebat com.netflix.ninja; do
  [[ "$top" == *"$p"* ]] && { echo "pulando $p (em uso)"; continue; }
  adbd shell am force-stop "$p"
done
sleep 3
after=$(adbd shell grep MemAvailable /proc/meminfo | awk '{print int($2/1024)}')
echo "RAM disponível: ${before} MB -> ${after} MB"

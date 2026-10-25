#!/usr/bin/env bash
# Instala (ou --uninstall) o launcher. Desinstalar restaura 100% a experiência original.
set -uo pipefail
source "$(dirname "$0")/env.sh"
if [[ "${1:-}" == "--uninstall" ]]; then adbd uninstall "$PKG"; exit $?; fi
[[ -f "$APK" ]] || (cd "$ROOT" && ./gradlew -q assembleRelease) || exit 1
# Lembra se a trava do Home estava ativa: reinstalar desliga o serviço de acessibilidade.
WAS_ON=$(adbd shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r' | grep -c "$PKG")
adbd install -r "$APK" | tail -1
if [ "${WAS_ON:-0}" -ge 1 ]; then
  "$(dirname "$0")/home-lock.sh" on >/dev/null && echo "trava do Home religada"
fi

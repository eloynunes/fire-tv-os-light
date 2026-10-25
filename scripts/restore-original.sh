#!/usr/bin/env bash
# Volta o Fire TV ao estado original: desliga a trava da Home, restaura animações (1.0) e desinstala o TV Light.
source "$(dirname "$0")/env.sh"
"$(dirname "$0")/home-lock.sh" off
"$(dirname "$0")/background-limits.sh" revert >/dev/null
for k in window_animation_scale transition_animation_scale; do adbd shell settings put global $k 1.0; done
adbd shell settings delete global animator_duration_scale >/dev/null
[[ "${1:-}" == "--keep-app" ]] || adbd uninstall "$PKG"
echo "Restaurado."

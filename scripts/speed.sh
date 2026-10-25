#!/usr/bin/env bash
# Acelera a sensação de navegação reduzindo as animações do sistema (via ADB, sem root).
#
#   ./scripts/speed.sh status          # mostra as escalas atuais
#   ./scripts/speed.sh fast [escala]   # aplica escala (padrão 0 = instantâneo)
#   ./scripts/speed.sh restore         # volta às escalas originais gravadas
#
# Grava os valores originais UMA vez em testing/results/speed-backup.txt.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/env.sh

BACKUP="$RESULTS/speed-backup.txt"
CMD="${1:-status}"
KEYS=(window_animation_scale transition_animation_scale animator_duration_scale)

getv() { adbd shell settings get global "$1" </dev/null | tr -d '\r'; }
putv() { adbd shell settings put global "$1" "$2" </dev/null; }

show() { for k in "${KEYS[@]}"; do printf '%-28s = %s\n' "$k" "$(getv "$k")"; done; }

case "$CMD" in
  status) show ;;
  fast)
    scale="${2:-0}"
    if [[ ! -s "$BACKUP" ]]; then
      mkdir -p "$(dirname "$BACKUP")"
      : > "$BACKUP"; for k in "${KEYS[@]}"; do echo "$k $(getv "$k")" >> "$BACKUP"; done
      echo "Originais gravados em $BACKUP"
    fi
    for k in "${KEYS[@]}"; do putv "$k" "$scale"; done
    echo "Animações em $scale."; show ;;
  restore)
    if [[ ! -s "$BACKUP" ]]; then echo "Sem backup — nada para restaurar."; exit 0; fi
    while read -r k v; do [[ -z "$k" ]] && continue; putv "$k" "$v"; echo "restaurado: $k = $v"; done < "$BACKUP"
    echo "Escalas originais restauradas." ;;
  *)
    echo "Uso: $0 {status|fast [escala]|restore}"; exit 1 ;;
esac

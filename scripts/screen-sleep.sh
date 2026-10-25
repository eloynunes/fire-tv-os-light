#!/usr/bin/env bash
# Ajusta protetor de tela e suspensão do Fire TV via ADB (sem root), reversível.
#
#   ./scripts/screen-sleep.sh status            # mostra os valores atuais
#   ./scripts/screen-sleep.sh relax [minutos]   # desliga o protetor e adia a suspensão
#                                                 (padrão 30 min; "never" = não suspende)
#   ./scripts/screen-sleep.sh restore           # volta aos valores originais gravados
#
# "relax" grava os valores originais UMA vez (testing/results/screen-sleep-backup.txt);
# "restore" os reaplica. Desligar o protetor evita a "tela de stand by" da Amazon.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/env.sh

BACKUP="$RESULTS/screen-sleep-backup.txt"
CMD="${1:-status}"

getv() { adbd shell settings get "$1" "$2" </dev/null | tr -d '\r'; }
putv() { adbd shell settings put "$1" "$2" "$3" </dev/null; }

show() {
  printf '%-22s screensaver_enabled   = %s\n' "secure"  "$(getv secure screensaver_enabled)"
  printf '%-22s screen_off_timeout    = %s ms\n' "system" "$(getv system screen_off_timeout)"
  printf '%-22s sleep_timeout         = %s ms\n' "secure"  "$(getv secure sleep_timeout)"
}

case "$CMD" in
  status) show ;;
  relax)
    mins="${2:-30}"
    # Grava backup só na primeira vez, pra não sobrescrever os originais.
    if [[ ! -s "$BACKUP" ]]; then
      mkdir -p "$(dirname "$BACKUP")"
      {
        echo "secure screensaver_enabled $(getv secure screensaver_enabled)"
        echo "system screen_off_timeout $(getv system screen_off_timeout)"
        echo "secure sleep_timeout $(getv secure sleep_timeout)"
      } > "$BACKUP"
      echo "Originais gravados em $BACKUP"
    fi
    if [[ "$mins" == "never" ]]; then ms=2147483647; else ms=$(( mins * 60000 )); fi
    putv secure screensaver_enabled 0        # some com a tela de descanso da Amazon
    putv system screen_off_timeout "$ms"     # tempo até apagar a tela
    putv secure sleep_timeout "$ms"          # tempo até suspender (standby)
    echo "Protetor de tela desligado; suspensão em ${mins} min."
    show ;;
  restore)
    if [[ ! -s "$BACKUP" ]]; then echo "Sem backup — nada para restaurar."; exit 0; fi
    while read -r ns key val; do
      [[ -z "$ns" ]] && continue
      putv "$ns" "$key" "$val"; echo "restaurado: $ns $key = $val"
    done < "$BACKUP"
    echo "Valores originais restaurados." ;;
  *)
    echo "Uso: $0 {status|relax [minutos|never]|restore}"; exit 1 ;;
esac

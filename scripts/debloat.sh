#!/usr/bin/env bash
# Debloat reversível do Fire TV via ADB (sem root, sem desinstalar).
# Desativa pacotes de telemetria/anúncio/apps não usados com "pm disable-user".
#
#   ./scripts/debloat.sh status            # mostra o estado de cada pacote da lista
#   ./scripts/debloat.sh on                # desativa a seção [safe]
#   ./scripts/debloat.sh on --aggressive   # desativa [safe] + [aggressive]
#   ./scripts/debloat.sh off               # reativa tudo que ESTE script desativou
#
# "off" só reativa o que nós desativamos (gravado em testing/results/debloat-applied.txt),
# nunca mexe em pacotes que já vinham desativados de fábrica.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/env.sh

LIST="$ROOT/scripts/debloat-sheldon.txt"
APPLIED="$RESULTS/debloat-applied.txt"
CMD="${1:-status}"
AGGRESSIVE=0
[[ "${2:-}" == "--aggressive" ]] && AGGRESSIVE=1

# Lê os pacotes da lista respeitando as seções [safe]/[aggressive] e a flag.
packages() {
  local section="" line
  while IFS= read -r line; do
    line="${line%%#*}"; line="${line// /}"; line="${line//$'\t'/}"
    [[ -z "$line" ]] && continue
    case "$line" in
      "[safe]") section="safe"; continue ;;
      "[aggressive]") section="aggressive"; continue ;;
    esac
    if [[ "$section" == "safe" ]] || { [[ "$section" == "aggressive" ]] && [[ $AGGRESSIVE -eq 1 ]]; }; then
      echo "$line"
    fi
  done < "$LIST"
}

# Busca as listas do aparelho UMA vez (</dev/null evita que o adb consuma o loop).
ALL="$(adbd shell pm list packages </dev/null)"
DIS="$(adbd shell pm list packages -d </dev/null)"

state_of() {
  grep -q "^package:$1$" <<<"$ALL" || { echo "absent"; return; }
  grep -q "^package:$1$" <<<"$DIS" && echo "disabled" || echo "enabled"
}

case "$CMD" in
  status)
    printf '%-40s %s\n' "PACOTE" "ESTADO"
    while read -r p; do printf '%-40s %s\n' "$p" "$(state_of "$p")"; done < <(packages)
    ;;
  on)
    # Acumula no registro (dedup): rodar "on" e depois "on --aggressive" soma, não zera.
    mkdir -p "$(dirname "$APPLIED")"; touch "$APPLIED"; n=0
    while read -r p; do
      case "$(state_of "$p")" in
        enabled)
          if adbd shell pm disable-user --user 0 "$p" </dev/null >/dev/null 2>&1; then
            grep -qx "$p" "$APPLIED" || echo "$p" >> "$APPLIED"
            echo "desativado: $p"; n=$((n+1))
          else
            echo "FALHOU (protegido?): $p"
          fi ;;
        *) echo "ignorado ($(state_of "$p")): $p" ;;
      esac
    done < <(packages)
    echo "---"; echo "$n pacote(s) desativado(s) nesta execução. Registro: $APPLIED"
    ;;
  off)
    if [[ ! -s "$APPLIED" ]]; then echo "Nada registrado como desativado por este script."; exit 0; fi
    while read -r p; do
      [[ -z "$p" ]] && continue
      if adbd shell pm enable "$p" </dev/null >/dev/null 2>&1; then echo "reativado: $p"; else echo "FALHOU: $p"; fi
    done < "$APPLIED"
    : > "$APPLIED"
    echo "---"; echo "Restauração concluída."
    ;;
  *)
    echo "Uso: $0 {status|on [--aggressive]|off}"; exit 1 ;;
esac

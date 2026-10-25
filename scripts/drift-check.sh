#!/usr/bin/env bash
# Verifica "drift": uma atualização do Fire OS pode reativar pacotes que você
# desativou. Compara o registro (debloat-applied.txt) com o estado real e avisa o
# que voltou a ligar. Com --fix, desativa de novo.
#
#   ./scripts/drift-check.sh        # só relata o que voltou a ligar
#   ./scripts/drift-check.sh --fix  # relata e reaplica o debloat nos que voltaram
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/env.sh

APPLIED="$RESULTS/debloat-applied.txt"
FIX=0
[[ "${1:-}" == "--fix" ]] && FIX=1

if [[ ! -s "$APPLIED" ]]; then
  echo "Nada foi desativado por este projeto ainda (rode ./scripts/debloat.sh on)."
  exit 0
fi

ALL="$(adbd shell pm list packages </dev/null)"
DIS="$(adbd shell pm list packages -d </dev/null)"

drift=0
while read -r p; do
  [[ -z "$p" ]] && continue
  if ! grep -q "^package:$p$" <<<"$ALL"; then
    echo "sumiu (removido por update?): $p"; drift=$((drift+1)); continue
  fi
  if ! grep -q "^package:$p$" <<<"$DIS"; then
    drift=$((drift+1))
    if [[ $FIX -eq 1 ]]; then
      if adbd shell pm disable-user --user 0 "$p" </dev/null >/dev/null 2>&1; then
        echo "reativou -> desativado de novo: $p"
      else
        echo "reativou -> FALHOU ao desativar: $p"
      fi
    else
      echo "voltou a LIGAR: $p"
    fi
  fi
done < "$APPLIED"

echo "---"
if [[ $drift -eq 0 ]]; then
  echo "Sem drift: tudo que você desativou continua desativado."
elif [[ $FIX -eq 1 ]]; then
  echo "$drift pacote(s) tinham voltado; reaplicado."
else
  echo "$drift pacote(s) voltaram a ligar. Rode com --fix para corrigir."
fi

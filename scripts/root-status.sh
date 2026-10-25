#!/usr/bin/env bash
# Checa, via ADB, se o aparelho está rooteado (su funcional) — sem rootear nada.
# Útil depois de rodar o GhostLock para confirmar que o Magisk está ativo.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/env.sh

echo "Aparelho: $(adbd shell getprop ro.product.device </dev/null | tr -d '\r') / $(adbd shell getprop ro.build.id </dev/null | tr -d '\r')"

# "su -c id" só retorna uid=0 se houver root concedido ao shell.
OUT="$(adbd shell 'su -c id 2>/dev/null || true' </dev/null | tr -d '\r')"
if echo "$OUT" | grep -q 'uid=0'; then
  echo "ROOT: disponível ✅  ($OUT)"
  echo "O TV Light vai encerrar o Prime Video e tentar fixar o Home de verdade."
else
  echo "ROOT: não detectado ❌"
  echo "Se você já rodou o GhostLock, abra o Magisk e confirme que o shell/app tem permissão."
fi

# TV Light — launcher leve para Fire TV
Fluxo: `./scripts/test-local.sh [--lint]` → `./scripts/connect.sh` → `./scripts/diagnose-device.sh` → `./scripts/test-device.sh`.
Instalar: `./scripts/install.sh` · Remover (restaura tudo): `./scripts/install.sh --uninstall`.
IP diferente: `DEVICE=192.168.1.X:5555 ./scripts/connect.sh`.
Controle: setas navegam · OK abre · ☰ ou segurar OK = opções · em "Mover", setas reposicionam e OK conclui.
Docs: docs/ (decisões, compatibilidade, progresso, erros). Resultados: testing/results/ (fora do git).

Botão Home → TV Light: `./scripts/home-lock.sh on` (desligar: `off`). Voltar ao original: `./scripts/restore-original.sh`.
Ajustes → "Abrir tela inicial da Amazon" mostra a Home original sem redirecionar.
Memória: o launcher libera a RAM sozinho (Ajustes → Limpeza automática/Modo agressivo/Liberar agora). Prime Video só sai por completo via `./scripts/free-memory.sh` (Mac no cabo) ou reinício.

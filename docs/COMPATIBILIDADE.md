# Compatibilidade Fire OS 7.7.1.7 (PS7717/5741)
Fontes: AFTVnews, XDA, Tom's Guide (relatos da comunidade; nada oficial da Amazon).
- Amazon bloqueia launchers personalizados como Home desde PS7273 (2020). Relatos indicam builds ≥ PS7706 corrigidos contra os truques de "hijack" (acessibilidade/logcat). PS7717 é posterior → **substituir a Home: tratado como inviável** sem root (fora de escopo).
- `pm disable-user com.amazon.tv.launcher`: não usar (arriscado; pode deixar o aparelho sem Home).
- O que funciona: app sideloaded com `LEANBACK_LAUNCHER` aparece em "Seus apps" e pode ser fixado na fileira da Home da Amazon → 1 clique para abrir.
- Alternativa sem risco: controle remoto de terceiros com botão programável + app que abra o launcher.
- O manifesto declara HOME/DEFAULT (inofensivo). A verificar no aparelho: `cmd package resolve-activity ... HOME` (em `diagnose-device.sh`).

## Testado no aparelho (24/10/2026)
- `cmd package set-home-activity` → "Success", mas Home continua Amazon (Fire OS ignora). Revertido.
- `pm disable-user com.amazon.tv.launcher` (pacote e componente) → `SecurityException: Cannot disable a protected package`. Nada mudou.
- Filtro de teclas por acessibilidade: NENHUMA tecla do controle chega ao serviço (nem setas, nem Home). Teclas via `adb input` também não passam.
- Serviço de acessibilidade detecta a Home da Amazon e abre o TV Light: funciona, mas o Android segura a abertura ~3–5 s após Home (atraso padrão de "app switch"; Amazon NÃO aborta: log `shouldAbortActivityStart() not aborted`).
- Solução adotada: "cortina" (overlay de acessibilidade, sem permissão extra) com a última imagem do TV Light em ~0,3 s; teclas ficam bloqueadas até o TV Light real assumir (~3 s); limite de segurança 8 s.

## Tempos do botão Home (medidos, 24/10/2026)
- Home Amazon aparece 0,7–1,4 s após o Home; o serviço só é avisado ~0,1–1 s depois → cortina em ~1–1,8 s.
- TV Light real: ~4,4–5 s após o Home. Limite do sistema (regra de app switch ~5 s; AlarmManager min_futurity=5 s). Abrir via alarme ganhou só ~0,5 s.
- TYPE_WINDOWS_CHANGED não antecipa a detecção → descartado (exigia ler conteúdo das janelas = mais custo).
- Abrir Netflix a partir do TV Light: 0,2–0,3 s (quente), ~1,1 s (frio). Lentidão restante é carregamento interno do Netflix ou OK apertado durante a cortina (teclas bloqueadas; agora há aviso "Um instante…").

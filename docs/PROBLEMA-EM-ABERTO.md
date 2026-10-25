# Problema em aberto — para revisão de um colaborador

## Resumo em uma frase
No **Fire TV Stick HD 3ª geração (AFTSS), Fire OS 7.7.1.7 / PS7717/5741**, não foi
possível fazer o launcher (TV Light) substituir de verdade a tela inicial (Home) da
Amazon **sem root**. O botão Home sempre abre a Home da Amazon primeiro; só depois o
TV Light assume.

## Comportamento atual (contornado, não resolvido)
Ao apertar **Home**:
1. A Home da Amazon aparece em ~0,7–1,4 s.
2. Um serviço de acessibilidade (`HomeRedirectService`) detecta isso e cobre a tela
   com uma "cortina" que é o próprio TV Light, interativa, em ~0,3–0,8 s.
3. O TV Light "real" assume em ~3 s (via alarme imediato do AlarmManager).

Funciona, mas há um **flash da Home da Amazon** antes da cortina, e um pequeno atraso.
O ideal era o Home abrir o TV Light instantaneamente, sem a Amazon aparecer.

## O que foi testado e por que falhou (tudo por ADB, sem root)
| Tentativa | Resultado |
|---|---|
| `cmd package set-home-activity <nosso>/.MainActivity` | "Success", mas o Fire OS ignora e mantém a Amazon como Home |
| `pm disable-user com.amazon.tv.launcher` (pacote e componente) | `SecurityException: Cannot disable a protected package` |
| Capturar o botão Home via `flagRequestFilterKeyEvents` (acessibilidade) | O Fire OS **não entrega** nenhuma tecla do controle ao serviço (nem Home, nem setas) |
| Abrir o launcher logo após o Home (startActivity / alarme) | Android segura a abertura ~5 s (regra de "app switch"); alarme ganha ~0,5 s |
| Detectar a Home mais cedo (`TYPE_WINDOWS_CHANGED`) | Não antecipou; exigia ler conteúdo das janelas (mais custo) |

Observação relevante dos logs: `AmazonProfileService.shouldAbortActivityStart() ...
not aborted` — a Amazon **não** está bloqueando nossa abertura; o atraso é a regra
padrão de app-switch do Android + `AlarmManager min_futurity=5s`.

## Problema secundário (relacionado a RAM)
O **Prime Video (com.amazon.firebat, ~185 MB)** fica fixado pela Home da Amazon
(binding em `PlaybackApiService`) no nível **adj=700 (previous app)**. Nosso launcher
usa `ActivityManager.killBackgroundProcesses` (permissão `KILL_BACKGROUND_PROCESSES`),
que só alcança processos em cache (**adj ≥ 900**), então **não consegue encerrar o Prime
enquanto ele é o app mais recente**. Só sai com `am force-stop` (shell/ADB) ou root.
Netflix e os demais são encerrados normalmente.

## Pergunta objetiva para o colaborador
Existe um método **sem root e sem comprometer a segurança do aparelho** (nada de ADB
embutido no app, que daria poder total a ele) para, no Fire OS 7.7.1.7 / PS7717:
1. Fazer o botão Home abrir diretamente um launcher de terceiros, **sem** a Home da
   Amazon aparecer antes? e/ou
2. Interceptar o botão Home a partir de um app/serviço comum?

Métodos antigos (Launcher Manager, Launcher Hijack, FTVLaunchX) dependiam de ADB
interno ou de versões < PS7706 e **não funcionam** nesta build.

## Onde olhar no código
- `app/src/main/java/com/firetvoslight/launcher/HomeRedirectService.kt` — serviço de
  acessibilidade + cortina.
- `app/src/main/res/xml/home_redirect.xml` — configuração do serviço.
- `scripts/home-lock.sh` — liga/desliga a trava (`on`/`off`).
- `docs/COMPATIBILIDADE.md` e `docs/ERROS_CONHECIDOS.md` — detalhes dos testes.

## Como reproduzir
1. `./scripts/connect.sh` (Mac no cabo; UniFi isola Wi-Fi) e `./scripts/install.sh`.
2. `./scripts/home-lock.sh on` (aceitar o serviço na TV).
3. Abrir um app, apertar Home e cronometrar até o TV Light assumir.

## Restrições do projeto (não negociáveis)
Sem root, sem exploits, sem modificar firmware/partições, sempre reversível
(desinstalar restaura a experiência original da Amazon).

# Decisões
- Kotlin + Views do framework, **sem AndroidX**: APK release 45 KB, abertura mais leve.
- Layout montado em código (sem inflar XML); GridLayout + HorizontalScrollView; foco nativo do D-pad.
- Banners 16:9 (`loadBanner`) com fallback para ícone; carregados numa thread única com cache; liberados em `onTrimMemory(MODERATE)`.
- Persistência: SharedPreferences com listas de chaves `pacote/atividade` (favoritos, ordem, ocultos).
- Lógica pura em `core/LauncherState.kt` → testada na JVM (sem Robolectric/emulador).
- Release minificado (R8) assinado com chave debug local: instala/desinstala via ADB sem keystore.
- Histórico de testes em `testing/results/history.jsonl` (SQLite seria excesso).
- Toolchain: JDK 17 (brew openjdk@17), Gradle 8.11.1 (wrapper), AGP 8.7.3, Kotlin 2.0.21, SDK 34 em /opt/homebrew/share/android-commandlinetools.
- Voltar não sai do launcher; ☰ ou OK longo = opções (favoritar, mover, ocultar, info).

## Interface de Ajustes
- Tela cheia estilo Apple TV (SettingsActivity + SettingRow): Memória, Apps e armazenamento (desinstalar via diálogo do sistema), Tela inicial (status + reativar), Config Fire, Sobre.
- Desinstalar usa ACTION_DELETE: a própria Amazon confirma; reversível pela loja.
- Serviço de acessibilidade é desligado a cada reinstalação do APK → install.sh religa sozinho; a flag homeLockDesired permite reativar pelo próprio controle (Ajustes → Tela inicial).

## Cortina interativa (botão Home)
- `LauncherRepo` (estado/apps/ícones no processo) + `LauncherScreen` (UI reutilizável) compartilhados entre a Activity e a cortina do serviço de acessibilidade. Ícones (bitmaps) não são duplicados.
- Ao apertar Home: a cortina (overlay de acessibilidade = a própria tela do TV Light, viva) aparece em ~0,3–0,8 s e o controle já navega. Escolher um app abre direto (sem esperar o TV Light "real"). Se ninguém tocar, a Activity assume em ~3 s, no mesmo item focado, sem piscar.
- Abertura via alarme imediato (isento da espera de app-switch do Android). Voltar/☰ inertes na cortina; menus ficam para a Activity.
- Limite: a Home da Amazon ainda renderiza atrás por ~0,3 s (não dá para evitar sem root). Nada de root, exploits ou acesso de depuração embutido.

## Limpeza de memória
- `MemoryPolicy` (pura, testada) decide o que encerrar; `MemoryManager` executa via killBackgroundProcesses.
- Padrão: só Prime/Netflix (hogs conhecidos). Agressivo opcional: todos de 2º plano exceto Spotify/áudio e sistema.
- Nunca encerra: app em foreground, o launcher, apps de áudio, pacotes de sistema.
- Sem Mac, sem root, sem ADB embutido. Desinstalar remove tudo.

## Home limpa, ícone e Appstore
- Entulho de sistema da Amazon ocultado por padrão no 1º uso (DefaultHidden, reversível em Ajustes → Apps → Restaurar ocultos).
- Appstore: atividade resolvida é um alias que não abre; LaunchFix força com.amazon.venezia.grid.AppsGridLauncherActivity.
- Semeadura de favoritos só roda uma vez e só com favoritos vazios (toggle nunca remove favorito existente).
- Ícone/banner novo: azul vivo com tela+play, reconhecível entre os apps da Amazon.

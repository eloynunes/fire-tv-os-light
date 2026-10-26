> **Historical document — not current operational guidance.** See [README](../README.md), [SECURITY](../SECURITY.md) and [current known issues](KNOWN-ISSUES.md). Root/Magisk, compatibility, performance and automatic-rollback claims below may be obsolete or unverified.

# Diagnóstico real (ADB, 24/10/2026)
- Modelo: AFTSS (sheldon) | Fabricante: Amazon
- Fire OS: Fire OS 7.7.1.7 (PS7717/5741) | build 0036408028548 | Android 9 / API 28
- ABI: armeabi-v7a,armeabi | SoC: mt8695 / Generic DT based system | núcleos: 4 | freq máx: 1690 MHz
- RAM total: 900 MB | disponível: 225 MB | livre: 48 MB | swap livre: 194/383 MB
- GPU: Imagination Technologies, PowerVR Rogue GE8300, OpenGL ES 3.2 build 23.2@6654982
- Tela: Physical size: 1920x1080 Physical density: 320
- /data: /dev/block/mmcblk0p13 4.9G  3.9G  968M  81% /data
- Pacotes: 265 total, 27 de terceiros | atividades LEANBACK: 28
- Maiores processos (PSS): system 78,241K, com.amazon.tv.launcher 77,440K, com.amazon.tv.settings.v2 72,948K, com.amazon.device.services 64,862K, surfaceflinger 61,198K, com.amazon.vizzini 33,583K, /init 32,020K, com.amazon.tahoe 15,718K
- Heap por app: 128m (limite 256m) | low_ram: ?
- Hardware esperado CONFIRMADO: quad-core 1,69 GHz, ARM 32 bits (armeabi-v7a), ~1 GB (900 MB visíveis), Android 9/API 28. Aparelho real = Fire TV Stick 3ª ger. (AFTSS "sheldon").
- Gargalo principal: RAM. Só ~225 MB disponíveis com a Home da Amazon aberta; swap já metade usado. A Home da Amazon sozinha ocupa ~77 MB.

## Medições do TV Light no aparelho (release)
| Métrica | v0.1 inicial | Após otimização |
|---|---|---|
| Memória própria (Java+nativa+gráficos) | ~76 MB | ~30 MB |
| 1ª tela desenhada | 1,3 s | 0,36–0,53 s |
| Abertura a frio (am start) | 0,31–0,63 s | 0,31–0,56 s |
| Volta da Home Amazon | 0,82 s | instantânea (tarefa trazida à frente) |

## Fluidez de navegação (mesma sequência de 12 teclas, `compare-smoothness.sh`)
| Tela | Quadros travados | p50 | p90 | p95 |
|---|---|---|---|---|
| Home Amazon | 35,6% | 9 ms | 27 ms | 42 ms |
| TV Light | 1,7% | 6 ms | 10 ms | 12 ms |

## RAM após reinício (24/10/2026)
- Prime Video (com.amazon.firebat) sobe sozinho no boot: ~185 MB, mantido pela Home da Amazon (PlaybackApiService). Netflix também (~43 MB).
- `am force-stop com.amazon.firebat` libera ~170 MB (RAM disponível 196 → 365 MB); vale até o próximo reinício. Desativar o componente: bloqueado (SecurityException).
- Restrição de segundo plano (appop RUN_ANY_IN_BACKGROUND=ignore) aplicada a 23 apps de streaming (Spotify fora). Não impede o autostart feito pelo sistema no boot.
- Após reinício: TV Light abre sozinho, trava do Home e animações 0,5 se mantêm.

## Limpeza de memória nativa (no aparelho, sem Mac) — 24/10/2026
- Launcher usa `ActivityManager.killBackgroundProcesses` (permissão KILL_BACKGROUND_PROCESSES, sem root). Roda ao voltar ao TV Light e a cada 20 s enquanto ele está na tela.
- Netflix e demais apps: encerrados em 2º plano → +86 MB num teste (200 → 287 MB). Spotify preservado (áudio).
- Prime Video (firebat): fica em adj=700 (app "anterior"), fixado pela Home da Amazon (PlaybackApiService). `killBackgroundProcesses` não alcança adj<900, então Prime só é liberado quando deixa de ser o app mais recente (aí a varredura o pega) — ou via `am force-stop` (Mac/cabo) / root. Limite real do Fire OS para app sem root.
- Efeito líquido: nunca mais Prime + Netflix + outros fixados juntos; só o app mais recente permanece.
- Fluidez do launcher sob uso real: 4,8% de quadros travados, p50 7 ms, p95 15 ms.
- Ajustes do TV Light: "Liberar memória agora", "Limpeza automática" (padrão ligada), "Modo agressivo" (encerra todos de 2º plano, menos Spotify).

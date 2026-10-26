> **Historical document — not current operational guidance.** See [README](../README.md), [SECURITY](../SECURITY.md) and [current known issues](KNOWN-ISSUES.md). Root/Magisk, compatibility, performance and automatic-rollback claims below may be obsolete or unverified.

# Resumo para o especialista — root de Fire TV Stick HD (sheldon) + projeto TV Light

Oi! Este resumo é pra te ajudar a validar **compatibilidade** e **segurança** antes/durante o root do
meu Fire Stick. Eu vou executar o root seguindo o guia; queria sua conferência. Abaixo estão os dados
exatos do aparelho (lidos por ADB), o que já fiz sem root, o que está em aberto e perguntas diretas.

---

## 1. Aparelho (lido via ADB, não é chute)

| Propriedade | Valor |
|---|---|
| `ro.product.device` / `ro.product.name` | **sheldon** |
| `ro.product.model` | **AFTSS** (Fire TV Stick HD) |
| `ro.hardware` / `ro.boot.hardware` | **mt8695** (MediaTek) |
| `ro.build.id` | **PS7717.5741N** |
| `ro.build.fingerprint` | `Amazon/sheldon/sheldon:9/PS7717.5741N/0036408028416:user/amz-p,release-keys` |
| `ro.build.version.name` | **Fire OS 7.7.1.7 (PS7717/5741)** |
| Android / SDK | **9 / 28** |
| `ro.build.version.security_patch` | **2026-03-01** |
| RAM | ~900 MB | 
| ADB | ativo por rede (porta 5555) |

## 2. Método de root que estou mirando

- **GhostLock-sheldon** (GitHub `seb5594/GhostLock-sheldon`), exploit de kernel **CVE-2026-43499**,
  aparentemente **por software** (sem abrir o aparelho), instala **Magisk**. A versão recente lista
  **PS7717.5741N** como suportado para a família sheldon.
- Tentativas antigas (Launcher Hijack, métodos < PS7706, preloader via FT232H) **não** servem nesta
  build; por isso o foco no GhostLock.

## 3. O que já fiz SEM root (tudo reversível, via ADB)

- **Debloat reversível** (`pm disable-user`, nada desinstalado): 12 pacotes desativados (telemetria do
  framework, Amazon Photos, Game Center, Notícias, miniTV, Live Shopping, What Should I Watch, Amazon
  Kids, spiderpork). Registro do que foi desativado p/ reverter com `debloat.sh off`.
- **Não desativam sem root (protegidos):** `com.amazon.device.metrics`, `device.logmanager`,
  `ftvads.deeplinking`, `com.amazon.tcomm`, `tcomm.client`.
- **Protetor de tela desligado + suspensão 5 min → 30 min** (`screensaver_enabled=0`,
  `screen_off_timeout`/`sleep_timeout`), reversível.
- **Animações do sistema em 0** (transições instantâneas).
- **Launcher próprio (TV Light)** no lugar do home da Amazon via serviço de acessibilidade + "cortina".

## 4. Problemas em aberto (o que o root deve resolver)

1. **Botão Home não abre o launcher de terceiros direto.** No Fire OS 7.7.1.7, a Home da Amazon
   (`com.amazon.tv.launcher`) é protegida: `pm disable-user` dá `SecurityException`, e
   `cmd package set-home-activity <nosso>/.MainActivity` retorna "Success" mas é **ignorado**. Hoje
   contorno com uma cortina de acessibilidade, mas há um **flash da Home da Amazon** antes.
   → **Pergunta:** com root/Magisk nesta build, qual o método confiável de trava de Home? (Magisk module?
   editar `role`/preferred-activity? desativar o launcher da Amazon de fato?)
2. **Prime Video fixado em RAM.** `com.amazon.firebat` (~185 MB) fica em **adj=700 (previous app)**,
   preso por binding da Home da Amazon (`PlaybackApiService`). `killBackgroundProcesses` só pega adj≥900,
   então sem root não encerra. Com root, meu app já faz `am force-stop` nele.

## 5. O que o meu app (TV Light) já faz quando houver root — para você revisar a segurança

Após uma **revisão de segurança**, a execução de comandos privilegiados foi **retirada desta versão**:
- O app **não roda `su`**, não encerra o Prime por root e não altera a Home por root.
- O redirecionamento de Home é só por **acessibilidade** (cortina), sem privilégios.
- A limpeza de RAM usa apenas `killBackgroundProcesses` (sem root) e está **desligada por padrão**.
- **Sem exploit, sem ADB embutido, sem porta aberta, sem execução de shell como root dentro do app.**
- A integração com root (encerrar Prime / fixar Home) fica para um **backend tipado, revisado e testado**
  — não exposição de shell arbitrária. Por isso a pergunta 4 abaixo é a mais importante pra você.
- Desinstalar o app remove tudo; o root (quando existir) é gerenciado por você no Magisk.

## 6. Perguntas diretas (compatibilidade + segurança)

1. O GhostLock-sheldon cobre **exatamente** `sheldon` / `AFTSS` / `mt8695` / **PS7717.5741N** / Fire OS
   7.7.1.7, ou o alvo listado é o `sheldonp` (Stick 3ª ger.) e eu preciso de outra variante?
2. Esse método **wipa** o aparelho (factory reset)? Qual o risco real de **brick** e há caminho de
   unbrick (ex.: via mtkclient/preloader) pra essa placa mt8695?
3. Dá pra manter **OTA travado** (pra Amazon não reverter) sem comprometer segurança? Algum Magisk module
   recomendado?
4. Com root, qual a forma **mais limpa e reversível** de fixar um launcher de terceiros como Home nesta
   build (sem quebrar updates nem DRM de apps de streaming)?
5. Algum cuidado de segurança específico depois do root (ex.: negar `su` por padrão, DenyList do Magisk
   pra apps de banco/DRM)?

---

**Projeto completo (código + scripts + docs):** repositório privado `eloynunes/fire-tv-os-light`
(peça acesso ao dono). Arquivos-chave: `docs/PROBLEMA-EM-ABERTO.md`, `docs/ROOT-GHOSTLOCK.md`,
`app/.../MemoryPolicy.kt`, `app/.../HomeRedirectService.kt`, `app/.../RootShell.kt`,
`scripts/root-status.sh`, `scripts/debloat.sh`.

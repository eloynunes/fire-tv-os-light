# Preparado para root (GhostLock) — o que o TV Light faz quando você rootear

Este documento explica o que já está **pronto no app** para aproveitar o root, e o que **você**
precisa fazer. O root em si **não é executado por aqui**: você o faz seguindo o guia da comunidade,
por sua conta e risco. O app só "acende" os recursos de root quando detecta um `su` funcional.

## Seu aparelho (confirmado via ADB)

| | |
|---|---|
| Codename | `sheldon` |
| Modelo | `AFTSS` (Fire TV Stick HD) |
| Chip | MediaTek `mt8695` |
| Firmware | `PS7717.5741N` |
| Fire OS | 7.7.1.7 (Android 9) |

Esse conjunto **bate** com o alvo declarado pelo método de root por software de 2026 da comunidade
(GhostLock, baseado em uma falha de kernel). Confirme sempre o codename/firmware na fonte antes de
qualquer passo.

## O que VOCÊ faz (fora deste projeto)

1. Rooteia o aparelho seguindo o guia da comunidade para a família **sheldon** (repositório
   **GhostLock-sheldon** e a thread correspondente no XDA). **Risco real de brick e de factory reset
   (apaga tudo).** Leia os avisos com calma.
2. Ao final, você deve ter o **Magisk** instalado (gerenciador de root).

## O que o TV Light JÁ FAZ sozinho quando há root

Nada disso liga sem root — é tudo dormente até existir um `su`:

- **Encerra o Prime Video de verdade.** Sem root, o Prime (`com.amazon.firebat`) fica fixado pela Home
  da Amazon em `adj=700` e não dá para encerrá-lo; ele come ~185 MB e deixa tudo mais lento. Com root,
  o launcher roda `am force-stop` nele ao voltar para a tela inicial, liberando RAM → apps abrem mais
  rápido. (Veja `MemoryManager` + `MemoryPolicy`.)
- **Tenta fixar o TV Light como Home de verdade** pelo caminho padrão do Android
  (`cmd package set-home-activity`), em `HomeRedirectService.onServiceConnected()`. Se o Fire OS ainda
  ignorar mesmo com root, a **cortina** continua cobrindo o Home da Amazon (nada piora).
  > O método **definitivo** de trava de Home nesta build deve ser confirmado no guia do GhostLock; o app
  > usa o caminho padrão e não embute nenhum passo de exploit.
- **Mostra o status do root** em Ajustes → Memória ("Root (GhostLock)"): *Verificando… → Disponível /
  Não detectado*.

## Como conceder e verificar o root

1. Abra o TV Light → **Ajustes → Memória**. Na primeira vez que ele tocar no `su`, o **Magisk** vai
   pedir permissão — **conceda** para `com.firetvoslight.launcher` (marque "lembrar").
2. A linha "Root (GhostLock)" deve virar **Disponível**.
3. Pelo computador, dá para checar direto:

   ```bash
   ./scripts/root-status.sh
   ```

## Reversível

- Tudo que o app faz com root é comportamento em tempo de execução (encerrar Prime, tentar fixar Home);
  **desinstalar o TV Light remove tudo** e restaura a experiência da Amazon.
- O root em si é gerenciado pelo Magisk (você revoga a permissão do app quando quiser) e independe deste
  projeto.

## Restrições do projeto (mantidas)

Nenhum exploit/ADB embutido no app, nenhum passo de root operacionalizado aqui. O app apenas **usa** um
`su` que já exista — quem cria esse `su` é você, pelo guia da comunidade.

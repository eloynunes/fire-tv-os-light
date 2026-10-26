> **Historical document — not current operational guidance.** See [README](../README.md), [SECURITY](../SECURITY.md) and [current known issues](KNOWN-ISSUES.md). Root/Magisk, compatibility, performance and automatic-rollback claims below may be obsolete or unverified.

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

## Estado atual do app (após revisão de segurança)

Numa revisão de segurança, a **execução de comandos privilegiados foi retirada desta versão**: o app
**não** roda `su`, não encerra o Prime por root e não altera a Home por root. Em Ajustes → Memória, a
linha "Integração privilegiada" aparece como **Indisponível nesta versão**. O redirecionamento de Home
por **acessibilidade** (cortina) permanece como alternativa, sem privilégios.

A integração com root (encerrar o Prime, fixar a Home) fica para um **backend tipado, revisado e
testado** — em vez de expor execução de shell arbitrária como root dentro do app. Quando isso existir,
este documento será atualizado com o passo de concessão no Magisk e a verificação.

## Verificar root pelo computador (independe do app)

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

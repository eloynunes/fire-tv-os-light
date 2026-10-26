> **Historical document — not current operational guidance.** See [README](../README.md), [SECURITY](../SECURITY.md) and [current known issues](KNOWN-ISSUES.md). Root/Magisk, compatibility, performance and automatic-rollback claims below may be obsolete or unverified.

# Erros conhecidos
- `HashMap.putIfAbsent` exige API 24 (lint NewApi, minSdk 22) → substituído. 
- Fire Stick sem resposta em 192.0.2.1 (ping/5555) em 24/10/2026: aparelho dormindo, IP mudou ou ADB desligado.
- Lint (avisos aceitos): VectorRaster no banner 320×180; ViewConstructor em TileView (criado só por código).
- Rede UniFi com isolamento de clientes: Mac no Wi-Fi não alcança o Stick (ARP responde por proxy, TCP expira). Solução: Mac no cabo.
- Fire Stick não responde ping (ICMP bloqueado) → connect.sh testa só a porta.
- uiautomator dump só lista o que está visível → persistência é verificada pelo log `TVLight state fav=`.
- 1º teste de persistência trocou Disney+ por Game Center; dados do app limpos (pm clear) e favoritos padrão refeitos.
- gfxinfo "High input latency" = artefato do `input keyevent` via ADB.
- PSS total oscila (bibliotecas compartilhadas); comparar Native/Graphics.
- Banners com proporção ≠16:9 (ex.: Amazon Live) ficam levemente cortados (center-crop).
- `pm disable-user com.amazon.tv.launcher` → SecurityException (pacote protegido). Não insistir.
- Controle remoto não entrega teclas a serviços de acessibilidade no Fire OS 7.7 → impossível capturar o Home.
- compare-smoothness.sh mede 0 quadros da Home Amazon com a trava ligada (Home redireciona). Desligar a trava antes de comparar.

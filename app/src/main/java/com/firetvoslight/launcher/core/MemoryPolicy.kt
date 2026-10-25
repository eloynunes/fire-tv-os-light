package com.firetvoslight.launcher.core

/**
 * Decide quais apps o launcher pode encerrar em segundo plano para liberar RAM. Lógica pura (testável na JVM).
 * Nunca entra nesta lista: o próprio launcher, apps de áudio (tocam em 2º plano) e pacotes essenciais do sistema.
 */
object MemoryPolicy {
    /**
     * Apps que a Amazon mantém vivos consumindo muita RAM. São alvos SÓ no modo agressivo manual.
     * Nota: Prime Video (firebat/avod) fica fixado pela Home da Amazon em adj=700 e um app sem root
     * NÃO consegue encerrá-lo; permanece aqui para quando houver root. Netflix e os demais apps de
     * streaming que o usuário usa saem daqui de propósito: encerrá-los só forçava um "cold start" lento
     * na reabertura — o oposto do que o usuário quer.
     */
    val DEFAULT_HOGS = setOf(
        "com.amazon.firebat",   // Prime Video (só cai com root)
        "com.amazon.avod",      // Prime Video (legado)
    )

    /**
     * Nunca encerrar — mantidos "quentes" para reabrir rápido: áudio em 2º plano, o próprio launcher
     * e os apps de streaming que o usuário realmente usa.
     */
    val NEVER = setOf(
        "com.spotify.tv.android",
        "com.spotify.music",
        "com.amazon.mp3",
        "com.netflix.ninja",          // Netflix
        "com.disney.disneyplus",      // Disney+
        "com.hbo.hbonow",             // HBO Max (legado)
        "com.wbd.stream",             // HBO Max / Max
        "com.globo.globotv",          // Globoplay
        "com.amazon.firetv.youtube",  // YouTube
        "com.google.android.youtube.tv",
    )

    /** Prefixos de sistema protegidos (não encerrar nem no modo agressivo). */
    private val SYSTEM_PREFIXES = listOf(
        "android", "com.android.", "com.amazon.tv.", "com.amazon.device.",
        "com.amazon.ssm", "com.amazon.whisper", "com.amazon.vizzini", "com.amazon.tcomm",
        "com.amazon.alta", "com.amazon.ags", "com.amazon.dp.", "com.amazon.sync",
        "com.amazon.providers", "com.amazon.kindle.cms", "com.amazon.bueller",
    )

    /**
     * Pacotes a encerrar agora.
     * @param aggressive se true, encerra todos os apps de terceiros em [running] (menos exceções); senão, só [hogs].
     * @param running pacotes atualmente com processo (não encerra o que não está rodando).
     * @param foreground pacote em primeiro plano (nunca encerrado).
     * @param thirdParty pacotes instalados pelo usuário (base do modo agressivo).
     */
    fun toKill(
        hogs: Set<String>,
        aggressive: Boolean,
        running: Set<String>,
        foreground: String?,
        self: String,
        thirdParty: Set<String>,
    ): List<String> {
        // Agressivo: todos os apps de terceiros + os hogs conhecidos (Prime é "sistema", mas é hog).
        val candidates = if (aggressive) thirdParty + hogs else hogs
        return candidates.asSequence()
            .filter { it in running }
            .filter { it != foreground && it != self }
            .filter { it !in NEVER }
            // Hogs conhecidos passam mesmo sendo do fabricante; os demais do sistema, não.
            .filter { it in hogs || !isSystem(it) }
            .distinct()
            .sorted()
            .toList()
    }

    fun isSystem(pkg: String): Boolean = SYSTEM_PREFIXES.any { pkg == it || pkg.startsWith(it) }
}

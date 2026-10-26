package com.firetvoslight.launcher

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.firetvoslight.launcher.core.MemoryPolicy

/**
 * Solicita ao Android encerramento de processos elegíveis em segundo plano, sem root.
 * Uma chamada aceita não comprova encerramento nem ganho de memória.
 */
class MemoryManager(private val ctx: Context) {
    private val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var lastRun = 0L

    /** Chame ao voltar ao launcher. [minIntervalMs] evita repetir em reentradas muito próximas. */
    fun sweep(aggressive: Boolean, minIntervalMs: Long = 0L): Int {
        val now = SystemClock.uptimeMillis()
        if (now - lastRun < minIntervalMs) return 0
        lastRun = now
        val running = runningPackages()
        // A varredura de apps instalados só é necessária no modo agressivo.
        val third = if (aggressive) thirdPartyPackages() else emptySet()
        val kill = MemoryPolicy.toKill(
            hogs = MemoryPolicy.DEFAULT_HOGS,
            aggressive = aggressive,
            running = running,
            foreground = null, // o launcher está em foreground; os demais estão em 2º plano
            self = ctx.packageName,
            thirdParty = third,
        )
        var n = 0
        for (p in kill) {
            try {
                am.killBackgroundProcesses(p)
                n++
            } catch (t: Throwable) {
                Log.w("TVLight", "kill $p", t)
            }
        }
        // Sem force-stop privilegiado automático; a via ADB externa é separada.
        if (n > 0) Log.i("TVLight", "mem sweep: solicitados=$n [${kill.joinToString(",")}] aggressive=$aggressive")
        return n
    }

    fun availableMb(): Long {
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem / (1024 * 1024)
    }

    private fun runningPackages(): Set<String> {
        val out = HashSet<String>()
        try {
            // getRunningAppProcesses no Fire OS (API 28) ainda lista os processos do sistema e apps vivos.
            am.runningAppProcesses?.forEach { it.pkgList?.forEach(out::add) }
        } catch (_: Throwable) {
        }
        return out
    }

    private fun thirdPartyPackages(): Set<String> {
        val out = HashSet<String>()
        try {
            val pm = ctx.packageManager
            for (ai in pm.getInstalledApplications(0)) {
                if (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) out.add(ai.packageName)
            }
        } catch (_: Throwable) {
        }
        return out
    }
}

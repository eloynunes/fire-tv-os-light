package com.firetvoslight.launcher

import android.util.Log
import java.util.concurrent.Executors

/**
 * Executa comandos como root via `su`, mas SÓ quando o aparelho estiver rooteado (ex.: Magisk, depois do
 * GhostLock). Sem root é inofensivo: a detecção falha, [known] fica `false` e nada roda. Por isso instalar
 * o app num aparelho NÃO rooteado não muda nada — os recursos de root ficam dormentes até o `su` existir.
 *
 * Tudo que fala com o `su` roda numa thread própria (nunca na UI): abrir o `su` pode bloquear e, na primeira
 * vez, dispara o pedido de permissão do Magisk.
 */
object RootShell {
    private const val TAG = "TVLight"
    private val exec = Executors.newSingleThreadExecutor()
    @Volatile private var cached: Boolean? = null

    /** Resultado já conhecido: null = ainda não checado, true/false = há root ou não. Não bloqueia. */
    fun known(): Boolean? = cached

    /** Detecta root em background e entrega o resultado a [cb] (na thread de trabalho — faça post p/ a UI). */
    fun check(cb: (Boolean) -> Unit) {
        cached?.let { cb(it); return }
        exec.execute { cb(ensureDetected()) }
    }

    /** Roda [commands] como root numa única sessão `su`, em background. No-op sem root. */
    fun run(vararg commands: String) {
        if (commands.isEmpty()) return
        exec.execute {
            if (!ensureDetected()) return@execute
            try {
                val p = ProcessBuilder("su").redirectErrorStream(true).start()
                p.outputStream.bufferedWriter().use { w ->
                    commands.forEach { w.write(it); w.write("\n") }
                    w.write("exit\n")
                }
                Log.i(TAG, "root run (${commands.size} cmd) ok=${p.waitFor() == 0}")
            } catch (t: Throwable) {
                Log.w(TAG, "root run falhou", t)
            }
        }
    }

    private fun ensureDetected(): Boolean {
        cached?.let { return it }
        val ok = try {
            val p = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            out.contains("uid=0")
        } catch (_: Throwable) {
            false
        }
        cached = ok
        if (ok) Log.i(TAG, "root disponível")
        return ok
    }
}

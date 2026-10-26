package com.firetvoslight.launcher

/**
 * Contenção OPCIONAL e TEMPORÁRIA para revisão, não uma solução de Home/Prime.
 * Não executa su nem identifica se o aparelho tem root.
 * known/check informam a disponibilidade de integração aprovada nesta versão.
 * A assinatura check é preservada para os consumidores existentes.
 * Aplicar junto ao patch de call sites e textos; não instalar isoladamente.
 */
object RootShell {
    fun known(): Boolean = false

    fun check(cb: (Boolean) -> Unit) {
        cb(false)
    }

    // Sem run(String...): comandos privilegiados não ficam expostos ao app.
    // Um backend futuro deverá ter operações tipadas, revisão e testes próprios.
}

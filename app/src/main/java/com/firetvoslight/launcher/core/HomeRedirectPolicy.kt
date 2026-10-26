package com.firetvoslight.launcher.core

/** Only actual Home windows may trigger redirection; Amazon also hosts Settings. */
object HomeRedirectPolicy {
    enum class Action { IGNORE, REDIRECT, DISMISS, LEAVE }

    fun decide(pkg: String?, self: String, bypass: Boolean, launching: String?, windowClass: String?): Action = when {
        pkg.isNullOrBlank() -> Action.IGNORE
        pkg == launching -> Action.DISMISS
        pkg == "com.amazon.tv.launcher" -> when {
            windowClass !in HOME_WINDOWS -> Action.LEAVE
            !bypass && launching == null -> Action.REDIRECT
            else -> Action.IGNORE
        }
        pkg == self || pkg == "android" || pkg == "com.android.systemui" -> Action.IGNORE
        else -> Action.LEAVE
    }

    private val HOME_WINDOWS = setOf(
        "com.amazon.tv.launcher.ui.HomeActivity_vNext",
        "com.amazon.tv.launcher.ui.HomeActivity",
    )
}

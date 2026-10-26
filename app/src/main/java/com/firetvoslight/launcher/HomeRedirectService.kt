package com.firetvoslight.launcher

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import com.firetvoslight.launcher.core.AppEntry
import com.firetvoslight.launcher.core.HomeRedirectPolicy
import com.firetvoslight.launcher.core.KeyAction
import com.firetvoslight.launcher.core.KeyMap
import com.firetvoslight.launcher.core.Section

/**
 * Faz o botão Home abrir o TV Light. O Fire OS não deixa desativar a Home da Amazon nem entrega o botão Home
 * a apps, e o Android segura aberturas de apps por alguns segundos após o Home. Por isso, quando a Home da
 * Amazon aparece, mostramos na hora uma "cortina" que é o próprio TV Light, funcionando (o controle já navega),
 * e pedimos a abertura da Activity; a cortina sai quando ela assume. Ativado/desativado via scripts/home-lock.sh.
 */
class HomeRedirectService : AccessibilityService(), LauncherScreen.Host {

    private val main = Handler(Looper.getMainLooper())
    private var screen: LauncherScreen? = null
    private var curtain: FrameLayout? = null
    private var notice: TextView? = null
    private var shown = false
    private var launchingPkg: String? = null
    private var pendingAppIntent: Intent? = null
    private val dropCurtain = Runnable {
        cancelAlarm(this, Intent(this, MainActivity::class.java), REQ_LAUNCHER)
        cancelPendingApp()
        hideCurtain()
    }

    private fun cancelPendingApp() {
        pendingAppIntent?.let { cancelAlarm(this, it, REQ_APP) }
        pendingAppIntent = null
    }

    override fun onServiceConnected() {
        instance = this
        LauncherRepo.init(this)
        LauncherRepo.store.homeLockDesired = true
        // A conexão do serviço não altera preferências de Home por root.
        // O redirecionamento por acessibilidade abaixo permanece como alternativa.
        // Monta a cortina com antecedência para o primeiro Home já ser instantâneo.
        main.postDelayed({ if (instance === this) ensureCurtain() }, 2000)
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        cancelAlarm(this, Intent(this, MainActivity::class.java), REQ_LAUNCHER)
        cancelPendingApp()
        hideCurtain()
        screen?.let { LauncherRepo.removeListener(it) }
        screen = null
        curtain = null
        notice = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        when (HomeRedirectPolicy.decide(event.packageName?.toString(), packageName, bypass, launchingPkg, event.className?.toString())) {
            HomeRedirectPolicy.Action.REDIRECT -> {
                showCurtain()
                openViaAlarm(Intent(this, MainActivity::class.java), REQ_LAUNCHER)
            }
            // O app escolhido na cortina abriu: a cortina já pode sair.
            HomeRedirectPolicy.Action.DISMISS -> {
                cancelAlarm(this, Intent(this, MainActivity::class.java), REQ_LAUNCHER)
                cancelPendingApp()
                hideCurtain()
            }
            // Saiu da Home da Amazon para outro app: o desvio pedido pelo usuário termina.
            HomeRedirectPolicy.Action.LEAVE -> {
                cancelAlarm(this, Intent(this, MainActivity::class.java), REQ_LAUNCHER)
                cancelPendingApp()
                hideCurtain()
                if (event.packageName?.toString() != AMAZON_HOME) bypass = false
            }
            HomeRedirectPolicy.Action.IGNORE -> Unit
        }
    }

    override fun onInterrupt() {
        cancelAlarm(this, Intent(this, MainActivity::class.java), REQ_LAUNCHER)
        cancelPendingApp()
        hideCurtain()
    }

    // ---------- cortina ----------

    private fun ensureCurtain(): FrameLayout {
        curtain?.let { return it }
        val s = LauncherScreen(this, this)
        LauncherRepo.addListener(s)
        if (LauncherRepo.installed.isNotEmpty()) s.render(LauncherRepo.lastFocus)
        val dp = resources.displayMetrics.density
        val frame = object : FrameLayout(this) {
            override fun dispatchKeyEvent(e: KeyEvent): Boolean {
                // Voltar e ☰ não fazem nada aqui; navegação e OK funcionam normalmente.
                if (launchingPkg != null) return true
                return when (KeyMap.map(e.keyCode)) {
                    KeyAction.BACK, KeyAction.OPTIONS -> true
                    else -> super.dispatchKeyEvent(e)
                }
            }
        }
        frame.addView(s.root, FrameLayout.LayoutParams(-1, -1))
        val n = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            background = GradientDrawable().apply { cornerRadius = 18 * dp; setColor(0xCC000000.toInt()) }
            visibility = View.GONE
        }
        frame.addView(n, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, (40 * dp).toInt(), (32 * dp).toInt())
        })
        screen = s
        notice = n
        curtain = frame
        return frame
    }

    private fun showCurtain() {
        if (shown) return
        val frame = ensureCurtain()
        val s = screen ?: return
        if (s.isEmpty && LauncherRepo.installed.isNotEmpty()) s.render(null)
        s.updateClock()
        notice?.visibility = View.GONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE,
        ).apply { windowAnimations = 0 }
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(frame, lp)
            shown = true
            s.focus(LauncherRepo.lastFocus)
            main.postDelayed(dropCurtain, CURTAIN_MAX_MS)
        } catch (t: Throwable) {
            Log.w(TAG, "curtain failed", t)
        }
    }

    private fun hideCurtain() {
        main.removeCallbacks(dropCurtain)
        launchingPkg = null
        if (!shown) return
        shown = false
        screen?.focusedTag()?.let { LauncherRepo.lastFocus = it }
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeViewImmediate(curtain)
        } catch (_: Throwable) {
        }
    }

    // ---------- Host (cortina) ----------

    override fun launch(e: AppEntry) {
        // O usuário escolheu um app antes do TV Light assumir: não traz mais o TV Light, abre o app.
        cancelAlarm(this, Intent(this, MainActivity::class.java), REQ_LAUNCHER)
        launchingPkg = e.packageName
        notice?.apply {
            text = getString(R.string.curtain_opening, e.label)
            visibility = View.VISIBLE
        }
        val i = Intent(Intent.ACTION_MAIN).setComponent(ComponentName(e.packageName, e.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        cancelPendingApp()
        pendingAppIntent = i
        openViaAlarm(i, REQ_APP)
        main.removeCallbacks(dropCurtain)
        main.postDelayed(dropCurtain, CURTAIN_MAX_MS)
    }

    // Menus ficam para o TV Light real (aparece em instantes).
    override fun options(section: Section, e: AppEntry) {}
    override fun settings() {}

    private fun openViaAlarm(i: Intent, req: Int) {
        val now = SystemClock.uptimeMillis()
        if (req == REQ_LAUNCHER && now - lastOpen < 400) return
        lastOpen = now
        Log.i(TAG, "redirect: req=$req")
        try {
            // startActivity direto ficaria retido pela regra de "app switch" após o Home;
            // o alarme é entregue pelo sistema e chega antes.
            val pi = PendingIntent.getActivity(this, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            (getSystemService(ALARM_SERVICE) as AlarmManager)
                .setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pi)
        } catch (t: Throwable) {
            Log.w(TAG, "redirect failed", t)
            cancelPendingApp()
            hideCurtain()
        }
    }

    companion object {
        private const val TAG = "TVLight"
        const val AMAZON_HOME = "com.amazon.tv.launcher"
        private const val CURTAIN_MAX_MS = 10_000L
        private const val REQ_LAUNCHER = 0
        private const val REQ_APP = 1
        private var lastOpen = 0L
        private var instance: HomeRedirectService? = null

        /** true enquanto o usuário pediu para ver a Home da Amazon (menu Ajustes). */
        @Volatile var bypass = false

        /** true quando o serviço de acessibilidade está vinculado (a trava do Home está ativa). */
        val isActive: Boolean get() = instance != null

        private fun cancelAlarm(ctx: Context, i: Intent, req: Int) {
            PendingIntent.getActivity(ctx, req, i, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { pi ->
                (ctx.getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pi)
                pi.cancel()
            }
        }

        /**
         * Chamado quando o TV Light real já está na tela: cancela o alarme pendente e tira a cortina.
         * Retorna o item focado na cortina, para a Activity continuar exatamente nele.
         */
        fun onLauncherVisible(ctx: Context): Pair<Section, String>? {
            cancelAlarm(ctx, Intent(ctx, MainActivity::class.java), REQ_LAUNCHER)
            val s = instance ?: return null
            if (!s.shown || s.launchingPkg != null) return null
            val tag = s.screen?.focusedTag()
            // Pequeno atraso: a Activity desenha primeiro, então a troca não pisca.
            s.main.removeCallbacks(s.dropCurtain)
            s.main.postDelayed(s.dropCurtain, 120)
            return tag
        }
    }
}

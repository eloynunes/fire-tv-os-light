package com.firetvoslight.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.firetvoslight.launcher.core.AppEntry
import com.firetvoslight.launcher.core.KeyAction
import com.firetvoslight.launcher.core.KeyMap
import com.firetvoslight.launcher.core.Section

class MainActivity : Activity(), LauncherScreen.Host {

    private val repo = LauncherRepo
    private lateinit var screen: LauncherScreen
    private lateinit var memory: MemoryManager
    private val main = Handler(Looper.getMainLooper())
    private var iconsCleared = false
    private var drawnReported = false
    private var resumed = false
    private val createdAt = SystemClock.uptimeMillis()

    // Varre a RAM periodicamente enquanto o launcher está na tela (apps podem acordar sozinhos).
    private val periodicSweep = object : Runnable {
        override fun run() {
            if (!resumed) return
            if (repo.store.autoFree) memory.sweep(repo.store.aggressiveFree)
            main.postDelayed(this, SWEEP_INTERVAL_MS)
        }
    }

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = screen.updateClock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo.init(this)
        memory = MemoryManager(this)
        screen = LauncherScreen(this, this)
        setContentView(screen.root)
        repo.addListener(screen)
        // Desenha imediatamente a partir da lista em cache; o sistema é consultado em seguida.
        if (repo.installed.isNotEmpty()) render(repo.lastFocus)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(timeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })
        screen.updateClock()
        // Se a cortina estava na tela, continua no mesmo item que o usuário já tinha escolhido.
        HomeRedirectService.onLauncherVisible(this)?.let { screen.focus(it) }
        if (iconsCleared) {
            iconsCleared = false
            screen.rebindIcons()
        }
        if (screen.isEmpty && repo.installed.isNotEmpty()) render(repo.lastFocus)
        repo.reload()
        resumed = true
        // Libera a RAM logo após desenhar (não compete com o primeiro render).
        if (repo.store.autoFree) main.postDelayed({ memory.sweep(repo.store.aggressiveFree) }, 350)
        main.postDelayed(periodicSweep, SWEEP_INTERVAL_MS)
    }

    override fun onPause() {
        super.onPause()
        resumed = false
        main.removeCallbacks(periodicSweep)
        screen.finishMove()
        repo.lastFocus = screen.focusedTag()
        unregisterReceiver(timeReceiver)
    }

    override fun onDestroy() {
        repo.removeListener(screen)
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Em segundo plano sob pressão: devolve a memória dos ícones; recarrega ao voltar.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            repo.clearIcons()
            iconsCleared = true
        }
    }

    private fun render(focus: Pair<Section, String>?) {
        screen.render(focus)
        if (!drawnReported) {
            drawnReported = true
            screen.root.post {
                Log.i(TAG, "firstRenderMs=${SystemClock.uptimeMillis() - createdAt}")
                try {
                    reportFullyDrawn()
                } catch (_: Throwable) {
                }
            }
        }
    }

    // ---------- Host ----------

    override fun launch(e: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(e.packageName, e.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(i)
        } catch (t: Throwable) {
            Log.w(TAG, "launch failed ${e.key}", t)
            Toast.makeText(this, getString(R.string.launch_failed, e.label), Toast.LENGTH_SHORT).show()
            repo.reload()
        }
    }

    override fun options(section: Section, e: AppEntry) {
        val state = repo.state
        val fav = state.isFavorite(e.key)
        val items = arrayOf(
            getString(if (fav) R.string.fav_remove else R.string.fav_add),
            getString(R.string.move),
            getString(R.string.hide),
            getString(R.string.app_info),
        )
        AlertDialog.Builder(this, DIALOG_THEME).setTitle(e.label).setItems(items) { _, which ->
            when (which) {
                0 -> {
                    val nowFav = state.toggleFavorite(e.key)
                    repo.saveAndNotify()
                    screen.focus((if (nowFav) Section.FAVORITES else Section.APPS) to e.key)
                }
                1 -> {
                    screen.startMove(section, e.key)
                    Toast.makeText(this, R.string.move_hint, Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    state.hide(e.key)
                    repo.saveAndNotify()
                }
                3 -> openSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", e.packageName, null)))
            }
        }.show()
    }

    override fun settings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openSafely(i: Intent) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (t: Throwable) {
            Log.w(TAG, "open failed $i", t)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (screen.handleMoveKey(event)) return true
        return when (KeyMap.map(event.keyCode)) {
            // Launcher não "sai" com Voltar.
            KeyAction.BACK -> true
            KeyAction.OPTIONS -> screen.handleOptionsKey(event)
            else -> super.dispatchKeyEvent(event)
        }
    }

    private companion object {
        const val TAG = "TVLight"
        const val SWEEP_INTERVAL_MS = 20_000L
        const val DIALOG_THEME = android.R.style.Theme_Material_Dialog_Alert
    }
}

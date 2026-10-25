package com.firetvoslight.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.firetvoslight.launcher.core.AppEntry
import com.firetvoslight.launcher.core.DefaultFavorites
import com.firetvoslight.launcher.core.LauncherState
import com.firetvoslight.launcher.core.Section
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Dados do launcher compartilhados no processo: a Activity e a cortina do botão Home mostram a mesma lista,
 * os mesmos favoritos e os mesmos ícones (bitmaps não são duplicados).
 */
object LauncherRepo {
    interface Listener {
        /** [listChanged] = apps instalados mudaram (redesenhar); senão só há ActivityInfo novos (ícones faltantes). */
        fun onRepo(listChanged: Boolean)
        fun onIconsCleared()
    }

    private const val TAG = "TVLight"
    private val main = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val listeners = ArrayList<Listener>()
    private var loading = false
    private var reloadPending = false
    private lateinit var app: Context
    private lateinit var source: AppSource

    lateinit var store: PrefsStore
        private set
    lateinit var state: LauncherState
        private set
    var installed: List<AppEntry> = emptyList()
        private set
    var infos: Map<String, ActivityInfo> = emptyMap()
        private set
    private var icons: IconLoader? = null

    /** Último item focado (Activity ou cortina), para a troca entre as duas ser imperceptível. */
    var lastFocus: Pair<Section, String>? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            val pkg = i?.data?.schemeSpecificPart ?: return
            if (i.action == Intent.ACTION_PACKAGE_FULLY_REMOVED && state.forgetPackage(pkg)) store.save(state)
            icons?.invalidatePackage(pkg)
            reload()
        }
    }

    fun init(ctx: Context) {
        if (::store.isInitialized) return
        app = ctx.applicationContext
        store = PrefsStore(app)
        state = store.load()
        source = AppSource(app.packageManager, app.packageName)
        installed = store.loadApps()
        store.logState(state)
        app.registerReceiver(packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        })
    }

    fun icons(bannerW: Int, bannerH: Int): IconLoader =
        icons ?: IconLoader(app.packageManager, app.resources, main, File(app.cacheDir, "icons"), bannerW, bannerH).also { icons = it }

    fun addListener(l: Listener) {
        if (l !in listeners) listeners.add(l)
    }

    fun removeListener(l: Listener) = listeners.remove(l)

    /** Salva o estado e avisa as telas para redesenharem. */
    fun saveAndNotify() {
        store.save(state)
        listeners.toList().forEach { it.onRepo(true) }
    }

    fun clearIcons() {
        icons?.clear()
        listeners.toList().forEach { it.onIconsCleared() }
    }

    fun reload() {
        if (loading) {
            reloadPending = true
            return
        }
        loading = true
        val t0 = SystemClock.uptimeMillis()
        worker.execute {
            val loaded = try {
                source.load()
            } catch (t: Throwable) {
                Log.e(TAG, "load failed", t)
                null
            }
            main.post {
                loading = false
                if (loaded != null) {
                    Log.i(TAG, "apps=${loaded.entries.size} loadMs=${SystemClock.uptimeMillis() - t0}")
                    applyLoaded(loaded)
                }
                if (reloadPending) {
                    reloadPending = false
                    reload()
                }
            }
        }
    }

    private fun applyLoaded(loaded: LoadedApps) {
        infos = loaded.infos
        var changedSeed = false
        if (loaded.entries.isNotEmpty()) {
            // Semeia favoritos só uma vez e só se não houver nenhum — toggle nunca remove favorito existente.
            if (!store.seeded) {
                if (state.favorites.isEmpty()) {
                    DefaultFavorites.pick(loaded.entries).forEach { state.toggleFavorite(it.key) }
                    changedSeed = true
                }
                store.seeded = true
            }
            // Oculta o entulho de sistema da Amazon no primeiro uso (reversível em Ajustes).
            if (!store.hideSeeded) {
                loaded.entries.filter { it.packageName in com.firetvoslight.launcher.core.DefaultHidden.PACKAGES }
                    .forEach { state.hide(it.key) }
                store.hideSeeded = true
                changedSeed = true
            }
            if (changedSeed) store.save(state)
        }
        val changed = loaded.entries != installed
        if (changed) {
            installed = loaded.entries
            store.saveApps(installed)
        }
        // changedSeed também força redesenho (favoritos/ocultos mudaram mesmo com a mesma lista de apps).
        listeners.toList().forEach { it.onRepo(changed || changedSeed) }
        icons?.refreshStale(infos) { listeners.toList().forEach { l -> l.onRepo(false) } }
    }
}

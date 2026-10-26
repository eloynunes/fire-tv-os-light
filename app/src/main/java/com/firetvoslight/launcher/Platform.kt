package com.firetvoslight.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import com.firetvoslight.launcher.core.AppEntry
import com.firetvoslight.launcher.core.LauncherState
import com.firetvoslight.launcher.core.PrefsCodec
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LoadedApps(val entries: List<AppEntry>, val infos: Map<String, ActivityInfo>)

/** Lista apps inicializáveis: primeiro LEANBACK_LAUNCHER (apps de TV), depois LAUNCHER (apps sideloaded). */
class AppSource(private val pm: PackageManager, private val selfPackage: String) {
    fun load(): LoadedApps {
        val byPkg = LinkedHashMap<String, Pair<AppEntry, ActivityInfo>>()
        for (cat in CATEGORIES) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val ai = ri.activityInfo ?: continue
                if (ai.packageName == selfPackage || byPkg.containsKey(ai.packageName)) continue
                val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { ai.packageName }
                val activity = com.firetvoslight.launcher.core.LaunchFix.BY_PACKAGE[ai.packageName] ?: ai.name
                byPkg[ai.packageName] = AppEntry(ai.packageName, activity, label) to ai
            }
        }
        val entries = byPkg.values.map { it.first }
        val infos = HashMap<String, ActivityInfo>(entries.size * 2)
        for ((e, ai) in byPkg.values) infos[e.key] = ai
        return LoadedApps(entries, infos)
    }

    private companion object {
        val CATEGORIES = arrayOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
    }
}

class PrefsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var seeded: Boolean
        get() = sp.getBoolean(SEEDED, false)
        set(v) = sp.edit().putBoolean(SEEDED, v).apply()

    fun load() = LauncherState(
        PrefsCodec.decode(sp.getString(FAV, null)),
        PrefsCodec.decode(sp.getString(ORDER, null)),
        PrefsCodec.decode(sp.getString(HIDDEN, null)).toSet(),
    )

    fun save(s: LauncherState) {
        sp.edit()
            .putString(FAV, PrefsCodec.encode(s.favorites))
            .putString(ORDER, PrefsCodec.encode(s.order))
            .putString(HIDDEN, PrefsCodec.encode(s.hidden))
            .apply()
        logState(s)
    }

    fun loadApps(): List<AppEntry> = PrefsCodec.decodeApps(sp.getString(APPS, null))
    fun saveApps(apps: List<AppEntry>) = sp.edit().putString(APPS, PrefsCodec.encodeApps(apps)).apply()

    /** Solicitações de limpeza sem root; desativadas por padrão nesta revisão. */
    var autoFree: Boolean
        get() = sp.getBoolean("review_auto_free_v1", false)
        set(v) = sp.edit().putBoolean("review_auto_free_v1", v).apply()

    /** Modo agressivo: encerra todos os apps de 2º plano, não só Prime/Netflix (Spotify sempre preservado). */
    var aggressiveFree: Boolean
        get() = sp.getBoolean(AGGRESSIVE, false)
        set(v) = sp.edit().putBoolean(AGGRESSIVE, v).apply()

    /** O usuário já ativou o TV Light como tela inicial alguma vez (sobrevive à reinstalação). */
    var homeLockDesired: Boolean
        get() = sp.getBoolean(HOME_DESIRED, false)
        set(v) = sp.edit().putBoolean(HOME_DESIRED, v).apply()

    /** Apps de sistema da Amazon já foram ocultados no primeiro uso. */
    var hideSeeded: Boolean
        get() = sp.getBoolean(HIDE_SEEDED, false)
        set(v) = sp.edit().putBoolean(HIDE_SEEDED, v).apply()

    /** Usado pelo teste no aparelho para verificar persistência sem depender do que está visível. */
    fun logState(s: LauncherState) = Log.i("TVLight", "state fav=${s.favorites.size} [${s.favorites.joinToString(",")}]")

    private companion object {
        const val SEEDED = "seeded"
        const val FAV = "favorites"
        const val ORDER = "order"
        const val HIDDEN = "hidden"
        const val APPS = "apps_cache"
        const val AUTO_FREE = "auto_free"
        const val AGGRESSIVE = "aggressive_free"
        const val HOME_DESIRED = "home_desired"
        const val HIDE_SEEDED = "hide_seeded"
    }
}

/**
 * Banners/ícones reduzidos ao tamanho do cartão (RGB_565 para banners) e guardados em disco,
 * para que a abertura a frio não precise decodificar os recursos de cada app.
 * Cache em memória acessado só na thread principal.
 */
class IconLoader(
    private val pm: PackageManager,
    private val res: Resources,
    private val main: Handler,
    private val dir: File,
    private val bannerW: Int,
    private val bannerH: Int,
) {
    class Icon(val bitmap: Bitmap, val isBanner: Boolean)

    private val exec: ExecutorService = Executors.newSingleThreadExecutor()
    private val cache = HashMap<String, Icon>()
    private val iconSize = (bannerH * 0.48f).toInt()
    private var generation = 0

    init {
        dir.mkdirs()
    }

    /** [info] nulo = só tenta o disco (abertura a partir da lista em cache). */
    fun bind(key: String, info: ActivityInfo?, tile: TileView) {
        tile.boundKey = key
        cache[key]?.let { tile.setIcon(BitmapDrawable(res, it.bitmap), it.isBanner); return }
        val gen = generation
        exec.execute {
            val icon = try {
                load(key, info)
            } catch (t: Throwable) {
                Log.w("TVLight", "icon $key", t)
                null
            }
            main.post {
                if (icon == null || gen != generation) return@post
                cache[key] = icon
                if (tile.boundKey == key) tile.setIcon(BitmapDrawable(res, icon.bitmap), icon.isBanner)
            }
        }
    }

    fun hasIcon(key: String) = cache.containsKey(key)

    /** Descarta ícones cujo APK foi atualizado depois do cache em disco e pede novo bind. */
    fun refreshStale(infos: Map<String, ActivityInfo>, rebind: (String) -> Unit) {
        val snapshot = HashMap(infos)
        exec.execute {
            val stale = snapshot.filter { (key, info) ->
                val apk = info.applicationInfo?.sourceDir?.let { File(it).lastModified() } ?: 0L
                booleanArrayOf(true, false).any { b -> file(key, b).let { it.exists() && it.lastModified() < apk } }
            }.keys
            if (stale.isEmpty()) return@execute
            stale.forEach { k -> booleanArrayOf(true, false).forEach { file(k, it).delete() } }
            main.post { stale.forEach { cache.remove(it); rebind(it) } }
        }
    }

    fun invalidatePackage(pkg: String) {
        cache.keys.removeAll { it.startsWith("$pkg/") }
        exec.execute { dir.listFiles()?.filter { it.name.startsWith(fileId("$pkg/")) }?.forEach { it.delete() } }
    }

    fun clear() {
        generation++
        cache.clear()
    }

    fun shutdown() = exec.shutdownNow()

    private fun load(key: String, info: ActivityInfo?): Icon? {
        val apkTime = info?.applicationInfo?.sourceDir?.let { File(it).lastModified() } ?: 0L
        for (banner in booleanArrayOf(true, false)) {
            val f = file(key, banner)
            if (f.exists() && f.lastModified() >= apkTime) {
                BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply {
                    inPreferredConfig = if (banner) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
                })?.let { return Icon(it, banner) }
            }
        }
        if (info == null) return null
        val banner = loadBanner(info)
        val icon = if (banner != null) Icon(banner, true) else Icon(render(info.loadIcon(pm), iconSize, iconSize, Bitmap.Config.ARGB_8888, false), false)
        file(key, !icon.isBanner).delete()
        FileOutputStream(file(key, icon.isBanner)).use {
            @Suppress("DEPRECATION")
            icon.bitmap.compress(Bitmap.CompressFormat.WEBP, 90, it)
        }
        return icon
    }

    private fun loadBanner(info: ActivityInfo): Bitmap? {
        val id = if (info.banner != 0) info.banner else info.applicationInfo.banner
        if (id == 0) return null
        val appRes = pm.getResourcesForApplication(info.applicationInfo)
        // Bitmap: decodifica já subamostrado para não alocar o banner em resolução total.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(appRes, id, bounds)
        if (bounds.outWidth > 0) {
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= bannerW && bounds.outHeight / (sample * 2) >= bannerH) sample *= 2
            val src = BitmapFactory.decodeResource(appRes, id, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            val out = render(BitmapDrawable(res, src), bannerW, bannerH, Bitmap.Config.RGB_565, true)
            src.recycle()
            return out
        }
        // Vetor/XML: desenha direto no tamanho final.
        val d = info.loadBanner(pm) ?: return null
        return render(d, bannerW, bannerH, Bitmap.Config.RGB_565, true)
    }

    private fun render(d: Drawable, w: Int, h: Int, cfg: Bitmap.Config, crop: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, cfg)
        val iw = d.intrinsicWidth.takeIf { it > 0 } ?: w
        val ih = d.intrinsicHeight.takeIf { it > 0 } ?: h
        val s = if (crop) maxOf(w.toFloat() / iw, h.toFloat() / ih) else minOf(w.toFloat() / iw, h.toFloat() / ih)
        val dw = (iw * s).toInt()
        val dh = (ih * s).toInt()
        val l = (w - dw) / 2
        val t = (h - dh) / 2
        d.setBounds(l, t, l + dw, t + dh)
        d.draw(Canvas(bmp))
        return bmp
    }

    private fun file(key: String, banner: Boolean) = File(dir, fileId(key) + if (banner) "_b.webp" else "_i.webp")

    private fun fileId(key: String) = key.replace('/', '#')
}

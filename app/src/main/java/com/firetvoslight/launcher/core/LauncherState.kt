package com.firetvoslight.launcher.core

import java.text.Collator
import java.util.Locale

// Lógica pura (sem Android) para ser testada na JVM.

data class AppEntry(val packageName: String, val activityName: String, val label: String) {
    val key: String get() = "$packageName/$activityName"
}

enum class Section { FAVORITES, APPS }

data class Arranged(val favorites: List<AppEntry>, val apps: List<AppEntry>, val hiddenCount: Int)

class LauncherState(
    favorites: List<String> = emptyList(),
    order: List<String> = emptyList(),
    hidden: Set<String> = emptySet(),
) {
    private val fav = favorites.distinct().toMutableList()
    private val ord = order.distinct().toMutableList()
    private val hid = hidden.toMutableSet()

    val favorites: List<String> get() = fav
    val order: List<String> get() = ord
    val hidden: Set<String> get() = hid

    fun isFavorite(key: String) = key in fav

    /** Favoritos na ordem do usuário; demais apps na ordem salva e, depois, os novos em ordem alfabética. */
    fun arrange(installed: List<AppEntry>): Arranged {
        val byKey = LinkedHashMap<String, AppEntry>()
        for (e in installed) if (e.key !in byKey) byKey[e.key] = e
        val favs = fav.mapNotNull { k -> if (k in hid) null else byKey[k] }
        val favKeys = favs.mapTo(HashSet()) { it.key }
        val rank = HashMap<String, Int>()
        ord.forEachIndexed { i, k -> rank[k] = i }
        val rest = byKey.values.filter { it.key !in favKeys && it.key !in hid }
        val known = rest.filter { it.key in rank }.sortedBy { rank.getValue(it.key) }
        val unknown = rest.filter { it.key !in rank }.sortedWith(LABEL_ORDER)
        return Arranged(favs, known + unknown, byKey.keys.count { it in hid })
    }

    /** Retorna true se o app passou a ser favorito. */
    fun toggleFavorite(key: String): Boolean {
        if (fav.remove(key)) return false
        fav.add(key)
        hid.remove(key)
        return true
    }

    fun hide(key: String) {
        hid.add(key)
        fav.remove(key)
    }

    fun unhideAll(): Boolean {
        if (hid.isEmpty()) return false
        hid.clear()
        return true
    }

    fun resetOrder() = ord.clear()

    /**
     * Move [key] por [delta] posições dentro da lista visível da seção.
     * Retorna o novo índice, ou -1 se a chave não estiver visível.
     */
    fun move(section: Section, visibleKeys: List<String>, key: String, delta: Int): Int {
        val list = visibleKeys.toMutableList()
        val i = list.indexOf(key)
        if (i < 0) return -1
        val j = (i + delta).coerceIn(0, list.size - 1)
        if (i == j) return i
        list.removeAt(i)
        list.add(j, key)
        val target = if (section == Section.FAVORITES) fav else ord
        val rest = target.filter { it !in list }
        target.clear()
        target.addAll(list)
        target.addAll(rest)
        return j
    }

    /** Esquece um pacote desinstalado definitivamente. Retorna true se algo mudou. */
    fun forgetPackage(pkg: String): Boolean {
        val prefix = "$pkg/"
        val a = fav.removeAll { it.startsWith(prefix) }
        val b = ord.removeAll { it.startsWith(prefix) }
        val c = hid.removeAll { it.startsWith(prefix) }
        return a || b || c
    }

    companion object {
        private val collator: Collator = Collator.getInstance(Locale("pt", "BR")).apply { strength = Collator.PRIMARY }
        val LABEL_ORDER: Comparator<AppEntry> = Comparator { x, y ->
            val c = collator.compare(x.label, y.label)
            if (c != 0) c else x.key.compareTo(y.key)
        }
    }
}

object PrefsCodec {
    fun encode(keys: Collection<String>): String = keys.joinToString("\n")
    fun decode(raw: String?): List<String> =
        raw?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()

    /** Lista de apps em cache (pacote\tatividade\tnome) para desenhar a tela antes de consultar o sistema. */
    fun encodeApps(apps: List<AppEntry>): String =
        apps.joinToString("\n") { "${it.packageName}\t${it.activityName}\t${it.label.replace('\t', ' ').replace('\n', ' ')}" }

    fun decodeApps(raw: String?): List<AppEntry> =
        raw?.split('\n')?.mapNotNull { line ->
            val p = line.split('\t')
            if (p.size == 3 && p[0].isNotEmpty() && p[1].isNotEmpty()) AppEntry(p[0], p[1], p[2]) else null
        } ?: emptyList()
}

enum class KeyAction { UP, DOWN, LEFT, RIGHT, SELECT, OPTIONS, BACK, NONE }

object KeyMap {
    // Valores de android.view.KeyEvent, repetidos para manter este arquivo livre de Android.
    fun map(keyCode: Int): KeyAction = when (keyCode) {
        19 -> KeyAction.UP
        20 -> KeyAction.DOWN
        21 -> KeyAction.LEFT
        22 -> KeyAction.RIGHT
        23, 66, 160 -> KeyAction.SELECT // DPAD_CENTER, ENTER, NUMPAD_ENTER
        82 -> KeyAction.OPTIONS // MENU (☰ no controle Fire TV)
        4, 111 -> KeyAction.BACK // BACK, ESCAPE
        else -> KeyAction.NONE
    }

    fun moveDelta(action: KeyAction, section: Section, columns: Int): Int = when (action) {
        KeyAction.LEFT -> -1
        KeyAction.RIGHT -> 1
        KeyAction.UP -> if (section == Section.APPS) -columns else 0
        KeyAction.DOWN -> if (section == Section.APPS) columns else 0
        else -> 0
    }
}

/** Apps de sistema da Amazon ocultados por padrão no primeiro uso (reversível em Ajustes). */
object DefaultHidden {
    val PACKAGES = setOf(
        "com.amazon.tv.launcher",            // Início (Home da Amazon)
        "com.amazon.gamehub",                // Game Center
        "com.amazon.tahoe",                  // Amazon Kids
        "com.amazon.shoptv.firetv.client",   // Live Shopping
        "com.amazon.minitv.android.app",     // miniTV
        "com.amazon.bueller.music",          // Amazon Music
        "com.amazon.bueller.photos",         // Amazon Photos
        "com.amazon.gallery.thor",           // Amazon Photos (galeria)
        "com.amazon.hedwig",                 // Notícias
        "com.amazon.spiderpork",             // Monitor do estado do sistema
        "com.amazon.ssm",                    // Painel de controle do sistema
        "com.amazon.vizzini",                // What Should I Watch
        "com.amazon.vizzini.ftvcds",         // What Should I Watch (stub)
        "com.amazon.ftv.profilepicker",      // Interface do perfil
        "com.amazon.ftv.screensaver",        // Protetor de tela
        "com.amazon.firetv.troubleshooting", // Solução de problemas
        "com.amazon.cloud9",                 // Navegador Silk
        "com.android.providers.downloads",   // Downloads
        "com.android.providers.downloads.ui", // Downloads (UI)
    )
}

/** Correções de componente de lançamento para apps cuja atividade resolvida não abre. */
object LaunchFix {
    val BY_PACKAGE = mapOf(
        "com.amazon.venezia" to "com.amazon.venezia.grid.AppsGridLauncherActivity",
    )
}

object DefaultFavorites {
    // Pacotes conhecidos de streaming no Fire TV; só entram se estiverem instalados.
    val KNOWN = listOf(
        "com.netflix.ninja",
        "com.amazon.firebat",
        "com.amazon.avod",
        "com.amazon.firetv.youtube",
        "com.google.android.youtube.tv",
        "com.globo.globotv",
        "com.disney.disneyplus",
        "com.wbd.stream",
        "com.hbo.hbonow",
        "com.spotify.tv.android",
    )

    fun pick(installed: List<AppEntry>, max: Int = 6): List<AppEntry> {
        val byPkg = installed.groupBy { it.packageName }
        val out = ArrayList<AppEntry>()
        for (p in KNOWN) {
            if (out.size >= max) break
            byPkg[p]?.firstOrNull()?.let { out.add(it) }
        }
        // Prime Video aparece com dois pacotes em algumas versões; mantém só um.
        if (out.any { it.packageName == "com.amazon.firebat" }) out.removeAll { it.packageName == "com.amazon.avod" }
        return out
    }
}

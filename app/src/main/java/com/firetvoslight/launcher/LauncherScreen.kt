package com.firetvoslight.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.firetvoslight.launcher.core.AppEntry
import com.firetvoslight.launcher.core.Arranged
import com.firetvoslight.launcher.core.KeyAction
import com.firetvoslight.launcher.core.KeyMap
import com.firetvoslight.launcher.core.Section
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A tela do launcher (cabeçalho, favoritos, grade). Usada pela MainActivity e pela cortina do botão Home;
 * o que muda entre as duas é o [Host] (abrir app, menus).
 */
class LauncherScreen(private val ctx: Context, private val host: Host) : LauncherRepo.Listener {

    interface Host {
        fun launch(e: AppEntry)
        fun options(section: Section, e: AppEntry)
        fun settings()
    }

    private val repo = LauncherRepo
    private val dm = ctx.resources.displayMetrics
    private val dp = dm.density
    val columns = if (dm.widthPixels / dp < 700) 4 else COLUMNS
    private val gap = (GAP_DP * dp).toInt()
    private val pad = (PAD_DP * dp).toInt()
    private val gridW = (dm.widthPixels - 2 * pad - (columns - 1) * gap) / columns
    private val favW = (dm.widthPixels - 2 * pad - 3 * gap) / 4
    private val icons = repo.icons(favW, favW * 9 / 16)

    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var favScroll: HorizontalScrollView
    private lateinit var favRow: LinearLayout
    private lateinit var favEmpty: TextView
    private lateinit var grid: GridLayout
    private lateinit var appsEmpty: TextView
    val root: View = build()

    var arranged = Arranged(emptyList(), emptyList(), 0)
        private set
    private val tiles = HashMap<String, TileView>()
    private var moving: Pair<Section, String>? = null
    val isMoving get() = moving != null
    val isEmpty get() = tiles.isEmpty()

    private val clockFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))

    // ---------- construção ----------

    private fun build(): View {
        val root = FrameLayout(ctx)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(0x1C, 0x1F, 0x29), Color.rgb(0x0B, 0x0B, 0x0F)),
        )
        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            clipChildren = false
            clipToPadding = false
            isVerticalScrollBarEnabled = false
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(pad, (28 * dp).toInt(), pad, (40 * dp).toInt())
        }

        val header = LinearLayout(ctx).apply { gravity = Gravity.BOTTOM }
        val title = text(ctx.getString(R.string.app_name), 22f, Typeface.create("sans-serif-medium", Typeface.NORMAL), 0.9f)
        header.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        val clockBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
        clock = text("", 30f, Typeface.create("sans-serif-light", Typeface.NORMAL), 1f).apply { gravity = Gravity.END }
        date = text("", 13f, Typeface.DEFAULT, 0.6f).apply { gravity = Gravity.END }
        clockBox.addView(clock)
        clockBox.addView(date)
        header.addView(clockBox)
        col.addView(header)

        col.addView(sectionLabel(R.string.favorites))
        favScroll = HorizontalScrollView(ctx).apply {
            clipChildren = false
            clipToPadding = false
            isHorizontalScrollBarEnabled = false
            // Folga para o zoom do foco não ser cortado.
            setPadding(0, (12 * dp).toInt(), 0, (4 * dp).toInt())
        }
        favRow = LinearLayout(ctx).apply { clipChildren = false; clipToPadding = false }
        favScroll.addView(favRow)
        col.addView(favScroll)
        favEmpty = text(ctx.getString(R.string.favorites_empty), 14f, Typeface.DEFAULT, 0.55f).apply {
            setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
            visibility = View.GONE
        }
        col.addView(favEmpty)

        col.addView(sectionLabel(R.string.apps))
        grid = GridLayout(ctx).apply {
            columnCount = columns
            clipChildren = false
            clipToPadding = false
            setPadding(0, (12 * dp).toInt(), 0, 0)
        }
        col.addView(grid)
        appsEmpty = text(ctx.getString(R.string.apps_empty), 14f, Typeface.DEFAULT, 0.55f).apply { visibility = View.GONE }
        col.addView(appsEmpty)
        col.addView(text(ctx.getString(R.string.hint), 12f, Typeface.DEFAULT, 0.4f).apply {
            setPadding(0, (16 * dp).toInt(), 0, 0)
        })

        scroll.addView(col)
        root.addView(scroll)
        return root
    }

    private fun text(s: String, sp: Float, tf: Typeface, a: Float) = TextView(ctx).apply {
        text = s
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        typeface = tf
        alpha = a
    }

    private fun sectionLabel(res: Int) =
        text(ctx.getString(res), 15f, Typeface.create("sans-serif-medium", Typeface.NORMAL), 0.7f).apply {
            setPadding(0, (22 * dp).toInt(), 0, 0)
        }

    fun updateClock() {
        val now = Date()
        clock.text = clockFmt.format(now)
        date.text = dateFmt.format(now).replaceFirstChar { it.uppercase() }
    }

    // ---------- dados ----------

    override fun onRepo(listChanged: Boolean) {
        if (listChanged || tiles.isEmpty()) render(focusedTag())
        // Ícones sem cache em disco (ou desatualizados) agora podem ser carregados com o ActivityInfo.
        for (t in tiles.values) {
            val key = t.boundKey ?: continue
            if (!icons.hasIcon(key)) icons.bind(key, repo.infos[key], t)
        }
    }

    override fun onIconsCleared() {
        tiles.values.forEach { it.clearIcon() }
    }

    fun rebindIcons() {
        for (i in 0 until favRow.childCount) rebind(favRow.getChildAt(i))
        for (i in 0 until grid.childCount) rebind(grid.getChildAt(i))
    }

    private fun rebind(v: View) {
        @Suppress("UNCHECKED_CAST")
        val tag = v.tag as? Pair<Section, String> ?: return
        icons.bind(tag.second, repo.infos[tag.second], v as TileView)
    }

    fun render(focus: Pair<Section, String>?) {
        arranged = repo.state.arrange(repo.installed)
        tiles.clear()
        favRow.removeAllViews()
        grid.removeAllViews()
        for (e in arranged.favorites) addTile(Section.FAVORITES, e)
        for (e in arranged.apps) addTile(Section.APPS, e)
        addSettingsTile()

        val noFav = arranged.favorites.isEmpty()
        favScroll.visibility = if (noFav) View.GONE else View.VISIBLE
        favEmpty.visibility = if (noFav) View.VISIBLE else View.GONE
        appsEmpty.visibility = if (arranged.apps.isEmpty()) View.VISIBLE else View.GONE
        focus(focus)
    }

    /** Foca o item [tag]; sem ele (ou se sumiu), o primeiro favorito. */
    fun focus(tag: Pair<Section, String>?) {
        val target = tag?.let { tiles[id(it.first, it.second)] } ?: favRow.getChildAt(0) ?: grid.getChildAt(0)
        target?.requestFocus()
    }

    fun focusedTag(): Pair<Section, String>? {
        @Suppress("UNCHECKED_CAST")
        return root.findFocus()?.tag as? Pair<Section, String>
    }

    private fun addTile(section: Section, e: AppEntry) {
        val big = section == Section.FAVORITES
        val w = if (big) favW else gridW
        val t = TileView(ctx, e.label, w, w * 9 / 16, dp)
        t.contentDescription = ctx.getString(if (big) R.string.cd_favorite else R.string.cd_app, e.label)
        t.tag = section to e.key
        t.setOnClickListener { host.launch(e) }
        t.setOnLongClickListener { host.options(section, e); true }
        icons.bind(e.key, repo.infos[e.key], t)
        tiles[id(section, e.key)] = t
        if (big) {
            favRow.addView(t, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = gap })
        } else {
            grid.addView(t, GridLayout.LayoutParams().apply { setMargins(0, 0, gap, gap) })
        }
    }

    private fun addSettingsTile() {
        val label = ctx.getString(R.string.settings)
        val t = TileView(ctx, label, gridW, gridW * 9 / 16, dp)
        t.contentDescription = label
        @Suppress("DEPRECATION")
        t.setIcon(ctx.resources.getDrawable(android.R.drawable.ic_menu_preferences), false)
        t.setOnClickListener { host.settings() }
        grid.addView(t, GridLayout.LayoutParams().apply { setMargins(0, 0, gap, gap) })
    }

    // ---------- teclas / mover ----------

    /** Opções (☰) no item focado. Retorna true se a tecla foi tratada. */
    fun handleOptionsKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val tag = focusedTag()
            val e = tag?.let { t -> repo.installed.firstOrNull { it.key == t.second } }
            if (tag != null && e != null) host.options(tag.first, e)
        }
        return true
    }

    fun startMove(section: Section, key: String) {
        moving = section to key
        tiles[id(section, key)]?.setMoving(true)
    }

    fun finishMove() {
        val m = moving ?: return
        moving = null
        tiles[id(m.first, m.second)]?.setMoving(false)
        repo.store.save(repo.state)
    }

    /** Em modo mover, consome todas as teclas. */
    fun handleMoveKey(event: KeyEvent): Boolean {
        val m = moving ?: return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (val action = KeyMap.map(event.keyCode)) {
                KeyAction.SELECT, KeyAction.BACK, KeyAction.OPTIONS -> finishMove()
                else -> {
                    val d = KeyMap.moveDelta(action, m.first, columns)
                    if (d != 0) moveBy(m, d)
                }
            }
        }
        return true
    }

    private fun moveBy(m: Pair<Section, String>, delta: Int) {
        val (section, key) = m
        val parent: ViewGroup = if (section == Section.FAVORITES) favRow else grid
        val visible = (if (section == Section.FAVORITES) arranged.favorites else arranged.apps).map { it.key }
        val from = visible.indexOf(key)
        val to = repo.state.move(section, visible, key, delta)
        if (to < 0 || to == from) return
        arranged = repo.state.arrange(repo.installed)
        val tile = tiles[id(section, key)] ?: return
        // Reposiciona só a view movida em vez de reconstruir a tela.
        parent.removeView(tile)
        parent.addView(tile, to, tile.layoutParams)
        tile.requestFocus()
        tile.setMoving(true)
    }

    private fun id(s: Section, key: String) = "${s.name}:$key"

    private companion object {
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val COLUMNS = 5
        const val PAD_DP = 56
        const val GAP_DP = 22
    }
}

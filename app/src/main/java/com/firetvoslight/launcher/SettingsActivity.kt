package com.firetvoslight.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.firetvoslight.launcher.core.AppEntry
import com.firetvoslight.launcher.core.KeyAction
import com.firetvoslight.launcher.core.KeyMap

/**
 * Ajustes em tela cheia, estilo Apple TV: título grande, lista de linhas com realce translúcido,
 * subtelas que entram da direita. Navegação só com setas / OK / Voltar.
 */
class SettingsActivity : Activity() {

    private data class Panel(val title: String, val build: (LinearLayout) -> Unit)

    private val repo = LauncherRepo
    private val main = Handler(Looper.getMainLooper())
    private lateinit var memory: MemoryManager
    private var dp = 1f
    private lateinit var titleView: TextView
    private lateinit var listHolder: FrameLayout
    private val stack = ArrayDeque<Panel>()
    private var current: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo.init(this)
        memory = MemoryManager(this)
        dp = resources.displayMetrics.density
        setContentView(buildRoot())
        push(mainPanel())
    }

    override fun onResume() {
        super.onResume()
        repo.reload()
    }

    private fun buildRoot(): View {
        val root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(0x16, 0x18, 0x20), Color.rgb(0x08, 0x08, 0x0C)),
        )
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((96 * dp).toInt(), (64 * dp).toInt(), (96 * dp).toInt(), (48 * dp).toInt())
        }
        titleView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setPadding(0, 0, 0, (28 * dp).toInt())
        }
        col.addView(titleView)
        listHolder = FrameLayout(this)
        col.addView(listHolder, LinearLayout.LayoutParams(MATCH, MATCH))
        root.addView(col)
        return root
    }

    // ---------- pilha de painéis ----------

    private fun push(p: Panel) {
        stack.addLast(p)
        show(p, enter = true)
    }

    private fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeLast()
        show(stack.last(), enter = false)
        return true
    }

    private fun show(p: Panel, enter: Boolean) {
        titleView.text = p.title
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isFillViewport = true
            clipChildren = false
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            // Largura confortável de coluna, estilo Apple TV (não ocupa a tela toda).
            layoutParams = FrameLayout.LayoutParams((760 * dp).toInt(), WRAP)
        }
        p.build(list)
        scroll.addView(list)
        listHolder.removeAllViews()
        listHolder.addView(scroll)
        current = scroll
        list.getChildAt(0)?.requestFocus()
        scroll.translationX = if (enter) 40 * dp else -40 * dp
        scroll.alpha = 0f
        scroll.animate().translationX(0f).alpha(1f).setDuration(180).start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyMap.map(keyCode) == KeyAction.BACK) {
            if (pop()) return true
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ---------- painéis ----------

    private fun row(title: String, value: String? = null, subtitle: String? = null, accent: Boolean = false, onClick: (SettingRow) -> Unit): SettingRow =
        SettingRow(this, dp, title, value, subtitle, accent).apply {
            setOnClickListener { onClick(this) }
        }

    private fun add(list: LinearLayout, r: View) {
        list.addView(r, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = (4 * dp).toInt() })
    }

    private fun sectionHeader(list: LinearLayout, text: String) {
        val t = TextView(this).apply {
            this.text = text.uppercase()
            setTextColor(0xFF6B7280.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.08f
            setPadding((22 * dp).toInt(), (22 * dp).toInt(), 0, (6 * dp).toInt())
        }
        list.addView(t)
    }

    private fun mainPanel() = Panel(getString(R.string.settings)) { list ->
        add(list, row(getString(R.string.set_memory), subtitle = getString(R.string.set_memory_sub)) { push(memoryPanel()) })
        add(list, row(getString(R.string.set_apps), subtitle = getString(R.string.set_apps_sub)) { push(appsPanel()) })
        add(list, row(getString(R.string.set_home), value = homeStatus(), accent = true) { push(homePanel()) })
        add(list, row(getString(R.string.settings_fire)) {
            openSafely(Intent(Settings.ACTION_SETTINGS))
        })
        add(list, row(getString(R.string.set_about), value = "v" + BuildInfo.version(this)) { push(aboutPanel()) })
    }

    private fun memoryPanel() = Panel(getString(R.string.set_memory)) { list ->
        val freeRow = row(getString(R.string.settings_free_now), value = "${memory.availableMb()} MB") { r ->
            val before = memory.availableMb()
            val n = memory.sweep(repo.store.aggressiveFree)
            main.postDelayed({
                r.setValue("${memory.availableMb()} MB")
                Toast.makeText(this, getString(R.string.freed, n, maxOf(0, memory.availableMb() - before)), Toast.LENGTH_SHORT).show()
            }, 700)
        }
        add(list, freeRow)
        add(list, toggle(getString(R.string.set_autofree), getString(R.string.set_autofree_sub), repo.store.autoFree) {
            repo.store.autoFree = it
        })
        add(list, toggle(getString(R.string.set_aggressive), getString(R.string.set_aggressive_sub), repo.store.aggressiveFree) {
            repo.store.aggressiveFree = it
        })
    }

    private fun appsPanel() = Panel(getString(R.string.set_apps)) { list ->
        val hidden = repo.state.hidden.size
        if (hidden > 0) {
            add(list, row(getString(R.string.set_unhide), value = hidden.toString()) {
                repo.state.unhideAll(); repo.saveAndNotify()
                Toast.makeText(this, R.string.set_unhide, Toast.LENGTH_SHORT).show()
            })
        }
        add(list, row(getString(R.string.set_reset_order)) {
            repo.state.resetOrder(); repo.saveAndNotify()
            Toast.makeText(this, R.string.set_reset_order, Toast.LENGTH_SHORT).show()
        })
        sectionHeader(list, getString(R.string.set_apps_hint))
        val apps = repo.installed.sortedBy { it.label.lowercase() }
        if (apps.isEmpty()) {
            add(list, infoRow(getString(R.string.apps_empty)))
            return@Panel
        }
        for (e in apps) {
            add(list, row(e.label, subtitle = e.packageName) { confirmUninstall(e) })
        }
    }

    private fun homePanel() = Panel(getString(R.string.set_home)) { list ->
        sectionHeader(list, getString(R.string.set_home_status))
        val active = HomeRedirectService.isActive
        add(list, infoRow(if (active) getString(R.string.set_home_on) else getString(R.string.set_home_off)))
        if (!active) {
            add(list, row(getString(R.string.set_home_reactivate), subtitle = getString(R.string.set_home_reactivate_sub)) {
                openSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
        }
        add(list, row(getString(R.string.settings_amazon_home)) {
            HomeRedirectService.bypass = true
            openSafely(Intent(Intent.ACTION_MAIN).setClassName(HomeRedirectService.AMAZON_HOME, "com.amazon.tv.launcher.ui.HomeActivity_vNext"))
        })
    }

    private fun aboutPanel() = Panel(getString(R.string.set_about)) { list ->
        add(list, infoRow(getString(R.string.app_name) + "  ·  v" + BuildInfo.version(this)))
        add(list, infoRow(getString(R.string.set_about_text)))
        if (repo.store.homeLockDesired && !HomeRedirectService.isActive) {
            add(list, infoRow(getString(R.string.set_home_off)))
        }
    }

    // ---------- helpers ----------

    private fun toggle(title: String, sub: String, initial: Boolean, onSet: (Boolean) -> Unit): SettingRow {
        var on = initial
        lateinit var r: SettingRow
        r = SettingRow(this, dp, title, value = valueOf(on), subtitle = sub, accentValue = true).apply {
            setOnClickListener {
                on = !on
                onSet(on)
                r.setValue(valueOf(on))
            }
        }
        return r
    }

    private fun valueOf(on: Boolean) = getString(if (on) R.string.on else R.string.off)

    private fun infoRow(text: String): View = TextView(this).apply {
        this.text = text
        setTextColor(0xFFB8BDC7.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setLineSpacing(4 * dp, 1f)
        setPadding((22 * dp).toInt(), (14 * dp).toInt(), (22 * dp).toInt(), (14 * dp).toInt())
    }

    private fun homeStatus() =
        if (HomeRedirectService.isActive) getString(R.string.app_name) else getString(R.string.set_home_amazon_now)

    private fun confirmUninstall(e: AppEntry) {
        // Pede a desinstalação ao sistema: a própria tela da Amazon confirma antes de remover (reversível pela loja).
        try {
            startActivity(
                Intent(Intent.ACTION_DELETE, Uri.fromParts("package", e.packageName, null))
                    .putExtra(Intent.EXTRA_RETURN_RESULT, false)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (t: Throwable) {
            Toast.makeText(this, getString(R.string.launch_failed, e.label), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSafely(i: Intent) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}

object BuildInfo {
    fun version(ctx: android.content.Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (_: Throwable) {
        "?"
    }
}

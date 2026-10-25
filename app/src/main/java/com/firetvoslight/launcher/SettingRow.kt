package com.firetvoslight.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Linha de ajuste estilo Apple TV: título + valor/subtítulo, realce translúcido e leve escala no foco. */
class SettingRow(
    ctx: Context,
    private val dp: Float,
    title: String,
    value: String? = null,
    subtitle: String? = null,
    private val accentValue: Boolean = false,
) : FrameLayout(ctx) {

    private val fill = GradientDrawable().apply {
        cornerRadius = 14 * dp
        setColor(Color.TRANSPARENT)
    }
    private val valueView: TextView

    init {
        background = fill
        outlineProvider = ViewOutlineProvider.BACKGROUND
        isFocusable = true
        isFocusableInTouchMode = false
        val padH = (22 * dp).toInt()
        val padV = (if (subtitle != null) 14 else 18) * dp
        setPadding(padH, padV.toInt(), padH, padV.toInt())

        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 17f, Color.WHITE, Typeface.create("sans-serif", Typeface.NORMAL)))
        if (subtitle != null) {
            texts.addView(
                label(subtitle, 13f, 0xFF9AA0AA.toInt(), Typeface.DEFAULT),
                LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = (2 * dp).toInt() },
            )
        }
        row.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))

        valueView = label(value ?: "", 15f, if (accentValue) ACCENT else 0xFF8A909A.toInt(), Typeface.DEFAULT)
        valueView.gravity = Gravity.END
        valueView.visibility = if (value == null) View.GONE else View.VISIBLE
        row.addView(valueView, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = (16 * dp).toInt() })
        addView(row, LayoutParams(MATCH, WRAP))

        pivotX = 0f
        setOnFocusChangeListener { _, f -> onFocus(f) }
    }

    fun setValue(v: String?) {
        valueView.text = v ?: ""
        valueView.visibility = if (v == null) View.GONE else View.VISIBLE
        valueView.setTextColor(if (accentValue) ACCENT else 0xFF8A909A.toInt())
    }

    private fun onFocus(focused: Boolean) {
        fill.setColor(if (focused) 0x1FFFFFFF else Color.TRANSPARENT)
        animate().scaleX(if (focused) FOCUS else 1f).scaleY(if (focused) FOCUS else 1f)
            .setDuration(120).withLayer().start()
    }

    private fun label(s: String, sp: Float, color: Int, tf: Typeface) = TextView(context).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        typeface = tf
        maxLines = 1
    }

    private companion object {
        const val MATCH = LayoutParams.MATCH_PARENT
        const val WRAP = LayoutParams.WRAP_CONTENT
        const val FOCUS = 1.02f
        val ACCENT = Color.rgb(0x4D, 0xA3, 0xFF)
    }
}

package com.firetvoslight.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Cartão 16:9 com legenda que aparece no foco (estilo "TV shelf"). */
class TileView(
    ctx: Context,
    label: String,
    private val cardW: Int,
    private val cardH: Int,
    private val dp: Float,
) : LinearLayout(ctx) {

    var boundKey: String? = null

    private val card = FrameLayout(ctx)
    private val image = ImageView(ctx)
    private val inner = TextView(ctx)
    private val caption = TextView(ctx)
    private val border = GradientDrawable().apply {
        cornerRadius = RADIUS * dp
        setColor(Color.TRANSPARENT)
    }
    private var moving = false
    private var showCaption = true

    init {
        orientation = VERTICAL
        isFocusable = true
        clipChildren = false
        clipToPadding = false

        card.background = GradientDrawable().apply {
            cornerRadius = RADIUS * dp
            setColor(PALETTE[(label.hashCode() and 0x7fffffff) % PALETTE.size])
        }
        card.outlineProvider = ViewOutlineProvider.BACKGROUND
        card.clipToOutline = true
        card.foreground = border

        card.addView(image, FrameLayout.LayoutParams(MATCH, MATCH))
        inner.text = label
        inner.setTextColor(Color.WHITE)
        inner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        inner.gravity = Gravity.CENTER
        inner.maxLines = 1
        inner.ellipsize = TextUtils.TruncateAt.END
        inner.setPadding((6 * dp).toInt(), 0, (6 * dp).toInt(), (6 * dp).toInt())
        inner.visibility = View.GONE
        card.addView(inner, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
        addView(card, LayoutParams(cardW, cardH))

        caption.text = label
        caption.setTextColor(Color.WHITE)
        caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        caption.gravity = Gravity.CENTER
        caption.maxLines = 1
        caption.ellipsize = TextUtils.TruncateAt.END
        caption.alpha = 0f
        addView(caption, LayoutParams(cardW, WRAP).apply { topMargin = (10 * dp).toInt() })

        pivotX = cardW / 2f
        pivotY = cardH / 2f
        setOnFocusChangeListener { _, focused -> onFocus(focused) }
    }

    fun setIcon(d: Drawable, isBanner: Boolean) {
        image.setImageDrawable(d)
        val lp = image.layoutParams as FrameLayout.LayoutParams
        if (isBanner) {
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            lp.width = MATCH; lp.height = MATCH; lp.gravity = Gravity.NO_GRAVITY; lp.bottomMargin = 0
            inner.visibility = View.GONE
            showCaption = true
        } else {
            // O nome já aparece dentro do cartão.
            showCaption = false
            caption.alpha = 0f
            val s = (cardH * 0.48f).toInt()
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            lp.width = s; lp.height = s; lp.gravity = Gravity.CENTER; lp.bottomMargin = (cardH * 0.14f).toInt()
            inner.visibility = View.VISIBLE
        }
        image.layoutParams = lp
    }

    fun clearIcon() {
        image.setImageDrawable(null)
        boundKey = null
    }

    fun setMoving(m: Boolean) {
        moving = m
        updateBorder(hasFocus())
    }

    private fun onFocus(focused: Boolean) {
        val s = if (focused) FOCUS_SCALE else 1f
        // withLayer: o zoom é feito sobre uma textura, sem redesenhar o cartão a cada quadro.
        animate().scaleX(s).scaleY(s).setDuration(ANIM_MS).withLayer().start()
        if (showCaption) caption.alpha = if (focused) 1f else 0f
        translationZ = if (focused) 1f else 0f
        updateBorder(focused)
    }

    private fun updateBorder(focused: Boolean) {
        when {
            moving -> border.setStroke((4 * dp).toInt(), ACCENT)
            focused -> border.setStroke((3 * dp).toInt(), Color.WHITE)
            else -> border.setStroke(0, Color.TRANSPARENT)
        }
    }

    private companion object {
        const val MATCH = LayoutParams.MATCH_PARENT
        const val WRAP = LayoutParams.WRAP_CONTENT
        const val RADIUS = 10f
        const val FOCUS_SCALE = 1.1f
        const val ANIM_MS = 130L
        val ACCENT = Color.rgb(0x4D, 0xA3, 0xFF)
        val PALETTE = intArrayOf(
            Color.rgb(0x2A, 0x2D, 0x3A), Color.rgb(0x28, 0x3B, 0x4A), Color.rgb(0x3A, 0x2B, 0x3F),
            Color.rgb(0x2F, 0x3E, 0x35), Color.rgb(0x40, 0x36, 0x2A), Color.rgb(0x2B, 0x35, 0x50),
        )
    }
}

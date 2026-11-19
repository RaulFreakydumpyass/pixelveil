package com.pixelveil.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.view.View

/** Draws the pixel pattern 1:1 with screen pixels, clipped to the chosen shape. */
class PixelMaskView(ctx: Context) : View(ctx) {
    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }
    private var s = MaskSettings()
    private var tileKey = ""
    private var tile: Bitmap? = null

    fun setMask(ns: MaskSettings) {
        val key = "${ns.pattern}|${ns.coverage}|${ns.cell}|${ns.strength}"
        if (key != tileKey || tile == null) {
            val t = PatternEngine.buildTile(ns.pattern, ns.coverage, ns.cell, ns.strength)
            tile = t
            tileKey = key
            paint.shader = BitmapShader(t, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        s = ns
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || tile == null) return
        val save = canvas.save()
        val path = ShapeFactory.build(s.shape, w, h, s.cx, s.cy, s.sw, s.sh)
        if (path != null) {
            if (s.invert) canvas.clipOutPath(path) else canvas.clipPath(path)
        } else if (s.invert) {
            canvas.restoreToCount(save)
            return
        }
        canvas.drawRect(0f, 0f, w, h, paint)
        canvas.restoreToCount(save)
    }
}

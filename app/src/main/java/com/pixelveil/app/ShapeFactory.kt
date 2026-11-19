package com.pixelveil.app

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object ShapeFactory {
    /** Returns the shape outline, or null for FULL (whole screen). Percent args are 0..100. */
    fun build(shape: ShapeType, w: Float, h: Float, cxP: Int, cyP: Int, swP: Int, shP: Int): Path? {
        if (shape == ShapeType.FULL) return null
        val bw = w * swP / 100f
        val bh = h * shP / 100f
        val cx = w * cxP / 100f
        val cy = h * cyP / 100f
        val l = cx - bw / 2f
        val t = cy - bh / 2f
        val r = cx + bw / 2f
        val b = cy + bh / 2f
        val p = Path()
        when (shape) {
            ShapeType.FULL -> Unit
            ShapeType.CIRCLE -> p.addCircle(cx, cy, minOf(bw, bh) / 2f, Path.Direction.CW)
            ShapeType.OVAL -> p.addOval(RectF(l, t, r, b), Path.Direction.CW)
            ShapeType.RECT -> p.addRect(l, t, r, b, Path.Direction.CW)
            ShapeType.ROUNDED -> {
                val rr = minOf(bw, bh) * 0.22f
                p.addRoundRect(RectF(l, t, r, b), rr, rr, Path.Direction.CW)
            }
            ShapeType.TRIANGLE -> {
                p.moveTo(cx, t); p.lineTo(r, b); p.lineTo(l, b); p.close()
            }
            ShapeType.DIAMOND -> {
                p.moveTo(cx, t); p.lineTo(r, cy); p.lineTo(cx, b); p.lineTo(l, cy); p.close()
            }
            ShapeType.HEXAGON -> {
                p.moveTo(l + bw * 0.25f, t)
                p.lineTo(r - bw * 0.25f, t)
                p.lineTo(r, cy)
                p.lineTo(r - bw * 0.25f, b)
                p.lineTo(l + bw * 0.25f, b)
                p.lineTo(l, cy)
                p.close()
            }
            ShapeType.STAR -> {
                val rx = bw / 2f
                val ry = bh / 2f
                for (i in 0 until 10) {
                    val ang = Math.toRadians(-90.0 + i * 36.0)
                    val k = if (i % 2 == 0) 1f else 0.42f
                    val x = cx + (rx * k * cos(ang)).toFloat()
                    val y = cy + (ry * k * sin(ang)).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
            }
            ShapeType.HEART -> {
                fun x(u: Float) = l + bw * u
                fun y(v: Float) = t + bh * v
                p.moveTo(x(0.5f), y(1.0f))
                p.cubicTo(x(0.0f), y(0.7f), x(0.0f), y(0.3f), x(0.25f), y(0.15f))
                p.cubicTo(x(0.4f), y(0.05f), x(0.5f), y(0.2f), x(0.5f), y(0.3f))
                p.cubicTo(x(0.5f), y(0.2f), x(0.6f), y(0.05f), x(0.75f), y(0.15f))
                p.cubicTo(x(1.0f), y(0.3f), x(1.0f), y(0.7f), x(0.5f), y(1.0f))
                p.close()
            }
            ShapeType.CROSS -> {
                p.addRect(cx - bw * 0.17f, t, cx + bw * 0.17f, b, Path.Direction.CW)
                p.addRect(l, cy - bh * 0.17f, r, cy + bh * 0.17f, Path.Direction.CW)
            }
            ShapeType.FRAME -> {
                p.fillType = Path.FillType.EVEN_ODD
                p.addRect(l, t, r, b, Path.Direction.CW)
                val inset = minOf(bw, bh) * 0.2f
                p.addRect(l + inset, t + inset, r - inset, b - inset, Path.Direction.CW)
            }
        }
        return p
    }
}

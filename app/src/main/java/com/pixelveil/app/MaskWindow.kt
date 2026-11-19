package com.pixelveil.app

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

/** Owns one full-screen, touch-through overlay window containing a PixelMaskView. */
class MaskWindow(private val ctx: Context, private val type: Int) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: PixelMaskView? = null

    val isShowing: Boolean get() = view != null

    fun show(s: MaskSettings): Boolean {
        var v = view
        if (v == null) {
            v = PixelMaskView(ctx)
            try {
                wm.addView(v, params())
            } catch (e: Exception) {
                return false
            }
            view = v
        }
        v.setMask(s)
        return true
    }

    fun hide() {
        view?.let { runCatching { wm.removeViewImmediate(it) } }
        view = null
    }

    private fun params(): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "PixelVeil"
            // Android 12+ blocks touches through other apps' overlays above 80% opacity.
            // Accessibility overlays are trusted, so they can be fully opaque.
            alpha = if (type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) 0.8f else 1f
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                fitInsetsTypes = 0
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}

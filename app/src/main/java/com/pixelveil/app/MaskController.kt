package com.pixelveil.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Single entry point: makes the real overlay match the saved settings. */
object MaskController {
    fun sync(ctx: Context) {
        val app = ctx.applicationContext
        val s = Prefs.load(app)
        val acc = PixelVeilAccessibility.instance
        val svc = Intent(app, OverlayService::class.java)

        if (!s.enabled) {
            acc?.hide()
            if (OverlayService.window != null) app.stopService(svc)
            return
        }
        if (s.useAccessibility && acc != null) {
            if (OverlayService.window != null) app.stopService(svc)
            acc.show(s)
            return
        }
        acc?.hide()
        if (!Settings.canDrawOverlays(app)) return
        val w = OverlayService.window
        if (w != null && w.isShowing) {
            w.show(s)
        } else {
            try {
                ContextCompat.startForegroundService(app, svc)
            } catch (e: Exception) {
                // Not allowed to start from the background right now; the app will retry when opened.
            }
        }
    }
}

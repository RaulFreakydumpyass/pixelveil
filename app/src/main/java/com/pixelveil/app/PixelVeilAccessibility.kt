package com.pixelveil.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Only used to host the overlay window above the status bar and navigation bar.
 * It never reads the screen (no events, no window content).
 */
class PixelVeilAccessibility : AccessibilityService() {

    companion object {
        var instance: PixelVeilAccessibility? = null
    }

    private var window: MaskWindow? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        MaskController.sync(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun show(s: MaskSettings): Boolean {
        val w = window ?: MaskWindow(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { window = it }
        return w.show(s)
    }

    fun hide() {
        window?.hide()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        window?.hide()
        window = null
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        window?.hide()
        window = null
        if (instance === this) instance = null
        super.onDestroy()
    }
}

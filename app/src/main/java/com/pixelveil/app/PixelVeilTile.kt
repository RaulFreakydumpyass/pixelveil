package com.pixelveil.app

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class PixelVeilTile : TileService() {

    override fun onStartListening() {
        refresh()
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        val s = Prefs.load(this)
        val canRun = if (s.useAccessibility && PixelVeilAccessibility.instance != null) true
        else Settings.canDrawOverlays(this)
        if (!s.enabled && !canRun) {
            startActivityAndCollapse(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        Prefs.save(this, s.copy(enabled = !s.enabled))
        MaskController.sync(this)
        refresh()
    }

    private fun refresh() {
        val t = qsTile ?: return
        t.state = if (Prefs.load(this).enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = "Pixels off"
        t.updateTile()
    }
}

package com.pixelveil.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.view.WindowManager

/** Foreground service that hosts the normal "display over other apps" overlay. */
class OverlayService : Service() {

    companion object {
        var window: MaskWindow? = null
        private const val CHANNEL = "pixelveil"
        private const val NOTIF_ID = 1
        const val ACTION_STOP = "com.pixelveil.app.STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.save(this, Prefs.load(this).copy(enabled = false))
            stopSelf()
            return START_NOT_STICKY
        }
        val s = Prefs.load(this)
        if (!s.enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val n = buildNotification(s)
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        val w = window ?: MaskWindow(this, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY).also { window = it }
        if (!w.show(s)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        window?.hide()
        window = null
        super.onDestroy()
    }

    private fun buildNotification(s: MaskSettings): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Pixel mask", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val action = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notification), "Turn off", stop
        ).build()
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pixels off: ${s.coverage}%")
            .setContentText("${s.pattern.label} · ${s.shape.label}")
            .setContentIntent(open)
            .addAction(action)
            .setOngoing(true)
            .build()
    }
}

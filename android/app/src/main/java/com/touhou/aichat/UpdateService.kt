package com.touhou.aichat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UpdateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildProgressNotification(0, "准备下载..."))
        val info = UpdateManager.pendingUpdate
        if (info != null) {
            scope.launch {
                UpdateManager.downloadAndInstall(this@UpdateService, info) { p ->
                    updateNotification(p)
                }
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "应用更新",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(p: UpdateManager.Progress) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (p.phase) {
            UpdateManager.Phase.DOWNLOADING -> {
                val percent = if (p.total > 0) ((p.downloaded * 100 / p.total)).toInt() else 0
                val text = "下载中 $percent%（${fmtMb(p.downloaded)} / ${fmtMb(p.total)}）"
                nm.notify(NOTIF_ID, buildProgressNotification(percent, text))
            }
            UpdateManager.Phase.PROCESSING -> {
                nm.notify(NOTIF_ID, buildProgressNotification(100, "校验中..."))
            }
            UpdateManager.Phase.DONE -> {
                nm.notify(NOTIF_ID, buildDoneNotification())
            }
            UpdateManager.Phase.ERROR -> {
                nm.notify(NOTIF_ID, buildErrorNotification(p.error))
            }
            else -> { /* ignore */ }
        }
    }

    private fun buildProgressNotification(percent: Int, text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("更新 ${UpdateManager.pendingUpdate?.versionName ?: ""}")
            .setContentText(text)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun buildDoneNotification(): Notification {
        val installIntent = Intent(this, InstallReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            this, 1, installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("更新下载完成")
            .setContentText("点击安装")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
    }

    private fun buildErrorNotification(error: String?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("更新失败")
            .setContentText(error ?: "未知错误")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun fmtMb(bytes: Long): String {
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    }

    companion object {
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIF_ID = 1001
    }
}

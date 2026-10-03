package com.touhou.aichat

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotifHelper {
    const val CHANNEL_ID = "dm_messages"
    const val RUNNING_CHANNEL_ID = "dm_running"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "私信通知", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "收到新私信时提醒"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            nm.createNotificationChannel(channel)
            val running = NotificationChannel(RUNNING_CHANNEL_ID, "私信提醒服务", NotificationManager.IMPORTANCE_MIN).apply {
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(running)
        }
    }

    fun buildRunningNotification(context: Context): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setContentTitle("私信提醒已开启")
            .setContentText("后台接收新私信通知")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    fun showDmNotification(context: Context, peerId: Long, nickname: String, avatar: String, preview: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("dm_peer_id", peerId)
            putExtra("dm_peer_name", nickname)
            putExtra("dm_peer_avatar", avatar)
        }
        val pi = PendingIntent.getActivity(
            context,
            peerId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(nickname)
            .setContentText(preview)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                nm.notify(peerId.toInt(), notif)
            }
        } else {
            nm.notify(peerId.toInt(), notif)
        }
    }
}

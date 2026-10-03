package com.touhou.aichat

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DmPollingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    private val prefs by lazy { getSharedPreferences("dm_notif", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        NotifHelper.ensureChannel(this)
        startForeground(1, NotifHelper.buildRunningNotification(this))
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startPolling() {
        if (pollJob != null) return
        pollJob = scope.launch {
            var lastAfter = prefs.getLong("dm_last_after", 0L)
            while (isActive) {
                try {
                val token = AuthStore.loadToken(this@DmPollingService)
                if (token != null) {
                    when (val r = Api.fetchUnreadDm(token, lastAfter)) {
                        is ApiResult.Success -> {
                            for (m in r.data) {
                                lastAfter = maxOf(lastAfter, m.id)
                                if (m.senderId != DmNotifState.currentOpenPeer) {
                                    NotifHelper.showDmNotification(
                                        this@DmPollingService,
                                        m.senderId,
                                        m.nickname.ifBlank { m.username },
                                        m.avatar,
                                        m.content
                                    )
                                    DmNotifState.incrementUnread(m.senderId)
                                }
                            }
                            if (r.data.isNotEmpty()) {
                                prefs.edit().putLong("dm_last_after", lastAfter).apply()
                            }
                        }
                        is ApiResult.Error -> { /* 忽略 */ }
                    }
                }
                } catch (e: Throwable) { /* 单次轮询异常忽略，不崩溃 */ }
                delay(30000)
            }
        }
    }
}

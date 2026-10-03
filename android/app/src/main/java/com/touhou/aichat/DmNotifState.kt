package com.touhou.aichat

import kotlinx.coroutines.flow.MutableStateFlow

object DmNotifState {
    @Volatile
    var currentOpenPeer: Long = 0

    data class PendingDm(val id: Long, val name: String, val avatar: String)

    val pendingOpen = MutableStateFlow<PendingDm?>(null)

    val unread = MutableStateFlow<Map<Long, Int>>(emptyMap())

    fun incrementUnread(peerId: Long) {
        val m = unread.value.toMutableMap()
        m[peerId] = (m[peerId] ?: 0) + 1
        unread.value = m
    }

    fun clearUnread(peerId: Long) {
        if (unread.value.containsKey(peerId)) {
            val m = unread.value.toMutableMap()
            m.remove(peerId)
            unread.value = m
        }
    }
}

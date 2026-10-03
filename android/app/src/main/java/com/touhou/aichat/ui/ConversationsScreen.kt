package com.touhou.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.CharacterAvatars
import com.touhou.aichat.ConversationInfo
import com.touhou.aichat.DmConversation
import com.touhou.aichat.DmNotifState

@Composable
fun ConversationsScreen(
    token: String,
    onOpenAiChat: (String) -> Unit,
    onOpenUserProfile: (Long) -> Unit
) {
    var tab by remember { mutableStateOf(0) } // 0=私信 1=智能体
    val unread by DmNotifState.unread.collectAsState()
    var aiConversations by remember { mutableStateOf<List<ConversationInfo>>(emptyList()) }
    var dmConversations by remember { mutableStateOf<List<DmConversation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(token) {
        when (val r = Api.fetchConversations(token)) {
            is ApiResult.Success -> aiConversations = r.data
            is ApiResult.Error -> { /* 忽略 */ }
        }
        when (val r = Api.fetchDmConversations(token)) {
            is ApiResult.Success -> dmConversations = r.data
            is ApiResult.Error -> { /* 忽略 */ }
        }
        loading = false
    }

    val aiUnique = remember(aiConversations) {
        val seen = LinkedHashMap<String, ConversationInfo>()
        for (c in aiConversations) {
            if (!seen.containsKey(c.persona)) seen[c.persona] = c
        }
        seen.values.toList()
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("私信") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("智能体") })
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tab == 0) {
            if (dmConversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有私信会话", style = MaterialTheme.typography.titleMedium, color = WinTextDark)
                        Text("点右上角搜索找人私信吧", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    items(dmConversations, key = { "dm-" + it.userId }) { conv ->
                        DmRow(conv = conv, unreadCount = unread[conv.userId] ?: 0, onClick = { onOpenUserProfile(conv.userId) })
                    }
                }
            }
        } else {
            if (aiUnique.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有 AI 会话", style = MaterialTheme.typography.titleMedium, color = WinTextDark)
                        Text("去首页或搜索找角色聊天吧", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    items(aiUnique, key = { "ai-" + it.persona }) { conv ->
                        AiRow(conv, onClick = { onOpenAiChat(conv.persona) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DmRow(conv: DmConversation, unreadCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(conv.avatar, Modifier.size(48.dp), cornerRadius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(conv.nickname.ifBlank { conv.username }, style = MaterialTheme.typography.titleMedium, color = WinTextDark)
            Text("@${conv.username}", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
            val preview = if (conv.lastContentType == "image") "[图片]" else conv.lastContent
            Text(preview, style = MaterialTheme.typography.bodyMedium, color = WinTextGray, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        if (unreadCount > 0) {
            Box(
                Modifier.size(18.dp).background(Color(0xFFE53935), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (unreadCount > 99) "99+" else "$unreadCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(conv.lastTime, style = MaterialTheme.typography.labelSmall, color = WinTextGray)
    }
}

@Composable
private fun AiRow(conv: ConversationInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CharacterAvatar(CharacterAvatars.map[conv.persona], Modifier.size(48.dp), cornerRadius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(conv.persona, style = MaterialTheme.typography.titleMedium, color = WinTextDark)
            val preview = if (conv.contentType == "image") "[图片]" else conv.content
            Text(preview, style = MaterialTheme.typography.bodyMedium, color = WinTextGray, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(conv.createdAt, style = MaterialTheme.typography.labelSmall, color = WinTextGray)
    }
}

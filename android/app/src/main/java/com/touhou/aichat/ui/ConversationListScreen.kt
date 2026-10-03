package com.touhou.aichat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Conversation(
    val id: Long,
    val title: String,
    val subtitle: String
)

@Composable
fun ConversationListScreen(query: String) {
    val all = listOf(
        Conversation(1L, "当前会话", "与角色的聊天（单会话）")
    )
    val filtered = if (query.isBlank()) all else all.filter {
        it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
    }

    if (filtered.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("没有匹配的会话", style = MaterialTheme.typography.titleMedium, color = WinTextDark)
                Text("换个关键词试试", style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
            items(filtered) { conv ->
                ConversationItem(conv)
            }
        }
    }
}

@Composable
private fun ConversationItem(conv: Conversation) {
    Surface(
        color = GlassWhiteSoft,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(conv.title, style = MaterialTheme.typography.titleMedium, color = WinTextDark)
            Text(conv.subtitle, style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
        }
    }
}

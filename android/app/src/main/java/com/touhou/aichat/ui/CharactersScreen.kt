package com.touhou.aichat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.CharacterAvatars
import com.touhou.aichat.CharacterInfo

@Composable
fun CharacterSearchResults(query: String, onOpenChat: (String) -> Unit) {
    var characters by remember { mutableStateOf<List<CharacterInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        when (val r = Api.fetchCharacters()) {
            is ApiResult.Success -> characters = r.data
            is ApiResult.Error -> { /* 忽略 */ }
        }
        loading = false
    }

    // 官方角色只保留有头像的；自建角色全部保留
    val visible = remember(characters) {
        characters.filter { it.type == "custom" || CharacterAvatars.map.containsKey(it.name) }
    }

    val trimmed = query.trim()
    val filtered = if (trimmed.isEmpty()) emptyList() else visible.filter { it.name.contains(trimmed, ignoreCase = true) }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        trimmed.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("输入角色名搜索", style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
        }
        filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有匹配的角色", color = WinTextDark)
        }
        else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            items(filtered, key = { it.name }) { ch ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenChat(ch.name) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ch.type == "custom") {
                        UserAvatar(ch.avatar, Modifier.size(44.dp), cornerRadius = 10.dp)
                    } else {
                        CharacterAvatar(CharacterAvatars.map[ch.name], Modifier.size(44.dp), cornerRadius = 10.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(ch.name, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                    if (ch.type == "custom") {
                        Spacer(Modifier.width(6.dp))
                        Text("自建", style = MaterialTheme.typography.labelSmall, color = WinBlue)
                    }
                }
            }
        }
    }
}

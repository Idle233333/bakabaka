package com.touhou.aichat.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.touhou.aichat.CharacterInfo
import com.touhou.aichat.UserInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    token: String,
    onBack: () -> Unit,
    onOpenUserProfile: (Long) -> Unit,
    onOpenAiChat: (String) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var userResults by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var aiResults by remember { mutableStateOf<List<CharacterInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(query, tab) {
        val q = query.trim()
        if (q.isBlank()) {
            userResults = emptyList()
            aiResults = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        if (tab == 0) {
            when (val r = Api.searchUsers(token, q)) {
                is ApiResult.Success -> userResults = r.data
                is ApiResult.Error -> userResults = emptyList()
            }
        } else {
            when (val r = Api.fetchCharacters()) {
                is ApiResult.Success -> aiResults = r.data.filter { it.name.contains(q, ignoreCase = true) }
                is ApiResult.Error -> aiResults = emptyList()
            }
        }
        loading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GlassWhite,
                    titleContentColor = WinTextDark,
                    navigationIconContentColor = WinTextDark
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                title = { Text("搜索") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.padding(top = 4.dp))
            GlassOutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("输入 @用户名 或关键词") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.padding(top = 8.dp))
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("用户") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("智能体") })
            }
            Spacer(Modifier.padding(top = 8.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                query.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("输入关键词搜索", color = WinTextGray)
                }
                tab == 0 -> {
                    if (userResults.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有找到用户", color = WinTextGray) }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(userResults, key = { it.id }) { user ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onOpenUserProfile(user.id)
                                    }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(user.avatar, Modifier.size(44.dp), cornerRadius = 12.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(user.nickname.ifBlank { user.username }, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                                        Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (aiResults.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有找到角色", color = WinTextGray) }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(aiResults, key = { it.name }) { ch ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        onOpenAiChat(ch.name)
                                    }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (ch.type == "custom") {
                                        UserAvatar(ch.avatar, Modifier.size(44.dp), cornerRadius = 12.dp)
                                    } else {
                                        CharacterAvatar(CharacterAvatars.map[ch.name], Modifier.size(44.dp), cornerRadius = 12.dp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(ch.name, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

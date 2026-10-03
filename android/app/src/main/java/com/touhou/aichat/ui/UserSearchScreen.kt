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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.UserInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    token: String,
    onBack: () -> Unit,
    onOpenUserProfile: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                title = { Text("找真人私信") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            GlassOutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.width(8.dp))
            ScaleButton(
                onClick = {
                    if (query.isBlank()) {
                        error = "请输入用户名"
                    } else {
                        scope.launch {
                            when (val r = Api.searchUsers(token, query.trim())) {
                                is ApiResult.Success -> {
                                    results = r.data
                                    searched = true
                                    error = null
                                }
                                is ApiResult.Error -> {
                                    error = r.message
                                    results = emptyList()
                                    searched = true
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text("搜索")
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.padding(top = 8.dp))
            when {
                !searched -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("输入用户名搜索", color = WinTextDark)
                }
                results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到用户", color = WinTextDark)
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(results, key = { it.id }) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onOpenUserProfile(user.id)
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(user.avatar, Modifier.size(44.dp), cornerRadius = 12.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                user.nickname.ifBlank { user.username },
                                style = MaterialTheme.typography.bodyLarge,
                                color = WinTextDark
                            )
                        }
                    }
                }
            }
        }
    }
}

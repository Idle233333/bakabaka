package com.touhou.aichat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.RemarkStore
import com.touhou.aichat.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    token: String,
    userId: Long,
    onBack: () -> Unit,
    onSendDm: (DmPeer) -> Unit,
    onOpenSpace: () -> Unit
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val remarks by RemarkStore.state.collectAsState()
    var showRemarkDialog by remember { mutableStateOf(false) }
    var remarkInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token, userId) {
        when (val r = Api.fetchUserProfile(token, userId)) {
            is ApiResult.Success -> profile = r.data
            is ApiResult.Error -> error = r.message
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
                title = { Text("用户主页") }
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            profile != null -> {
                val p = profile!!
                val remark = remarks[p.id]
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        UserAvatar(base64 = p.avatar, modifier = Modifier.size(96.dp), cornerRadius = 24.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(remark ?: p.nickname.ifBlank { p.username }, style = MaterialTheme.typography.headlineSmall, color = WinTextDark)
                        if (remark != null) {
                            Spacer(Modifier.height(2.dp))
                            Text("(${p.nickname.ifBlank { p.username }})", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("@${p.username}", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                        if (p.ipLocation.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = WinTextGray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("IP属地：${p.ipLocation}", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (p.signature.isNotBlank()) "「${p.signature}」" else "这个人很懒，还没有写简介。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WinTextGray
                    )
                    if (p.gender.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        InfoRow(Icons.Default.Person, "性别：${p.gender}")
                    }
                    if (p.company.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        InfoRow(Icons.Default.Home, "公司：${p.company}")
                    }
                    if (p.address.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        InfoRow(Icons.Default.Place, "地址：${p.address}")
                    }
                    Spacer(Modifier.height(20.dp))
                    ScaleButton(
                        onClick = { onSendDm(DmPeer(p.id, p.nickname.ifBlank { p.username }, p.avatar)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("发私信")
                    }
                    Spacer(Modifier.height(10.dp))
                    ScaleButton(onClick = onOpenSpace, modifier = Modifier.fillMaxWidth()) {
                        Text("进入 TA 的空间")
                    }
                    Spacer(Modifier.height(10.dp))
                    ScaleButton(
                        onClick = {
                            remarkInput = remark ?: ""
                            showRemarkDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("设置备注")
                    }
                }

                if (showRemarkDialog) {
                    AlertDialog(
                        onDismissRequest = { showRemarkDialog = false },
                        title = { Text("设置备注") },
                        text = {
                            GlassOutlinedTextField(
                                value = remarkInput,
                                onValueChange = { remarkInput = it },
                                label = { Text("备注（留空删除）") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showRemarkDialog = false
                                scope.launch {
                                    when (val r = Api.setRemark(token, p.id, remarkInput.trim())) {
                                        is ApiResult.Success -> RemarkStore.set(p.id, remarkInput.trim())
                                        is ApiResult.Error -> { /* 忽略 */ }
                                    }
                                }
                            }) { Text("保存") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRemarkDialog = false }) { Text("取消") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = WinTextGray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
    }
}

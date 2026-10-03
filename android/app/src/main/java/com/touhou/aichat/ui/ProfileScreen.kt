package com.touhou.aichat.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.touhou.aichat.MeInfo

@Composable
fun ProfileScreen(
    token: String,
    username: String,
    profile: MeInfo?,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onOpenSpace: () -> Unit
) {
    val signature = profile?.signature ?: ""
    val ipLocation = profile?.ipLocation ?: ""
    val gender = profile?.gender
    val company = profile?.company
    val address = profile?.address

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            UserAvatar(base64 = profile?.avatar ?: "", modifier = Modifier.size(96.dp), cornerRadius = 24.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                profile?.nickname?.ifBlank { username } ?: username,
                style = MaterialTheme.typography.headlineSmall,
                color = WinTextDark
            )
            Spacer(Modifier.height(2.dp))
            Text("@$username", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
            if (ipLocation.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = WinTextGray, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("IP属地：$ipLocation", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (signature.isNotBlank()) "「$signature」" else "这个人很懒，还没有写简介。",
            style = MaterialTheme.typography.bodyMedium,
            color = WinTextGray
        )
        if (!gender.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            InfoRow(Icons.Default.Person, "性别：$gender")
        }
        if (!company.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            InfoRow(Icons.Default.Home, "公司：$company")
        }
        if (!address.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            InfoRow(Icons.Default.Place, "地址：$address")
        }
        if (profile != null && profile.messageLimit > 0) {
            Spacer(Modifier.height(16.dp))
            Text(
                "今日用量 ${profile.messageCount} / ${profile.messageLimit}",
                style = MaterialTheme.typography.bodyMedium,
                color = WinTextGray
            )
        }
        Spacer(Modifier.height(20.dp))
        ScaleButton(onClick = onOpenSpace, modifier = Modifier.fillMaxWidth()) {
            Text("个人空间")
        }
        Spacer(Modifier.height(10.dp))
        ScaleButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("设置")
        }
        Spacer(Modifier.height(10.dp))
        ScaleButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("退出登录")
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("Idle", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
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

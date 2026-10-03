package com.touhou.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.touhou.aichat.CallManager
import com.touhou.aichat.IncomingCall

private val CallBlue = Color(0xFF1E2A4A)

@Composable
fun IncomingCallDialog(call: IncomingCall, onAnswer: () -> Unit, onReject: () -> Unit) {
    Dialog(onDismissRequest = { /* 只能点接听/拒绝 */ }) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                UserAvatar(base64 = call.avatar, modifier = Modifier.size(72.dp), cornerRadius = 18.dp)
                Spacer(Modifier.height(12.dp))
                Text(call.nickname.ifBlank { call.username }, style = MaterialTheme.typography.titleLarge, color = WinTextDark)
                Spacer(Modifier.height(4.dp))
                Text("邀请你语音通话...", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxSize().let { Modifier }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ScaleButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("拒绝") }
                    ScaleButton(onClick = onAnswer, modifier = Modifier.weight(1f)) { Text("接听") }
                }
            }
        }
    }
}

@Composable
fun CallScreen(state: CallManager.State, onMinimize: () -> Unit, onHangUp: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CallBlue, Color(0xFF2A3A6B)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            UserAvatar(base64 = state.peerAvatar, modifier = Modifier.size(96.dp), cornerRadius = 24.dp)
            Spacer(Modifier.height(16.dp))
            Text(state.peerName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(6.dp))
            if (state.phase == CallManager.Phase.OUTGOING) {
                Text("等待对方接听...", style = MaterialTheme.typography.bodyMedium, color = Color(0xCCFFFFFF))
            } else {
                Text(formatDuration(state.durationSeconds), style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Color(0x66FFFFFF)).clickable(onClick = onMinimize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("—", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("最小化", style = MaterialTheme.typography.labelSmall, color = Color(0xCCFFFFFF))
                }
                Spacer(Modifier.width(48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE53935)).clickable(onClick = onHangUp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "挂断", tint = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("挂断", style = MaterialTheme.typography.labelSmall, color = Color(0xCCFFFFFF))
                }
            }
        }
    }
}

@Composable
fun MinimizedCallBubble(state: CallManager.State, onRestore: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Surface(
            onClick = onRestore,
            shape = RoundedCornerShape(24.dp),
            color = Color(0xE6000000),
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("●", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
                Text(
                    "通话中 ${formatDuration(state.durationSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

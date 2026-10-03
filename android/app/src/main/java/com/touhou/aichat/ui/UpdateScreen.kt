package com.touhou.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.touhou.aichat.UpdateInfo
import com.touhou.aichat.UpdateManager

@Composable
fun UpdateScreen(
    updateInfo: UpdateInfo,
    onBackground: () -> Unit
) {
    val context = LocalContext.current
    val progress by UpdateManager.progress.collectAsState()

    LaunchedEffect(progress.phase) {
        if (progress.phase == UpdateManager.Phase.DONE) {
            UpdateManager.installFromCache(context)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("发现新版本", style = MaterialTheme.typography.headlineSmall, color = WinTextDark)
            Spacer(Modifier.height(8.dp))
            Text("版本 ${updateInfo.versionName}（code ${updateInfo.versionCode}）", color = WinTextGray)
            Spacer(Modifier.height(8.dp))
            Text(updateInfo.note, style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
            Spacer(Modifier.height(24.dp))

            when (progress.phase) {
                UpdateManager.Phase.IDLE -> {
                    ScaleButton(
                        onClick = { UpdateManager.start(context, updateInfo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("立即更新")
                    }
                }
                UpdateManager.Phase.DOWNLOADING -> {
                    val percent = if (progress.total > 0) ((progress.downloaded * 100f / progress.total).toInt()) else 0
                    LinearProgressIndicator(
                        progress = { if (progress.total > 0) progress.downloaded.toFloat() / progress.total else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$percent%  ${fmtMb(progress.downloaded)} / ${fmtMb(progress.total)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WinTextDark
                    )
                    Text(
                        "速度 ${fmtMb(progress.speed)}/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = WinTextGray
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBackground) { Text("后台下载（继续使用应用）") }
                }
                UpdateManager.Phase.PROCESSING -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("正在校验...", color = WinTextDark)
                }
                UpdateManager.Phase.DONE -> {
                    Text("下载完成，正在安装...", color = WinTextDark)
                    Spacer(Modifier.height(12.dp))
                    ScaleButton(
                        onClick = { UpdateManager.installFromCache(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("安装")
                    }
                }
                UpdateManager.Phase.ERROR -> {
                    Text(progress.error ?: "更新失败", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    ScaleButton(
                        onClick = { UpdateManager.start(context, updateInfo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadBanner(onTap: () -> Unit, modifier: Modifier = Modifier) {
    val progress by UpdateManager.progress.collectAsState()
    if (progress.phase == UpdateManager.Phase.DOWNLOADING || progress.phase == UpdateManager.Phase.PROCESSING) {
        Surface(
            color = GlassWhite,
            shadowElevation = 4.dp,
            modifier = modifier.fillMaxWidth().clickable(onClick = onTap)
        ) {
            Column(Modifier.padding(12.dp)) {
                val percent = if (progress.total > 0) ((progress.downloaded * 100f / progress.total).toInt()) else 0
                Text(
                    "更新下载中 $percent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WinTextDark
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (progress.total > 0) progress.downloaded.toFloat() / progress.total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun fmtMb(bytes: Long): String = String.format("%.1f MB", bytes / 1024.0 / 1024.0)

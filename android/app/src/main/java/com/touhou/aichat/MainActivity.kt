package com.touhou.aichat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.touhou.aichat.ui.DownloadBanner
import com.touhou.aichat.ui.HomeScreen
import com.touhou.aichat.ui.LoginScreen
import com.touhou.aichat.ui.RegisterScreen
import com.touhou.aichat.ui.TouhouTheme
import com.touhou.aichat.ui.UpdateScreen
import com.touhou.aichat.ui.WallpaperBackground

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    data class Home(val token: String, val username: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDmIntent(intent)
        setContent {
            AppRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDmIntent(intent)
    }

    private fun handleDmIntent(intent: Intent?) {
        val id = intent?.getLongExtra("dm_peer_id", 0L) ?: 0L
        if (id > 0) {
            val name = intent?.getStringExtra("dm_peer_name") ?: ""
            val avatar = intent?.getStringExtra("dm_peer_avatar") ?: ""
            DmNotifState.pendingOpen.value = DmNotifState.PendingDm(id, name, avatar)
        }
    }
}

@Composable
fun AppRoot() {
    TouhouTheme {
        val context = LocalContext.current
        var screen by remember {
            val token = AuthStore.loadToken(context)
            mutableStateOf<Screen>(
                if (token != null) {
                    Screen.Home(token, AuthStore.loadUsername(context) ?: "")
                } else {
                    Screen.Login
                }
            )
        }
        var wallpaperIndex by remember { mutableStateOf(SettingsStore.loadWallpaperIndex(context)) }
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var showUpdate by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            when (val r = UpdateManager.checkForUpdate()) {
                is UpdateManager.CheckResult.Available -> {
                    updateInfo = r.info
                    showUpdate = true
                }
                else -> { /* 已是最新或检查失败：静默（已打 log） */ }
            }
        }

        Box(Modifier.fillMaxSize().background(Color.White)) {
            if (wallpaperIndex >= 0 && wallpaperIndex < Wallpapers.list.size) {
                WallpaperBackground(Wallpapers.list[wallpaperIndex], Modifier.fillMaxSize())
            }

            val ui = updateInfo
            if (ui != null && showUpdate) {
                UpdateScreen(updateInfo = ui, onBackground = { showUpdate = false })
            } else {
                Crossfade(targetState = screen, label = "screen") { s ->
                    when (s) {
                        is Screen.Login -> LoginScreen(
                            onLogin = { token, username ->
                                AuthStore.saveSession(context, token, username)
                                screen = Screen.Home(token, username)
                            },
                            onGoRegister = { screen = Screen.Register }
                        )
                        is Screen.Register -> RegisterScreen(
                            onRegistered = { screen = Screen.Login },
                            onGoLogin = { screen = Screen.Login }
                        )
                        is Screen.Home -> HomeScreen(
                            token = s.token,
                            username = s.username,
                            wallpaperIndex = wallpaperIndex,
                            onWallpaperChanged = { idx ->
                                wallpaperIndex = idx
                                SettingsStore.saveWallpaperIndex(context, idx)
                            },
                            onUpdateFound = { info ->
                                updateInfo = info
                                showUpdate = true
                            },
                            onLogout = {
                                AuthStore.clear(context)
                                screen = Screen.Login
                            }
                        )
                    }
                }
                if (ui != null) {
                    DownloadBanner(
                        onTap = { showUpdate = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

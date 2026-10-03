package com.touhou.aichat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.CallManager
import com.touhou.aichat.DmNotifState
import com.touhou.aichat.DmPollingService
import com.touhou.aichat.IncomingCall
import com.touhou.aichat.MeInfo
import com.touhou.aichat.Post
import com.touhou.aichat.RemarkStore
import com.touhou.aichat.UpdateInfo
import kotlinx.coroutines.delay

enum class HomeTab(val label: String) {
    Home("首页"),
    Conversations("聊天列表"),
    Profile("我的")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    token: String,
    username: String,
    wallpaperIndex: Int,
    onWallpaperChanged: (Int) -> Unit,
    onUpdateFound: (UpdateInfo) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(HomeTab.Home) }
    var openAgentChat by remember { mutableStateOf(false) }
    var agentPersona by remember { mutableStateOf<String?>(null) }
    var activeDm by remember { mutableStateOf<DmPeer?>(null) }
    var userProfileId by remember { mutableStateOf<Long?>(null) }
    var postDetail by remember { mutableStateOf<Post?>(null) }
    var feedRefresh by remember { mutableStateOf(0) }
    var spaceUserId by remember { mutableStateOf<Long?>(null) }
    var spaceIsOwner by remember { mutableStateOf(false) }
    var createAgent by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<MeInfo?>(null) }

    // 通话
    val callState by CallManager.state.collectAsState()
    var incomingCall by remember { mutableStateOf<IncomingCall?>(null) }
    var pendingAnswer by remember { mutableStateOf<IncomingCall?>(null) }
    var callMinimized by remember { mutableStateOf(false) }

    val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val call = pendingAnswer
        pendingAnswer = null
        if (granted && call != null) {
            CallManager.answerCall(call.callId, call.callerId, call.nickname.ifBlank { call.username }, call.avatar)
        } else if (!granted) {
            Toast.makeText(context, "需要麦克风权限才能通话", Toast.LENGTH_SHORT).show()
        }
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* 忽略 */ }

    LaunchedEffect(token) {
        when (val r = Api.getMe(token)) {
            is ApiResult.Success -> profile = r.data
            is ApiResult.Error -> { /* 头像/昵称回退默认 */ }
        }
        when (val r = Api.fetchRemarks(token)) {
            is ApiResult.Success -> RemarkStore.load(r.data)
            is ApiResult.Error -> { /* 忽略 */ }
        }
    }

    LaunchedEffect(token) {
        CallManager.init(context, token)
        while (true) {
            delay(3000)
            if (!CallManager.isInCall() && incomingCall == null) {
                when (val r = Api.fetchIncomingCall(token)) {
                    is ApiResult.Success -> {
                        if (r.data != null) incomingCall = r.data
                    }
                    is ApiResult.Error -> { /* 忽略 */ }
                }
            }
        }
    }

    LaunchedEffect(token) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, DmPollingService::class.java))
        } catch (_: Exception) { /* 忽略 */ }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val pendingDm by DmNotifState.pendingOpen.collectAsState()
    LaunchedEffect(pendingDm) {
        pendingDm?.let { p ->
            activeDm = DmPeer(p.id, p.name, p.avatar)
            DmNotifState.pendingOpen.value = null
        }
    }

    val userAvatar = profile?.avatar ?: ""

    when {
        createAgent -> CreateAgentScreen(
            token = token,
            onBack = { createAgent = false },
            onCreated = { createAgent = false }
        )
        openAgentChat -> AgentChatPage(
            token = token,
            initialPersona = agentPersona,
            userAvatar = userAvatar,
            onBack = { openAgentChat = false }
        )
        postDetail != null -> PostDetailScreen(
            token = token,
            post = postDetail!!,
            isOwn = postDetail!!.authorId == (profile?.id ?: -1L),
            onBack = {
                postDetail = null
                feedRefresh++
            },
            onOpenUserProfile = { id -> userProfileId = id }
        )
        spaceUserId != null -> SpaceScreen(
            token = token,
            userId = spaceUserId!!,
            isOwner = spaceIsOwner,
            feedRefresh = feedRefresh,
            onBack = { spaceUserId = null },
            onOpenDetail = { p -> postDetail = p }
        )
        userProfileId != null -> UserProfileScreen(
            token = token,
            userId = userProfileId!!,
            onBack = { userProfileId = null },
            onSendDm = { peer ->
                activeDm = peer
                userProfileId = null
            },
            onOpenSpace = {
                spaceUserId = userProfileId
                spaceIsOwner = false
            }
        )
        activeDm != null -> DmChatScreen(
            token = token,
            peer = activeDm!!,
            myAvatar = userAvatar,
            onBack = { activeDm = null },
            onOpenUserProfile = { id -> userProfileId = id }
        )
        search -> SearchScreen(
            token = token,
            onBack = { search = false },
            onOpenUserProfile = { id ->
                userProfileId = id
                search = false
            },
            onOpenAiChat = { p ->
                agentPersona = p
                search = false
                openAgentChat = true
            }
        )
        settings -> SettingsScreen(
            token = token,
            profile = profile,
            wallpaperIndex = wallpaperIndex,
            onWallpaperChanged = onWallpaperChanged,
            onProfileUpdated = { profile = it },
            onUpdateFound = onUpdateFound,
            onBack = { settings = false }
        )
        else -> Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GlassWhite,
                        titleContentColor = WinTextDark,
                        actionIconContentColor = WinTextDark
                    ),
                    title = { Text(currentTab.label) },
                    actions = {
                        IconButton(onClick = { search = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = GlassWhite) {
                    NavigationBarItem(
                        selected = currentTab == HomeTab.Home,
                        onClick = { currentTab = HomeTab.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                        label = { Text("首页") }
                    )
                    NavigationBarItem(
                        selected = currentTab == HomeTab.Conversations,
                        onClick = { currentTab = HomeTab.Conversations },
                        icon = { Icon(Icons.Default.List, contentDescription = "聊天列表") },
                        label = { Text("聊天列表") }
                    )
                    NavigationBarItem(
                        selected = currentTab == HomeTab.Profile,
                        onClick = { currentTab = HomeTab.Profile },
                        icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                        label = { Text("我的") }
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { createAgent = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "创建角色") },
                    text = { Text("创建角色") },
                    containerColor = WinBlue,
                    contentColor = Color.White
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                Crossfade(targetState = currentTab, label = "tab") { tab ->
                    when (tab) {
                        HomeTab.Home -> HomeScreenContent(
                            token = token,
                            feedRefresh = feedRefresh,
                            onOpenDetail = { p -> postDetail = p }
                        )
                        HomeTab.Conversations -> ConversationsScreen(
                            token = token,
                            onOpenAiChat = { p ->
                                agentPersona = p
                                openAgentChat = true
                            },
                            onOpenUserProfile = { id -> userProfileId = id }
                        )
                        HomeTab.Profile -> ProfileScreen(
                            token = token,
                            username = username,
                            profile = profile,
                            onOpenSettings = { settings = true },
                            onLogout = onLogout,
                            onOpenSpace = {
                                spaceUserId = profile?.id
                                spaceIsOwner = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ===== 来电弹窗 =====
    if (incomingCall != null && !CallManager.isInCall()) {
        IncomingCallDialog(
            call = incomingCall!!,
            onAnswer = {
                val call = incomingCall
                incomingCall = null
                if (call != null) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        CallManager.answerCall(call.callId, call.callerId, call.nickname.ifBlank { call.username }, call.avatar)
                    } else {
                        pendingAnswer = call
                        recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            onReject = {
                val call = incomingCall
                incomingCall = null
                if (call != null) CallManager.rejectCall(call.callId)
            }
        )
    }

    // ===== 通话界面 / 小窗 =====
    when (callState.phase) {
        CallManager.Phase.OUTGOING, CallManager.Phase.ACTIVE -> {
            if (callMinimized) {
                MinimizedCallBubble(callState, onRestore = { callMinimized = false })
            } else {
                CallScreen(
                    state = callState,
                    onMinimize = { callMinimized = true },
                    onHangUp = {
                        CallManager.hangUp()
                        callMinimized = false
                    }
                )
            }
        }
        CallManager.Phase.ENDED -> {
            LaunchedEffect(callState.phase) {
                delay(1200)
                callMinimized = false
                CallManager.dismiss()
            }
        }
        else -> {}
    }
}

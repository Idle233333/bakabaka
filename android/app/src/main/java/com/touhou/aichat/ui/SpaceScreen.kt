package com.touhou.aichat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.Post
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(
    token: String,
    userId: Long,
    isOwner: Boolean,
    feedRefresh: Int,
    onBack: () -> Unit,
    onOpenDetail: (Post) -> Unit
) {
    val scope = rememberCoroutineScope()
    var nickname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var ipLocation by remember { mutableStateOf("") }

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var postsLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showNewPost by remember { mutableStateOf(false) }

    LaunchedEffect(userId, isOwner, feedRefresh) {
        if (isOwner) {
            when (val r = Api.getMe(token)) {
                is ApiResult.Success -> {
                    nickname = r.data.nickname
                    username = r.data.username
                    avatar = r.data.avatar
                    signature = r.data.signature
                    ipLocation = r.data.ipLocation
                }
                is ApiResult.Error -> { /* 忽略 */ }
            }
        } else {
            when (val r = Api.fetchUserProfile(token, userId)) {
                is ApiResult.Success -> {
                    nickname = r.data.nickname
                    username = r.data.username
                    avatar = r.data.avatar
                    signature = r.data.signature
                    ipLocation = r.data.ipLocation
                }
                is ApiResult.Error -> { /* 忽略 */ }
            }
        }
        when (val r = Api.fetchPosts(token, userId)) {
            is ApiResult.Success -> posts = r.data
            is ApiResult.Error -> error = r.message
        }
        postsLoading = false
    }

    if (showNewPost) {
        NewPostScreen(
            token = token,
            onBack = { showNewPost = false },
            onPosted = {
                showNewPost = false
                scope.launch {
                    when (val r = Api.fetchPosts(token, userId)) {
                        is ApiResult.Success -> posts = r.data
                        is ApiResult.Error -> error = r.message
                    }
                }
            }
        )
        return
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
                title = { Text("个人空间") }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item(key = "cover") {
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxWidth().height(130.dp)
                            .background(Brush.verticalGradient(listOf(WinBlue, Color(0xFF8AD6F5))))
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(top = 76.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(base64 = avatar, modifier = Modifier.size(88.dp), cornerRadius = 22.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            nickname.ifBlank { username }.ifBlank { "用户" },
                            style = MaterialTheme.typography.titleLarge,
                            color = WinTextDark
                        )
                        if (ipLocation.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text("IP属地：$ipLocation", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            signature.ifBlank { "这个人很懒，还没有写简介。" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = WinTextGray
                        )
                    }
                }
            }
            if (isOwner) {
                item(key = "new-post") {
                    ScaleButton(
                        onClick = { showNewPost = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("发动态")
                    }
                }
            }
            if (postsLoading) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.padding(24.dp))
                    }
                }
            } else if (posts.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("还没有动态", color = WinTextGray, modifier = Modifier.padding(24.dp))
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        token = token,
                        post = post,
                        isOwn = isOwner,
                        onOpenDetail = onOpenDetail,
                        onDeleted = { posts = posts.filter { it.id != post.id } }
                    )
                }
            }
        }
    }
}

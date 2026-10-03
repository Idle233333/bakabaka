package com.touhou.aichat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.Post
import com.touhou.aichat.PostComment
import kotlinx.coroutines.launch

private val LikeRed = Color(0xFFE0245E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    token: String,
    post: Post,
    isOwn: Boolean,
    onBack: () -> Unit,
    onOpenUserProfile: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentPost by remember(post.id) { mutableStateOf(post) }
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var viewerImage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun refreshComments() {
        scope.launch {
            when (val r = Api.fetchComments(token, post.id)) {
                is ApiResult.Success -> comments = r.data
                is ApiResult.Error -> { /* 忽略 */ }
            }
            loading = false
        }
    }

    LaunchedEffect(post.id) {
        refreshComments()
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
                title = { Text("动态详情") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                item(key = "post") {
                    Surface(
                        color = GlassWhiteSoft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOpenUserProfile(currentPost.authorId) }.padding(bottom = 8.dp)
                            ) {
                                UserAvatar(currentPost.authorAvatar, Modifier.size(40.dp), cornerRadius = 12.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    currentPost.authorNickname.ifBlank { currentPost.authorUsername }.ifBlank { "用户" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WinTextDark
                                )
                            }
                            if (currentPost.content.isNotBlank()) {
                                Text(currentPost.content, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                            }
                            if (currentPost.images.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(currentPost.images) { img ->
                                        val bmp = remember(img) { decodeBase64Resized(img, 256) }
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).clickable { viewerImage = img }
                                            )
                                        }
                                    }
                                }
                            }
                            if (currentPost.pollQuestion.isNotBlank() && currentPost.pollOptions.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Text(currentPost.pollQuestion, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                                Spacer(Modifier.height(4.dp))
                                val totalVotes = currentPost.pollVotes.sum()
                                currentPost.pollOptions.forEachIndexed { i, option ->
                                    val votes = currentPost.pollVotes.getOrElse(i) { 0 }
                                    val selected = currentPost.myVote == i
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            if (currentPost.myVote != i) {
                                                scope.launch {
                                                    when (val r = Api.votePost(token, currentPost.id, i)) {
                                                        is ApiResult.Success -> {
                                                            val newVotes = currentPost.pollVotes.toMutableList()
                                                            while (newVotes.size < currentPost.pollOptions.size) newVotes.add(0)
                                                            val old = currentPost.myVote
                                                            if (old in 0 until newVotes.size) newVotes[old] = (newVotes[old] - 1).coerceAtLeast(0)
                                                            newVotes[i] = newVotes[i] + 1
                                                            currentPost = currentPost.copy(myVote = i, pollVotes = newVotes)
                                                        }
                                                        is ApiResult.Error -> { /* 忽略 */ }
                                                    }
                                                }
                                            }
                                        }.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            if (selected) "●" else "○",
                                            color = if (selected) LikeRed else WinTextGray,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            option,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = WinTextDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("$votes", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("共 $totalVotes 票", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(relativeTime(currentPost.createdAt), style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            when (val r = Api.toggleLike(token, currentPost.id)) {
                                                is ApiResult.Success -> {
                                                    val newLiked = r.data
                                                    val delta = if (newLiked && !currentPost.liked) 1 else if (!newLiked && currentPost.liked) -1 else 0
                                                    currentPost = currentPost.copy(liked = newLiked, likeCount = (currentPost.likeCount + delta).coerceAtLeast(0))
                                                }
                                                is ApiResult.Error -> { /* 忽略 */ }
                                            }
                                        }
                                    }.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        if (currentPost.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "点赞",
                                        tint = if (currentPost.liked) LikeRed else WinTextGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("${currentPost.likeCount}", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                                }
                                Spacer(Modifier.width(20.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "评论",
                                        tint = WinTextGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("${currentPost.commentCount}", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                                }
                                Spacer(Modifier.weight(1f))
                                if (isOwn) {
                                    IconButton(onClick = { showDeleteConfirm = true }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = WinTextGray)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "comments-title") {
                    Text(
                        "评论（${comments.size}）",
                        style = MaterialTheme.typography.titleMedium,
                        color = WinTextDark,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                if (loading) {
                    item(key = "comments-loading") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (comments.isEmpty()) {
                    item(key = "comments-empty") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("还没有评论", color = WinTextGray)
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { c ->
                        CommentRow(c, onClick = { onOpenUserProfile(c.authorId) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                GlassOutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("写评论...") },
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                ScaleButton(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            input = ""
                            scope.launch {
                                when (val r = Api.addComment(token, currentPost.id, text)) {
                                    is ApiResult.Success -> {
                                        refreshComments()
                                        currentPost = currentPost.copy(commentCount = currentPost.commentCount + 1)
                                    }
                                    is ApiResult.Error -> { /* 忽略 */ }
                                }
                            }
                        }
                    }
                ) {
                    Text("发送")
                }
            }
        }
    }

    if (viewerImage != null) {
        ImageViewerDialog(viewerImage!!, onDismiss = { viewerImage = null })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除动态") },
            text = { Text("确定删除这条动态吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        when (val r = Api.deletePost(token, currentPost.id)) {
                            is ApiResult.Success -> onBack()
                            is ApiResult.Error -> { /* 忽略 */ }
                        }
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CommentRow(comment: PostComment, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(comment.avatar, Modifier.size(36.dp), cornerRadius = 10.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.nickname.ifBlank { comment.username },
                    style = MaterialTheme.typography.labelMedium,
                    color = WinTextGray
                )
                Spacer(Modifier.width(6.dp))
                Text(relativeTime(comment.createdAt), style = MaterialTheme.typography.labelSmall, color = WinTextGray)
            }
            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
        }
    }
}

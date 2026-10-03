package com.touhou.aichat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val LikeRed = Color(0xFFE0245E)

@Composable
fun PostCard(
    token: String,
    post: Post,
    isOwn: Boolean,
    onOpenDetail: (Post) -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var liked by remember(post.id) { mutableStateOf(post.liked) }
    var likeCount by remember(post.id) { mutableStateOf(post.likeCount) }
    var myVote by remember(post.id) { mutableStateOf(post.myVote) }
    var pollVotes by remember(post.id) { mutableStateOf(post.pollVotes.toMutableList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var viewerImage by remember { mutableStateOf<String?>(null) }

    Surface(
        color = GlassWhiteSoft,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(14.dp).clickable { onOpenDetail(post) }) {
            if (post.authorNickname.isNotBlank() || post.authorUsername.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    UserAvatar(post.authorAvatar, Modifier.size(36.dp), cornerRadius = 10.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        post.authorNickname.ifBlank { post.authorUsername },
                        style = MaterialTheme.typography.titleSmall,
                        color = WinTextDark
                    )
                }
            }
            if (post.content.isNotBlank()) {
                Text(post.content, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
            }
            if (post.images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(post.images) { img ->
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
            if (post.pollQuestion.isNotBlank() && post.pollOptions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(post.pollQuestion, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                Spacer(Modifier.height(4.dp))
                val totalVotes = pollVotes.sum()
                post.pollOptions.forEachIndexed { i, option ->
                    val votes = pollVotes.getOrElse(i) { 0 }
                    val selected = myVote == i
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (myVote != i) {
                                scope.launch {
                                    when (val r = Api.votePost(token, post.id, i)) {
                                        is ApiResult.Success -> {
                                            val newVotes = pollVotes.toMutableList()
                                            while (newVotes.size < post.pollOptions.size) newVotes.add(0)
                                            if (myVote in 0 until newVotes.size) {
                                                newVotes[myVote] = (newVotes[myVote] - 1).coerceAtLeast(0)
                                            }
                                            newVotes[i] = newVotes[i] + 1
                                            pollVotes = newVotes
                                            myVote = i
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
            Text(relativeTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = WinTextGray)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        scope.launch {
                            when (val r = Api.toggleLike(token, post.id)) {
                                is ApiResult.Success -> {
                                    val newLiked = r.data
                                    if (newLiked != liked) likeCount += if (newLiked) 1 else -1
                                    liked = newLiked
                                }
                                is ApiResult.Error -> { /* 忽略 */ }
                            }
                        }
                    }.padding(vertical = 4.dp)
                ) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "点赞",
                        tint = if (liked) LikeRed else WinTextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("$likeCount", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                }
                Spacer(Modifier.width(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenDetail(post) }.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "评论",
                        tint = WinTextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${post.commentCount}", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
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
                        when (val r = Api.deletePost(token, post.id)) {
                            is ApiResult.Success -> onDeleted()
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

fun relativeTime(input: String): String {
    if (input.isBlank()) return ""
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )
    for (p in patterns) {
        try {
            val fmt = SimpleDateFormat(p, Locale.US)
            if (p.contains("'Z'")) fmt.timeZone = TimeZone.getTimeZone("UTC")
            val date = fmt.parse(input) ?: continue
            val diff = System.currentTimeMillis() - date.time
            if (diff < 0) return "刚刚"
            val m = diff / 60000
            return when {
                m < 1 -> "刚刚"
                m < 60 -> "${m}分钟前"
                m < 1440 -> "${m / 60}小时前"
                else -> "${m / 1440}天前"
            }
        } catch (_: Exception) { /* 尝试下一种格式 */ }
    }
    return input
}

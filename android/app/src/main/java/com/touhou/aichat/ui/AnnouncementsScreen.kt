package com.touhou.aichat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Announcement
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.Post

@Composable
fun HomeScreenContent(
    token: String,
    feedRefresh: Int,
    onOpenDetail: (Post) -> Unit
) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var feedPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(token, feedRefresh) {
        when (val r = Api.fetchAnnouncements()) {
            is ApiResult.Success -> announcements = r.data
            is ApiResult.Error -> { /* 忽略 */ }
        }
        when (val r = Api.fetchFeed(token)) {
            is ApiResult.Success -> feedPosts = r.data
            is ApiResult.Error -> { /* 忽略 */ }
        }
        loading = false
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            item(key = "ann-title") {
                Text(
                    "公告",
                    style = MaterialTheme.typography.titleMedium,
                    color = WinTextDark,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 8.dp)
                )
            }
            if (announcements.isEmpty()) {
                item(key = "ann-empty") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("暂无公告", color = WinTextGray)
                    }
                }
            } else {
                items(announcements) { ann ->
                    AnnouncementCard(ann)
                }
            }
            item(key = "feed-title") {
                Text(
                    "大家分享的动态",
                    style = MaterialTheme.typography.titleMedium,
                    color = WinTextDark,
                    modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            if (feedPosts.isEmpty()) {
                item(key = "feed-empty") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("还没有动态，去发第一条吧", color = WinTextGray)
                    }
                }
            } else {
                items(feedPosts, key = { it.id }) { post ->
                    PostCard(
                        token = token,
                        post = post,
                        isOwn = false,
                        onOpenDetail = onOpenDetail,
                        onDeleted = { feedPosts = feedPosts.filter { it.id != post.id } }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(ann: Announcement) {
    Surface(
        color = GlassWhiteSoft,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(ann.title, style = MaterialTheme.typography.titleMedium, color = WinTextDark)
            Spacer(Modifier.height(6.dp))
            Text(ann.content, style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
            Spacer(Modifier.height(8.dp))
            Text(ann.date, style = MaterialTheme.typography.labelSmall, color = WinTextGray)
        }
    }
}

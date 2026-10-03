package com.touhou.aichat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.PostComment
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    token: String,
    postId: Long,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            when (val r = Api.fetchComments(token, postId)) {
                is ApiResult.Success -> {
                    comments = r.data
                    error = null
                }
                is ApiResult.Error -> error = r.message
            }
            loading = false
        }
    }

    LaunchedEffect(token, postId) {
        refresh()
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
                title = { Text("评论") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                    comments.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("还没有评论", color = WinTextGray)
                    }
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(comments, key = { it.id }) { comment ->
                            CommentRow(comment)
                        }
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
                                when (val r = Api.addComment(token, postId, text)) {
                                    is ApiResult.Success -> refresh()
                                    is ApiResult.Error -> error = r.message
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
}

@Composable
private fun CommentRow(comment: PostComment) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(comment.avatar, Modifier.size(36.dp), cornerRadius = 10.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                comment.nickname.ifBlank { comment.username },
                style = MaterialTheme.typography.labelMedium,
                color = WinTextGray
            )
            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = WinTextDark)
        }
    }
}

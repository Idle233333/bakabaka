package com.touhou.aichat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    token: String,
    onBack: () -> Unit,
    onPosted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var newPost by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var pollEnabled by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    val pollOptions = remember { mutableStateListOf("", "") }
    var posting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            scope.launch {
                val remaining = 4 - images.size
                if (remaining > 0) {
                    for (uri in uris.take(remaining)) {
                        val bmp = withContext(Dispatchers.IO) { decodeAndResize(context, uri, 1024) }
                        if (bmp != null) {
                            val b64 = withContext(Dispatchers.IO) { bitmapToBase64Jpeg(bmp) }
                            images = images + b64
                        }
                    }
                }
            }
        }
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
                title = { Text("发动态") }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            GlassOutlinedTextField(
                value = newPost,
                onValueChange = { newPost = it },
                label = { Text("说点什么...") },
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            if (images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    images.forEachIndexed { idx, img ->
                        val bmp = remember(img) { decodeBase64Resized(img, 256) }
                        Box {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                            IconButton(
                                onClick = { images = images.filterIndexed { i, _ -> i != idx } },
                                modifier = Modifier.size(18.dp).align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "移除", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            Row {
                if (images.size < 4) {
                    TextButton(onClick = { pickImages.launch("image/*") }) { Text("添加图片") }
                }
                TextButton(onClick = { pollEnabled = !pollEnabled }) {
                    Text(if (pollEnabled) "取消投票" else "添加投票")
                }
            }
            if (pollEnabled) {
                GlassOutlinedTextField(
                    value = pollQuestion,
                    onValueChange = { pollQuestion = it },
                    label = { Text("投票问题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                pollOptions.forEachIndexed { i, opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassOutlinedTextField(
                            value = opt,
                            onValueChange = { pollOptions[i] = it },
                            label = { Text("选项 ${i + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (pollOptions.size > 2) {
                            IconButton(onClick = { pollOptions.removeAt(i) }) {
                                Icon(Icons.Default.Close, contentDescription = "删除选项", tint = WinTextGray)
                            }
                        }
                    }
                }
                if (pollOptions.size < 10) {
                    TextButton(onClick = { pollOptions.add("") }) { Text("添加选项") }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(4.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
            ScaleButton(
                onClick = {
                    val text = newPost.trim()
                    val hasImages = images.isNotEmpty()
                    val validOpts = pollOptions.map { it.trim() }.filter { it.isNotEmpty() }
                    val hasPoll = pollEnabled && pollQuestion.isNotBlank() && validOpts.size >= 2
                    if (text.isEmpty() && !hasImages && !hasPoll) {
                        error = "请输入内容"
                    } else {
                        posting = true
                        error = null
                        scope.launch {
                            try {
                                when (val r = Api.createPost(
                                    token = token,
                                    content = text,
                                    images = if (hasImages) images else null,
                                    pollQuestion = if (hasPoll) pollQuestion.trim() else null,
                                    pollOptions = if (hasPoll) validOpts else null
                                )) {
                                    is ApiResult.Success -> onPosted()
                                    is ApiResult.Error -> error = r.message
                                }
                            } catch (e: Throwable) {
                                error = e.message ?: "发布失败"
                            }
                            posting = false
                        }
                    }
                },
                enabled = !posting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (posting) "发布中..." else "发布")
            }
        }
    }
}

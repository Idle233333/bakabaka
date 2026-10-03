package com.touhou.aichat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.touhou.aichat.CharacterAvatars
import com.touhou.aichat.CharacterInfo
import com.touhou.aichat.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatContent(
    token: String,
    persona: String,
    userAvatar: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lastId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val avatarRes = CharacterAvatars.map[persona]
    val typewriterProgress = remember { mutableStateMapOf<Long, Int>() }

    fun onNewMessages(newMessages: List<Message>) {
        if (newMessages.isEmpty()) return
        messages = messages + newMessages
        lastId = newMessages.maxOfOrNull { it.id } ?: lastId
        for (m in newMessages) {
            if (m.role == "assistant" && m.contentType != "image" && m.content.isNotEmpty()) {
                typewriterProgress[m.id] = 0
                scope.launch {
                    var shown = 0
                    val total = m.content.length
                    while (shown < total) {
                        delay(40)
                        shown++
                        typewriterProgress[m.id] = shown
                    }
                    typewriterProgress.remove(m.id)
                }
            }
        }
    }

    LaunchedEffect(token, persona) {
        when (val r = Api.fetchMessages(token, persona, null)) {
            is ApiResult.Success -> {
                messages = r.data
                lastId = r.data.maxOfOrNull { it.id }
            }
            is ApiResult.Error -> error = r.message
        }
    }

    LaunchedEffect(token, persona) {
        while (true) {
            delay(3000)
            when (val r = Api.fetchMessages(token, persona, lastId)) {
                is ApiResult.Success -> onNewMessages(r.data)
                is ApiResult.Error -> { /* 忽略 */ }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bmp = withContext(Dispatchers.IO) { decodeAndResize(context, uri, 1024) }
                    if (bmp != null) {
                        val b64 = withContext(Dispatchers.IO) { bitmapToBase64Jpeg(bmp) }
                        when (val r = Api.sendChatImage(token, persona, b64)) {
                            is ApiResult.Success -> {
                                when (val m = Api.fetchMessages(token, persona, lastId)) {
                                    is ApiResult.Success -> onNewMessages(m.data)
                                    is ApiResult.Error -> { /* 忽略 */ }
                                }
                            }
                            is ApiResult.Error -> error = r.message
                        }
                    } else {
                        error = "无法读取图片"
                    }
                } catch (e: Throwable) {
                    error = e.message ?: "发送图片失败"
                }
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(220), initialOffsetY = { it / 3 })
                ) {
                    MessageBubble(
                        token = token,
                        msg = msg,
                        isSelf = msg.role == "user",
                        avatarRes = avatarRes,
                        userAvatar = userAvatar,
                        typewriterCount = typewriterProgress[msg.id]
                    )
                }
            }
        }
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(
                onClick = { pickImage.launch("image/*") },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Default.Add, contentDescription = "发送图片", tint = WinBlue)
            }
            Spacer(Modifier.width(4.dp))
            GlassOutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            ScaleButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        input = ""
                        error = null
                        scope.launch {
                            try {
                                when (val r = Api.sendChat(token, persona, text)) {
                                    is ApiResult.Success -> {
                                        when (val m = Api.fetchMessages(token, persona, lastId)) {
                                            is ApiResult.Success -> onNewMessages(m.data)
                                            is ApiResult.Error -> { /* 忽略 */ }
                                        }
                                    }
                                    is ApiResult.Error -> error = r.message
                                }
                            } catch (e: Throwable) {
                                error = e.message ?: "发送失败"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatPage(
    token: String,
    initialPersona: String?,
    userAvatar: String,
    onBack: () -> Unit
) {
    var selectedPersona by remember { mutableStateOf(initialPersona) }
    var characters by remember { mutableStateOf<List<CharacterInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        when (val r = Api.fetchCharacters()) {
            is ApiResult.Success -> characters = r.data
            is ApiResult.Error -> { /* 忽略 */ }
        }
    }

    val visible = remember(characters) {
        characters.filter { it.type == "custom" || CharacterAvatars.map.containsKey(it.name) }
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
                title = { Text(selectedPersona ?: "选择角色") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (selectedPersona != null) {
                val ch = visible.firstOrNull { it.name == selectedPersona }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ch != null) {
                        if (ch.type == "custom") {
                            UserAvatar(ch.avatar, Modifier.size(28.dp), cornerRadius = 8.dp)
                        } else {
                            CharacterAvatar(CharacterAvatars.map[ch.name], Modifier.size(28.dp), cornerRadius = 8.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(selectedPersona!!, style = MaterialTheme.typography.titleMedium, color = WinTextDark)
                    Spacer(Modifier.weight(1f))
                    Text("右上角搜索切换", style = MaterialTheme.typography.labelSmall, color = WinTextGray)
                }
                ChatContent(token = token, persona = selectedPersona!!, userAvatar = userAvatar, modifier = Modifier.weight(1f))
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("搜索或选择角色", style = MaterialTheme.typography.titleMedium, color = WinTextDark)
                        Text("开始和 TA 聊天", style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    token: String,
    msg: Message,
    isSelf: Boolean,
    avatarRes: Int?,
    userAvatar: String,
    typewriterCount: Int?
) {
    val bubbleColor = if (isSelf) GlassBlue else GlassWhiteSoft
    val textColor = if (isSelf) Color.White else WinTextDark
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isSelf) {
            BubbleContent(token, msg, bubbleColor, textColor, typewriterCount)
            Spacer(Modifier.width(8.dp))
            UserAvatar(userAvatar, Modifier.size(44.dp), cornerRadius = 14.dp)
        } else {
            CharacterAvatar(avatarRes, Modifier.size(44.dp), cornerRadius = 14.dp)
            Spacer(Modifier.width(8.dp))
            BubbleContent(token, msg, bubbleColor, textColor, typewriterCount)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BubbleContent(token: String, msg: Message, bubbleColor: Color, textColor: Color, typewriterCount: Int?) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showSelect by remember { mutableStateOf(false) }
    var showViewer by remember { mutableStateOf(false) }
    var showForward by remember { mutableStateOf(false) }

    if (msg.contentType == "image") {
        val bmp = remember(msg.content) { decodeBase64Resized(msg.content, 512) }
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(14.dp)).clickable { showViewer = true }
            )
            if (showViewer) {
                ImageViewerDialog(msg.content, onDismiss = { showViewer = false }, onForward = { showForward = true })
            }
            if (showForward) {
                ForwardImageDialog(token, msg.content, onDismiss = { showForward = false })
            }
        } else {
            Text(
                "[图片]",
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .background(bubbleColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    } else {
        val animating = typewriterCount != null && typewriterCount < msg.content.length
        val displayText = if (typewriterCount != null) msg.content.take(typewriterCount) else msg.content
        Box {
            Text(
                text = displayText + if (animating) "▌" else "",
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .background(bubbleColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = 300.dp)
                    .combinedClickable(onClick = {}, onLongClick = { showMenu = true })
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("复制") }, onClick = { copyTextToClipboard(context, msg.content); showMenu = false })
                DropdownMenuItem(text = { Text("选择") }, onClick = { showMenu = false; showSelect = true })
            }
        }
        if (showSelect) {
            SelectTextDialog(msg.content, onDismiss = { showSelect = false })
        }
    }
}

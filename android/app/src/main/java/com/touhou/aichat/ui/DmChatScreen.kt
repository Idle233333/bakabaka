package com.touhou.aichat.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.CallManager
import com.touhou.aichat.DmMessage
import com.touhou.aichat.DmNotifState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DmPeer(val userId: Long, val displayName: String, val avatar: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmChatScreen(
    token: String,
    peer: DmPeer,
    myAvatar: String,
    onBack: () -> Unit,
    onOpenUserProfile: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(peer.userId) {
        DmNotifState.currentOpenPeer = peer.userId
        DmNotifState.clearUnread(peer.userId)
        onDispose { DmNotifState.currentOpenPeer = 0 }
    }
    var messages by remember { mutableStateOf<List<DmMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lastId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    // 语音输入
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "需要麦克风权限才能语音输入", Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle) {
                val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!list.isNullOrEmpty()) input = list[0]
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle) {
                val list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!list.isNullOrEmpty()) input = list[0]
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { speechRecognizer.destroy() }
    }

    val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            CallManager.startCall(peer.userId, peer.displayName, peer.avatar)
        } else {
            Toast.makeText(context, "需要麦克风权限才能通话", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(token, peer.userId) {
        when (val r = Api.fetchDmMessages(token, peer.userId, null)) {
            is ApiResult.Success -> {
                messages = r.data
                lastId = r.data.maxOfOrNull { it.id }
            }
            is ApiResult.Error -> error = r.message
        }
    }

    LaunchedEffect(token, peer.userId) {
        while (true) {
            delay(3000)
            when (val r = Api.fetchDmMessages(token, peer.userId, lastId)) {
                is ApiResult.Success -> {
                    if (r.data.isNotEmpty()) {
                        messages = messages + r.data
                        lastId = r.data.maxOfOrNull { it.id } ?: lastId
                    }
                }
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
                        when (val r = Api.sendDmImage(token, peer.userId, b64)) {
                            is ApiResult.Success -> {
                                when (val m = Api.fetchDmMessages(token, peer.userId, lastId)) {
                                    is ApiResult.Success -> {
                                        if (m.data.isNotEmpty()) {
                                            messages = messages + m.data
                                            lastId = m.data.maxOfOrNull { it.id } ?: lastId
                                        }
                                    }
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
                title = { Text(peer.displayName) },
                actions = {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            CallManager.startCall(peer.userId, peer.displayName, peer.avatar)
                        } else {
                            recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(Icons.Default.Call, contentDescription = "语音通话", tint = WinBlue)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isSelf = msg.senderId != peer.userId
                    DmBubble(token, msg, isSelf, peerAvatar = peer.avatar, myAvatar = myAvatar, onPeerClick = { onOpenUserProfile(peer.userId) })
                }
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = { pickImage.launch("image/*") }, modifier = Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.Default.Add, contentDescription = "发送图片", tint = WinBlue)
                }
                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        }
                        isListening = true
                        try { speechRecognizer.startListening(intent) } catch (e: Throwable) { isListening = false }
                    } else {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }, modifier = Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.Default.Mic, contentDescription = "语音输入", tint = if (isListening) WinBlue else WinTextGray)
                }
                Spacer(Modifier.width(4.dp))
                GlassOutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("发私信...") },
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
                                    when (val r = Api.sendDm(token, peer.userId, text)) {
                                        is ApiResult.Success -> {
                                            when (val m = Api.fetchDmMessages(token, peer.userId, lastId)) {
                                                is ApiResult.Success -> {
                                                    if (m.data.isNotEmpty()) {
                                                        messages = messages + m.data
                                                        lastId = m.data.maxOfOrNull { it.id } ?: lastId
                                                    }
                                                }
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DmBubble(
    token: String,
    msg: DmMessage,
    isSelf: Boolean,
    peerAvatar: String,
    myAvatar: String,
    onPeerClick: () -> Unit
) {
    val bubbleColor = if (isSelf) GlassBlue else GlassWhiteSoft
    val textColor = if (isSelf) Color.White else WinTextDark
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isSelf) {
            DmContent(token, msg, bubbleColor, textColor)
            Spacer(Modifier.width(8.dp))
            UserAvatar(myAvatar, Modifier.size(40.dp), cornerRadius = 12.dp)
        } else {
            UserAvatar(peerAvatar, Modifier.size(40.dp).clickable { onPeerClick() }, cornerRadius = 12.dp)
            Spacer(Modifier.width(8.dp))
            DmContent(token, msg, bubbleColor, textColor)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DmContent(token: String, msg: DmMessage, bubbleColor: Color, textColor: Color) {
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
        Box {
            Text(
                msg.content,
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

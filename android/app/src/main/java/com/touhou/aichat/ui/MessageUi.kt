package com.touhou.aichat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.CharacterAvatars
import com.touhou.aichat.CharacterInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

fun copyTextToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("message", text))
}

fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/bakabaka")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) false
            else {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                true
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val folder = File(dir, "bakabaka")
            folder.mkdirs()
            val file = File(folder, "IMG_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            true
        }
    } catch (e: Exception) {
        false
    }
}

fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

@Composable
fun SelectTextDialog(text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
            Column(Modifier.padding(16.dp)) {
                Text("选择文本", style = MaterialTheme.typography.titleMedium, color = WinTextDark)
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("关闭") }
            }
        }
    }
}

@Composable
fun ImageViewerDialog(base64: String, onDismiss: () -> Unit, onForward: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val bmp = remember(base64) { decodeBase64(base64) }
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss)
                )
            }
            Row(
                Modifier.align(Alignment.BottomCenter).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScaleButton(onClick = {
                    val ok = bmp != null && saveImageToGallery(context, bmp)
                    Toast.makeText(context, if (ok) "已保存到相册" else "保存失败", Toast.LENGTH_SHORT).show()
                }) { Text("保存") }
                if (onForward != null) {
                    ScaleButton(onClick = { onForward(base64) }) { Text("转发") }
                }
                ScaleButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    }
}

@Composable
fun ForwardImageDialog(token: String, imageBase64: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var characters by remember { mutableStateOf<List<CharacterInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        when (val r = Api.fetchCharacters()) {
            is ApiResult.Success -> characters = r.data.filter { it.type == "custom" || CharacterAvatars.map.containsKey(it.name) }
            is ApiResult.Error -> { /* 忽略 */ }
        }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("转发给 AI 角色", style = MaterialTheme.typography.titleMedium, color = WinTextDark)
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        items(characters, key = { it.name }) { ch ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    scope.launch {
                                        Api.sendChatImage(token, ch.name, imageBase64)
                                        onDismiss()
                                    }
                                }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ch.type == "custom") {
                                    UserAvatar(ch.avatar, Modifier.size(40.dp), cornerRadius = 10.dp)
                                } else {
                                    CharacterAvatar(CharacterAvatars.map[ch.name], Modifier.size(40.dp), cornerRadius = 10.dp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(ch.name, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
                            }
                        }
                    }
                }
            }
        }
    }
}


package com.touhou.aichat.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import com.touhou.aichat.MeInfo
import com.touhou.aichat.UpdateInfo
import com.touhou.aichat.UpdateManager
import com.touhou.aichat.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    token: String,
    profile: MeInfo?,
    wallpaperIndex: Int,
    onWallpaperChanged: (Int) -> Unit,
    onProfileUpdated: (MeInfo) -> Unit,
    onUpdateFound: (UpdateInfo) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf(profile?.nickname ?: "") }
    var avatarBase64 by remember { mutableStateOf(profile?.avatar ?: "") }
    var signature by remember { mutableStateOf(profile?.signature ?: "") }
    var gender by remember { mutableStateOf(profile?.gender?.ifBlank { "保密" } ?: "保密") }
    var company by remember { mutableStateOf(profile?.company ?: "") }
    var address by remember { mutableStateOf(profile?.address ?: "") }
    var genderExpanded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }

    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { decodeAndResize(context, uri, 256) }
                if (bmp != null) {
                    avatarBase64 = withContext(Dispatchers.IO) { bitmapToBase64Jpeg(bmp) }
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
                title = { Text("设置") }
            )
        }
    ) { padding ->
        if (showEdit) {
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    base64 = avatarBase64,
                    modifier = Modifier.size(96.dp).clickable { pickAvatar.launch("image/*") },
                    cornerRadius = 16.dp
                )
                Text("点击更换头像", style = MaterialTheme.typography.bodySmall, color = WinTextDark, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(16.dp))
                GlassOutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                GlassOutlinedTextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text("个人签名") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))

                // 性别下拉
                Text("性别", style = MaterialTheme.typography.labelSmall, color = WinTextGray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF8A8A8A), RoundedCornerShape(6.dp))
                        .clickable { genderExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(gender, style = MaterialTheme.typography.bodyLarge, color = WinTextDark, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择性别", tint = WinTextGray)
                    }
                }
                DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                    listOf("男", "女", "保密").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { gender = option; genderExpanded = false }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                GlassOutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("公司") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                GlassOutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ScaleButton(
                        onClick = {
                            saving = true
                            error = null
                            scope.launch {
                                val r = Api.updateProfile(
                                    token = token,
                                    nickname = nickname.trim(),
                                    avatar = avatarBase64.ifEmpty { null },
                                    signature = signature.trim(),
                                    gender = gender,
                                    company = company.trim(),
                                    address = address.trim()
                                )
                                when (r) {
                                    is ApiResult.Success -> {
                                        when (val me = Api.getMe(token)) {
                                            is ApiResult.Success -> onProfileUpdated(me.data)
                                            is ApiResult.Error -> { /* 忽略 */ }
                                        }
                                        showEdit = false
                                    }
                                    is ApiResult.Error -> error = r.message
                                }
                                saving = false
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (saving) "保存中..." else "保存")
                    }
                    ScaleButton(onClick = { showEdit = false }, modifier = Modifier.weight(1f)) {
                        Text("取消")
                    }
                }
            }
        } else {
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                SectionHeader("资料")
                SettingRow("编辑资料", onClick = { showEdit = true })
                Spacer(Modifier.height(12.dp))
                SectionHeader("外观")
                Text("壁纸", style = MaterialTheme.typography.bodyMedium, color = WinTextDark, modifier = Modifier.padding(vertical = 4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item(key = "white") {
                        WallpaperThumb(
                            label = "纯白",
                            selected = wallpaperIndex == -1,
                            onClick = { onWallpaperChanged(-1) }
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.White))
                        }
                    }
                    items(Wallpapers.list.size) { i ->
                        WallpaperThumb(
                            label = "壁纸 ${i + 1}",
                            selected = wallpaperIndex == i,
                            onClick = { onWallpaperChanged(i) }
                        ) {
                            Image(
                                painter = painterResource(Wallpapers.list[i]),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingRow("主题", value = "浅色")
                SettingRow("字体", value = "微软雅黑")
                Spacer(Modifier.height(12.dp))
                SectionHeader("关于")
                SettingRow("检查更新", onClick = {
                    if (!checkingUpdate) {
                        checkingUpdate = true
                        scope.launch {
                            when (val r = UpdateManager.checkForUpdate()) {
                                is UpdateManager.CheckResult.Available -> onUpdateFound(r.info)
                                is UpdateManager.CheckResult.UpToDate -> Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                                is UpdateManager.CheckResult.Failed -> Toast.makeText(context, "检查失败: ${r.message}", Toast.LENGTH_SHORT).show()
                            }
                            checkingUpdate = false
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = WinTextGray,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun SettingRow(label: String, value: String? = null, onClick: (() -> Unit)? = null) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = clickableModifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = WinTextDark)
        Spacer(Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = WinTextGray)
        }
    }
}

@Composable
private fun WallpaperThumb(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 64.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) WinBlue else Color(0x22000000),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onClick)
        ) {
            content()
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) WinBlue else WinTextDark)
    }
}

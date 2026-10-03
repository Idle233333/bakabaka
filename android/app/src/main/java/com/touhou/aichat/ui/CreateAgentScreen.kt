package com.touhou.aichat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAgentScreen(
    token: String,
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var backstory by remember { mutableStateOf("") }
    var speakingStyle by remember { mutableStateOf("") }
    var avatarBase64 by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                title = { Text("创建角色") }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(
                base64 = avatarBase64,
                modifier = Modifier.size(88.dp).clickable { pickAvatar.launch("image/*") },
                cornerRadius = 16.dp
            )
            Text("点头像选头像（可选）", style = MaterialTheme.typography.bodySmall, color = WinTextDark, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(16.dp))

            GlassOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名字") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            GlassOutlinedTextField(
                value = appearance,
                onValueChange = { appearance = it },
                label = { Text("长相（外貌特征）") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            GlassOutlinedTextField(
                value = personality,
                onValueChange = { personality = it },
                label = { Text("性格（脾气、喜好）") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            GlassOutlinedTextField(
                value = backstory,
                onValueChange = { backstory = it },
                label = { Text("背景故事") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            GlassOutlinedTextField(
                value = speakingStyle,
                onValueChange = { speakingStyle = it },
                label = { Text("说话风格（语气、口癖）") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            ScaleButton(
                onClick = {
                    if (name.isBlank()) {
                        error = "请输入名字"
                    } else {
                        val persona = listOf(
                            "长相" to appearance,
                            "性格" to personality,
                            "背景故事" to backstory,
                            "说话风格" to speakingStyle
                        ).filter { it.second.isNotBlank() }
                            .joinToString("\n") { "${it.first}：${it.second.trim()}" }
                        creating = true
                        error = null
                        scope.launch {
                            when (val r = Api.createAgent(token, name.trim(), persona, avatarBase64.ifEmpty { null })) {
                                is ApiResult.Success -> onCreated()
                                is ApiResult.Error -> error = r.message
                            }
                            creating = false
                        }
                    }
                },
                enabled = !creating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (creating) "创建中..." else "创建")
            }
        }
    }
}

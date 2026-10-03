package com.touhou.aichat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onGoLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("注册", style = MaterialTheme.typography.headlineMedium, color = WinTextDark)
        Spacer(Modifier.height(12.dp))
        GlassOutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名（至少2个字符）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        GlassOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码（至少6个字符）") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            GlassOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            ScaleButton(
                onClick = {
                    if (email.isBlank()) {
                        error = "请先输入邮箱"
                    } else {
                        error = null
                        info = null
                        scope.launch {
                            when (val r = Api.sendCode(email.trim())) {
                                is ApiResult.Success -> {
                                    info = "验证码已发送"
                                    countdown = 60
                                }
                                is ApiResult.Error -> error = r.message
                            }
                        }
                    }
                },
                enabled = countdown == 0
            ) {
                Text(if (countdown > 0) "${countdown}s" else "发送验证码")
            }
        }
        Spacer(Modifier.height(8.dp))
        GlassOutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("验证码") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (info != null) Text(info!!, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        ScaleButton(
            onClick = {
                if (username.length < 2) {
                    error = "用户名至少2个字符"
                } else if (password.length < 6) {
                    error = "密码至少6个字符"
                } else if (email.isBlank() || code.isBlank()) {
                    error = "请填写邮箱和验证码"
                } else {
                    loading = true
                    error = null
                    scope.launch {
                        when (val r = Api.register(username.trim(), password, email.trim(), code.trim())) {
                            is ApiResult.Success -> {
                                info = "注册成功，请登录"
                                onRegistered()
                            }
                            is ApiResult.Error -> error = r.message
                        }
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "注册中..." else "注册")
        }
        TextButton(onClick = onGoLogin, modifier = Modifier.fillMaxWidth()) {
            Text("已有账号？去登录")
        }
    }
}

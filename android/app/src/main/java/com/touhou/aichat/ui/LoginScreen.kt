package com.touhou.aichat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.touhou.aichat.Api
import com.touhou.aichat.ApiResult
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLogin: (token: String, username: String) -> Unit,
    onGoRegister: () -> Unit
) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = GlassWhite,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("登录", style = MaterialTheme.typography.headlineMedium, color = WinTextDark)
                Spacer(Modifier.height(20.dp))
                GlassOutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("账号（用户名或邮箱）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                GlassOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(20.dp))
                ScaleButton(
                    onClick = {
                        if (account.isBlank() || password.isBlank()) {
                            error = "请输入账号和密码"
                        } else {
                            loading = true
                            error = null
                            scope.launch {
                                when (val r = Api.login(account.trim(), password)) {
                                    is ApiResult.Success -> onLogin(r.data.token, r.data.username)
                                    is ApiResult.Error -> error = r.message
                                }
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    Text(if (loading) "登录中..." else "登录")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
                    Text("没有账号？去注册")
                }
            }
        }
    }
}

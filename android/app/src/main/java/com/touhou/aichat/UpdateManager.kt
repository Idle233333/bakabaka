package com.touhou.aichat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object UpdateManager {
    enum class Phase { IDLE, DOWNLOADING, PROCESSING, DONE, ERROR }

    data class Progress(
        val phase: Phase = Phase.IDLE,
        val downloaded: Long = 0,
        val total: Long = 0,
        val speed: Long = 0,
        val error: String? = null
    )

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    @Volatile
    var pendingUpdate: UpdateInfo? = null

    fun start(context: Context, info: UpdateInfo) {
        pendingUpdate = info
        _progress.value = Progress(Phase.IDLE, 0, info.size, 0)
        val intent = Intent(context, UpdateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        info: UpdateInfo,
        onProgress: ((Progress) -> Unit)? = null
    ) {
        fun emit(p: Progress) {
            _progress.value = p
            onProgress?.invoke(p)
        }
        try {
            emit(Progress(Phase.DOWNLOADING, 0, info.size, 0))
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()
            val encrypted = Api.downloadUpdateStreaming { downloaded ->
                val now = System.currentTimeMillis()
                if (now - lastTime >= 200) {
                    val speed = if (now > lastTime) (downloaded - lastBytes) * 1000L / (now - lastTime) else 0L
                    lastBytes = downloaded
                    lastTime = now
                    emit(Progress(Phase.DOWNLOADING, downloaded, info.size, speed))
                }
            }
            emit(Progress(Phase.PROCESSING, info.size, info.size, 0))
            val apk = decryptAndVerify(encrypted, info.sha256)
            saveToCache(context, apk)
            emit(Progress(Phase.DONE, info.size, info.size, 0))
        } catch (e: Throwable) {
            emit(Progress(Phase.ERROR, 0, info.size, 0, e.message ?: "更新失败"))
        }
    }

    fun installFromCache(context: Context): Boolean {
        val file = File(context.cacheDir, "updates/app.apk")
        if (!file.exists()) return false
        return if (canInstallUnknownSources(context)) {
            install(context, file)
            true
        } else {
            openInstallPermissionSettings(context)
            false
        }
    }

    fun decryptAndVerify(encrypted: ByteArray, expectedSha256: String): ByteArray {
        val decrypted = UpdateCrypto.decrypt(encrypted)
        val actual = UpdateCrypto.sha256Hex(decrypted)
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            throw IllegalStateException("SHA-256 校验失败")
        }
        return decrypted
    }

    fun saveToCache(context: Context, apkBytes: ByteArray): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "app.apk")
        file.writeBytes(apkBytes)
        return file
    }

    fun install(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "拉起安装失败: " + (e.message ?: "未知错误"))
            Toast.makeText(context, "无法打开安装页面，请手动安装", Toast.LENGTH_SHORT).show()
        }
    }

    fun canInstallUnknownSources(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:" + context.packageName)
            }
            context.startActivity(intent)
        }
    }

    sealed class CheckResult {
        data class Available(val info: UpdateInfo) : CheckResult()
        object UpToDate : CheckResult()
        data class Failed(val message: String) : CheckResult()
    }

    suspend fun checkForUpdate(): CheckResult {
        var lastError: String? = null
        repeat(3) { attempt ->
            when (val r = Api.checkUpdate()) {
                is ApiResult.Success -> {
                    return if (r.data.versionCode > BuildConfig.VERSION_CODE) {
                        CheckResult.Available(r.data)
                    } else {
                        CheckResult.UpToDate
                    }
                }
                is ApiResult.Error -> {
                    lastError = r.message
                    Log.w("UpdateManager", "检查更新失败(第${attempt + 1}次): ${r.message}")
                    if (attempt < 2) delay(1500)
                }
            }
        }
        Log.w("UpdateManager", "检查更新多次失败，放弃: $lastError")
        return CheckResult.Failed(lastError ?: "检查更新失败")
    }
}

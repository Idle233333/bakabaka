package com.touhou.aichat

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object UpdateCrypto {
    private val KEY_HEX = "REPLACE_WITH_64_HEX_CHARS" // TODO: 你自己的 AES-256 密钥（64 位 hex），需与 server .env 的 UPDATE_KEY 一致
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    fun decrypt(data: ByteArray): ByteArray {
        require(data.size >= 28) { "加密数据太短" }
        val iv = data.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(hexToBytes(KEY_HEX), "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        // Java AES/GCM 解密时输入需为 ciphertext 后接 authTag；不复制密文，直接拷进合并缓冲
        val ciphertextLen = data.size - 28
        val input = ByteArray(ciphertextLen + 16)
        System.arraycopy(data, 28, input, 0, ciphertextLen)
        System.arraycopy(data, 12, input, ciphertextLen, 16)
        return cipher.doFinal(input)
    }

    fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xFF
            sb.append(HEX_CHARS[v ushr 4]).append(HEX_CHARS[v and 0x0F])
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = hexNibble(hex[i * 2])
            val lo = hexNibble(hex[i * 2 + 1])
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private fun hexNibble(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> throw IllegalArgumentException("非法十六进制字符: $c")
    }
}

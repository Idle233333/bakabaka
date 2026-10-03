package com.touhou.aichat

object NativeToken {
    init {
        System.loadLibrary("native")
    }

    external fun xorBytes(data: ByteArray, key: Byte): ByteArray
}

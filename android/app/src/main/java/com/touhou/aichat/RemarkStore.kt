package com.touhou.aichat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RemarkStore {
    private val _remarks = MutableStateFlow<Map<Long, String>>(emptyMap())
    val state: StateFlow<Map<Long, String>> = _remarks.asStateFlow()

    fun load(remarks: List<Remark>) {
        _remarks.value = remarks.associate { it.targetId to it.remark }.filterValues { it.isNotBlank() }
    }

    fun set(targetId: Long, remark: String) {
        val m = _remarks.value.toMutableMap()
        if (remark.isBlank()) m.remove(targetId) else m[targetId] = remark
        _remarks.value = m
    }

    fun get(targetId: Long): String? = _remarks.value[targetId]
}

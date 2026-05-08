package com.leonjose.tvbooster

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    var isSelected: Boolean = false
)

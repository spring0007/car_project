package com.launcher.yfd_ui6.chemo2

// 车型分组（如aveo、captiva）
data class CarModelGroup(
    val groupName: String, // 如 "aveo"
    val displayName: String, // 如 "Aveo"
    val versions: List<CarModelVersion> // 该车型的不同年份版本
)
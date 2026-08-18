package com.launcher.yfd_ui6.chemo2

import java.io.File

data class CarModel(
    val name: String,
    val displayName: String,
    val imageName: String,
    val imagePath: String, // 可以是 assets 路径或文件路径
    val brandFolder: String,
    val assetPath: String? = null, // 新增字段，保存 assets 原始路径
) {
    // 判断是否是 assets 路径
    val isAssetsPath: Boolean
        get() = imagePath.startsWith("chemo/") || !File(imagePath).exists()
}
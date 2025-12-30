package com.launcher.yfd_ui01.chemo2

// 车型版本（如2012_2016、2006_2010）
data class CarModelVersion(
    val name: String, // 完整名称，如 "aveo_2012_2016"
    val displayName: String, // 如 "2012-2016"
    val imagePath: String, // assets路径，如 "chemo/Chevrolet/aveo_2012_2016.png"
    val brandFolder: String // 品牌文件夹
)
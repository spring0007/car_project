package com.launcher.yfd_ui6.chemo2

data class CarBrand(
    val name: String,
    val folderName: String,
    val initial: Char,
    val modelGroups: List<CarModelGroup> // 改为车型分组列表
)


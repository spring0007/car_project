package com.launcher.yfd_ui01.chemo2

// 车型版本（如2012_2016、2006_2010）

enum class CarModelSource {
    SDCARD,
    USB,
    CUSTOM
}
data class CarModelVersion(
    val name: String, // 完整名称，如 "aveo_2012_2016"
    val displayName: String, // 如 "2012-2016"
    val imagePath: String, // assets路径，如 "chemo/Chevrolet/aveo_2012_2016.png" ,storage/emulated/0/823a43690df6f86e071971529f4e5acd.jpeg
    val brandFolder: String?, // 显示assets品牌文件夹 ;在显示sd卡或者U盘时，此字段为空
    val carSource: CarModelSource = CarModelSource.CUSTOM,
)
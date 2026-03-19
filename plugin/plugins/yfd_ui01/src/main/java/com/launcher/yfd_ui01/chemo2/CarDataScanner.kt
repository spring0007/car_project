package com.launcher.yfd_ui01.chemo2

import android.content.Context
import com.launcher.yfd_ui01.utils.LogUtil
import java.io.File
import java.io.FileOutputStream

// 文件扫描工具类
object CarDataScanner {
    
    private const val ASSETS_FOLDER = "chemo"
    
    fun scanCarData(context: Context): Map<Char, List<CarBrand>> {
        val brandMap = mutableMapOf<Char, MutableList<CarBrand>>()
        
        try {
            // 获取 AssetManager
            val assetManager = context.assets
            
            // 列出根目录下的所有文件/文件夹（车企文件夹）
            val brandFolders = assetManager.list(ASSETS_FOLDER)
            
            if (brandFolders.isNullOrEmpty()) {
                LogUtil.e( "assets/chemo 文件夹为空或不存在")
                return emptyMap()
            }
            
            brandFolders.forEach { brandFolderName ->
                // 检查是否是文件夹（在 assets 中，文件夹也会被列出）
                try {
                    // 尝试列出该文件夹下的文件
                    val modelFiles = assetManager.list("$ASSETS_FOLDER/$brandFolderName")
                    
                    if (!modelFiles.isNullOrEmpty()) {
                        val brandName = formatBrandName(brandFolderName)
                        val initial = brandName.firstOrNull()?.uppercaseChar() ?: '#'
                        
                        // 扫描车型分组
                        val modelGroups = scanModelGroups(context, brandFolderName, modelFiles)
                        
                        if (modelGroups.isNotEmpty()) {
                            val carBrand = CarBrand(
                                name = brandName,
                                folderName = brandFolderName,
                                initial = initial,
                                modelGroups = modelGroups
                            )
                            
                            brandMap.getOrPut(initial) { mutableListOf() }.add(carBrand)
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.e( "扫描车企文件夹失败: $brandFolderName", e)
                }
            }
            
            // 按品牌名称排序
            brandMap.forEach { (_, brands) ->
                brands.sortBy { it.name }
            }
            
        } catch (e: Exception) {
            LogUtil.e( "扫描assets数据失败", e)
        }
        
        return brandMap
    }
    
    private fun scanModelGroups(
        context: Context,
        brandFolder: String,
        fileNames: Array<String>
    ): List<CarModelGroup> {
        // 按车型名称分组
        val modelMap = mutableMapOf<String, MutableList<CarModelVersion>>()
        
        fileNames.forEach { fileName ->
            if (isImageFile(fileName)) {
                val modelVersion = parseCarModelVersion(context, brandFolder, fileName)
                val groupName = extractGroupName(fileName)
                
                modelMap.getOrPut(groupName) { mutableListOf() }.add(modelVersion)
            }
        }
        
        // 创建车型分组，并按车型名称排序
        return modelMap.entries.map { (groupName, versions) ->
            // 按年份排序版本
            val sortedVersions = versions.sortedBy { it.displayName }
            
            CarModelGroup(
                groupName = groupName,
                displayName = formatModelGroupName(groupName),
                versions = sortedVersions
            )
        }.sortedBy { it.displayName }
    }
    
    private fun parseCarModelVersion(
        context: Context,
        brandFolder: String,
        fileName: String
    ): CarModelVersion {
        val displayName = parseYearInfo(fileName)
        val assetPath = "$ASSETS_FOLDER/$brandFolder/$fileName"
        
        return CarModelVersion(
            name = fileName,
            displayName = displayName,
            imagePath = assetPath,
            brandFolder = brandFolder,

           // assetPath = assetPath,
            // 新增字段，专门保存 assets 路径
        )
    }
    
//    /**
//     * 从 assets 读取图片到 Bitmap
//     */
//    fun loadImageFromAssets(context: Context, assetPath: String): Bitmap? {
//        return try {
//            val inputStream = context.assets.open(assetPath)
//            val bitmap = BitmapFactory.decodeStream(inputStream)
//            inputStream.close()
//            bitmap
//        } catch (e: Exception) {
//            Log.e("CarDataScanner", "从assets加载图片失败: $assetPath", e)
//            null
//        }
//    }
    
    /**
     * 复制 assets 图片到应用私有目录
     */
    fun copyImageToPrivateStorage(context: Context, assetPath: String, targetFileName: String): String? {
        return try {
            // 创建目标目录
            val destDir = File(context.filesDir, "saved_cars")
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            
            // 目标文件
            val destFile = File(destDir, targetFileName)
            
            // 从 assets 读取并写入到私有目录
            val inputStream = context.assets.open(assetPath)
            val outputStream = FileOutputStream(destFile)
            
            inputStream.copyTo(outputStream)
            
            inputStream.close()
            outputStream.close()
            
            destFile.absolutePath
        } catch (e: Exception) {
            LogUtil.e( "复制图片到私有目录失败", e)
            null
        }
    }

    
//    private fun parseDisplayName(fileName: String): String {
//        // 解析文件名格式：sonic_2012_2016 -> Sonic (2012-2016)
//        val parts = fileName.split("_")
//
//        return if (parts.size >= 3) {
//            val carName = parts[0].replaceFirstChar { it.uppercase() }
//            val startYear = parts[1]
//            val endYear = parts[2]
//            "$carName ($startYear-$endYear)"
//        } else {
//            fileName.replaceFirstChar { it.uppercase() }
//        }
//    }
//
    
    /**
     * 从文件名提取车型组名（如 aveo_2012_2016.png -> aveo）
     */
    private fun extractGroupName(fileName: String): String {
        val withoutExtension = fileName.substringBeforeLast(".")
        // 按第一个下划线分割
        return withoutExtension.substringBefore("_").lowercase()
    }
    
    /**
     * 提取年份信息（如 aveo_2012_2016.png -> 2012-2016）
     */
    private fun parseYearInfo(fileName: String): String {
        val withoutExtension = fileName.substringBeforeLast(".")
        val parts = withoutExtension.split("_")
        
        return when (parts.size) {
            3 -> "${parts[1]}-${parts[2]}" // aveo_2012_2016 -> 2012-2016
            4 -> "${parts[2]}-${parts[3]}" // 可能的格式：aveo_sport_2012_2016
            else -> parts.lastOrNull() ?: "Unknown"
        }
    }
    
    /**
     * 格式化车型组名（如 aveo -> Aveo）
     */
    private fun formatModelGroupName(groupName: String): String {
        return groupName.replaceFirstChar { it.uppercase() }
    }
    private fun formatBrandName(folderName: String): String {
        return folderName
            .replace("_", " ")
            .replaceFirstChar { it.uppercase() }
    }
    private fun isImageFile(fileName: String): Boolean {
        val extensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp")
        val lowerName = fileName.lowercase()
        return extensions.any { lowerName.endsWith(it) }
    }
}
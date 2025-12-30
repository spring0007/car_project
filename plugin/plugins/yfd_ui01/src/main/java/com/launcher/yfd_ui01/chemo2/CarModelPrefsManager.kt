package com.launcher.yfd_ui01.chemo2

import android.content.Context
import java.io.File
import java.security.MessageDigest

object CarModelPrefsManager {
    
    private const val PREFS_NAME = "car_model_settings"
    private const val KEY_IMAGE_PATH = "image_path"
    private const val KEY_MODEL_NAME = "model_name"
    private const val KEY_BRAND_FOLDER = "brand_folder"
    private const val KEY_IMAGE_NAME = "image_name"
    
    fun saveCarModel(context: Context, carModel: CarModel) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 先复制图片到私有目录
        val savedPath = copyImageToPrivateDir(context, carModel)
        
        prefs.edit().apply {
            putString(KEY_IMAGE_PATH, savedPath)
            putString(KEY_MODEL_NAME, carModel.displayName)
            putString(KEY_BRAND_FOLDER, carModel.brandFolder)
            putString(KEY_IMAGE_NAME, carModel.imageName)
            apply()
        }
    }
    
    private fun copyImageToPrivateDir(context: Context, carModel: CarModel): String {
        val sourceFile = File(carModel.imagePath)
        val destDir = File(context.filesDir, "user_cars")
        
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        
        // 使用MD5作为文件名避免重复
        val fileName = "${md5(carModel.imagePath)}.${getFileExtension(carModel.imageName)}"
        val destFile = File(destDir, fileName)
        
        if (sourceFile.exists()) {
            sourceFile.copyTo(destFile, overwrite = true)
        }
        
        return destFile.absolutePath
    }
    
    fun loadCarModel(context: Context): CarModel? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val imagePath = prefs.getString(KEY_IMAGE_PATH, null)
        val modelName = prefs.getString(KEY_MODEL_NAME, null)
        
        if (imagePath != null && modelName != null && File(imagePath).exists()) {
            return CarModel(
                name = modelName,
                displayName = modelName,
                imageName = prefs.getString(KEY_IMAGE_NAME, "") ?: "",
                imagePath = imagePath,
                brandFolder = prefs.getString(KEY_BRAND_FOLDER, "") ?: "",
            )
        }
        
        return null
    }
    
    fun clearCarModel(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val imagePath = prefs.getString(KEY_IMAGE_PATH, null)
        
        // 删除保存的图片
        if (imagePath != null) {
            File(imagePath).delete()
        }
        
        // 清除设置
        prefs.edit().clear().apply()
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
    
    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }
}
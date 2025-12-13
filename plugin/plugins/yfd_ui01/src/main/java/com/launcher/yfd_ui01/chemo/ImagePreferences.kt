package com.launcher.yfd_ui01.chemo

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit

class ImagePreferences(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "image_settings"
        private const val KEY_SELECTED_IMAGE_PATH = "selected_image_path"
        private const val KEY_SELECTED_ASSET_PATH = "selected_asset_path"
        private const val IMAGE_DIR = "selected_images"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 保存选择的图片
     */
    fun saveSelectedImage(assetPath: String, bitmap: Bitmap): Boolean {
        return try {
            // 1. 保存asset路径
            saveAssetPath(assetPath)
            
            // 2. 保存bitmap到文件
            saveBitmapToFile(bitmap)
            
            true
        } catch (e: Exception) {
            Log.e("ImagePreferences", "保存图片失败: ${e.message}")
            false
        }
    }
    
    /**
     * 获取保存的图片
     */
    fun getSavedImage(): Bitmap? {
        return try {
            // 先尝试从文件加载
            getBitmapFromFile() ?: 
            // 如果文件不存在，从assets加载
            getBitmapFromAssets()
        } catch (e: Exception) {
            Log.e("ImagePreferences", "加载保存的图片失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取保存的asset路径
     */
    fun getSavedAssetPath(): String? {
        return sharedPreferences.getString(KEY_SELECTED_ASSET_PATH, null)
    }
    
    /**
     * 清除保存的图片
     */
    fun clearSavedImage() {
        // 删除图片文件
        deleteImageFile()
        // 清除SharedPreferences中的记录
        sharedPreferences.edit {
            remove(KEY_SELECTED_IMAGE_PATH)
            remove(KEY_SELECTED_ASSET_PATH)
        }
    }
    
    // ============ 私有方法 ============
    
    private fun saveAssetPath(assetPath: String) {
        sharedPreferences.edit {putString(KEY_SELECTED_ASSET_PATH, assetPath)}
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Boolean {
        return try {
            val file = getSavedImageFile()
            
            // 创建目录（如果不存在）
            file.parentFile?.mkdirs()
            
            // 保存图片到文件
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // 保存文件路径到SharedPreferences
            sharedPreferences.edit {
                putString(KEY_SELECTED_IMAGE_PATH, file.absolutePath)
            }

            true
        } catch (e: Exception) {
            Log.e("ImagePreferences", "保存图片到文件失败: ${e.message}")
            false
        }
    }
    
    private fun getBitmapFromFile(): Bitmap? {
        return try {
            val file = getSavedImageFile()
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImagePreferences", "从文件加载图片失败: ${e.message}")
            null
        }
    }
    
    private fun getBitmapFromAssets(): Bitmap? {
        return try {
            val assetPath = getSavedAssetPath()
            if (assetPath != null) {
                val inputStream = context.assets.open(assetPath)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImagePreferences", "从assets加载图片失败: ${e.message}")
            null
        }
    }
    
    private fun getSavedImageFile(): File {
        // 使用应用私有存储，不需要权限
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        
        return File(dir, "$IMAGE_DIR/selected_image.png")
    }
    
    private fun deleteImageFile() {
        try {
            val file = getSavedImageFile()
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("ImagePreferences", "删除图片文件失败: ${e.message}")
        }
    }
}
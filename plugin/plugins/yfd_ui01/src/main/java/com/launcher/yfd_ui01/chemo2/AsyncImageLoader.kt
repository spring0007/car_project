package com.launcher.yfd_ui01.chemo2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AsyncImageLoader {

    suspend fun loadBitmapFromAssets(context: Context, assetPath: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open(assetPath)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap
            } catch (e: Exception) {
                Log.e("AsyncImageLoader", "加载图片失败: $assetPath", e)
                null
            }
        }
    }

    suspend fun copyAssetToFile(context: Context, assetPath: String, destFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open(assetPath)
                val outputStream = FileOutputStream(destFile)

                inputStream.copyTo(outputStream)

                inputStream.close()
                outputStream.close()

                true
            } catch (e: Exception) {
                Log.e("AsyncImageLoader", "复制文件失败", e)
                false
            }
        }
    }
}
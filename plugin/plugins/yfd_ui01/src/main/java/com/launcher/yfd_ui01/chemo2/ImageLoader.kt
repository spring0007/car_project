package com.launcher.yfd_ui01.chemo2

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.launcher.yfd_ui01.R
import com.awell.library.util.LogUtil
import java.io.File

object ImageLoader {

    /**
     * 从 assets 加载图片到 ImageView
     */
    fun loadImageFromAssets(context: Context, imagePath: String, imageView: ImageView) {
        if (imagePath.startsWith("chemo/")) {
            try {
                // 直接使用 InputStream 加载，避免 Glide 的问题
                val inputStream = context.assets.open(imagePath)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // 如果加载失败，设置一个简单的占位图
                imageView.setImageResource(R.drawable.a3_2008_2012)
                LogUtil.e( "从assets加载图片失败: $imagePath", e)
            }
        } else {
            // 普通文件路径
            Glide.with(context)
                .load(File(imagePath))
                .into(imageView)
        }
    }

    /**
     * 加载缩略图
     */
    fun loadThumbnail(context: Context, imagePath: String, imageView: ImageView) {
        if (imagePath.startsWith("chemo/")) {
            try {
                val inputStream = context.assets.open(imagePath)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4 // 缩小图片，节省内存
                }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.a3_2008_2012)
                LogUtil.e( "加载缩略图失败: $imagePath", e)
            }
        } else {
            Glide.with(context)
                .load(File(imagePath))
                .into(imageView)
        }
    }
    
    /**
     * 加载大图
     */
    fun loadLargeImage(context: Context, imagePath: String, imageView: ImageView) {
        if (imagePath.startsWith("chemo/")) {
            try {
                // 直接使用 InputStream 加载，避免 Glide 的限制
                val inputStream = context.assets.open(imagePath)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.a3_2008_2012)
                LogUtil.e( "加载大图失败: $imagePath", e)
            }
        } else {
            Glide.with(context.applicationContext)
                .load(File(imagePath))
                .into(imageView)
        }
    }
}
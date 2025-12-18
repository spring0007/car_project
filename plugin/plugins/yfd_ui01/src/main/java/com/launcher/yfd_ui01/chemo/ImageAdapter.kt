package com.launcher.yfd_ui01.chemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import com.launcher.yfd_ui01.R

class ImageAdapter(
    private val context: Context,
    private val imageList: List<ImageItem>,
    private val appScope: AppCoroutineScope  // 使用自定义的AppCoroutineScope
) : BaseAdapter() {
    
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val imageCache = ConcurrentHashMap<String, Bitmap>()
    private val loadingJobs = mutableMapOf<Int, kotlinx.coroutines.Job>()
    
    override fun getCount(): Int = imageList.size
    override fun getItem(position: Int): ImageItem = imageList[position]
    override fun getItemId(position: Int): Long = position.toLong()
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder
        
        if (convertView == null) {
            view = inflater.inflate(R.layout.item_image, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        
        val imageItem = getItem(position)
        viewHolder.tvImageName.text = imageItem.fileName
        
        // 取消该位置可能正在进行的加载任务
        loadingJobs[position]?.cancel()
        
        // 从缓存获取或异步加载
        val cachedBitmap = imageCache[imageItem.assetPath]
        if (cachedBitmap != null) {
            viewHolder.ivImage.setImageBitmap(cachedBitmap)
        } else {
            // 设置占位符
            viewHolder.ivImage.setImageResource(R.drawable.a3_2008_2012)
            // 异步加载图片
            loadImageAsync(viewHolder.ivImage, imageItem, position)
        }
        
        return view
    }
    
    private fun loadImageAsync(imageView: ImageView, imageItem: ImageItem, position: Int) {
        val job = appScope.launch {
            try {
                // 在IO线程加载图片
                val bitmap = appScope.io {
                    loadBitmapFromAssets(imageItem.assetPath)
                }
                
                bitmap?.let {
                    // 放入缓存
                    imageCache[imageItem.assetPath] = it
                    
                    // 确保是当前视图的请求（防止复用导致的图片错位）
                    if (imageView.tag == imageItem.assetPath) {
                        // 在主线程更新UI
                        appScope.main {
                            imageView.setImageBitmap(it)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 可以在这里处理加载失败的情况
            }
        }
        
        // 保存加载任务以便后续取消
        loadingJobs[position] = job
        
        // 设置tag以便验证（防止图片错位）
        imageView.tag = imageItem.assetPath
    }
    
    private suspend fun loadBitmapFromAssets(assetPath: String): Bitmap? {
        return try {
            val inputStream: InputStream = context.assets.open(assetPath)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1 // 缩小图片以减少内存占用
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 取消所有加载任务
        loadingJobs.values.forEach { it.cancel() }
        loadingJobs.clear()
        
        // 清理缓存
        imageCache.values.forEach { it.recycle() }
        imageCache.clear()
    }
    
    private class ViewHolder(view: View) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvImageName: TextView = view.findViewById(R.id.tvImageName)
    }
}
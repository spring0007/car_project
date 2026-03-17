package com.launcher.yfd_ui01.chemo2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.utils.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt

class ImagePickerAdapter(
    private val context: Context,
    private var images: List<CarModelVersion>,
    private val onImageClick: (CarModelVersion) -> Unit
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

   // private var selectedPosition: Int = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.iv_custom_image)
        val tvSource: TextView = view.findViewById(R.id.tv_custom_source)
        val tvName: TextView = view.findViewById(R.id.tv_custom_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val carModelVersion = images[position]
        
        // 设置图片名称
        holder.tvName.text = carModelVersion.displayName
       // LogUtil.i("ImagePickerAdapter: ${carModelVersion.displayName}, ${carModelVersion.carSource}, ${carModelVersion.imagePath}, ${carModelVersion.brandFolder}, ${carModelVersion.urlPath}")
        // 设置来源标签和样式
        when (carModelVersion.carSource) {
            CarModelSource.SDCARD -> {
                holder.tvSource.text = context.getString(R.string.sd_)
                holder.tvSource.setBackgroundColor("#4CAF50".toColorInt())
            }
            CarModelSource.USB -> {
                holder.tvSource.text = context.getString(R.string.usb_)
                holder.tvSource.setBackgroundColor("#2196F3".toColorInt())
            }
            CarModelSource.CUSTOM -> {
                holder.tvSource.text = context.getString(R.string.custom_)
                holder.tvSource.setBackgroundColor("#FF9800".toColorInt())
            }

        }
        
        // 加载图片
        loadImage(holder.ivImage, carModelVersion.imagePath)
        
        // 点击事件
        holder.itemView.setOnClickListener {
           // val previousPosition = selectedPosition
           // selectedPosition = position
            
            // 刷新之前选中的项和当前项
            /*if (previousPosition != -1) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(position)*/
            
            onImageClick(carModelVersion)
        }
    }

    private fun loadImage(imageView: ImageView, uri: String) {
       // LogUtil.i("loadImage: $uri")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri.toUri())
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1 // 缩小图片尺寸
                }
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(R.drawable.a3_2008_2012)
                }
            }
        }
    }

    override fun getItemCount() = images.size

    fun updateImages(newImages: List<CarModelVersion>) {
        LogUtil.i("updateImages: ${newImages.size}")
        images = newImages
        //selectedPosition = -1
        notifyDataSetChanged()
    }
}
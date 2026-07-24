package com.launcher.yfd_ui01.chemo2

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.launcher.yfd_ui01.R
import com.awell.library.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 专门用于PopupWindow的车型适配器
class CarModelPopupAdapter(
    private val models: List<CarModel>,
    private val onModelSelected: (CarModel) -> Unit
) : RecyclerView.Adapter<CarModelPopupAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvModelName: TextView = view.findViewById(R.id.tv_car_model_name)
        val ivModelImage: ImageView = view.findViewById(R.id.iv_car_model_image)
        val rootView: CardView = view.findViewById(R.id.cv_root)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_model_popup, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]
        
        holder.tvModelName.text = model.displayName

        LogUtil.i("ivModelImage="+(holder.ivModelImage.width))
	// 简化图片加载，不使用占位图
        holder.ivModelImage.post {
            loadImageAsync(holder.itemView.context, model.imagePath, holder.ivModelImage)
        }

	
	// 加载图片 - 从 assets 加载
       // ImageLoader.loadThumbnail(holder.itemView.context, model.imagePath, holder.ivModelImage)
	
        // 加载图片
       /* Glide.with(holder.itemView.context)
            .load(File(model.imagePath))
            .placeholder(R.drawable.ic_car_placeholder)
            .error(R.drawable.ic_car_placeholder)
            .override(160, 88)
            .centerCrop()
            .into(holder.ivModelImage)*/
        
        // 点击选择
        holder.rootView.setOnClickListener {
            onModelSelected(model)
        }
        
//        // 长按预览（可选）
//        holder.rootView.setOnLongClickListener {
//            showImagePreview(holder.itemView.context, model)
//            true
//        }
    }
    
    private fun loadImageAsync(context: Context, imagePath: String, imageView: ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (imagePath.startsWith("chemo/")) {
                    val inputStream = context.assets.open(imagePath)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // 缩小图片尺寸
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()
                    
                    withContext(Dispatchers.Main) {
                        bitmap?.let {
                            imageView.setImageBitmap(it)
                        } ?: run {
                            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                        }
                    }
                } else {
                    // 普通文件
                    val file = File(imagePath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        withContext(Dispatchers.Main) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                LogUtil.e( "加载图片失败: $imagePath", e)
            }
        }
    }
    
//    private fun showImagePreview(context: Context, model: CarModel) {
//        val dialog = Dialog(context)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setContentView(R.layout.dialog_image_preview)
//
//        val imageView = dialog.findViewById<ImageView>(R.id.iv_preview)
//        val tvName = dialog.findViewById<TextView>(R.id.tv_preview_name)
//
//        tvName.text = model.displayName
//
//        Glide.with(context)
//            .load(File(model.imagePath))
//            .into(imageView)
//
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.window?.setLayout(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT
//        )
//
//        imageView.setOnClickListener { dialog.dismiss() }
//        dialog.show()
//    }
    
    override fun getItemCount() = models.size
}
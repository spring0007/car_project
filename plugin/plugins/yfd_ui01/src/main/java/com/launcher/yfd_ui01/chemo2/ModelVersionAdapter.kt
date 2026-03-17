package com.launcher.yfd_ui01.chemo2

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.utils.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 车型版本适配器
class ModelVersionAdapter(
    private val versions: List<CarModelVersion>,
    private val onVersionClick: (CarModelVersion) -> Unit
) : RecyclerView.Adapter<ModelVersionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVersionName: TextView = view.findViewById(R.id.tv_version_name)
        val ivVersionImage: ImageView = view.findViewById(R.id.iv_version_image)
        val rootView: CardView = view.findViewById(R.id.cv_version_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model_version, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val version = versions[position]

        holder.tvVersionName.text = version.displayName

        // 加载版本图片
        loadThumbnail(holder.itemView.context, version.imagePath, holder.ivVersionImage)

        holder.rootView.setOnClickListener {
            onVersionClick(version)
        }
    }

    private fun loadThumbnail(context: Context, imagePath: String, imageView: ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (imagePath.startsWith("chemo/")) {
                    val inputStream = context.assets.open(imagePath)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    withContext(Dispatchers.Main) {
                        bitmap?.let { imageView.setImageBitmap(it) }
                    }
                }
            } catch (e: Exception) {
                LogUtil.e( "加载缩略图失败", e)
            }
        }
    }


    override fun getItemCount() = versions.size
}
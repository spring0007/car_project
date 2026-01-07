package com.launcher.yfd_ui01.chemo2

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui01.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 车型分组适配器
class ModelGroupAdapter(
    private val modelGroups: List<CarModelGroup>,
    private val onModelGroupClick: (CarModelGroup) -> Unit
) : RecyclerView.Adapter<ModelGroupAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupName: TextView = view.findViewById(R.id.tv_model_group_name)
        val tvVersionCount: TextView = view.findViewById(R.id.tv_model_version_count)
        val ivPreview: ImageView = view.findViewById(R.id.iv_model_preview)
        val rootView: CardView = view.findViewById(R.id.cv_model_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val modelGroup = modelGroups[position]

        holder.tvGroupName.text = modelGroup.displayName
        holder.tvVersionCount.text = "${modelGroup.versions.size} versions" // 个版本

        // 加载第一个版本作为预览图
        if (modelGroup.versions.isNotEmpty()) {
            val firstVersion = modelGroup.versions.first()
            loadThumbnail(holder.itemView.context, firstVersion.imagePath, holder.ivPreview)
        }

        holder.rootView.setOnClickListener {
            onModelGroupClick(modelGroup)
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
                Log.e("ModelGroupAdapter", "加载预览图失败", e)
            }
        }
    }

    override fun getItemCount() = modelGroups.size
}

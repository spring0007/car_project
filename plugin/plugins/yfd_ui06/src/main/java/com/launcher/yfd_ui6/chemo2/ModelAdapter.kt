package com.launcher.yfd_ui6.chemo2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.launcher.yfd_ui6.R
import java.io.File

class ModelAdapter (
    private val models: List<CarModel>,
    private val onModelClick: (CarModel) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvModelName: TextView = view.findViewById(R.id.tv_model_name)
        val ivModelImage: ImageView = view.findViewById(R.id.iv_model_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]

        holder.tvModelName.text = model.displayName

        // 加载缩略图
        Glide.with(holder.itemView.context)
            .load(File(model.imagePath))
            .placeholder(R.drawable.a3_2008_2012)
            .error(R.drawable.a3_2008_2012)
            .override(160, 88)
            .into(holder.ivModelImage)

        holder.itemView.setOnClickListener { onModelClick(model) }
    }

    override fun getItemCount() = models.size
}
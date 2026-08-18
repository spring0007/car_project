package com.launcher.yfd_ui6.chemo2

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui6.R
import com.awell.library.util.LogUtil

class BrandAdapter (
    private val groupedBrands: List<Pair<Char, List<CarBrand>>>,
    private val onBrandClick: (CarBrand) -> Unit,
    private val onCustomCarClick: (() -> Unit)? = null
) : RecyclerView.Adapter<BrandAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tv_header)
        val ivCustomIcon: ImageView = view.findViewById(R.id.iv_custom_icon)
        val flowLayout: FlowLayout = view.findViewById(R.id.flow_layout)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_letter_header, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 第一项显示自定义车模
        if (position == 0 && onCustomCarClick != null) {
            holder.tvHeader.text = "0"
            holder.tvHeader.visibility = View.GONE
            holder.ivCustomIcon.visibility = View.VISIBLE
            holder.flowLayout.removeAllViews()
            
            // 添加自定义车模按钮
            val customCarButton = createCustomCarButton(holder.itemView.context)
            holder.flowLayout.addView(customCarButton)
        } else {
            // 原有逻辑 - 注意：由于 position 0 是自定义车模，所以品牌数据需要减 1
            val actualIndex = if (onCustomCarClick != null) position - 1 else position
            
            if (actualIndex >= 0 && actualIndex < groupedBrands.size) {
                val (letter, brands) = groupedBrands[actualIndex]
                
                holder.tvHeader.text = letter.toString()
                holder.tvHeader.visibility = View.VISIBLE
                holder.ivCustomIcon.visibility = View.GONE
                
                // 清空 FlowLayout
                holder.flowLayout.removeAllViews()
                
                // 动态添加品牌按钮
                brands.forEach { brand ->
                    val brandButton = createBrandButton(holder.itemView.context, brand)
                    holder.flowLayout.addView(brandButton)
                }
            }
        }
    }
    
    private fun createCustomCarButton(context: Context): TextView {
        return TextView(context).apply {
            text = context.getString(R.string.custom_carmodel)
            gravity = Gravity.CENTER
            setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
            setTextColor(Color.parseColor("#333333"))
            textSize = 30f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            
            // 设置布局参数
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.MarginLayoutParams.WRAP_CONTENT,
                ViewGroup.MarginLayoutParams.WRAP_CONTENT
            )
            params.setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
            layoutParams = params
            
            // 设置点击事件
            setOnClickListener {
                onCustomCarClick?.invoke()
            }
        }
    }
    
    private fun createBrandButton(context: Context, brand: CarBrand): TextView {
        return TextView(context).apply {
            text = brand.name
            gravity = Gravity.CENTER
            setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
            //setBackgroundResource(R.drawable.bg_brand_item)
            setTextColor(Color.parseColor("#333333"))
            textSize = 30f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            
            // 设置布局参数
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.MarginLayoutParams.WRAP_CONTENT,
                ViewGroup.MarginLayoutParams.WRAP_CONTENT
            )
            params.setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
            layoutParams = params
            
            // 设置点击事件
            setOnClickListener {
                onBrandClick(brand)
            }

//            // 长按显示详情
//            setOnLongClickListener {
//                Toast.makeText(context,
//                    "${brand.name} (${brand.modelGroups.size}个车型)",
//                    Toast.LENGTH_SHORT).show()
//                true
//            }
        }
    }
    
    override fun getItemCount() = groupedBrands.size
    
    // 获取字母位置（用于滚动定位）
    fun getPositionForLetter(letter: Char): Int {
        val index = groupedBrands.indexOfFirst { it.first == letter }
        // 如果有自定义车模入口，需要 +1（因为品牌数据从 position 1 开始）
        return if (index >= 0 && onCustomCarClick != null) index + 1 else index
    }
    
    // DP转PX的扩展函数
    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}

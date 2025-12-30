package com.launcher.yfd_ui01.chemo2

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui01.R

class BrandAdapter (
    private val groupedBrands: List<Pair<Char, List<CarBrand>>>,
    private val onBrandClick: (CarBrand) -> Unit
) : RecyclerView.Adapter<BrandAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tv_header)
        val flowLayout: FlowLayout = view.findViewById(R.id.flow_layout)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_letter_header, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (letter, brands) = groupedBrands[position]
        
        holder.tvHeader.text = letter.toString()

        // 清空FlowLayout
        holder.flowLayout.removeAllViews()

        // 动态添加品牌按钮
        brands.forEach { brand ->
            val brandButton = createBrandButton(holder.itemView.context, brand)
            holder.flowLayout.addView(brandButton)
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
        return groupedBrands.indexOfFirst { it.first == letter }
    }
    
    // DP转PX的扩展函数
    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}

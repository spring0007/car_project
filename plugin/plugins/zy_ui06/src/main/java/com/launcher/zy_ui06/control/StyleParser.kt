package com.launcher.zy_ui06.control

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.annotation.StyleRes

/**
 * 从指定的样式资源中解析属性值
 */
object StyleParser {

    /**
     * 获取样式中某个属性的资源ID（用于 Drawable 等引用类型）
     */
    fun getResourceIdFromStyle(
        context: Context,
        @StyleRes styleResId: Int,
        attr: Int // 例如 android.R.attr.background
    ): Int {
        val wrapper = android.view.ContextThemeWrapper(context, styleResId)
        val typedValue = TypedValue()
        return if (wrapper.theme.resolveAttribute(attr, typedValue, true)) {
            typedValue.resourceId
        } else {
            0
        }
    }

    /**
     * 获取样式中某个属性的颜色值
     */
    fun getColorFromStyle(
        context: Context,
        @StyleRes styleResId: Int,
        attr: Int // 例如 android.R.attr.textColor
    ): Int {
        val wrapper = android.view.ContextThemeWrapper(context, styleResId)
        val ta: TypedArray = wrapper.obtainStyledAttributes(styleResId, intArrayOf(attr))
        val color = ta.getColor(0, 0)
        ta.recycle()
        return color
    }

    /**
     * 获取样式中某个属性的尺寸（像素值）
     */
    fun getDimensionPixelSizeFromStyle(
        context: Context,
        @StyleRes styleResId: Int,
        attr: Int
    ): Int {
        val wrapper = android.view.ContextThemeWrapper(context, styleResId)
        val ta = wrapper.obtainStyledAttributes(styleResId, intArrayOf(attr))
        val size = ta.getDimensionPixelSize(0, 0)
        ta.recycle()
        return size
    }
}
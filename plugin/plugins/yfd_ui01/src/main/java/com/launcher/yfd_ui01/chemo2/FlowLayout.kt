package com.launcher.yfd_ui01.chemo2

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import java.lang.Math.max
import androidx.core.view.isGone

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // 存储每行的子View
    private val lines = mutableListOf<List<View>>()
    // 存储每行的高度
    private val lineHeights = mutableListOf<Int>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        lines.clear()
        lineHeights.clear()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var measuredWidth = 0
        var measuredHeight = 0

        var lineWidth = 0
        var lineHeight = 0
        var lineViews = mutableListOf<View>()

        val childCount = childCount

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.isGone) {
                continue
            }

            // 测量子View
            measureChild(child, widthMeasureSpec, heightMeasureSpec)

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            val params = child.layoutParams as? MarginLayoutParams
            val leftMargin = params?.leftMargin ?: 0
            val rightMargin = params?.rightMargin ?: 0
            val topMargin = params?.topMargin ?: 0
            val bottomMargin = params?.bottomMargin ?: 0

            val actualChildWidth = childWidth + leftMargin + rightMargin
            val actualChildHeight = childHeight + topMargin + bottomMargin

            // 如果加上当前子View后宽度超过父容器宽度，则换行
            if (lineWidth + actualChildWidth > widthSize - paddingLeft - paddingRight) {
                // 保存上一行
                lines.add(lineViews)
                lineHeights.add(lineHeight)
                measuredWidth = max(measuredWidth, lineWidth)
                measuredHeight += lineHeight

                // 开始新的一行
                lineViews = mutableListOf()
                lineWidth = 0
                lineHeight = 0
            }

            // 添加子View到当前行
            lineViews.add(child)
            lineWidth += actualChildWidth
            lineHeight = max(lineHeight, actualChildHeight)
        }

        // 添加最后一行
        if (lineViews.isNotEmpty()) {
            lines.add(lineViews)
            lineHeights.add(lineHeight)
            measuredWidth = max(measuredWidth, lineWidth)
            measuredHeight += lineHeight
        }

        // 加上padding
        measuredWidth += paddingLeft + paddingRight
        measuredHeight += paddingTop + paddingBottom

        // 设置最终测量的宽高
        setMeasuredDimension(
            resolveSize(measuredWidth, widthMeasureSpec),
            resolveSize(measuredHeight, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var currentTop = paddingTop
        val parentLeft = paddingLeft
        val parentRight = r - l - paddingRight

        for (i in lines.indices) {
            val line = lines[i]
            val lineHeight = lineHeights[i]

            var currentLeft = parentLeft

            for (view in line) {
                val params = view.layoutParams as? MarginLayoutParams
                val leftMargin = params?.leftMargin ?: 0
                val topMargin = params?.topMargin ?: 0
                val rightMargin = params?.rightMargin ?: 0
                val bottomMargin = params?.bottomMargin ?: 0

                val childWidth = view.measuredWidth
                val childHeight = view.measuredHeight

                // 计算子View的左上右下
                val childLeft = currentLeft + leftMargin
                val childTop = currentTop + topMargin
                val childRight = childLeft + childWidth
                val childBottom = childTop + childHeight

                view.layout(childLeft, childTop, childRight, childBottom)

                currentLeft += childWidth + leftMargin + rightMargin
            }

            currentTop += lineHeight
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
}
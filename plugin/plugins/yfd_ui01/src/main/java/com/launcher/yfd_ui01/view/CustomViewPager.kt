package com.launcher.yfd_ui01.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.view.isEmpty
import androidx.viewpager.widget.ViewPager
import com.launcher.yfd_ui01.utils.LogUtil

class CustomViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    // 按下时的X坐标
    private var downX = 0f

    // 记录按下时的滚动位置
    private var downScrollX = 0f

    private val viewConfiguration = ViewConfiguration.get(context)

    // 触摸滑动的阈值
    private var touchSlop = viewConfiguration.scaledTouchSlop

    private val overScroller = OverScroller(context)

    // 速度追踪器
    private val velocityTracker = VelocityTracker.obtain()
    private var minVelocity = viewConfiguration.scaledMinimumFlingVelocity
    private var maxVelocity = viewConfiguration.scaledMaximumFlingVelocity

    companion object {
        private const val OVERSCROLL_DAMPING_FACTOR = 0.3f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 统一测量所有子View
        // 让所有子View的大小都和ViewGroup一样
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

//    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
//        // 子View从左到右依次水平排列
//        var left = 0
//        val top = 0
//        val right = width
//        val bottom = height
//
//        for (child in children) {
//            child.layout(left, top, left + right, bottom)
//            left += right
//        }
//    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isEmpty()) return false
        LogUtil.i("onInterceptTouchEvent, ev=$ev")
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            // 清空速度追踪器
            velocityTracker.clear()
        }
        // 添加事件
        velocityTracker.addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 为之后滑动做准备
                downX = ev.x
                downScrollX = scrollX.toFloat()
            }

            MotionEvent.ACTION_MOVE -> {
                val moveX = ev.x
                val distanceX = kotlin.math.abs(moveX - downX)

                if (distanceX > touchSlop) {
                    // 让父View不拦截滑动
                    requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
        }

        // 默认不拦截
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEmpty()) return false
        LogUtil.i("onTouchEvent, event=$event")
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // 清空速度追踪器
            velocityTracker.clear()
        }
        // 添加事件
        velocityTracker.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downScrollX = scrollX.toFloat()
            }

            MotionEvent.ACTION_MOVE -> {
                val moveX = event.x
                // 计算目标滚动位置
                val targetScrollX = (downX - moveX + downScrollX)

                // 滚动边界
                val minScrollX = 0f
                val maxScrollX = (childCount - 1) * width.toFloat()
                val finalScrollX = if (targetScrollX < minScrollX) {
                    val dampedOverscroll = (targetScrollX - minScrollX) * OVERSCROLL_DAMPING_FACTOR
                    minScrollX + dampedOverscroll
                } else if (targetScrollX > maxScrollX) {
                    val dampedOverscroll = (targetScrollX - maxScrollX) * OVERSCROLL_DAMPING_FACTOR
                    maxScrollX + dampedOverscroll
                } else {
                    targetScrollX.coerceIn(minScrollX, maxScrollX)
                }

                scrollTo(finalScrollX.toInt(), 0)
            }

            MotionEvent.ACTION_UP -> {
                // 计算当前速度,单位px/s
                velocityTracker.computeCurrentVelocity(1000, maxVelocity.toFloat())
                val xVelocity = velocityTracker.xVelocity

                // 获取当前滚动位置
                val currentScrollX = scrollX
                // 获取之前的页码
                val oldPage = (downScrollX / width).toInt()

                // 滚动边界
                val minScrollInt = 0
                val maxScrollInt = (childCount - 1) * width

                val targetPage = when {
                    currentScrollX < minScrollInt -> {
                        0
                    }

                    currentScrollX > maxScrollInt -> {
                        childCount - 1
                    }

                    else -> {
                        if (kotlin.math.abs(xVelocity) > minVelocity) {
                            // 如果水平速度超过了系统定义的最小 fling 速度
                            // 根据速度方向来决定目标页面
                            if (xVelocity < 0) oldPage + 1 else oldPage - 1
                        } else {
                            // 否则，根据当前位置来决定目标页面
                            (currentScrollX + width / 2) / width
                        }
                    }
                }

                val finalTargetPage = targetPage.coerceIn(0, childCount - 1)
                val targetScrollX = finalTargetPage * width
                val scrollDistance = targetScrollX - currentScrollX

                overScroller.startScroll(currentScrollX, 0, scrollDistance, 0)
                postInvalidateOnAnimation()
            }
        }

        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        if (overScroller.computeScrollOffset()) {
            scrollTo(overScroller.currX, overScroller.currY)
            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        // 回收速度跟踪器
        velocityTracker.recycle()
        super.onDetachedFromWindow()
    }

}

package com.launcher.yfd_ui01.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.view.isEmpty
import androidx.viewpager.widget.ViewPager
import com.launcher.yfd_ui01.utils.LogUtil


/**
 * 垂直滑动监听器接口
 */
interface OnVerticalSwipeListener {
    /**
     * 当垂直滑动距离超过阈值时调用
     *
     * @param direction 滑动方向：1 表示向下滑动，-1 表示向上滑动
     * @param currentPage 当前页面索引
     * @param distanceY 垂直滑动距离（像素）
     * @return 返回 true 表示消费了该事件，ViewPager将不执行默认切换；返回 false 表示由ViewPager执行默认切换
     */
    fun onVerticalSwipe(direction: Int, currentPage: Int, distanceY: Float): Boolean
}

class CustomViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    // 按下时的坐标
    private var downX = 0f
    private var downY = 0f

    // 记录按下时的滚动位置
    private var downScrollX = 0f

    private val viewConfiguration = ViewConfiguration.get(context)

    // 触摸滑动的阈值
    private var touchSlop = viewConfiguration.scaledTouchSlop
    private val verticalThreshold = 150f // 垂直滑动阈值150dp，需要转换为px
    private var verticalThresholdPx = 0f

    private val overScroller = OverScroller(context)

    // 速度追踪器
    private val velocityTracker = VelocityTracker.obtain()
    private var minVelocity = viewConfiguration.scaledMinimumFlingVelocity
    private var maxVelocity = viewConfiguration.scaledMaximumFlingVelocity
    private var verticalSwipeListener: OnVerticalSwipeListener? = null
    // 当前页面索引
    private var currentPageIndex = 0
    // 页面总数
    private var totalPages = 0

    // 标志位：是否垂直滑动已被处理
    private var isVerticalSwipeHandled = false
    // 原始页面位置
    private var originalPage = 0
    // 原始滚动位置
    private var originalScrollX = 0

    companion object {
        private const val OVERSCROLL_DAMPING_FACTOR = 0.3f
        private const val DIRECTION_DOWN = 1
        private const val DIRECTION_UP = -1
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 将阈值转换为像素
        verticalThresholdPx = verticalThreshold * resources.displayMetrics.density
        totalPages = childCount
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 统一测量所有子View
        // 让所有子View的大小都和ViewGroup一样
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isEmpty()) return false
       // LogUtil.i("onInterceptTouchEvent, ev=$ev")
        
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 重置标志位
                isVerticalSwipeHandled = false
                // 清空速度追踪器
                velocityTracker.clear()
                // 添加事件
                velocityTracker.addMovement(ev)
                
                // 为之后滑动做准备
                downX = ev.x
                downY = ev.y
                downScrollX = scrollX.toFloat()
                currentPageIndex = (downScrollX / width).toInt()
                originalPage = currentPageIndex
                originalScrollX = currentPageIndex * width
                totalPages = childCount
            }

            MotionEvent.ACTION_MOVE -> {
                // 如果垂直滑动已被处理，直接拦截事件
                if (isVerticalSwipeHandled) {
                    return true
                }
                
                // 添加事件
                velocityTracker.addMovement(ev)
                
                val moveX = ev.x
                val moveY = ev.y
                val distanceX = kotlin.math.abs(moveX - downX)
                val distanceY = kotlin.math.abs(moveY - downY)
               // LogUtil.i("滑动距离: distanceX=$distanceX, distanceY=$distanceY, 阈值: touchSlop=$touchSlop, verticalThreshold=$verticalThresholdPx")
                // 检查是否为垂直滑动
                if (distanceY > verticalThresholdPx && distanceY > distanceX) {
                 //   LogUtil.i("检测到垂直滑动，距离: $distanceY, 阈值: $verticalThresholdPx")
                    val direction = if (moveY > downY) DIRECTION_DOWN else DIRECTION_UP
                    val handledByListener =  verticalSwipeListener?.onVerticalSwipe(direction, currentPageIndex, distanceY) ?: false
                    
                    if (handledByListener) {
                        isVerticalSwipeHandled = true
                        // 拦截事件，防止子View处理
                        return true
                    }
                }
                
                if (distanceX > touchSlop) {
                    // 让父View不拦截滑动
                    requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 重置标志位
                isVerticalSwipeHandled = false
            }
        }

        // 默认不拦截
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEmpty()) return false

        //LogUtil.i("onTouchEvent, event=$event")
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 重置标志位
                isVerticalSwipeHandled = false
                // 清空速度追踪器
                velocityTracker.clear()
                // 添加事件
                velocityTracker.addMovement(event)
                
                downX = event.x
                downY = event.y
                downScrollX = scrollX.toFloat()
                currentPageIndex = (downScrollX / width).toInt()
                originalPage = currentPageIndex
                originalScrollX = currentPageIndex * width
                totalPages = childCount
            }

            MotionEvent.ACTION_MOVE -> {
                // 如果垂直滑动已被处理，直接返回true消费事件，不执行水平滑动逻辑
                if (isVerticalSwipeHandled) {
                    // 继续添加事件到速度追踪器，但不处理
                    velocityTracker.addMovement(event)
                    return true
                }
                
                // 添加事件
                velocityTracker.addMovement(event)
                
                val moveX = event.x
                val moveY = event.y
                val distanceY = kotlin.math.abs(moveY - downY)
                // 检查是否为垂直滑动
                if (distanceY > verticalThresholdPx) {

                   // LogUtil.i("onTouchEvent: 检测到垂直滑动，距离: $distanceY")
                    // 计算滑动方向
                    val direction = if (moveY > downY) DIRECTION_DOWN else DIRECTION_UP

                    // 通知监听器
                    val handledByListener = verticalSwipeListener?.onVerticalSwipe(direction, currentPageIndex, distanceY) ?: false
                    
                    if (handledByListener) {
                        isVerticalSwipeHandled = true
                        // 立即回到原始页面位置
                        scrollToOriginalPage()
                        return true
                    }
                }
		    
                // 只有垂直滑动未被处理时，才执行水平滑动逻辑
                if (!isVerticalSwipeHandled) {
                // 每次移动时都重新获取页面总数
                val pageCount = childCount
                val pageWidth = width
                    // 计算目标滚动位置
                    val targetScrollX = (downX - moveX + downScrollX)

                // 滚动边界 - 使用实时计算的值
                val minScrollX = 0f
                val maxScrollX = if (pageCount > 0) (pageCount - 1) * pageWidth.toFloat() else 0f
                
                // 添加日志来调试边界值
                LogUtil.i("滑动边界: minScrollX=$minScrollX, maxScrollX=$maxScrollX, targetScrollX=$targetScrollX, pageCount=$pageCount, pageWidth=$pageWidth")
                
                val finalScrollX = when {
                    targetScrollX < minScrollX -> {
                        val dampedOverscroll = (targetScrollX - minScrollX) * OVERSCROLL_DAMPING_FACTOR
                        minScrollX + dampedOverscroll
                    }
                    targetScrollX > maxScrollX -> {
                        val dampedOverscroll = (targetScrollX - maxScrollX) * OVERSCROLL_DAMPING_FACTOR
                        maxScrollX + dampedOverscroll
                    }
                    else -> targetScrollX.coerceIn(minScrollX, maxScrollX)
                }

                scrollTo(finalScrollX.toInt(), 0)
                
                // 更新当前页面索引
                currentPageIndex = (finalScrollX / pageWidth).toInt().coerceIn(0, pageCount - 1)
            }
        }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 如果垂直滑动已被处理，回到原始页面并重置标志位
                if (isVerticalSwipeHandled) {
                    scrollToOriginalPage()
                    isVerticalSwipeHandled = false
                    return true
                }
                
                // 计算当前速度,单位px/s
                velocityTracker.computeCurrentVelocity(1000, maxVelocity.toFloat())
                val xVelocity = velocityTracker.xVelocity

                // 获取当前滚动位置
                val currentScrollX = scrollX
                // 获取之前的页码
                val oldPage = (downScrollX / width).toInt()

            // 使用实时计算的页面总数和宽度
            val pageCount = childCount
            val pageWidth = width
            
            val minScrollInt = 0
            val maxScrollInt = if (pageCount > 0) (pageCount - 1) * pageWidth else 0

            val targetPage = when {
                currentScrollX < minScrollInt -> 0
                currentScrollX > maxScrollInt -> pageCount - 1
                else -> {
                    if (kotlin.math.abs(xVelocity) > minVelocity) {
                        if (xVelocity < 0) oldPage + 1 else oldPage - 1
                    } else {
                        (currentScrollX + pageWidth / 2) / pageWidth
                    }
                }
            }

            val finalTargetPage = targetPage.coerceIn(0, pageCount - 1)
            val targetScrollX = finalTargetPage * pageWidth
            val scrollDistance = targetScrollX - currentScrollX

            overScroller.startScroll(currentScrollX, 0, scrollDistance, 0)
            postInvalidateOnAnimation()
            
            // 页面变化时通知监听器
            if (finalTargetPage != currentPageIndex) {
                currentPageIndex = finalTargetPage
                dispatchOnPageChanged(finalTargetPage)
            }
            
            isVerticalSwipeHandled = false
        }
    }

        return true
    }

    /**
     * 滚动回原始页面位置
     */
    private fun scrollToOriginalPage() {
        val currentScrollX = scrollX
        val targetScrollX = originalScrollX
        val scrollDistance = targetScrollX - currentScrollX
        
        // 如果已经在原始位置，不需要滚动
        if (scrollDistance == 0) return
        
        // 使用OverScroller平滑滚动回原始位置
        overScroller.startScroll(currentScrollX, 0, scrollDistance, 0)
        postInvalidateOnAnimation()
    }

    override fun computeScroll() {
        super.computeScroll()
        if (overScroller.computeScrollOffset()) {
            scrollTo(overScroller.currX, overScroller.currY)

            // 计算当前页面并通知监听器
            val pageWidth = width
            if (pageWidth > 0) {
                val newPage = overScroller.currX / pageWidth
                if (newPage != currentPageIndex) {
                    currentPageIndex = newPage
                    dispatchOnPageChanged(newPage)
                }
            }

            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        // 回收速度跟踪器
        velocityTracker.recycle()
        super.onDetachedFromWindow()
    }

    fun setOnVerticalSwipeListener(listener: OnVerticalSwipeListener) {
        this.verticalSwipeListener = listener
    }

    // 页面变化监听器列表
    private val onPageChangeListeners = mutableListOf<OnPageChangeListener>()

    // 添加页面变化监听器
    override fun addOnPageChangeListener(listener: OnPageChangeListener) {
        onPageChangeListeners.add(listener)
    }

    // 移除页面变化监听器
    override fun removeOnPageChangeListener(listener: OnPageChangeListener) {
        onPageChangeListeners.remove(listener)
    }

    // 通知页面变化
    private fun dispatchOnPageChanged(position: Int) {
        onPageChangeListeners.forEach { listener ->
            listener.onPageSelected(position)
        }
    }

}

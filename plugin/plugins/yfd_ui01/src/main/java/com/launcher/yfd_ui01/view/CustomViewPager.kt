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
    // 垂直滑动阈值 - 优化为更合理的值
    private var verticalThresholdPx = resources.displayMetrics.heightPixels / 3.0f  // 屏幕高度的1/3，更容易触发

    private val overScroller = OverScroller(context)

    // 速度追踪器
    private val velocityTracker = VelocityTracker.obtain()
    private var minVelocity = viewConfiguration.scaledMinimumFlingVelocity
    private var maxVelocity = viewConfiguration.scaledMaximumFlingVelocity
    private var verticalSwipeListener: OnVerticalSwipeListener? = null

    // 当前页面索引
    private var currentPageIndex = 0

    // 标志位：是否垂直滑动已被处理
    private var isVerticalSwipeHandled = false

    // 页面变化监听器列表
    private val onPageChangeListeners = mutableListOf<OnPageChangeListener>()

    companion object {
        private const val OVERSCROLL_DAMPING_FACTOR = 0.2f // 减小阻尼，使回弹更自然
        private const val DIRECTION_DOWN = 1
        private const val DIRECTION_UP = -1
        private const val SCROLL_DURATION_FACTOR = 0.3f // 滚动动画时间因子
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 将阈值转换为像素
        //verticalThresholdPx =  verticalThreshold * resources.displayMetrics.density
    }

    // 获取真实页面总数的方法
    private fun getRealPageCount(): Int {
        return adapter?.count ?: 0
    }

    // 获取当前页面索引（基于滚动位置）
    private fun getCurrentPageIndex(): Int {
        return if (width > 0) {
            (scrollX + width / 2) / width
        } else {
            0
        }
    }

    // 重写onMeasure，确保所有页面都被测量
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 统一测量所有子View
        // 让所有子View的大小都和ViewGroup一样
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isEmpty()) return false
         //LogUtil.i("onInterceptTouchEvent, ev=$ev")

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
                currentPageIndex = getCurrentPageIndex()  // 使用新方法
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
                 LogUtil.i("滑动距离: distanceX=$distanceX, distanceY=$distanceY, 阈值: touchSlop=$touchSlop, verticalThresholdPx=$verticalThresholdPx")
                // 检查是否为垂直滑动 - 优化检测逻辑
                if (distanceY > verticalThresholdPx && distanceY > distanceX) {
                    LogUtil.i("检测到垂直滑动，距离: $distanceY, 阈值: $verticalThresholdPx, 当前页面: $currentPageIndex")
                    val direction = if (moveY > downY) DIRECTION_DOWN else DIRECTION_UP
                    
                    // 只有在第一页向下滑动时才触发切换
                    if (direction == DIRECTION_DOWN && currentPageIndex == 0) {

                        val handledByListener = verticalSwipeListener?.onVerticalSwipe(
                            direction,
                            currentPageIndex,
                            distanceY
                        ) ?: false

                        if (handledByListener) {
                            isVerticalSwipeHandled = true
                            LogUtil.i("垂直滑动已处理，切换到主页面")
                            // 拦截事件，防止子View处理
                            return true
                        }
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
                currentPageIndex = getCurrentPageIndex()  // 使用新方法获取

                // 停止当前的滚动动画
                overScroller.forceFinished(true)
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
                // 检查是否为垂直滑动 - 优化处理逻辑
                if (distanceY > verticalThresholdPx) {
                    // LogUtil.i("onTouchEvent: 检测到垂直滑动，距离: $distanceY")
                    // 计算滑动方向
                    val direction = if (moveY > downY) DIRECTION_DOWN else DIRECTION_UP

                    // 只有在第一页向下滑动时才触发切换
                    if (direction == DIRECTION_DOWN && currentPageIndex == 0) {
                        
                        // 通知监听器
                        val handledByListener = verticalSwipeListener?.onVerticalSwipe(
                            direction,
                            currentPageIndex,
                            distanceY
                        ) ?: false

                        if (handledByListener) {
                            //LogUtil.i("onTouchEvent, handledByListener=$handledByListener")
                            // 立即回到原始页面位置
                            scrollToOriginalPage()
                            return true
                        }
                    }
                }

                // 只有垂直滑动未被处理时，才执行水平滑动逻辑
                if (!isVerticalSwipeHandled) {
                    // 水平滑动处理
                    val pageCount = getRealPageCount()
                    val pageWidth = width
                    if (pageCount == 0 || pageWidth == 0) return true
                    // 计算目标滚动位置
                    val targetScrollX = (downX - moveX + downScrollX)

                    // 滚动边界
                    val minScrollX = 0f
                    val maxScrollX = (pageCount - 1) * pageWidth.toFloat()

                    val finalScrollX = when {
                        targetScrollX < minScrollX -> {
                            val dampedOverscroll = 
                                (targetScrollX - minScrollX) * OVERSCROLL_DAMPING_FACTOR
                            minScrollX + dampedOverscroll
                        }

                        targetScrollX > maxScrollX -> {
                            val dampedOverscroll = 
                                (targetScrollX - maxScrollX) * OVERSCROLL_DAMPING_FACTOR
                            maxScrollX + dampedOverscroll
                        }

                        else -> targetScrollX
                    }

                    // 直接滚动到目标位置，不进行额外的边界检查，提高响应速度
                    scrollTo(finalScrollX.toInt(), 0)
                    
                    // 实时更新当前页面索引
                    val newPageIndex = (finalScrollX / pageWidth).toInt().coerceIn(0, pageCount - 1)
                    if (newPageIndex != currentPageIndex) {
                        currentPageIndex = newPageIndex
                        // 只在页面真正改变时通知监听器
                        dispatchOnPageChanged(newPageIndex)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 如果垂直滑动已被处理，回到原始页面并重置标志位
                if (isVerticalSwipeHandled) {
                    scrollToOriginalPage()
                    isVerticalSwipeHandled = false
                    return true
                }

                // 计算当前速度
                velocityTracker.computeCurrentVelocity(1000, maxVelocity.toFloat())
                val xVelocity = velocityTracker.xVelocity

                // 获取当前滚动位置
                val currentScrollX = scrollX
                // 获取页面宽度和总数
                val pageWidth = width
                val pageCount = getRealPageCount()

                if (pageCount == 0 || pageWidth == 0) return true

                // 计算当前页面（基于滚动位置）
                val currentPage = 
                    ((currentScrollX + pageWidth/2 ) / pageWidth).coerceIn(0, pageCount - 1)

                // 计算从按下位置到抬手位置的水平滑动距离（基于手指移动）
                val moveX = event.x
                val deltaX = downX - moveX
                val scrollDelta = deltaX // 使用实际的手指移动距离

                // 决定目标页面（优化策略）：更灵敏的滑动判断
                val calculatedTargetPage = when {
                    // 高速滑动时正常翻页
                    kotlin.math.abs(xVelocity) > minVelocity * 1.5 -> { // 降低速度阈值，提高灵敏度
                        if (xVelocity < 0) {
                            // 向左滑动，下一页
                            (currentPage + 1).coerceAtMost(pageCount - 1)
                        } else {
                            // 向右滑动，上一页
                            (currentPage - 1).coerceAtLeast(0)
                        }
                    }
                    // 中等速度滑动需要更小距离才翻页
                    kotlin.math.abs(xVelocity) > minVelocity * 0.8 -> {
                        if (kotlin.math.abs(scrollDelta) > pageWidth * 0.25) { // 降低距离阈值
                            if (xVelocity < 0) {
                                (currentPage + 1).coerceAtMost(pageCount - 1)
                            } else {
                                (currentPage - 1).coerceAtLeast(0)
                            }
                        } else {
                            currentPage
                        }
                    }
                    // 滑动距离较小时也能翻页
                    kotlin.math.abs(scrollDelta) > pageWidth * 0.15 -> { // 降低距离阈值
                        if (scrollDelta > 0) {
                            // 内容向左滚动，目标是下一页
                            (currentPage + 1).coerceAtMost(pageCount - 1)
                        } else {
                            // 内容向右滚动，目标是上一页
                            (currentPage - 1).coerceAtLeast(0)
                        }
                    }
                    // 否则回到当前页面
                    else -> currentPage
                }
                
                // 极端保护：超大滑动距离时限制跳页
                val targetPage = if (kotlin.math.abs(scrollDelta) > pageWidth * 1.2) {
                    // 超大滑动时最多只跳转一页
                    calculatedTargetPage.coerceIn(
                        (currentPage - 1).coerceAtLeast(0),
                        (currentPage + 1).coerceAtMost(pageCount - 1)
                    )
                } else {
                    calculatedTargetPage
                }
                
                //LogUtil.i("滑动结束判断: 当前页=$currentPage, 目标页=$targetPage, 速度=$xVelocity, 滑动距离=$scrollDelta, 页面宽度=$pageWidth")

                // 平滑滚动到目标页面
                setCurrentItem(targetPage, true)
                return true
            }
        }
        return true
    }


    /**
     * 使用 overScroller 平滑滚动到指定的 X 位置，时长根据滚动距离按比例计算
     */
    private fun smoothScrollToX(targetX: Int) {
        val startX = scrollX
        val dx = targetX - startX
        if (dx == 0) return

        try {
            // 基于滚动距离计算动画时间，使滑动更自然
            val distance = Math.abs(dx)
            val duration = (distance * SCROLL_DURATION_FACTOR).coerceIn(150F, 300F).toInt() // 限制动画时间在150-300ms之间
            
            //LogUtil.i("smoothScrollToX targetX=$targetX, distance=$distance, duration=$duration")
            overScroller.forceFinished(true)
            overScroller.startScroll(startX, 0, dx, 0, duration)
            postInvalidateOnAnimation()
        } catch (e: Exception) {
            LogUtil.e("smoothScrollToX error", e)
            scrollTo(targetX, 0)
        }
    }

    /**
     * 滚动回原始页面位置
     */
    private fun scrollToOriginalPage() {
        // 使用 ViewPager 的 setCurrentItem 方法确保页面被正确创建
        //LogUtil.i("scrollToOriginalPage")
        setCurrentItem(currentPageIndex, true)
    }


    override fun computeScroll() {
        super.computeScroll()
        if (overScroller.computeScrollOffset()) {
            scrollTo(overScroller.currX, overScroller.currY)
            //LogUtil.i("computeScroll")
            // 计算当前页面并通知监听器
            /*val pageWidth = width
            if (pageWidth > 0) {
                val pageCount = getRealPageCount()
                val newPage = (overScroller.currX + pageWidth / 2) / pageWidth
                val clampedPage = newPage.coerceIn(0, pageCount - 1)

                if (clampedPage != currentPageIndex) {
                    currentPageIndex = clampedPage
                    LogUtil.i("dispatchOnPageChanged,365")
                    dispatchOnPageChanged(clampedPage)
                }
            }*/

            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        // 回收速度跟踪器
        velocityTracker.recycle()
	     overScroller.forceFinished(true)
        super.onDetachedFromWindow()
    }

    /**
     * 设置当前页面（重写以更新内部状态）
     */
    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        val pageCount = getRealPageCount()
        if (pageCount == 0) return
        
        // 正常的页面设置（允许相邻页面切换）
        val targetItem = item.coerceIn(0, pageCount - 1)
        
        // 只有当目标页面与当前页面相差超过1页时才记录警告
        if (kotlin.math.abs(targetItem - currentPageIndex) > 1) {
            LogUtil.w("检测到大跨度页面切换: 当前页=$currentPageIndex, 目标页=$targetItem")
        }
        
        super.setCurrentItem(targetItem, smoothScroll)
        //LogUtil.i("setCurrentItem,item=$item")
        smoothScrollToX(targetItem * width)
        currentPageIndex = targetItem
        dispatchOnPageChanged(targetItem)
    }

//    override fun setCurrentItem(item: Int) {
//        super.setCurrentItem(item)
//        LogUtil.i("setCurrentItem2,item=$item")
//        currentPageIndex = item
//        LogUtil.i("dispatchOnPageChanged,394")
//        dispatchOnPageChanged(item)
//    }

    fun setOnVerticalSwipeListener(listener: OnVerticalSwipeListener) {
        this.verticalSwipeListener = listener
    }

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
        LogUtil.i("dispatchOnPageChanged,position=$position")
        onPageChangeListeners.forEach { listener ->
            listener.onPageSelected(position)
        }
    }
    // 获取当前页面
    fun getCurrentPage(): Int = currentPageIndex
}

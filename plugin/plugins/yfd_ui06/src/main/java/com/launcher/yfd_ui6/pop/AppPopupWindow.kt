package com.launcher.yfd_ui6.pop

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.awell.addapp.AppInfo
import com.launcher.yfd_ui6.R
import com.launcher.yfd_ui6.utils.IconManager
import androidx.core.graphics.drawable.toDrawable

/**
 * 自定义PopupWindow封装类
 * 包含显示、消失方法，支持自定义数字参数
 * 实现 OnPopupUpdateListener：AppPopAdapter 点击条目时回调 updateAppImage，由本类持久化并转发给宿主刷新槽位
 */
class AppPopupWindow(
    context: Context,
    private var customNumber: Int
) : PopupWindow(context), OnPopupActionListener, OnPopupUpdateListener {

    private var mContext: Context = context
    private var TAG: String = "AppPopupWindow"
    private lateinit var mContentView: View
    private var allAppInfoList: List<AppInfo>? = null
    private var updateListener: OnPopupUpdateListener? = null

    init {
        initView()
        setupPopupWindow()
    }

    /**
     * 初始化视图
     */
    private fun initView() {
        val inflater = LayoutInflater.from(mContext)
        mContentView = inflater.inflate(R.layout.layout_allapp, null)
        contentView = mContentView
    }

    /**
     * 设置PopupWindow属性
     */
    private fun setupPopupWindow() {
        // 设置宽高
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
        // 清除主题默认背景，否则不透明黑背景会盖住桌面导致半透明失效
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        allAppInfoList = getAllAppInfo(mContext)

        // 设置外部可点击
        isOutsideTouchable = true
        windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        // 设置焦点
        isFocusable = true
        // 设置动画
        animationStyle = android.R.style.Animation_Dialog

        val rvPop = mContentView.findViewById<RecyclerView>(R.id.rv_pop_allapp)
        val gridLayoutManager = GridLayoutManager(mContext, 2)
        gridLayoutManager.orientation = RecyclerView.HORIZONTAL
        val appInfoAdapter = AppPopAdapter(mContext, allAppInfoList, this as OnPopupUpdateListener)
        rvPop.layoutManager = gridLayoutManager
        rvPop.adapter = appInfoAdapter
        val verticalSpacing = (30 * mContext.resources.displayMetrics.density).toInt()
        rvPop.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.top = verticalSpacing
                outRect.bottom = verticalSpacing
            }
        })

        // 设置消失监听
        /*setOnDismissListener {
            hideBackground()
            onPopupDismissed()
        }*/

        mContentView.setOnClickListener { this.dismiss() }

    }

    /**
     * 获取应用信息：仅 IconManager.PACKAGE_ICON_MAP 中的应用，保持 map 插入顺序
     * （PACKAGE_ICON_MAP 后续会继续添加）
     */
    private fun getAllAppInfo(ctx: Context): List<AppInfo> {
        val appBeanList = mutableListOf<AppInfo>()
        val packageManager = ctx.packageManager
        for (packageName in IconManager.PACKAGE_ICON_MAP.keys) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                if (!appInfo.enabled) continue
                val bean = AppInfo()
                bean.label = packageManager.getApplicationLabel(appInfo).toString()
                bean.package_name = packageName
                appBeanList.add(bean)
            } catch (e: PackageManager.NameNotFoundException) {
                // 包名未安装，跳过
            }
        }
        return appBeanList
    }

    /**
     * 显示PopupWindow（在指定位置）
     * @param parentView 父View
     * @param gravity 位置
     * @param x x轴偏移
     * @param y y轴偏移
     */
    fun show(parentView: View, x: Int, y: Int) {
        if (!isShowing) {
            showAtLocation(parentView, Gravity.CENTER, x, y)
        }
    }

    public fun setOnPopupUpdateListener(onupdateListener: OnPopupUpdateListener) {
        updateListener = onupdateListener
    }

    /**
     * 隐藏背景
     */
    private fun hideBackground() {
        updateListener?.backgroundAlphaWindow(1.0f)
    }

    /**
     * 安全地消失PopupWindow
     */
    fun dismissSafely() {
        if (isShowing) {
            dismiss()
        }
        hideBackground()
    }

    override fun onPopupDismissed() {
        dismissSafely()
        allAppInfoList = null
    }

    override fun updateImage(info: AppInfo) {
        updateAppImage(info)
    }

    /**
     * 选择应用后：持久化到 Settings（launcher_app_icon_$customNumber）并通知宿主刷新对应槽位
     */
    override fun updateAppImage(info: AppInfo) {
        if (info != null) {
            Settings.System.putString(
                mContext.contentResolver, "launcher_app_icon_$customNumber",
                info.package_name
            )
            info.flags = customNumber
        }
        Log.i(TAG, "info = " + info)
        updateListener?.updateAppImage(info)
        onPopupDismissed()
    }

    /**
     * 弹窗自身不管理窗口透明度，忽略（透明度由宿主 UIActivity.backgroundAlphaWindow 处理）
     */
    override fun backgroundAlphaWindow(alpha: Float) {
        // no-op
    }

}

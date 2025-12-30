package com.launcher.yfd_ui01.pop

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.awell.addapp.AppInfo
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.app.IconManager

/**
 * 自定义PopupWindow封装类
 * 包含显示、消失方法，支持自定义数字参数
 */
class AppPopupWindow(
    context: Context,
    private var customNumber: Int
) : PopupWindow(context) ,OnPopupActionListener  {

    private var mContext: Context = context
    private var TAG: String = "AppPopupWindow"
    private lateinit var mContentView: View
    private var iconManager: IconManager? = null
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
        iconManager = IconManager.getInstance(mContext)
    }

    /**
     * 设置PopupWindow属性
     */
    private fun setupPopupWindow() {
        // 设置宽高
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT

        // 设置背景
        setBackgroundDrawable(ColorDrawable(Color.BLACK))

        allAppInfoList = getAllAppInfo(mContext)
        
        // 设置外部可点击
        isOutsideTouchable = true
        windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        // 设置焦点
        isFocusable = true
        // 设置动画
        animationStyle = android.R.style.Animation_Dialog

        val rvPop = mContentView.findViewById<RecyclerView>(R.id.rv_pop_allapp)
        val gridLayoutManager = GridLayoutManager(mContext, 10)
        gridLayoutManager.spanCount = 2
        gridLayoutManager.orientation = RecyclerView.HORIZONTAL
        val appInfoAdapter = AppPopAdapter(mContext, allAppInfoList, this)
        rvPop.layoutManager = gridLayoutManager
        rvPop.adapter = appInfoAdapter

        //showAtLocation(mContentView.rootView, Gravity.CENTER, 0, 10)
        //findViewById(android.R.id.content)

        // 设置消失监听
        setOnDismissListener {
            hideBackground()
            onPopupDismissed()
        }
    }

    /**
     * 获取所有应用信息
     */
    private fun getAllAppInfo(ctx: Context): List<AppInfo> {
        val appBeanList = mutableListOf<AppInfo>()
        val packageManager = ctx.packageManager

        try {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            val FLAG_SYSTEM = ApplicationInfo.FLAG_SYSTEM

            // 预计算过滤条件
            val hasFilterApps = IconManager.NEED_TO_BLOCKED_PACKAGE_NAMES.isNotEmpty()
            val hasNeedToShowApps = false // !IconManager.NEED_TO_SHOW_PACKAGE_NAMES.isEmpty()
            val hasPackageOrder = IconManager.PACKAGE_ORDER_LIST.isNotEmpty()

            // 第一阶段：快速收集基本信息
            val tempList = mutableListOf<AppInfo>()
            for (p in packages) {
                val appInfo = p.applicationInfo ?: continue
                if (!appInfo.enabled) continue

                val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (intent == null) continue
                if (appInfo.packageName.contains("launcher")) continue

                val packageName = appInfo.packageName
                val flags = appInfo.flags

                // 应用过滤逻辑
                if (shouldIncludeApp(packageName, hasFilterApps, hasNeedToShowApps)) {
                    continue
                }

                val bean = createAppInfo(packageManager, appInfo, packageName, flags)
                if (bean != null) {
                    tempList.add(bean)
                }
            }
             //单独处理
            var appInfo  = AppInfo()
            appInfo.package_name="com.launcher.yfd_ui01"
            appInfo.icon = ctx.getDrawable(R.drawable.yfd_ui1_menu)
            appInfo.label = ctx.getString(R.string.apps)
            tempList.add(appInfo)
            // 第二阶段：批量处理图标
            mContentView.run {
                updateAppIcons(tempList)
                appBeanList.addAll(tempList)

                // 第三阶段：排序
                sortAppList(appBeanList, hasPackageOrder)

//                // 完成数据处理后更新UI
//                onAppDataReady(appBeanList)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting app info", e)
        }

        return appBeanList
    }

    private fun updateAppIcons(appList: List<AppInfo>) {
        if (iconManager == null) return

        // 预加载图标管理器缓存
        iconManager?.preloadCommonIcons()

        for (app in appList) {
            val packageName = app.package_name ?: continue
            val customIcon = iconManager?.getIcon(packageName)
            if (customIcon != null) {
                app.icon = customIcon
            }
        }
    }

    /*
     * 判断是否应该包含该应用
     */
    private fun shouldIncludeApp(
        packageName: String,
        hasFilterApps: Boolean,
        hasNeedToShowApps: Boolean
    ): Boolean {
        // 过滤掉指定包名的应用
        return hasFilterApps && IconManager.NEED_TO_BLOCKED_PACKAGE_NAMES.contains(packageName)
    }

    /**
     * 创建应用信息对象
     */
    private fun createAppInfo(
        pm: PackageManager,
        appInfo: ApplicationInfo,
        packageName: String,
        flags: Int
    ): AppInfo? {
        return try {
            val bean = AppInfo()
            bean.icon = appInfo.loadIcon(pm)
            bean.label = pm.getApplicationLabel(appInfo).toString()
            bean.package_name = packageName
            bean.flags = flags
            bean
        } catch (e: Exception) {
            Log.w(TAG, "Error creating app info for: $packageName", e)
            null
        }
    }

    private fun sortAppList(appList: MutableList<AppInfo>, hasPackageOrder: Boolean) {
        if (!hasPackageOrder) {
            // 如果没有预定义顺序，直接按包名排序
            appList.sortBy { it.package_name?.lowercase() }
            return
        }

        // 按预定义顺序分组排序
        val orderedApps = mutableListOf<AppInfo>()
        val remainingApps = mutableListOf<AppInfo>()

        for (app in appList) {
            val packageName = app.package_name
            if (packageName != null && IconManager.PACKAGE_ORDER_LIST.contains(packageName)) {
                orderedApps.add(app)
            } else {
                remainingApps.add(app)
            }
        }

        // 按预定义顺序排序
        orderedApps.sortBy { app ->
            IconManager.PACKAGE_ORDER_LIST.indexOf(app.package_name)
        }

        // 剩余应用按包名排序
        remainingApps.sortBy { it.package_name?.lowercase() }

        // 合并结果
        appList.clear()
        appList.addAll(orderedApps)
        appList.addAll(remainingApps)
    }

    /**
     * 显示PopupWindow（在锚点View下方）
     * @param anchorView 锚点View
     */
    fun show(anchorView: View) {
        if (!isShowing) {
            showAsDropDown(anchorView)
        }
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

    /**
     * 显示PopupWindow（在锚点View下方，带偏移和对齐方式）
     * @param anchorView 锚点View
     * @param xoff x轴偏移
     * @param yoff y轴偏移
     * @param gravity 对齐方式
     */
//    fun show(anchorView: View, xoff: Int, yoff: Int, gravity: Int) {
//        if (!isShowing) {
//            showAsDropDown(anchorView, xoff, yoff, gravity)
//        }
//    }

    /**
     * 显示在锚点View正下方（居中）
     * @param anchorView 锚点View
     */
    fun showBelowCenter(anchorView: View) {
        if (!isShowing) {
            // 测量PopupWindow的宽度
            mContentView.measure(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED
            )
            val popupWidth = mContentView.measuredWidth
            
            // 获取锚点View的位置
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            val anchorWidth = anchorView.width
            
            // 计算x偏移使PopupWindow居中
            val xoff = (anchorWidth - popupWidth) / 2
            
            showAsDropDown(anchorView, xoff, 0)
        }
    }

    public fun setOnPopupUpdateListener(onupdateListener: OnPopupUpdateListener){
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

    /**
     * 更新自定义数字参数
     * @param customNumber 新的数字
     */
    fun updateCustomNumber(customNumber: Int) {
        this.customNumber = customNumber

    }

    /**
     * 获取自定义数字参数
     */
    fun getCustomNumber(): Int = customNumber
    override fun onPopupDismissed() {
        dismissSafely()
        allAppInfoList = null
    }

    override fun updateImage(info: AppInfo?) {
        if(info!=null) {
            Settings.System.putString(
                mContext.contentResolver, "launcher_app_icon_$customNumber",
                info.package_name
            )
            info.flags = customNumber
        }
        Log.i(TAG,"info = "+ info)
        updateListener?.updateAppImage(info)
        onPopupDismissed()
    }

    interface OnPopupUpdateListener {
        /**
         * 数字参数变化时调用（可选）
         * @param  info
         */
        fun updateAppImage(info: AppInfo?)

        fun backgroundAlphaWindow(alpha: Float)
    }


}
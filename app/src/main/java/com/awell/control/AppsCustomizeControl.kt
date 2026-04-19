package com.awell.control

import AnimationHandler
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.awell.launcher.library.R
import com.awell.launcher2.ApplicationInfo
import com.awell.launcher2.AppsCustomizePagedView
import com.awell.launcher2.AppsCustomizeTabHost
import com.awell.launcher2.IconCache
import com.awell.launcher2.LauncherModel
import com.awell.launcher2.PluginIconManager
import com.awell.launcher2.Utilities
import com.awell.ui.AppsCustomizeIndicatorPanel
import com.awell.utils.LogUtil
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object AppsCustomizeControl {

    private val TAG = AppsCustomizeControl::class.simpleName

    private var DEBUG = false

    private var currentViewGroup: WeakReference<ViewGroup>? = null

    private var originalViewsVisibility = mutableMapOf<Int, Int>()

    private val animationHandle = AnimationHandler()

    private var mIsInitialized = false

    /**
     * 二级菜单全部app根布局
     */
    @SuppressLint("StaticFieldLeak")
    var mAppsCustomizeTabHost: AppsCustomizeTabHost? = null

    /**
     * 二级菜单图标显示内容
     */
    private var mAppsCustomizeContent: AppsCustomizePagedView? = null

    private lateinit var mAppContext: Context
    private lateinit var mModel: LauncherModel
    private lateinit var mIconCache: IconCache


    var mAllIsShowing = false


    fun initialize(context: Context, model: LauncherModel, iconCache: IconCache) {
        if (mIsInitialized)
            return
        //Log.i(TAG, "initialize: huang initialize==>${this}")
        mAppContext = context.applicationContext
        initView(context)
        mModel = model
        mIsInitialized = true
        mIconCache = iconCache

    }

    fun setPluginThemeMode(themeMode: Int) {
        mIconCache.setPluginThemeMode(themeMode)
        Utilities.setPluginThemeMode(themeMode, mAppContext)
        resetAppIconAttributes()
        LogUtil.d( "setPluginThemeMode: themeMode=$themeMode")
    }
    //还原app icon属性
    private fun resetAppIconAttributes() {
        resetAppIconSize()
        resetAppIconPadding()

    }

    private fun resetAppIconPadding() {
        val result = mAppsCustomizeContent?.resetAppIconPadding() ?: false
        if (result) {
            LogUtil.d("resetAppIconPadding: successfully reset to default padding")
        }
    }

    /**
     * 设置 Plugin 的图标映射表到主应用
     * @param iconMap Plugin 提供的图标映射表（包名 -> 资源名称）
     * @param defaultPluginBgName 默认背景资源名称
     * @param pluginPackageName Plugin 包名（可选，如果不传则使用之前的设置）
     */
    fun setPluginIconMap(iconMap: Map<String, String>, defaultPluginBgName: String? = null, pluginPackageName: String) {
        try {
            // 设置 Plugin 配置
            PluginIconManager.setPluginPackageName(pluginPackageName)
            if (defaultPluginBgName != null)// 设置默认背景,如果为空,表示不使用默认背景
                PluginIconManager.setDefaultPluginBgName(defaultPluginBgName)
            
            // 设置图标映射表
            PluginIconManager.setIconMap(iconMap)

            //如果启用了 Plugin 图标映射表，则主题模式设置为默认值 (1)，不与其他主题冲突
            setPluginThemeMode(0xff) // 使用plugin图标
            
            //Log.i(TAG, "setPluginIconMap: 成功设置 Plugin 图标映射表")
            //Log.i(TAG, "  - Plugin 包名: ${PluginIconManager.getPluginPackageName()} " + "  - 默认背景: $defaultPluginBgName " + "  - 图标数量: ${iconMap.size}")
        } catch (e: Exception) {
            LogUtil.e( "setPluginIconMap: 设置图标映射表失败", e)
            setPluginThemeMode(1) // 使用普通图标
        }
    }


    fun setActivity(context: Context?) {
        mAppsCustomizeContent?.setActivity(context)
    }

    fun setAppIconPadding(paddingTop: Int) {
        if (paddingTop > 0) {
            mAppsCustomizeContent?.setAppIconPadding(paddingTop)
        } else {
            LogUtil.w( "setAppIconPadding: AppIconPadding must be greater than 0")
        }
    }

    /**
     * 设置应用图标大小
     * @param iconSize 图标大小（像素）。传入 <= 0 的值将重置为默认值（兼容旧 plugin）
     * @return 是否设置成功
     */
    fun setAppIconSize(iconSize: Int): Boolean {
        val result = mAppsCustomizeContent?.setAppIconSize(iconSize) ?: false
        if (result) {
            if (iconSize <= 0) {
                LogUtil.d("setAppIconSize: successfully reset to default size")
            } else {
                LogUtil.d("setAppIconSize: successfully set icon size to $iconSize")
            }
        }
        return result
    }

    /**
     * 重置应用图标大小为默认值（R.dimen.app_icon_size）
     * @return 是否重置成功
     */
    fun resetAppIconSize(): Boolean {
        val result = mAppsCustomizeContent?.resetAppIconSize() ?: false
        if (result) {
            LogUtil.d("resetAppIconSize: successfully reset to default size")
        }
        return result
    }

    /**
     * 获取当前应用图标大小
     * @return 当前图标大小（像素），如果未设置则返回 -1
     */
    fun getAppIconSize(): Int {
        return mAppsCustomizeContent?.getAppIconSize() ?: -1
    }

    fun setAppGap(autoWidthGap: Boolean) {
        mAppsCustomizeContent?.setAutoAppWidthGap(autoWidthGap)
    }

    /**
     * 设置每排显示的应用图标数量
     * @param countPerRow 每排显示的应用图标数量（必须大于 0）
     */
    fun setAppCountPerRow(countPerRow: Int) {
        if (countPerRow > 0) {
            mAppsCustomizeContent?.appCountPerRow = countPerRow
        } else {
            LogUtil.w( "setAppCountPerRow: countPerRow must be greater than 0")
        }
    }

    /**
     * 获取当前每排显示的应用图标数量
     * @return 每排显示的应用图标数量
     */
    fun getAppCountPerRow(): Int {
        return mAppsCustomizeContent?.appCountPerRow ?: 6
    }

    /**
     * 初始化布局View
     */
    @SuppressLint("InflateParams")
    private fun initView(context: Context) {
        mAppsCustomizeTabHost = LayoutInflater.from(mAppContext)
            .inflate(R.layout.apps_customize_pane, null) as AppsCustomizeTabHost?
        mAppsCustomizeContent =
            mAppsCustomizeTabHost?.findViewById<View>(R.id.apps_customize_pane_content) as AppsCustomizePagedView

        mAppsCustomizeTabHost?.findViewById<LinearLayout>(R.id.apps_customize_content)?.visibility =
            View.VISIBLE

        //二级菜单全部app页码指示
        val mAppsCustomizeIndicatorPanel = AppsCustomizeIndicatorPanel(context)
        mAppsCustomizeContent?.pageIndication = mAppsCustomizeIndicatorPanel

        //二级菜单全部app页码指示父布局
        val mAppsCustomizeIndicatorDots =
            mAppsCustomizeTabHost?.findViewById<View>(R.id.mIndicatorDots) as ViewGroup
        mAppsCustomizeIndicatorDots.addView(mAppsCustomizeIndicatorPanel)

        mAppsCustomizeContent?.onPackagesUpdated()
    }

    fun showApps(viewGroup: ViewGroup) {
        if (!mIsInitialized) throw IllegalStateException("Apps control not initialized")

        if (mAllIsShowing) {
            if (DEBUG) {
                LogUtil.i( "showApps: huang already show all apps=>")
            }
            return
        }
        currentViewGroup = WeakReference(viewGroup)
        saveActivityState(viewGroup)

        // 隐藏所有内容视图
        hideAllContentViews(viewGroup)

        removeFromParent()

        viewGroup.addView(
            mAppsCustomizeTabHost,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (DEBUG) {
            LogUtil.i( "showApps: huang show all apps=>")
        }
        mAppsCustomizeTabHost?.requestFocus()
        mAppsCustomizeTabHost?.let { animationHandle.animateShow(it) }

        mAllIsShowing = true
    }

    private fun hideAllContentViews(viewGroup: ViewGroup) {
        val contentView = viewGroup
        for (i in 0 until contentView.childCount) {
            val child = contentView.getChildAt(i)
            if (child != mAppsCustomizeTabHost) {
                child.visibility = View.GONE
            }
        }
    }

    /**
     * 再添加全部app列表显示前确保先移除该View
     */
    private fun removeFromParent() {
        (mAppsCustomizeTabHost?.parent as? ViewGroup)?.removeView(mAppsCustomizeTabHost)
        mAllIsShowing = false

    }

    /**
     * 在显示全部app列表时需要隐藏当前Activity的全部子view，否则会显示在全部app列表后面
     *
     */
    private fun saveActivityState(viewGroup: ViewGroup) {
        // 保存视图可见性状态
        val contentView = viewGroup
        for (i in 0 until contentView.childCount) {
            val child = contentView.getChildAt(i)
            if (child != mAppsCustomizeTabHost) {
                originalViewsVisibility[child.id] = child.visibility
            }
        }
    }

    /**
     * 隐藏全部app时显示当前Activity的子view
     */
    private fun restoreActivityState() {
        currentViewGroup?.get()?.let { viewGroup ->

            // 恢复视图可见性
            val contentView = viewGroup
            for (i in 0 until contentView.childCount) {
                val child = contentView.getChildAt(i)
                if (child != mAppsCustomizeTabHost) {
                    originalViewsVisibility[child.id]?.let { visibility ->
                        child.visibility = visibility
                    }
                }
            }
            originalViewsVisibility.clear()
        }
    }

    fun hideApps() {
        if (!mAllIsShowing) {
            if (DEBUG)
                LogUtil.i( "hideApps: huang already hide all apps==>")
            return
        }

        removeFromParent()
        restoreActivityState()

        mAllIsShowing = false
    }


    fun bindPackagesUpdated() {
        //LogUtil.i( "bindPackagesUpdated: huang ==>")
        mAppsCustomizeContent?.onPackagesUpdated()
    }

    fun bindAppsRemoved(packageNames: ArrayList<String>?, permanent: Boolean) {
        //LogUtil.i( "bindAppsRemoved: huang ==>")
        mAppsCustomizeContent?.removeApps(packageNames)
    }

    fun bindAppsUpdated(apps: ArrayList<ApplicationInfo>?) {
        //LogUtil.i( "bindAppsUpdated: huang bind apps update =>")
        mAppsCustomizeContent?.updateApps(apps)

    }

    fun bindAppsAdded(apps: ArrayList<ApplicationInfo>?) {
        //LogUtil.i( "bindAppsAdded: huang apps ==>")
        mAppsCustomizeContent?.addApps(apps)
    }

    fun bindApps(apps: ArrayList<ApplicationInfo>?) {
        val setAllAppsRunnable = Runnable {
            //LogUtil.i( "bindApps: huang bind apps=>")
            mAppsCustomizeContent?.setApps(apps)
        }


        // Remove the progress bar entirely; we could also make it GONE
        // but better to remove it since we know it's not going to be used
        val progressBar =
            mAppsCustomizeTabHost?.findViewById<View>(R.id.apps_customize_progress_bar)
        if (progressBar != null) {
            (progressBar.parent as ViewGroup).removeView(progressBar)

            // We just post the call to setApps so the user sees the progress
            // bar
            // disappear-- otherwise, it just looks like the progress bar froze
            // which doesn't look great
            mAppsCustomizeTabHost?.post(setAllAppsRunnable)
        } else {
            // If we did not initialize the spinner in onCreate, then we can
            // directly set the
            // list of applications without waiting for any progress bars views
            // to be hidden.
            setAllAppsRunnable.run()
        }
    }
}
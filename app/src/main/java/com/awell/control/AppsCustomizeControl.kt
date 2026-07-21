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
    //private var mAppsCustomizeConfig: AppsCustomizeConfig? = null


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


    fun setPluginThemeMode(config: AppsCustomizeConfig) {

        //PluginIconManager.reset()
        // 设置 Plugin 配置
        PluginIconManager.setPluginConfig(config.iconMap,config.pluginPackageName,config.pluginOtherBgName )
        mIconCache.setPluginThemeMode(config.themeMode ,config.iconSizeSame, config.iconCropWidth ,config.iconCropHeight )

        // 直接传递给 AppsCustomizePagedView 进行数据处理
        mAppsCustomizeContent?.processAppsCustomizeConfig(config)

        LogUtil.d("setPluginThemeMode: config=${config.toString()}, attributes reset immediately")
    }

    fun setActivity(context: Context?) {
        mAppsCustomizeContent?.setActivity(context)
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
                LogUtil.i( "showApps: huang already show all apps=>")
            return
        }
        currentViewGroup = WeakReference(viewGroup)
        saveActivityState(viewGroup)

        // 隐藏所有内容视图
        hideAllContentViews(viewGroup)

        removeFromParent()
        mAppsCustomizeContent?.forceRefreshBeforeShow()

        viewGroup.addView(
            mAppsCustomizeTabHost,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        LogUtil.i( "showApps: huang show all apps=>")

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
                val parent = mAppsCustomizeTabHost?.parent as? ViewGroup
        if (parent != null && mAppsCustomizeTabHost != null) {
            try {
                parent.removeView(mAppsCustomizeTabHost)
                LogUtil.d("removeFromParent: successfully removed from parent")
            } catch (e: Exception) {
                LogUtil.e("removeFromParent: error removing view", e)
            }
        } else {
            LogUtil.d("removeFromParent: no parent or tabHost is null, nothing to remove")
        }
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
        LogUtil.d("hideApps: mAllIsShowing=$mAllIsShowing")
        
        // 无论 mAllIsShowing 是什么状态，都尝试清理
        // 这样可以确保在 plugin 切换时能够正确清理
        removeFromParent()
        restoreActivityState()
        
        // 强制重置状态
        mAllIsShowing = false
        LogUtil.d("hideApps: apps hidden successfully")
    }


    fun bindPackagesUpdated() {
        LogUtil.i( "bindPackagesUpdated: huang ==>")
        mAppsCustomizeContent?.onPackagesUpdated()
    }

    fun bindAppsRemoved(packageNames: ArrayList<String>?, permanent: Boolean) {
        LogUtil.i( "bindAppsRemoved: huang ==>")
        mAppsCustomizeContent?.removeApps(packageNames)
    }

    fun bindAppsUpdated(apps: ArrayList<ApplicationInfo>?) {
        LogUtil.i( "bindAppsUpdated: huang bind apps update =>")
        mAppsCustomizeContent?.updateApps(apps)

    }

    fun bindAppsAdded(apps: ArrayList<ApplicationInfo>?) {
        LogUtil.i( "bindAppsAdded: huang apps ==>")
        mAppsCustomizeContent?.addApps(apps)
    }

    fun bindApps(apps: ArrayList<ApplicationInfo>?) {
        val setAllAppsRunnable = Runnable {
            LogUtil.i( "bindApps: huang bind apps=>")
            mAppsCustomizeContent?.setApps(apps)
        }


        // Remove the progress bar entirely; we could also make it GONE
        // but better to remove it since we know it's not going to be used
        /*val progressBar =
            mAppsCustomizeTabHost?.findViewById<View>(R.id.apps_customize_progress_bar)
        if (progressBar != null) {
            (progressBar.parent as ViewGroup).removeView(progressBar)

            // We just post the call to setApps so the user sees the progress
            // bar
            // disappear-- otherwise, it just looks like the progress bar froze
            // which doesn't look great
            mAppsCustomizeTabHost?.post(setAllAppsRunnable)
        } else {*/
            // If we did not initialize the spinner in onCreate, then we can
            // directly set the
            // list of applications without waiting for any progress bars views
            // to be hidden.
            setAllAppsRunnable.run()
       // }
    }
}
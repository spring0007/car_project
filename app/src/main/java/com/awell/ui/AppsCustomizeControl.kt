package com.awell.ui

import AnimationHandler
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.awell.launcher.R
import com.awell.launcher2.ApplicationInfo
import com.awell.launcher2.AppsCustomizePagedView
import com.awell.launcher2.AppsCustomizeTabHost
import com.awell.launcher2.IconCache
import com.awell.launcher2.Launcher
import com.awell.launcher2.LauncherModel
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object AppsCustomizeControl {

    private val TAG = AppsCustomizeControl::class.simpleName

    private var currentActivity: WeakReference<Activity>? = null

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

    var mAllIsShowing = false


    fun initialize(context: Context, model: LauncherModel, iconCache: IconCache) {
        if (mIsInitialized)
            return
        mAppContext = context.applicationContext

        initView(context)

        mModel = model

        mIsInitialized = true

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



    fun showApps(activity: Activity) {
        if (!mIsInitialized) throw IllegalStateException("Apps control not initialized")

        currentActivity = WeakReference(activity)
        saveActivityState(activity)

        // 隐藏所有内容视图
        hideAllContentViews(activity)

        removeFromParent()

        activity.findViewById<ViewGroup>(android.R.id.content).addView(
            mAppsCustomizeTabHost,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        //showAppsCustomizeHelper(true, false)
        mModel.startLoader(true, -1)

        mAppsCustomizeContent?.setup(
            (activity as Launcher),
            (activity as Launcher).dragController
        )


        mAppsCustomizeTabHost?.requestFocus()
        mAppsCustomizeTabHost?.let { animationHandle.animateShow(it) }

        mAllIsShowing = true
    }

    private fun hideAllContentViews(activity: Activity) {
        val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
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
    private fun saveActivityState(activity: Activity) {
        // 保存视图可见性状态
        val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
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
        currentActivity?.get()?.let { activity ->

            // 恢复视图可见性
            val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
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

        removeFromParent()
        restoreActivityState()

        mAllIsShowing = false
    }

    fun bindPackagesUpdated() {
        Log.i(TAG, "bindPackagesUpdated: huang ==>")
        mAppsCustomizeContent?.onPackagesUpdated()
    }

    fun bindAppsRemoved(packageNames: ArrayList<String>?, permanent: Boolean) {
        Log.i(TAG, "bindAppsRemoved: huang ==>")
        mAppsCustomizeContent?.removeApps(packageNames)
    }

    fun bindAppsUpdated(apps: ArrayList<ApplicationInfo>?) {
        Log.i(TAG, "bindAppsUpdated: huang bind apps update =>")
        mAppsCustomizeContent?.updateApps(apps)

    }

    fun bindAppsAdded(apps: ArrayList<ApplicationInfo>?) {
        Log.i(TAG, "bindAppsAdded: huang apps ==>")
        mAppsCustomizeContent?.addApps(apps)
    }

    fun bindApps(apps: ArrayList<ApplicationInfo>?) {
        Log.i(TAG, "bindApps: huang apps=>${apps}")
        val setAllAppsRunnable = Runnable {
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


    private fun refreshAppList() {
        //todo show apps
    }
}
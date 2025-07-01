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
import com.awell.launcher2.LauncherModel

@SuppressLint("StaticFieldLeak")
object AppsCustomizeControl {

    private val TAG = AppsCustomizeControl::class.simpleName

    private val animationHandle = AnimationHandler()

    private var mIsInitialized = false

    @SuppressLint("StaticFieldLeak")
    var mAppsCustomizeTabHost: AppsCustomizeTabHost? = null

    /**
     * 二级菜单图标
     */
    private var mAppsCustomizeContent: AppsCustomizePagedView? = null

    private var mAppsCustomizeIndicatorDots: ViewGroup? = null
    var mAppsCustomizeIndicatorPanel: AppsCustomizeIndicatorPanel? = null

    private lateinit var mAppContext: Context
    private lateinit var mModel: LauncherModel

    var mAllIsShowing = false


    @SuppressLint("InflateParams")
    fun initialize(context: Context, model: LauncherModel, iconCache: IconCache) {
        if (mIsInitialized)
            return
        mAppContext = context.applicationContext

        mAppsCustomizeTabHost = LayoutInflater.from(mAppContext)
            .inflate(R.layout.apps_customize_pane, null) as AppsCustomizeTabHost?
        mAppsCustomizeIndicatorPanel = AppsCustomizeIndicatorPanel(context)
        mAppsCustomizeContent =
            mAppsCustomizeTabHost?.findViewById<View>(R.id.apps_customize_pane_content) as AppsCustomizePagedView

        mAppsCustomizeTabHost?.findViewById<LinearLayout>(R.id.apps_customize_content)?.visibility =
            View.VISIBLE

        mAppsCustomizeIndicatorDots =
            mAppsCustomizeTabHost?.findViewById<View>(R.id.mIndicatorDots) as ViewGroup

        mAppsCustomizeContent?.onPackagesUpdated()
        //todo
        //mAppsCustomizeContent?.setup(this, dragController)

        mModel = model

        //mModel.initialize(this)

        mIsInitialized = true

    }

    fun showApps(activity: Activity) {
        if (!mIsInitialized) throw IllegalStateException("Apps control not initialized")
        removeFromParent()
        activity.findViewById<ViewGroup>(android.R.id.content).addView(
            mAppsCustomizeTabHost,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        //showAppsCustomizeHelper(true, false)
        mModel.startLoader(true, -1)

        mAllIsShowing = true
        mAppsCustomizeTabHost?.visibility = View.VISIBLE

        mAppsCustomizeIndicatorDots?.addView(mAppsCustomizeIndicatorPanel)

        mAppsCustomizeTabHost?.requestFocus()

        mAppsCustomizeTabHost?.let { animationHandle.animateShow(it) }
    }

    private fun removeFromParent() {

        (mAppsCustomizeTabHost?.parent as? ViewGroup)?.removeView(mAppsCustomizeTabHost)

        (mAppsCustomizeIndicatorPanel?.parent as? ViewGroup)?.removeView(
            mAppsCustomizeIndicatorPanel
        )

        mAllIsShowing = false

    }

    fun hideApps() {

        mAllIsShowing = false
        mAppsCustomizeTabHost?.visibility = View.GONE

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
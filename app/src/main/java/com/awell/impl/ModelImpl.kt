package com.awell.impl

import android.util.Log
import com.awell.launcher2.ApplicationInfo
import com.awell.launcher2.FolderInfo
import com.awell.launcher2.ItemInfo
import com.awell.launcher2.LauncherAppWidgetInfo
import com.awell.launcher2.LauncherModel
import com.awell.control.AppsCustomizeControl
import com.awell.launcher2.Launcher

class ModelImpl : LauncherModel.Callbacks {

    private val TAG = ModelImpl::class.simpleName

    private var mPaused = false

    override fun setLoadOnResume(): Boolean {
        Log.i(TAG, "setLoadOnResume: huang apps control =>")
        return mPaused
    }

    override fun getCurrentWorkspaceScreen(): Int {
        Log.i(TAG, "getCurrentWorkspaceScreen: huang get current workspace screen =>")
        return if (Launcher.mWorkspace != null) {
            Launcher.mWorkspace.currentPage
        } else {
            Launcher.SCREEN_COUNT / 2
        }
    }

    override fun startBinding() {
        //todo bind workspace
        Log.i(TAG, "startBinding: huang start binding =>")
    }

    override fun bindItems(shortcuts: ArrayList<ItemInfo>?, start: Int, end: Int) {
        //todo bind workspace
        Log.i(
            TAG,
            "bindItems: huang workspace bind items=>${shortcuts}, start=>${start}, end=>${end}"
        )
    }

    override fun bindFolders(folders: HashMap<Long, FolderInfo>?) {
        Log.i(TAG, "bindFolders: huang workspace bind folders=>${folders} ")
    }

    override fun finishBindingItems() {
        Log.i(TAG, "finishBindingItems: huang workspace finish binding items =>")
    }

    override fun bindAppWidget(info: LauncherAppWidgetInfo?) {
        Log.i(TAG, "bindAppWidget: huang workspace bind app widget=>")
    }

    override fun bindAllApplications(apps: ArrayList<ApplicationInfo>?) {
        Log.i(TAG, "bindAllApplications: huang bind all applications =>${apps}")
        AppsCustomizeControl.bindApps(apps)
    }


    override fun bindAppsAdded(apps: ArrayList<ApplicationInfo>?) {
        Log.i(TAG, "bindAppsAdded: huang bind apps added =>")
        AppsCustomizeControl.bindAppsAdded(apps)
    }

    override fun bindAppsUpdated(apps: ArrayList<ApplicationInfo>?) {
        Log.i(TAG, "bindAppsUpdated: huang bind apps update =>")
        AppsCustomizeControl.bindAppsUpdated(apps)
    }

    override fun bindAppsRemoved(packageNames: ArrayList<String>?, permanent: Boolean) {
        Log.i(TAG, "bindAppsRemoved: huang ==>")
        AppsCustomizeControl.bindAppsRemoved(packageNames, permanent)
    }

    override fun bindPackagesUpdated() {
        Log.i(TAG, "bindPackagesUpdated: huang ==>")
        AppsCustomizeControl.bindPackagesUpdated()
    }

    override fun isAllAppsVisible(): Boolean {
        Log.i(TAG, "isAllAppsVisible: huang ==>")
        return AppsCustomizeControl.mAllIsShowing
    }

    override fun isAllAppsButtonRank(rank: Int): Boolean {
        Log.i(TAG, "isAllAppsButtonRank: huang ==>")
        return false
    }

    override fun bindSearchablesChanged() {
        Log.i(TAG, "bindSearchablesChanged: huang ==>")

    }

    override fun onPageBoundSynchronously(page: Int) {
        Log.i(TAG, "onPageBoundSynchronously: huang page=>${page}")
    }
}
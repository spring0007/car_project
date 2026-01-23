package com.launcher.yfd_ui01.fragment


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.awell.addapp.AppInfo
import com.launcher.yfd_ui01.MainActivity_YFD_UI01
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.app.AppPagerAdapter
import com.launcher.yfd_ui01.app.IconManager
import com.launcher.yfd_ui01.manager.FragmentAnimation
import com.launcher.yfd_ui01.utils.LogUtil
import com.launcher.yfd_ui01.view.CustomViewPager
import com.launcher.yfd_ui01.view.OnVerticalSwipeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment() {

    private val TAG: String = "Menu_Fragment"
    private val ITEMS_PER_PAGE = 18 // 6列 x 3行 = 18个应用每页

    private lateinit var viewPager: CustomViewPager
    private lateinit var pageIndicator: LinearLayout
    private var iconManager: IconManager? = null

    private var packageReceiver: BroadcastReceiver? = null
    private var homeReceiver: BroadcastReceiver? = null
    private var isLoading = false

    // 页面监听器和适配器
    private var pageChangeListener: ViewPager.OnPageChangeListener? = null
    private var currentAdapter: AppPagerAdapter? = null
    protected lateinit var swipeActivity: MainActivity_YFD_UI01
    private var loadJob: Job? = null

    // 状态标志
    private var isViewCreated = false
    private var isDataInitialized = false
    private var shouldLoadDataOnResume = false

    // 新增：用于等待视图布局完成的Handler
    private val handler = Handler(Looper.getMainLooper())
    private var layoutCheckRunnable: Runnable? = null
    private var layoutObserver: ViewTreeObserver.OnGlobalLayoutListener? = null

    companion object {
        fun newInstance(): MenuFragment {
            return MenuFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.i("onCreate called")
        // 确保Fragment不会被重建时重复添加
        retainInstance = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LogUtil.i("onCreateView called")
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtil.i("onViewCreated called, savedInstanceState=$savedInstanceState")
        swipeActivity = activity as MainActivity_YFD_UI01
        viewPager = view.findViewById(R.id.viewPager1)
        pageIndicator = view.findViewById(R.id.pageIndicator)
        iconManager = IconManager.getInstance(swipeActivity.applicationContext)
        isViewCreated = true

        // 先设置默认视图
        setupDefaultViews()

        // 监听ViewPager的尺寸变化
        setupViewPagerSizeListener();
        // 检查视图是否已经布局完成
        checkViewLayoutAndInitData()

        setupViewPagerListener()
        registerPackageReceiver()
    }

    private fun setupViewPagerSizeListener() {

        // 添加布局变化监听器
        viewPager.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val newWidth = right - left
            val newHeight = bottom - top
            val oldWidth = oldRight - oldLeft
            val oldHeight = oldBottom - oldTop

            if (newWidth != oldWidth || newHeight != oldHeight) {
                LogUtil.i("ViewPager尺寸变化: ${newWidth}x${newHeight} (之前: ${oldWidth}x${oldHeight})")

                // 尺寸变化后重新设置适配器
                if (newWidth > 0 && newHeight > 0 && oldHeight>0 && oldWidth> 0 && currentAdapter != null) {
                    viewPager.postDelayed(Runnable {
                        currentAdapter!!.updateAllPageLayouts(newWidth, newHeight)
                    }, 100)
                    LogUtil.i("ViewPager尺寸变化后重新布局完成")

                }
            }
        }
    }

    /**
     * 检查视图布局状态并初始化数据
     */
    private fun checkViewLayoutAndInitData() {
        if (view == null) {
            LogUtil.i("View为null，无法检查布局")
            return
        }

        // 方法1：使用ViewTreeObserver监听布局完成
        layoutObserver = ViewTreeObserver.OnGlobalLayoutListener {
            if (isViewReady()) {
                // 布局完成，初始化数据
                view?.viewTreeObserver?.removeOnGlobalLayoutListener(layoutObserver)
                layoutObserver = null
                safeInitData()
            }
        }

        view?.viewTreeObserver?.addOnGlobalLayoutListener(layoutObserver)

        // 方法2：同时使用Handler延迟检查作为备份
        /*layoutCheckRunnable = Runnable {
            if (isViewReady()) {
                safeInitData()
            } else {
                // 如果还没准备好，再次延迟检查
                handler.postDelayed(layoutCheckRunnable!!, 100) // 约1帧的时间
            }
        }

        handler.post(layoutCheckRunnable!!)*/
    }

    /**
     * 检查视图是否准备好
     */
    private fun isViewReady(): Boolean {
        return if (view != null && requireView().width > 0 && requireView().height > 0) {
            //LogUtil.i( "视图已准备好，宽=${requireView().width}, 高=${requireView().height}")
            true
        } else {
            //LogUtil.i( "视图未准备好，宽=${view?.width ?: 0}, 高=${view?.height ?: 0}")
            false
        }
    }

    /**
     * 安全地初始化数据
     */
    private fun safeInitData() {
        // LogUtil.i( "safeInitData called, viewCreated=$isViewCreated, initialized=$isDataInitialized")

        if (!isViewCreated || isDataInitialized) return

        // 检查视图是否可用
        if (!isAdded || view == null || view?.parent == null) {
            LogUtil.w("视图不可用，延迟初始化")
            view?.post {
                if (isViewCreated && !isDataInitialized) {
                    initData()
                }
            }
            return
        }

        initData()
    }

    private fun initData() {
        if (!isViewCreated || isDataInitialized || !isAdded) {
            LogUtil.w("initData跳过: viewCreated=$isViewCreated, initialized=$isDataInitialized, added=$isAdded")
            return
        }

        LogUtil.i("initData called")
        // 设置默认视图
        setupDefaultViews()
        // 立即开始加载应用数据
        loadApps()

        isDataInitialized = true
    }

    private fun setupDefaultViews() {
        LogUtil.i("setupDefaultViews called")
        // 设置初始页面指示器
        pageIndicator.removeAllViews()
        val dot = ImageView(swipeActivity.applicationContext).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(5, 0, 5, 0)
            }
            setImageResource(R.drawable.indicator_selected)
        }
        pageIndicator.addView(dot)
        pageIndicator.visibility = View.GONE // 初始隐藏，加载完成后再显示
    }


    private fun startFragment() {
        if (!isAdded || isDetached) return
        // 触发切换到Fragment
        try {
            /*requireActivity().supportFragmentManager.beginTransaction()
                //.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, MainFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()*/
            swipeActivity.goToFragment(0, FragmentAnimation.FADE)
        } catch (e: IllegalStateException) {
            LogUtil.e("Error starting fragment", e)
        }
    }


    private fun loadApps() {
        if (!isAdded || isLoading || view == null) {
            LogUtil.i("loadApps: 条件不满足 - added=$isAdded, loading=$isLoading, view=${view != null}")
            return
        }
        isLoading = true

        loadJob?.cancel() // 取消之前的加载任务

        loadJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                LogUtil.i("开始加载应用列表")

                // 检查Fragment状态
//                if (!isAdded || view == null) {
//                    LogUtil.i("loadApps: Fragment状态异常，取消加载")
//                    return@launch
//                }

                val allApps = withContext(Dispatchers.IO) {
                    getAllAppInfo(swipeActivity.applicationContext)
                }

                // 再次检查Fragment状态
                if (!isAdded || view == null) {
                    LogUtil.i("loadApps: Fragment状态异常，取消显示")
                    return@launch
                }

                LogUtil.i("应用列表加载完成，共${allApps.size}个应用")

                val pages = splitIntoPages(allApps)
                LogUtil.i("viewPager,height = ${viewPager.height} ,width =${viewPager.width} ")
                currentAdapter = AppPagerAdapter(swipeActivity, pages, ITEMS_PER_PAGE)

                // 确保ViewPager存在
                if (viewPager == null) {
                    LogUtil.i("ViewPager is null")
                    return@launch
                }

                // 等待ViewPager完成测量
                if (viewPager.height <= 0) {
                    LogUtil.i("ViewPager高度为0，等待测量完成")
                    viewPager.post {
                        setAdapterAndIndicator()
                    }
                } else {
                    setAdapterAndIndicator()
                }

            } catch (e: Exception) {
                LogUtil.e("Error loading apps", e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * 设置适配器和指示器（在视图测量完成后调用）
     */
    private fun setAdapterAndIndicator() {
        LogUtil.i("setAdapterAndIndicator, ViewPager高度=${viewPager.height}")

        try {
            if (viewPager.adapter != currentAdapter) {
                viewPager.adapter = currentAdapter
            }
            setupPageIndicator()

            // 预加载常用图标
           // viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
           //     iconManager?.preloadCommonIcons()
           // }

            LogUtil.i("ViewPager适配器设置完成")
        } catch (e: Exception) {
            LogUtil.e("Error setting adapter", e)
        }
    }

    /**
     * 将应用列表分页
     */
    private fun splitIntoPages(allApps: List<AppInfo>): List<List<AppInfo>> {
        val pages = mutableListOf<List<AppInfo>>()
        if (allApps.isEmpty()) return pages

        for (i in allApps.indices step ITEMS_PER_PAGE) {
            val end = (i + ITEMS_PER_PAGE).coerceAtMost(allApps.size)
            pages.add(allApps.subList(i, end))
        }
        return pages
    }

    /**
     * 获取所有应用信息
     */
    private suspend fun getAllAppInfo(ctx: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val appBeanList = mutableListOf<AppInfo>()
        val packageManager = ctx.packageManager

        try {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            //val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            val packages = getAppList(packageManager, resolveInfos)
            val FLAG_SYSTEM = ApplicationInfo.FLAG_SYSTEM
            // 预计算过滤条件
            //val hasFilterApps = IconManager.NEED_TO_BLOCKED_PACKAGE_NAMES.isNotEmpty()
            //val hasNeedToShowApps = false // !IconManager.NEED_TO_SHOW_PACKAGE_NAMES.isEmpty()
            //val hasPackageOrder = IconManager.PACKAGE_ORDER_LIST.isNotEmpty()
            val filterSet = IconManager.NEED_TO_BLOCKED_PACKAGE_NAMES.toSet()

            // 第一阶段：快速收集基本信息
            val tempList = packages.asSequence()
                .filter { it.applicationInfo.enabled }
                .filterNot { it.packageName.contains("launcher", ignoreCase = true) }
                .filterNot { it.packageName in filterSet }
                .mapNotNull { appInfo ->
                    val hasLaunch = packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
                    if (!hasLaunch) return@mapNotNull null

                    createAppInfo(
                        icon = null,//appInfo.applicationInfo.loadIcon(packageManager), // 图标留空，延迟加载
                        packageName = appInfo.packageName,
                        label = appInfo.applicationInfo.loadLabel(packageManager).toString(),
                        flags = appInfo.applicationInfo.flags

                    )
                }
                .toList()


            // 第二阶段：批量处理图标
            //updateAppIcons(tempList)//重复设置图片
            appBeanList.addAll(tempList)

            // 第三阶段：排序
            sortAppList(appBeanList)

        } catch (e: Exception) {
            LogUtil.e("Error getting app info", e)
        }

        LogUtil.i("获取到${appBeanList.size}个应用")
        return@withContext appBeanList
    }

    private fun getAppList(
        packageManager: PackageManager,
        resolveInfos: List<ResolveInfo>
    ): List<PackageInfo> {
        val appList: MutableList<PackageInfo> = ArrayList()
        val processed: MutableSet<String> = HashSet()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            if (processed.contains(packageName)) {
                continue
            }

            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                appList.add(packageInfo)
                processed.add(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                // 忽略异常
            }
        }
        return appList
    }

    /**
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

    private fun createAppInfo(
        icon: Drawable?,
        label: String,
        packageName: String,
        flags: Int
    ): AppInfo? {
        return try {
            val bean = AppInfo()
            bean.icon = icon
            bean.label = label
            bean.package_name = packageName
            bean.flags = flags
            bean
        } catch (e: Exception) {
            LogUtil.e("Error creating app info for: $packageName", e)
            null
        }
    }

    /**
     * 批量更新应用图标
     */
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

    /**
     * 排序应用列表
     */
    private fun sortAppList(appList: MutableList<AppInfo>) {
        val hasPackageOrder = IconManager.PACKAGE_ORDER_LIST.isNotEmpty()

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

    private fun setupPageIndicator() {
        val adapter = viewPager.adapter ?: return

        val pageCount = adapter.count

        if (pageCount < 2) {
            pageIndicator.visibility = View.GONE
            return
        }

        pageIndicator.visibility = View.VISIBLE
        pageIndicator.removeAllViews()

        for (i in 0 until pageCount) {
            val dot = ImageView(swipeActivity.applicationContext).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5, 0, 5, 0)
                }
                setImageResource(R.drawable.indicator_unselected)
            }
            pageIndicator.addView(dot)
        }

        updateIndicator(viewPager.currentItem)
    }

    private fun updateIndicator(currentPosition: Int) {
        for (i in 0 until pageIndicator.childCount) {
            val child = pageIndicator.getChildAt(i)
            if (child is ImageView) {
                val resId = if (i == currentPosition) {
                    R.drawable.indicator_selected
                } else {
                    R.drawable.indicator_unselected
                }
                child.setImageResource(resId)
            }
        }
    }

    private fun setupViewPagerListener() {
        viewPager.setOnVerticalSwipeListener(object : OnVerticalSwipeListener {
            override fun onVerticalSwipe(direction: Int, currentPage: Int, distanceY: Float): Boolean {
                LogUtil.d( "setupViewPagerListener: direction=$direction, currentPage=$currentPage, distanceY=$distanceY")
                if (direction == 1 && currentPage == 0) {
                    // 当在第一页时向下滑动,跳转上一个fragment
                    startFragment()
                    return true // 返回true表示已消费事件
                }

                // 返回false表示使用默认切换逻辑
                return false
            }
        })


        pageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position)
            }
        }
        pageChangeListener?.let { viewPager.addOnPageChangeListener(it) }
    }

    private fun registerPackageReceiver() {
        homeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!isVisible) return
                LogUtil.i("action=" + intent.action)
                when (intent.action) {
                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                        val reason = intent.getStringExtra("reason")
                        if (reason == "homekey") {
                            startFragment()
                        }
                    }
                }
            }
        }


        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //if(!isVisible)return
                LogUtil.i("pkg,action="+intent.action)
                //val packageName = intent.data?.schemeSpecificPart ?: return
                val packageName = intent.data!!.schemeSpecificPart
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

                var op = 0
                if (packageName == null || packageName.isEmpty()) {
                    return
                }
                when (intent.action) {
                    Intent.ACTION_PACKAGE_CHANGED -> op = 2
                    Intent.ACTION_PACKAGE_REMOVED -> if (!replacing) {
                        op = 3
                    }
                    Intent.ACTION_PACKAGE_ADDED -> if (!replacing) {
                        op = 1
                    } else {
                        op = 2
                    }
                }
                if(op!=0)
                    updateAppData()
            }
        }

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)

            addDataScheme("package")
        }

        val homeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }

        try {
            ContextCompat.registerReceiver(
                swipeActivity.applicationContext,
                homeReceiver,
                homeFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            ContextCompat.registerReceiver(
                swipeActivity.applicationContext,
                packageReceiver,
                packageFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            LogUtil.e("Error registering receivers", e)
        }
    }

    private fun updateAppData() {
        LogUtil.i("updateAppData")
        if (!isAdded ||isLoading) {
            LogUtil.i("正在加载中，忽略此次更新")
            return
        }

        loadJob?.cancel()

        // 仅更新 adapter 数据以避免替换 adapter 导致的 UI 颤抖
        loadJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                isLoading = true

                val allApps = withContext(Dispatchers.IO) {
                    getAllAppInfo(swipeActivity.applicationContext)
                }

                if (!isAdded) return@launch

                val pages = splitIntoPages(allApps)

                // 若已有 adapter，则直接更新数据；否则新建 adapter 并设置一次
                if (currentAdapter != null ){
                    // adapter 已存在但尚未绑定到 viewPager（罕见情况）
                    if(viewPager.adapter != currentAdapter)
                        viewPager.adapter = currentAdapter
                    currentAdapter?.updateData(pages)
                    setupPageIndicator()
                } else {
                    // 没有 adapter：创建后设置（仅第一次或极少出现）
                    currentAdapter = AppPagerAdapter(swipeActivity, pages, ITEMS_PER_PAGE)
                    viewPager.post {
                        try {
                            setAdapterAndIndicator()
                        } catch (e: Exception) {
                            LogUtil.e("Error setting adapter in updateAppData", e)
                        }
                    }
                }

            } catch (e: Exception) {
                LogUtil.e("Error updating app data", e)
            } finally {
                isLoading = false
            }
        }

    }

    private fun closeReceiver(){
        // 取消广播注册
        packageReceiver?.let {
            try {
                requireContext().unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // 忽略已经反注册的情况
            }
            packageReceiver = null
        }

        homeReceiver?.let {
            try {
                requireContext().unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // 忽略已经反注册的情况
            }
            homeReceiver = null
        }
    }

    private fun cleanupViews() {
        pageChangeListener?.let {
            viewPager.removeOnPageChangeListener(it)
        }
        pageChangeListener = null
        viewPager.adapter = null
        currentAdapter = null

        // 移除布局监听器和Runnable
        layoutObserver?.let {
            view?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            layoutObserver = null
        }

        layoutCheckRunnable?.let {
            handler.removeCallbacks(it)
            layoutCheckRunnable = null
        }
    }

    override fun onResume() {
        super.onResume()
        LogUtil.i("onResume called, viewCreated=$isViewCreated, dataInitialized=$isDataInitialized")

        // 如果需要加载数据，现在执行
        if (shouldLoadDataOnResume) {
            shouldLoadDataOnResume = false
            checkViewLayoutAndInitData()
        } else if (isViewCreated && !isDataInitialized) {
            // 视图已创建但数据未初始化，重新初始化
            checkViewLayoutAndInitData()
        }


    }

    override fun onPause() {
        super.onPause()
        LogUtil.i("onPause")
    }

    override fun onStop() {
        super.onStop()
        LogUtil.i("onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LogUtil.i("onDestroyView called")
        closeReceiver()
        cleanupViews()
        isViewCreated = false
        isDataInitialized = false
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.i("onDestroy")
        loadJob?.cancel()
        loadJob = null
        iconManager = null
        // 确保清理Handler相关资源
        layoutCheckRunnable?.let {
            handler.removeCallbacks(it)
            layoutCheckRunnable = null
        }
    }

}

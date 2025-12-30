package com.launcher.yfd_ui01.fragment


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.awell.addapp.AppInfo
import com.launcher.yfd_ui01.MainActivity_YFD_UI01
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.app.AppPagerAdapter
import com.launcher.yfd_ui01.app.IconManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isCancelled
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Objects
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class MenuFragment : Fragment() {

    private val TAG: String = "Menu_Fragment"
    private val ITEMS_PER_PAGE = 18 // 6列 x 3行 = 18个应用每页

    private lateinit var viewPager: ViewPager
    private lateinit var pageIndicator: LinearLayout
    private var iconManager: IconManager? = null
    
    private var packageReceiver: BroadcastReceiver? = null
    private var homeReceiver : BroadcastReceiver? = null
    private var executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var isLoading = false
    private var isFragmentActive = true
    
    // 页面监听器和适配器
    private var pageChangeListener: ViewPager.OnPageChangeListener? = null
    private var currentAdapter: AppPagerAdapter? = null
    protected lateinit var swipeActivity: MainActivity_YFD_UI01
    private var loadJob: Job? = null

    private lateinit var nestedScrollView: NestedScrollView
    private var lastY = 0f
    private var lastX = 0f
    private var countTouch:Int = 0
    private val SWIPE_THRESHOLD = 150 // 滑动阈值


    companion object {
        fun newInstance(): MenuFragment {
            return MenuFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)
        return view
       // return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeActivity = activity as MainActivity_YFD_UI01
        viewPager = view.findViewById(R.id.viewPager)
        nestedScrollView = view.findViewById(R.id.nestedScrollView)
        pageIndicator = view.findViewById(R.id.pageIndicator)
        iconManager = IconManager.getInstance(requireContext())
        initData()
    }
    
    private fun initData() {
        // 初始化数据
        loadApps()
        setupViewPagerListener()
        registerPackageReceiver()
        // 处理滑动冲突
        setupSwipeGesture()


    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeGesture() {
        nestedScrollView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    Log.i(TAG, "lastX=$lastX,lastY=$lastY")

                }
                MotionEvent.ACTION_UP -> {
                    countTouch= 0
                    Log.i(TAG, "action_up")
                }
                MotionEvent.ACTION_MOVE -> {
                    if(countTouch==0){
                        lastX = event.x
                        lastY = event.y
                        false
                    }
                    countTouch++
                    val deltaX = event.x - lastX
                    val deltaY = event.y - lastY

                    Log.i(TAG, "deltaY="+deltaY+",deltaX="+abs(deltaX))
                    if (deltaY > SWIPE_THRESHOLD  && abs(deltaX)< 80  ) {
                        // 检查是否需要切换到其他Fragment
                        if (shouldSwitchFragment()) {
                            return@setOnTouchListener true
                        }
                    }
                }
            }
            false
        }
    }

    private fun shouldSwitchFragment(): Boolean {
        // 如果向下滑动且已经在顶部
        if (!nestedScrollView.canScrollVertically(-1)) {
            startFragment()
            return true
        }
        return false
    }
    private fun startFragment(){
        Log.i(TAG,"startFragment")
        // 触发切换到Fragment
        requireActivity().supportFragmentManager.beginTransaction()
            //.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, MainFragment())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }


    /**
     * 设置 Fragment 活跃状态
     */
    fun setFragmentActive(active: Boolean) {
        this.isFragmentActive = active
        if (active && executorService.isShutdown) {
            // 如果线程池已关闭但需要重新激活，重新创建线程池
            executorService = Executors.newSingleThreadExecutor()
        }

    }

    fun isFragmentActive(): Boolean {
        return isFragmentActive
    }

    private fun loadApps() {
        if (!isFragmentActive || isLoading) return
        isLoading = true

        loadJob?.cancel() // 取消之前的加载任务

        loadJob = CoroutineScope(Dispatchers.IO).launch {
            if (!isFragmentActive) {
                isLoading = false
                return@launch
            }

            try {
                val allApps = getAllAppInfo(requireContext())
                if (isCancelled || !isFragmentActive) return@launch

                val pages = splitIntoPages(allApps)

                withContext(Dispatchers.Main) {
                    if (!isAdded || !isFragmentActive) return@withContext

                    try {
                        currentAdapter = AppPagerAdapter(requireContext(), pages, ITEMS_PER_PAGE)
                        viewPager.adapter = currentAdapter
                        setupPageIndicator()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting adapter", e)
                    } finally {
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
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

            // 第二阶段：批量处理图标
            requireActivity().runOnUiThread {
                updateAppIcons(tempList)
                appBeanList.addAll(tempList)

                // 第三阶段：排序
                sortAppList(appBeanList, hasPackageOrder)

                // 完成数据处理后更新UI
                onAppDataReady(appBeanList)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting app info", e)
        }

        return appBeanList
    }

    /**
     * 应用数据准备完成后的回调
     */
    private fun onAppDataReady(appList: List<AppInfo>) {
        if (!isFragmentActive || !isAdded) return

        val pages = splitIntoPages(appList)

        view?.post {
            if (!isAdded || !isFragmentActive) return@post

            try {
                currentAdapter = AppPagerAdapter(requireContext(), pages, ITEMS_PER_PAGE)
                viewPager.adapter = currentAdapter
                setupPageIndicator()

                // 预加载常用图标
                iconManager?.preloadCommonIcons()
            } catch (e: Exception) {
                Log.e(TAG, "Error setting adapter", e)
            } finally {
                isLoading = false
            }
        }
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
            val dot = ImageView(context)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(5, 0, 5, 0)
            dot.layoutParams = params
            dot.setImageResource(R.drawable.indicator_unselected)
            pageIndicator.addView(dot)
        }

        updateIndicator(0)
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
        pageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position)
            }
        }
        pageChangeListener?.let { viewPager.addOnPageChangeListener(it) }
    }

    private fun registerPackageReceiver() {

        homeReceiver= object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when (action) {
                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                        var reason = intent.getStringExtra("reason");
                        Log.i(TAG,"ACTION_CLOSE_SYSTEM_DIALOGS")
                        if (reason == "homekey") { //home键
                            Log.i(TAG,"ACTION_CLOSE_SYSTEM_DIALOGS")
                            startFragment()
                        }
                    }
                }
            }
        }


        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                val packageName = intent.data?.schemeSpecificPart

                if (packageName == null) return

                when (action) {
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_CHANGED -> {
                        updateAppGrid()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            addDataScheme("package")
        }

        val filter1 = IntentFilter().apply {
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }

        ContextCompat.registerReceiver(
            requireContext(),
            homeReceiver,
            filter1,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )


        ContextCompat.registerReceiver(
            requireContext(),
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun updateAppGrid() {
        if (!isFragmentActive) return

        val currentPage = viewPager.currentItem
        loadApps()

        // 延迟恢复页面位置，等待新数据加载完成
        view?.postDelayed({
            if (isAdded && viewPager.adapter != null) {
                val pageCount = viewPager.adapter?.count ?: 0
                val newIndex = currentPage.coerceAtMost(pageCount - 1).coerceAtLeast(0)
                viewPager.setCurrentItem(newIndex, false)
            }
        }, 100)
    }

    /**
     * 清理 Fragment 资源
     */
    private fun closeFragment() {
        try {
            // 首先标记为不活跃状态
            isFragmentActive = false
            isLoading = false

            // 取消所有协程任务
            loadJob?.cancel()
            loadJob = null

            // 停止所有后台任务
            if (!executorService.isShutdown) {
                executorService.shutdownNow()
            }

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

            // 清理视图相关资源
            cleanupViews()

            // 清理图标缓存
            iconManager?.clearCache()
            iconManager = null

        } catch (e: Exception) {
            Log.w(TAG, "Error in closeFragment", e)
        }
    }

    private fun cleanupViews() {
        pageChangeListener?.let {
            viewPager.removeOnPageChangeListener(it)
        }
        pageChangeListener = null
        viewPager.adapter = null
        pageIndicator.removeAllViews()
        currentAdapter = null
    }


    override fun onResume() {
        super.onResume()
        Log.i(TAG,"onResume")
        setFragmentActive(true)
        // 如果需要实时更新应用列表，可以在恢复时重新加载
        // loadApps()
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG,"onPause")
        setFragmentActive(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG,"onDestroy")
        closeFragment()
    }

}

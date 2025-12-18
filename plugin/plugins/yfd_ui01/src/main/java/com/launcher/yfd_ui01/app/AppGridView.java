package com.launcher.yfd_ui01.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.content.res.Resources;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.awell.addapp.AppInfo;
import com.awell.utils.Utils;
import com.launcher.yfd_ui01.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppGridView extends RelativeLayout {
    private static final String TAG = "AppGridView";
    static final int ITEMS_PER_PAGE = 18; // 6列 x 3行 = 18个应用每页
    
    private ViewPager viewPager;
    private IconManager iconManager;
    private LinearLayout pageIndicator;
    private BroadcastReceiver packageReceiver;
    
    // 使用线程池替代单个线程
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean isLoading = false;
	private volatile boolean isViewActive = true;
    // 预创建对象复用
    private ViewPager.OnPageChangeListener pageChangeListener;
    private AppPagerAdapter currentAdapter;
    
    public AppGridView(Context context) {
        super(context);
        init(context);
    }

    public AppGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.app_grid_view, this, true);
        initViews(context);
        loadApps(context);
        setupViewPagerListener();
        registerPackageReceiver(context);
    }

    private void initViews(Context context) {
        viewPager = findViewById(R.id.viewPager);
        pageIndicator = findViewById(R.id.pageIndicator);
        iconManager = IconManager.getInstance(context);
    }

    // 在 AppGridView 类中添加这些方法
    public void setViewActive(boolean active) {
        this.isViewActive = active;
        if (active && executorService.isShutdown()) {
            // 如果线程池已关闭但需要重新激活，重新创建线程池
            executorService = Executors.newSingleThreadExecutor();
        }
    }

    public boolean isViewActive() {
        return isViewActive;
    }

    private void loadApps(Context context) {
        if (!isViewActive || isLoading) return;
        isLoading = true;

        executorService.execute(() -> {
		if (!isViewActive) {
                isLoading = false;
                return;
            }
		
            try {
                List<AppInfo> allApps = getAllAppInfo(context);
                if (Thread.currentThread().isInterrupted()) return;
                
                List<List<AppInfo>> pages = splitIntoPages(allApps);
                
                post(() -> {
                    try {
                        currentAdapter = new AppPagerAdapter(context, pages, ITEMS_PER_PAGE);
                        viewPager.setAdapter(currentAdapter);
                        setupPageIndicator();
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting adapter", e);
                    } finally {
                        isLoading = false;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading apps", e);
                post(() -> isLoading = false);
            }
        });
    }

    /**
     * 将应用列表分页
     */
    private List<List<AppInfo>> splitIntoPages(List<AppInfo> allApps) {
        List<List<AppInfo>> pages = new ArrayList<>();
        if (allApps == null || allApps.isEmpty()) return pages;
        
        for (int i = 0; i < allApps.size(); i += ITEMS_PER_PAGE) {
            int end = Math.min(i + ITEMS_PER_PAGE, allApps.size());
            pages.add(new ArrayList<>(allApps.subList(i, end)));
        }
        return pages;
    }

    /**
     * 获取所有应用信息（优化版本）
     */
    private ArrayList<AppInfo> getAllAppInfo(Context ctx) {
        ArrayList<AppInfo> appBeanList = new ArrayList<>();
        PackageManager packageManager = ctx.getPackageManager();
        
        try {
            List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            final int FLAG_SYSTEM = ApplicationInfo.FLAG_SYSTEM;
            
            // 预计算过滤条件
            //boolean hasFilterApps = !Utils.filterAppPackageName.isEmpty();
            boolean hasFilterApps = !IconManager.NEED_TO_BLOCKED_PACKAGE_NAMES.isEmpty();
            boolean hasNeedToShowApps = false;//!IconManager.NEED_TO_SHOW_PACKAGE_NAMES.isEmpty();
            boolean hasPackageOrder = !IconManager.PACKAGE_ORDER_LIST.isEmpty();
            
            // 第一阶段：快速收集基本信息
            List<AppInfo> tempList = new ArrayList<>();
            for (PackageInfo p : packages) {
                ApplicationInfo appInfo = p.applicationInfo;
                if (appInfo == null) continue;
                if(!appInfo.enabled) continue;
                final Intent intent = packageManager.getLaunchIntentForPackage(appInfo.packageName);
                if(intent==null)continue;
                if(appInfo.packageName.contains("launcher"))continue;
                
                String packageName = appInfo.packageName;

                int flags = appInfo.flags;
                //boolean isSystemApp = (flags & FLAG_SYSTEM) != 0;
                //Log.i(TAG,"packageName="+packageName+",isSystemApp="+isSystemApp);
                // 应用过滤逻辑
                if (shouldIncludeApp(packageName, /*isSystemApp,*/ hasFilterApps ,hasNeedToShowApps)) {
                    continue;
                }
                
                AppInfo bean = createAppInfo(packageManager, appInfo, packageName, flags);
                if (bean != null) {
                    tempList.add(bean);
                }
            }
            
            // 第二阶段：批量处理图标（在主线程执行，避免闪烁）
            post(() -> {
                updateAppIcons(tempList);
                appBeanList.addAll(tempList);
                
                // 第三阶段：排序（在UI线程执行）
                sortAppList(appBeanList, hasPackageOrder);
                
                // 完成数据处理后更新UI
                onAppDataReady(appBeanList);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting app info", e);
            // 即使出错也要返回空列表，避免NPE
            return new ArrayList<>();
        }
        
        return appBeanList;
    }
	
	/**
     * 应用数据准备完成后的回调
     */
    private void onAppDataReady(List<AppInfo> appList) {
        if (!isViewActive) return;
        
        List<List<AppInfo>> pages = splitIntoPages(appList);
        
        post(() -> {
            try {
                currentAdapter = new AppPagerAdapter(getContext(), pages, ITEMS_PER_PAGE);
                viewPager.setAdapter(currentAdapter);
                setupPageIndicator();
                
                // 预加载常用图标
                if (iconManager != null) {
                    iconManager.preloadCommonIcons();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting adapter", e);
            } finally {
                isLoading = false;
            }
        });
    }

    /**
     * 判断是否应该包含该应用
     */
    private boolean shouldIncludeApp(String packageName, /*boolean isSystemApp,*/
                                   boolean hasFilterApps, boolean hasNeedToShowApps) {
        // 过滤掉非系统应用中的指定包名
       /* if (!isSystemApp && hasFilterApps && Utils.filterAppPackageName.contains(packageName)) {
            return true;
        }*/
        // 过滤掉(系统/非系统)应用中的指定包名
        return hasFilterApps && IconManager.NEED_TO_BLOCKED_PACKAGE_NAMES.contains(packageName);

        // 只显示非系统应用或需要显示的系统应用
       // return !isSystemApp ||  (hasNeedToShowApps && IconManager.NEED_TO_SHOW_PACKAGE_NAMES.contains(packageName));
    }

    /**
     * 创建应用信息对象
     */
    private AppInfo createAppInfo(PackageManager pm, ApplicationInfo appInfo, 
                                 String packageName, int flags) {
        try {
            AppInfo bean = new AppInfo();
            bean.setIcon(appInfo.loadIcon(pm));
            bean.setLabel(pm.getApplicationLabel(appInfo).toString());
            bean.setPackage_name(packageName);
            bean.setFlags(flags);
            return bean;
        } catch (Exception e) {
            Log.w(TAG, "Error creating app info for: " + packageName, e);
            return null;
        }
    }

    /**
     * 批量更新应用图标
     */
    private void updateAppIcons(List<AppInfo> appList) {
        if (iconManager == null || appList == null) return;
        
        // 预加载图标管理器缓存
        iconManager.preloadCommonIcons();
        
        for (AppInfo app : appList) {
            String packageName = app.getPackage_name();
            if (packageName == null) continue;
            
            Drawable customIcon = iconManager.getIcon(packageName);
            if (customIcon != null) {
                app.setIcon(customIcon);
            }
        }
    }

    /**
     * 排序应用列表
     */
    private void sortAppList(List<AppInfo> appList, boolean hasPackageOrder) {
        if (!hasPackageOrder) {
            // 如果没有预定义顺序，直接按包名排序
            Collections.sort(appList, (app1, app2) -> 
                app1.getPackage_name().compareToIgnoreCase(app2.getPackage_name()));
            return;
        }
        
        // 按预定义顺序分组排序
        List<AppInfo> orderedApps = new ArrayList<>();
        List<AppInfo> remainingApps = new ArrayList<>();
        
        for (AppInfo app : appList) {
            if (IconManager.PACKAGE_ORDER_LIST.contains(app.getPackage_name())) {
                orderedApps.add(app);
            } else {
                remainingApps.add(app);
            }
        }
        
        // 按预定义顺序排序
        Collections.sort(orderedApps, (a1, a2) -> {
            int i1 = IconManager.PACKAGE_ORDER_LIST.indexOf(a1.getPackage_name());
            int i2 = IconManager.PACKAGE_ORDER_LIST.indexOf(a2.getPackage_name());
            return Integer.compare(i1, i2);
        });
        
        // 剩余应用按包名排序
        Collections.sort(remainingApps, (app1, app2) -> 
            app1.getPackage_name().compareToIgnoreCase(app2.getPackage_name()));
        
        // 合并结果
        appList.clear();
        appList.addAll(orderedApps);
        appList.addAll(remainingApps);
    }

    private void setupPageIndicator() {
        if (viewPager == null || pageIndicator == null) return;
        
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) return;

        int pageCount = adapter.getCount();
        
        if (pageCount < 2) {
            pageIndicator.setVisibility(View.GONE);
            return;
        }

        pageIndicator.setVisibility(View.VISIBLE);
        pageIndicator.removeAllViews();

        for (int i = 0; i < pageCount; i++) {
            ImageView dot = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(5, 0, 5, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.indicator_unselected);
            pageIndicator.addView(dot);
        }

        updateIndicator(0);
    }

    private void updateIndicator(int currentPosition) {
        if (pageIndicator == null) return;

        for (int i = 0; i < pageIndicator.getChildCount(); i++) {
            View child = pageIndicator.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView dot = (ImageView) child;
                int resId = (i == currentPosition) ? R.drawable.indicator_selected : R.drawable.indicator_unselected;
                dot.setImageResource(resId);
            }
        }
    }

    private void setupViewPagerListener() {
        pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
            }
        };
        viewPager.addOnPageChangeListener(pageChangeListener);
    }

    private void registerPackageReceiver(Context context) {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getData() == null) return;
                
                String action = intent.getAction();
                if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                    Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                    Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                    
                    updateAppGrid(context);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");

        context.registerReceiver(packageReceiver, filter);
    }

    private void updateAppGrid(Context context) {
		if (!isViewActive) return;
        int currentPage = viewPager != null ? viewPager.getCurrentItem() : 0;
        loadApps(context);
        
        // 延迟恢复页面位置，等待新数据加载完成
        postDelayed(() -> {
            if (viewPager != null && viewPager.getAdapter() != null) {
                int pageCount = viewPager.getAdapter().getCount();
                int newIndex = Math.min(currentPage, Math.max(0, pageCount - 1));
                viewPager.setCurrentItem(newIndex, false);
            }
        }, 100);
    }

    public void closeView(Context context) {
        try {
            // 首先标记为不活跃状态
            isViewActive = false;
            isLoading = false;
            
            // 停止所有后台任务
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            
            // 取消广播注册
            if (packageReceiver != null) {
                try {
                    context.unregisterReceiver(packageReceiver);
                } catch (IllegalArgumentException e) {
                    // 忽略已经反注册的情况
                }
                packageReceiver = null;
            }
            
            // 清理视图相关资源
            cleanupViews();
            
            // 清理图标缓存
            if (iconManager != null) {
                iconManager.clearCache();
                iconManager = null;
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error in closeView", e);
        }
    }

    // 添加视图清理方法
    private void cleanupViews() {
        if (viewPager != null) {
            if (pageChangeListener != null) {
                viewPager.removeOnPageChangeListener(pageChangeListener);
                pageChangeListener = null;
            }
            viewPager.setAdapter(null);
        }
        
        if (pageIndicator != null) {
            pageIndicator.removeAllViews();
        }
        
        // 从父视图移除
        if (getParent() != null) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }
}
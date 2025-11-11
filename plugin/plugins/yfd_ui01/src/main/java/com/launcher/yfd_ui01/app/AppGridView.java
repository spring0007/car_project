package com.launcher.yfd_ui01.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.content.res.Resources;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.awell.addapp.AppInfo;
import com.awell.utils.Utils;
import com.launcher.yfd_ui01.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppGridView extends RelativeLayout {

    private ViewPager viewPager;
    private IconManager iconManager;
    private LinearLayout pageIndicator;
    private BroadcastReceiver packageReceiver;
    // Background loader thread reference so we can interrupt it when closing
    private Thread loadThread;
    // Keep a reference to the page change listener so we can remove it on close
    private ViewPager.OnPageChangeListener pageChangeListener;
    static final int ITEMS_PER_PAGE = 18; // 6列 x 3行 = 18个应用每页
    private static final String TAG = "AppGridView";
    private static Context mContext;
    private volatile boolean isLoading = false;

    public AppGridView(Context context) {
        super(context);
        mContext = context;
        init(context);
    }

    public AppGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    public AppGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init(context);
    }
    private void init(Context context) {
    // View 中的布局加载 — 使用 view 的上下文以确保插件/宿主资源能被正确解析
    LayoutInflater.from(context).inflate(R.layout.app_grid_view, this, true);
        
        initViews(context);
        loadApps(context);
        setupViewPagerListener();
        // 注册应用安装/卸载监听器
        registerPackageReceiver(context);
    }

    private void initViews(Context context) {
        viewPager = findViewById(R.id.viewPager);
        pageIndicator = findViewById(R.id.pageIndicator);
        iconManager = IconManager.getInstance(context);
    }

    private void loadApps(Context context) {
        // Load apps asynchronously to avoid blocking the UI on first open.
        if (iconManager == null) iconManager = IconManager.getInstance(context);
        // Prevent multiple concurrent loads
        if (isLoading) return;
        isLoading = true;

        Thread t = new Thread(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) return;
                List<AppInfo> allApps = getAllAppInfo(context, false);
                if (Thread.currentThread().isInterrupted()) return;
                // 将应用列表分页
                List<List<AppInfo>> pages = new ArrayList<>();
                for (int i = 0; i < allApps.size(); i += ITEMS_PER_PAGE) {
                    int end = Math.min(i + ITEMS_PER_PAGE, allApps.size());
                    pages.add(new ArrayList<>(allApps.subList(i, end)));
                }
                Log.i(TAG, " lqq,pages:SUCCESS " );
                // Preload icons for the first couple of pages to warm Glide cache and
                // reduce the perceived delay when the user opens the app grid.
//                try {
//                    int preloadCount = Math.min(allApps.size(), ITEMS_PER_PAGE * 2);
//                    java.util.List<Object> preloadModels = new java.util.ArrayList<>();
//                    for (int i = 0; i < preloadCount; i++) {
//                        AppInfo a = allApps.get(i);
//                        if (a != null) {
//                            if (a.getIcon() != null) {
//                                preloadModels.add(a.getIcon());
//                            } else if (a.getPackage_name() != null) {
//                                // try to get custom icon from IconManager
//                                try {
//                                    if (iconManager != null) {
//                                        Drawable d = iconManager.getIcon(a.getPackage_name());
//                                        if (d != null) preloadModels.add(d);
//                                    }
//                                } catch (Exception ignored) {}
//                            }
//                        }
//                    }
//                    if (!preloadModels.isEmpty()) {
//                        com.launcher.yfd_ui01.app.IconLoader.preloadIcons(context, preloadModels);
//                    }
//                } catch (Exception ignored) {
//                }

                // Post adapter setup back to UI thread
                post(() -> {
                    try {
                        AppPagerAdapter pagerAdapter = new AppPagerAdapter(context, pages, ITEMS_PER_PAGE);
                        Log.i(TAG, " lqq,pagerAdapter:init " );
                        viewPager.setAdapter(pagerAdapter);
                        setupPageIndicator(context);
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting adapter on UI thread", e);
                    } finally {
                        isLoading = false;
                        // clear reference to background thread when done
                        loadThread = null;
                    }
                    Log.i(TAG, " lqq,setAdapter:SUCCESS " );
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading apps in background", e);
                post(() -> isLoading = false);
                loadThread = null;
            }
        }, "AppGridView-LoadApps");
        loadThread = t;
        t.start();
    }


//    public static List<String> UiToShowPackageName = Arrays.asList("com.android.chrome", "com.google.android.youtube",
//            "com.google.android.apps.maps", "com.android.vending","org.chromium.chrome");

    private ArrayList<AppInfo> getAllAppInfo(Context ctx, boolean isFilterSystem) {
        ArrayList<AppInfo> appBeanList = new ArrayList<>();
        AppInfo bean = null;
        ApplicationInfo appInfo = null;
        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);

        final int FLAG_SYSTEM = android.content.pm.ApplicationInfo.FLAG_SYSTEM;
        for (PackageInfo p : list) {
            appInfo = p.applicationInfo;
            String pckaName = appInfo.packageName;
            int flags = appInfo.flags;

            // 检查是否需要过滤掉该应用
            boolean isSystemApp = (flags & FLAG_SYSTEM) != 0;
            if (!isSystemApp && Utils.filterAppPackageName.contains(pckaName)) {
                continue;
            }
            // 检查是否需要显示该应用
            if (!isSystemApp || IconManager.needToShowPackageName.contains(pckaName)) {
                bean = new AppInfo();
                bean.setIcon(appInfo.loadIcon(packageManager));
                bean.setLabel(packageManager.getApplicationLabel(appInfo).toString());
                bean.setPackage_name(pckaName);
                bean.setFlags(flags);
                appBeanList.add(bean);
            }
        }

        Log.i(TAG, " bean:2 ");
        for (AppInfo a : appBeanList) {
            String str = a.getPackage_name();
            Drawable icon = null;
            if (iconManager != null) {
                icon = iconManager.getIcon(str);
            }
            if (icon != null) {
                a.setIcon(icon);
            }
//            for (int i = 0; i < Utils.mHomePackName.length; i++) {
//                if (Utils.mHomePackName[i].equals(str)) {
//                    a.setIcon(getResources().getDrawable(Utils.mHomeIcon[i]));
//                    break;
//                }
//            }
        }

        // 按预定义包名顺序将应用分组，先把 packageOrderList 中的应用按该顺序放在前面，剩余应用按包名排序
        List<AppInfo> orderedApps = new ArrayList<>();
        List<AppInfo> remainingApps = new ArrayList<>();

        for (AppInfo app : appBeanList) {
            if (IconManager.packageOrderList.contains(app.getPackage_name())) {
                orderedApps.add(app);
            } else {
                remainingApps.add(app);
            }
        }
        // 按 packageOrderList 的顺序排序 orderedApps
        Collections.sort(orderedApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a1, AppInfo a2) {
                int i1 = IconManager.packageOrderList.indexOf(a1.getPackage_name());
                int i2 = IconManager.packageOrderList.indexOf(a2.getPackage_name());
                return Integer.compare(i1, i2);
            }
        });

        // 对剩余应用按包名排序
        Collections.sort(remainingApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                return app1.getPackage_name().compareToIgnoreCase(app2.getPackage_name());
            }
        });

        // 合并结果
        appBeanList.clear();
        appBeanList.addAll(orderedApps);
        appBeanList.addAll(remainingApps);
        Log.i(TAG, " lqq,appBeanList:SUCCESS " );
        return appBeanList;
    }

    private void setupPageIndicator(Context context) {
        try {
            // 获取ViewPager的页数
            PagerAdapter adapter = viewPager.getAdapter();
            if (adapter == null) return;

            int pageCount = adapter.getCount();

            // 如果页数小于2，隐藏页面指示器
            if (pageCount < 2) {
                if (pageIndicator != null)
                    pageIndicator.setVisibility(View.GONE);
                return;
            }

            // 页数大于等于2时，显示并设置指示器
            if (pageIndicator != null) {
                pageIndicator.setVisibility(View.VISIBLE);
                pageIndicator.removeAllViews();

                for (int i = 0; i < pageCount; i++) {
                    ImageView dot = new ImageView(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(5, 0, 5, 0); // 设置点之间的间距

                    dot.setLayoutParams(params);
                    // 默认设置为未选中状态，使用 ContextCompat 获取并保护性捕获异常
                    try {
                        dot.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.indicator_unselected));
                    } catch (Resources.NotFoundException rnfe) {
                        Log.w(TAG, "indicator_unselected resource not found", rnfe);
                        // 作为回退，尝试设置一个系统占位图
                        try {
                            dot.setImageResource(android.R.drawable.btn_radio);
                        } catch (Resources.NotFoundException ignored) {
                        }
                    }

                    pageIndicator.addView(dot);
                }
            }

            // 设置初始选中第一个点
            updateIndicator(0);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up page indicator", e);
        }
    }

    private void updateIndicator(int currentPosition) {
        try {
            if (pageIndicator == null) return;

            for (int i = 0; i < pageIndicator.getChildCount(); i++) {
                View child = pageIndicator.getChildAt(i);
                if (child instanceof ImageView) {
                    ImageView dot = (ImageView) child;
                    try {
                        if (i == currentPosition) {
                            dot.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indicator_selected));
                        } else {
                            dot.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indicator_unselected));
                        }
                    } catch (Resources.NotFoundException rnfe) {
                        Log.w(TAG, "indicator resource not found", rnfe);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating indicator", e);
        }
    }

    // 在 ViewPager 的页面监听器中更新指示点
    private void setupViewPagerListener() {
        try {
            pageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    // 页面滑动时的处理
                }

                @Override
                public void onPageSelected(int position) {
                    try {
                        // 页面选中时更新指示点
                        updateIndicator(position);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating indicator", e);
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    // 页面滚动状态改变时的处理
                }
            };
            viewPager.addOnPageChangeListener(pageChangeListener);
        } catch (Exception e) {
                Log.e(TAG, "Error setting up ViewPager listener", e);
        }

    }

    private void registerPackageReceiver(Context context) {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                        Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                        Intent.ACTION_PACKAGE_CHANGED.equals(action)) {

                    // 包名，做保护性检查以避免 NPE
                    if (intent.getData() != null) {
                        String packageName = intent.getData().getSchemeSpecificPart();
                        Log.i(TAG, "packageName: " + packageName);
                    } else {
                        Log.w(TAG, "Received package broadcast with null data: " + action);
                    }

                    // 更新界面
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
        // 保存当前页索引，重新加载数据后尽量恢复
        int currentPage = 0;
        if (viewPager != null) {
            try {
                currentPage = viewPager.getCurrentItem();
            } catch (Exception e) {
                // ignore and default to 0
            }
        }

        // 重新加载应用数据并刷新视图
        loadApps(context);

        // 恢复到之前的页（如果新页数较少，使用最后一页）
        if (viewPager != null && viewPager.getAdapter() != null) {
            int pageCount = viewPager.getAdapter().getCount();
            int newIndex = Math.min(currentPage, Math.max(0, pageCount - 1));
            viewPager.setCurrentItem(newIndex, false);
        }

        // 更新ViewPager适配器
//        if (viewPager.getAdapter() != null) {
//            viewPager.getAdapter().notifyDataSetChanged();
//        }
//        // 重新设置页面指示器
//        setupPageIndicator();
    }

    public  void closeView(Context context) {
        try {
            // Unregister package receiver if registered
            if (packageReceiver != null) {
                try {
                    context.unregisterReceiver(packageReceiver);
                } catch (IllegalArgumentException e) {
                    // 已经反注册或未注册，打印警告但不抛出
                    Log.w(TAG, "unregisterReceiver failed", e);
                }
                packageReceiver = null;
            }

            // Stop any ongoing loading and clear adapter to free references
            isLoading = false;

            if (viewPager != null) {
                try {
                    viewPager.setAdapter(null);
                    // remove child views of viewPager to ensure no view leakage
                    viewPager.removeAllViews();
                } catch (Exception e) {
                    Log.w(TAG, "clear viewPager failed", e);
                }
            }

            // Clear page indicators
            if (pageIndicator != null) {
                try {
                    pageIndicator.removeAllViews();
                } catch (Exception e) {
                    Log.w(TAG, "clear pageIndicator failed", e);
                }
            }

            // Attempt to remove self from parent to ensure it is detached
            try {
                if (this.getParent() != null && this.getParent() instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) this.getParent()).removeView(this);
                }
            } catch (Exception e) {
                Log.w(TAG, "remove self from parent failed", e);
            }

            // Drop references
            iconManager = null;

            // Interrupt any background loader thread
            if (loadThread != null) {
                try {
                    loadThread.interrupt();
                } catch (Exception e) {
                    Log.w(TAG, "interrupt loadThread failed", e);
                }
                loadThread = null;
            }

            // Remove page change listener to avoid callback leaks
            if (viewPager != null && pageChangeListener != null) {
                try {
                    viewPager.removeOnPageChangeListener(pageChangeListener);
                } catch (Exception e) {
                    Log.w(TAG, "removeOnPageChangeListener failed", e);
                }
                pageChangeListener = null;
            }
            if(iconManager!=null)
                iconManager.clearCache();

        } catch (Exception e) {
            Log.w(TAG, "closeView encountered exception", e);
        }
    }

}
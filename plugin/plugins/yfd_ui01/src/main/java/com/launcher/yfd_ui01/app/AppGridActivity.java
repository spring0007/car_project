package com.launcher.yfd_ui01.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.awell.addapp.AppInfo;
import com.awell.utils.Utils;
import com.launcher.yfd_ui01.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppGridActivity extends Activity {

    private ViewPager viewPager;
    private List<AppInfo> allApps;
    private IconManager iconManager;
    private LinearLayout pageIndicator;
    private BroadcastReceiver packageReceiver;
    private static final int ITEMS_PER_PAGE = 18; // 6列 x 3行 = 18个应用每页

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_grid);
        
        // 设置默认壁纸为背景
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        
        initViews();
        loadApps();
        setupViewPager();
        setupViewPagerListener();
        // 注册应用安装/卸载监听器
        registerPackageReceiver();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        pageIndicator = findViewById(R.id.pageIndicator);
    }

    private void loadApps() {
        if(iconManager==null) iconManager = new IconManager();
        allApps = getAllAppInfo(this, false);
    }


//    public static List<String> UiToShowPackageName = Arrays.asList("com.android.chrome", "com.google.android.youtube",
//            "com.google.android.apps.maps", "com.android.vending","org.chromium.chrome");

    private ArrayList<AppInfo> getAllAppInfo(Context ctx, boolean isFilterSystem) {
        ArrayList<AppInfo> appBeanList = new ArrayList<>();
        com.awell.addapp.AppInfo bean = null;
        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(0);
        for (PackageInfo p : list) {
            bean = new AppInfo();
            //int randome = new Random().nextInt(5);
            bean.setIcon(p.applicationInfo.loadIcon(packageManager));
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString());
            String pckaName = p.applicationInfo.packageName;
            bean.setPackage_name(pckaName);
            int flags = p.applicationInfo.flags;
            bean.setFlags(flags);
            //Log.i("AppGridActivity", "bean: " + bean.getPackage_name() +",flags="+(flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM));
            if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 && Utils.needToShowPackageName.contains(pckaName)) {
                appBeanList.add(bean);
            } else if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && !Utils.filterAppPackageName.contains(pckaName)) {
                appBeanList.add(bean);
            } else if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 && Utils.otherNeedToShowPackageName.contains(pckaName)) {
                appBeanList.add(bean);
            }
        }

        for (AppInfo a : appBeanList) {
            String str = a.getPackage_name();
            int icon = iconManager.getIcon(str);
            if(icon != 0) a.setIcon(getResources().getDrawable(icon));
//            for (int i = 0; i < Utils.mHomePackName.length; i++) {
//                if (Utils.mHomePackName[i].equals(str)) {
//                    a.setIcon(getResources().getDrawable(Utils.mHomeIcon[i]));
//                    break;
//                }
//            }
        }
        //方法1. 假设是 AppBean 对象，按包名排序
        Collections.sort(appBeanList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                return app1.getPackage_name().compareToIgnoreCase(app2.getPackage_name());
            }
        });

        return appBeanList;
    }

    private void setupViewPager() {
        // 将应用列表分页
        List<List<AppInfo>> pages = new ArrayList<>();
        for (int i = 0; i < allApps.size(); i += ITEMS_PER_PAGE) {
            int end = Math.min(i + ITEMS_PER_PAGE, allApps.size());
            pages.add(new ArrayList<>(allApps.subList(i, end)));
        }

        AppPagerAdapter pagerAdapter = new AppPagerAdapter(this, pages, ITEMS_PER_PAGE);
        viewPager.setAdapter(pagerAdapter);
        setupPageIndicator();
       // viewPager.setOrientation(ViewPager.ORIENTATION_HORIZONTAL);
    }

    private void setupPageIndicator() {
        // 获取ViewPager的页数
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) return;

        int pageCount = adapter.getCount();

        // 如果页数小于2，隐藏页面指示器
        if (pageCount < 2) {
            pageIndicator.setVisibility(View.GONE);
            return;
        }

        // 页数大于等于2时，显示并设置指示器
        pageIndicator.setVisibility(View.VISIBLE);
        pageIndicator.removeAllViews();

        for (int i = 0; i < pageCount; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(5, 0, 5, 0); // 设置点之间的间距

            dot.setLayoutParams(params);

            // 默认设置为未选中状态
            dot.setImageResource(R.drawable.indicator_unselected);

            pageIndicator.addView(dot);
        }

        // 设置初始选中第一个点
        updateIndicator(0);
    }

    private void updateIndicator(int currentPosition) {
        for (int i = 0; i < pageIndicator.getChildCount(); i++) {
            ImageView dot = (ImageView) pageIndicator.getChildAt(i);
            if (i == currentPosition) {
                dot.setImageResource(R.drawable.indicator_selected);
            } else {
                dot.setImageResource(R.drawable.indicator_unselected);
            }
        }
    }

    // 在 ViewPager 的页面监听器中更新指示点
    private void setupViewPagerListener() {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // 页面滑动时的处理
            }

            @Override
            public void onPageSelected(int position) {
                // 页面选中时更新指示点
                updateIndicator(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // 页面滚动状态改变时的处理
            }
        });
    }

    private void registerPackageReceiver() {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                        Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                        Intent.ACTION_PACKAGE_CHANGED.equals(action)) {

                    // 包名
                    String packageName = intent.getData().getSchemeSpecificPart();
                    Log.i("AppGridActivity", "packageName: " + packageName);
                    // 更新界面
                    updateAppGrid();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");

        registerReceiver(packageReceiver, filter);
    }

    private void updateAppGrid() {
        // 重新加载应用数据
        loadApps();
        setupViewPager();

        // 更新ViewPager适配器
//        if (viewPager.getAdapter() != null) {
//            viewPager.getAdapter().notifyDataSetChanged();
//        }
//        // 重新设置页面指示器
//        setupPageIndicator();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (packageReceiver != null) {
            unregisterReceiver(packageReceiver);
        }
    }

        /*
    * // 定义包名顺序列表
List<String> packageOrderList = Arrays.asList(
    "com.awell.localvideo",
    "com.awell.localmusic",
    "com.awell.radio",
    "com.awell.bluetooth"
    // ... 其他预定义包名
);

// 分离在列表中和不在列表中的应用
List<AppBean> orderedApps = new ArrayList<>();
List<AppBean> remainingApps = new ArrayList<>();

for (AppBean app : appBeanList) {
    if (packageOrderList.contains(app.getPackageName())) {
        orderedApps.add(app);
    } else {
        remainingApps.add(app);
    }
}

// 对预定义列表中的应用按指定顺序排序
orderedApps.sort((app1, app2) -> {
    int index1 = packageOrderList.indexOf(app1.getPackageName());
    int index2 = packageOrderList.indexOf(app2.getPackageName());
    return Integer.compare(index1, index2);
});

// 对剩余应用按包名排序
remainingApps.sort((app1, app2) ->
    app1.getPackageName().compareTo(app2.getPackageName()));

// 合并结果
appBeanList.clear();
appBeanList.addAll(orderedApps);
appBeanList.addAll(remainingApps);

    *
    * */


}

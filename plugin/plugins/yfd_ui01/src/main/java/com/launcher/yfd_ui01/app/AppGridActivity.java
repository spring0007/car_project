package com.launcher.yfd_ui01.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.viewpager.widget.ViewPager;

import com.awell.addapp.AppInfo;
import com.awell.utils.Utils;
import com.launcher.yfd_ui01.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AppGridActivity extends Activity {

    private ViewPager viewPager;
    private List<AppInfo> allApps;
    private IconManager iconManager;
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
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
    }

    private void loadApps() {
        iconManager = new IconManager();
        allApps = getAllAppInfo(this, false);
    }


    private ArrayList<AppInfo> getAllAppInfo(Context ctx, boolean isFilterSystem) {
        ArrayList<AppInfo> appBeanList = new ArrayList<>();
        com.awell.addapp.AppInfo bean = null;
        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(0);
        for (PackageInfo p : list) {
            bean = new AppInfo();
            int randome = new Random().nextInt(5);
            bean.setIcon(p.applicationInfo.loadIcon(packageManager));
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString());
            String pckaName = p.applicationInfo.packageName;
            bean.setPackage_name(pckaName);
            int flags = p.applicationInfo.flags;
            bean.setFlags(flags);
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
       // viewPager.setOrientation(ViewPager.ORIENTATION_HORIZONTAL);
    }
}

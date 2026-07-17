package com.launcher.ui3;

import static com.awell.utils.Utils.startWallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.addapp.AddSelectAppCallback;
import com.awell.addapp.AppInfo;
import com.awell.addapp.AppListStorage;

import com.awell.addapp.ShowPopupI;
import com.awell.control.AppsCustomizeConfig;
import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;
import com.awell.utils.Utils;
import com.launcher.ui3.databinding.ActivityMainUi3Binding;
import com.launcher.ui3.databinding.MusicWidgetBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivityUI3 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI3.class.getSimpleName();
    private ActivityMainUi3Binding binding;
    //private MusicWidgetBinding musicWidgetBinding;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private View contentView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUi3Binding.inflate(getLayoutInflater());


        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        //musicWidgetBinding = binding.layoutMusicWidget;
        musicWidget =  binding.layoutMusicWidget.musicWidgetLayout;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);

        setContentView(binding.getRoot());

        initLongTouch();

        clickApp();

        initAddAppView();
		registerPackageReceiver();
        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(new AppsCustomizeConfig.Builder().setThemeMode(1).build());
    }

    private void initLongTouch() {
        contentView = findViewById(android.R.id.content);
        handler = new Handler(Looper.getMainLooper());
        viewConfiguration = ViewConfiguration.get(this);
    }

    private AppInofAdapter appInfoAdapter;
    private List<AppInfo> allAppInfoList, showAppInfoList;
    private AppInfo placehodlerInfo;
    //private MyDbHelper myDbHelper;
    //private SQLiteDatabase sqLiteDatabase;

    private void initAddAppView() {
        showAppInfoList = new ArrayList<>();
        allAppInfoList = new ArrayList<>();

        placehodlerInfo = createMismatchPlaceholder();

        // 获取已保存需要显示的app包名，如果没有，则显示默认
        //myDbHelper = new MyDbHelper(this, "show_app", null, 1);
        //sqLiteDatabase = myDbHelper.getWritableDatabase();

        appInfoAdapter = new AppInofAdapter(this, showAppInfoList, showPopupI, addSelectAppCallback);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rv.setLayoutManager(linearLayoutManager);
        binding.rv.setAdapter(appInfoAdapter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                getShowHotApp();
            }
        }).start();

    }

    private void getShowHotApp() {
        // 刷新应用列表
        refreshAppListAsync();

        runOnUiThread(() -> {
                appInfoAdapter.setContentList(showAppInfoList);
                //appInfoAdapter.notifyDataSetChanged();
        });
    }

    private ShowPopupI showPopupI = new ShowPopupI() {
        @Override
        public void showPopup() {
            showPopupAllapp();
            //backgroundAlpha(0.2f);
        }

        @Override
        public void hidePopup() {
            if (popupWindow != null) popupWindow.dismiss();
        }
    };

    private AddSelectAppCallback addSelectAppCallback = new AddSelectAppCallback() {
        @Override
        public void addAppInfo(AppInfo appInfo) {
            for (AppInfo pack : showAppInfoList)
                if (pack.package_name.equals(appInfo.package_name)) {
                    if (popupWindow != null) popupWindow.dismiss();

                    showPopupI.hidePopup();
                    return;
                }
            showAppInfoList.remove(placehodlerInfo);

            showAppInfoList.add(appInfo);
            showAppInfoList.add(placehodlerInfo);
            appInfoAdapter.setContentList(showAppInfoList);
            //appInfoAdapter.notifyDataSetChanged();

            // 替代 sqLiteDatabase.insert() — 无需新线程，SP.apply() 本身就是异步
            AppListStorage.addItem(MainActivityUI3.this, appInfo.package_name);

            showPopupI.hidePopup();
        }

        @Override
        public void removeAppInfo(String packageName) {
            // 替代 sqLiteDatabase.delete()
            AppListStorage.removeItem(MainActivityUI3.this, packageName);

            for (AppInfo pcka : showAppInfoList)
                if (pcka.package_name.equals(packageName)) {
                    showAppInfoList.remove(pcka);
                    break;
                }

            if (appInfoAdapter != null) {
                appInfoAdapter.setContentList(showAppInfoList);
            }
        }
    };

    private PopupWindow popupWindow;

    /**
     * popupwindow列表显示所有已安装应用
     */
    private void showPopupAllapp() {

        View view = LayoutInflater.from(this).inflate(R.layout.layout_allapp, null);
        popupWindow = new PopupWindow(view, RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT , true);

        popupWindow.setOutsideTouchable(true);
        //popupWindow.setFocusable(true);

        allAppInfoList = getAllAppInfo(this, false);

        RecyclerView rvPop = view.findViewById(R.id.rv_pop_allapp);
        AppPopAdapter appInofAdapter = new AppPopAdapter(this, allAppInfoList, addSelectAppCallback);
        int verticalSpacing = (int) (20 * getResources().getDisplayMetrics().density);
        rvPop.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.top = verticalSpacing;
                outRect.bottom = verticalSpacing;
            }
        });
        rvPop.setLayoutManager(new GridLayoutManager(this, 2,RecyclerView.HORIZONTAL,false));
        rvPop.setAdapter(appInofAdapter);



        view.setOnClickListener(v -> popupWindow.dismiss());

        popupWindow.showAtLocation(contentView, Gravity.CENTER, 0, 10);

    }

    public List<PackageInfo> getAppList(PackageManager packageManager, List<ResolveInfo> resolveInfos) {
        List<PackageInfo> appList = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;
            if (processed.contains(packageName)) {
                continue;
            }

            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                appList.add(packageInfo);
                processed.add(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // 忽略异常
            }
        }
        return appList;
    }

    /**
     * 获取手机已安装应用列表
     *
     * @param context
     * @param isFilterSystem 是否过滤系统应用
     * @return
     */
    private ArrayList<AppInfo> getAllAppInfo(Context context, boolean isFilterSystem) {
        ArrayList<AppInfo> appBeanList = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);

        // 获取所有输入法包名
        Set<String> imePackages = new HashSet<>();
        Intent imeIntent = new Intent("android.view.InputMethod");
        List<ResolveInfo> imeServices = packageManager.queryIntentServices(imeIntent, 0);
        for (ResolveInfo ri : imeServices) {
            imePackages.add(ri.serviceInfo.packageName);
        }

        // 获取所有launcher包名（CATEGORY_HOME）
        Set<String> launcherPackages = new HashSet<>();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> homeActivities = packageManager.queryIntentActivities(homeIntent, 0);
        for (ResolveInfo ri : homeActivities) {
            launcherPackages.add(ri.activityInfo.packageName);
        }

        List<String> noNeedToShow = Utils.notDisplayedPackageName;

        // 首先从resolveInfos提取包名并获取PackageInfo，然后去重，再过滤
        List<PackageInfo> appList = new ArrayList<>();
        Set<String> processedPackages = new HashSet<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            String pkgName = resolveInfo.activityInfo.packageName;
            if (processedPackages.contains(pkgName)) continue;
            processedPackages.add(pkgName);
            try {
                PackageInfo pkgInfo = packageManager.getPackageInfo(pkgName, 0);
                // 过滤条件
                if (!imePackages.contains(pkgName) && !launcherPackages.contains(pkgName) && !noNeedToShow.contains(pkgName)) {
                    appList.add(pkgInfo);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // ignore
            }
        }

        // 也可以像Kotlin那样先收集再过滤，但这里直接过滤。
        for (PackageInfo p : appList) {
            AppInfo bean = new AppInfo();
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString());
            bean.setPackage_name(p.applicationInfo.packageName);
            bean.setFlags(p.applicationInfo.flags);
            appBeanList.add(bean);
        }

        LogUtil.w("appBeanList size: " + appBeanList.size());
        return appBeanList;
    }

    @Override
    public void onBackPressed() {
        AppsCustomizeControl.INSTANCE.hideApps();
        //super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppsCustomizeControl.INSTANCE.hideApps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
		unregisterPackageReceiver();
        AppsCustomizeControl.INSTANCE.setActivity(null);
        AppsCustomizeControl.INSTANCE.hideApps();
        try {
            mediaControl.unBindDataService(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clickApp() {
        binding.hotsetAllapp.setOnClickListener(this);
        binding.radioLayout.radioIv.setOnClickListener(this);
        binding.time.setOnClickListener(this);
        binding.page2DateM.setOnClickListener(this);
        binding.radioLayout.ivRadioPre.setOnClickListener(v -> {
            if (ClickUtils.isFastClick()) {
                return;
            }
            Log.d(TAG, "quickclickApp");
            mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS);
        });
        binding.radioLayout.ivRadioNext.setOnClickListener(v -> {
            if (ClickUtils.isFastClick()) {
                return;
            }
            mediaControl.sendStrToHost(AwellTool.RADIO.NEXT);
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.hotsetAllapp.getId()) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        } else if (v.getId() == binding.radioLayout.radioIv.getId()) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
        }else if(v.getId() == binding.time.getId()|| v.getId() == binding.page2DateM.getId()){
            Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    /**
     * 跳转Activity
     *
     * @param packName
     * @param className
     */
    private void startActivity(String packName, String className) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packName);
        boolean isboot = true;
        if (intent != null) {
            for (int index = 0; index < IconCache.WorkSpacePackageName.length; index++) {
                Log.d(TAG, "packagename11=" + packName);
                if (!packName.equals(IconCache.WorkSpacePackageName[index])) {
                    isboot = false;
                    break;
                }
            }
            if (packName.contains("com.autonavi")) {
                if (isboot)
                    Settings.System.putString(getContentResolver(), "boot_apk1", packName);
            } else {
                if (isboot)
                    Settings.System.putString(getContentResolver(), "boot_apk2", packName);
            }
            startActivity(intent);
        }

    }

    private final AwellMediaControl.UpdateMediaDataToView mediaImpl = new AwellMediaControl.UpdateMediaDataToView() {
        @Override
        public void updateViewMusicPlay(@NotNull Bundle bundle, @NotNull String pkg, @NotNull String command, int mediaType, int currentMedia) {
            if (!"com.awell.radio".equals(pkg)) {
                musicWidget.switchMediaController(pkg, command, mediaType, currentMedia);
            }
        }

        @Override
        public void updateViewPlayStatus(@NotNull Bundle bundle, boolean status, int type) {
            musicWidget.setCurMusicState(status, type);

        }

        @Override
        public void updateViewMusicPlayImage(@NotNull Bundle bundle) {

        }

        @Override
        public void updateViewPlayInfo(@NotNull Bundle bundle, @NotNull String songName, @NotNull String singerName, @NotNull String album, int type) {
            musicWidget.setMusicNameTextView(songName, type);
            musicWidget.setArtistNameTextView(singerName, type);
            if ("NO_MUSIC_LIST".equals(songName)
                    && "NO_MUSIC_LIST".equals(singerName)
                    && "NO_MUSIC_LIST".equals(album)) {
                musicWidget.setMusicNameTextView(getResources().getString(R.string.click_play_music), MusicWidget.MUSIC);
                musicWidget.setArtistNameTextView(getResources().getString(R.string.music_artist), MusicWidget.MUSIC);
            }

            if (MusicWidget.OTHER_MUSIC == type) {
                if (!TextUtils.isEmpty(songName)) {
                    musicWidget.setMusicNameTextView(songName, MusicWidget.OTHER_MUSIC);
                } else {
                    musicWidget.setMusicNameTextView(getResources().getString(R.string.click_play_music), MusicWidget.OTHER_MUSIC);
                }
                if (!TextUtils.isEmpty(singerName)) {
                    musicWidget.setArtistNameTextView(singerName, MusicWidget.OTHER_MUSIC);
                } else {
                    musicWidget.setArtistNameTextView(getResources().getString(R.string.music_artist), MusicWidget.OTHER_MUSIC);
                }
            }
        }

        @Override
        public void updateViewPlayTime(@NotNull Bundle bundle, long currentTime, long totalTime, int type) {
            musicWidget.setMusicSeekBar((int) currentTime, (int) totalTime, type);
        }

        @Override
        public void updateViewRadioFreq(@NotNull Bundle bundle, @NotNull String fmOrAm, @NotNull String freq, @NotNull String unit) {
            runOnUiThread(() -> {
                binding.radioLayout.tvRadioFreq.setText(freq);
                binding.radioLayout.tvRadioAmFm.setText(fmOrAm);
            });
        }

        @Override
        public void handleOriginBundle(@NotNull Bundle bundle) {

        }
    };


    private boolean isEventConsumedByChild = false;
    private boolean isLongPressPossible = false;
    private float startX = 0f;
    private float startY = 0f;
    private ViewConfiguration viewConfiguration;
    private Handler handler;

    /**
     * 重写只为长按弹出壁纸选择
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 先让子 View 处理事件
        boolean consumed = super.dispatchTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 重置状态
                isEventConsumedByChild = false;
                isLongPressPossible = true;

                // 记录触摸起始位置
                startX = ev.getRawX();
                startY = ev.getRawY();

                // 启动长按检测
                startLongPressDetection();
                break;

            case MotionEvent.ACTION_MOVE:
                // 检查是否移动超过阈值
                float dx = Math.abs(ev.getRawX() - startX);
                float dy = Math.abs(ev.getRawY() - startY);
                float touchSlop = 0f;
                if (viewConfiguration != null) {
                    touchSlop = viewConfiguration.getScaledTouchSlop();
                }

                if (dx > touchSlop || dy > touchSlop) {
                    cancelLongPressDetection();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelLongPressDetection();
                break;
        }

        // 记录事件是否被子 View 消费
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            isEventConsumedByChild = consumed;
        }

        return consumed;
    }

    private Runnable longPressRunnable = null;

    private void startLongPressDetection() {
        cancelLongPressDetection();

        longPressRunnable = () -> {
            // 只有事件没有被消费且长按可能时才触发
            if (!isEventConsumedByChild && isLongPressPossible) {
                handleLongPressAction();
            }
        };

        if (handler != null) {
            handler.postDelayed(
                    longPressRunnable,
                    ViewConfiguration.getLongPressTimeout()
            );
        }
    }

    private void cancelLongPressDetection() {
        if (longPressRunnable != null) {
            if (handler != null) {
                handler.removeCallbacks(longPressRunnable);
            }
            longPressRunnable = null;
        }
    }

    private void backgroundAlpha(float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha; //0.0-1.0
        getWindow().setAttributes(lp);
    }

    private void handleLongPressAction() {
        startWallpaper();
    }

    // Broadcast receiver to listen for package removal events
    private final IntentFilter packageFilter = new IntentFilter();
    private final android.content.BroadcastReceiver packageReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
                String packageName = intent.getDataString();
                if (packageName != null) {
                    // Remove the package scheme prefix
                    packageName = packageName.replace("package:", "");
                    // Update the app list to remove the uninstalled app
                    updateAppListAfterUninstall(packageName);
                }
            }
        }
    };

    private void registerPackageReceiver() {
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        registerReceiver(packageReceiver, packageFilter);
    }

    private void unregisterPackageReceiver() {
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAppListAfterUninstall(String packageName) {
        // Remove from showAppInfoList
        Iterator<AppInfo> iterator = showAppInfoList.iterator();
        while (iterator.hasNext()) {
            AppInfo appInfo = iterator.next();
            if (appInfo != null && packageName.equals(appInfo.package_name)) {
                iterator.remove();
                break;
            }
        }

        // 从 SharedPreferences 中移除
        AppListStorage.removeItem(this, packageName);
        // Refresh the adapter
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appInfoAdapter != null) {
                    appInfoAdapter.setContentList(showAppInfoList);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verify all apps in the list are still installed
        verifyInstalledApps();
    }

    private void refreshAppListAsync() {
        // 1. 后台：获取所有已安装应用
        ArrayList<AppInfo> freshAllList = getAllAppInfo(this, false);
        allAppInfoList = freshAllList;

        // 2. 后台：从 SP 加载保存的应用
        List<String> storageAppList = AppListStorage.load(this);

        // 3. 后台：构建新的显示列表
        List<AppInfo> newShowList = new ArrayList<>();
        for (String packageName : storageAppList) {
            AppInfo app = Utils.getAppInfoFromPackage(packageName, freshAllList);
            if (app != null) {
                newShowList.add(app);
            }
        }
        if (newShowList.isEmpty()) {
            // 后台：加载默认应用
            loadDefaultAppsInto(newShowList, freshAllList);
        }
        // 4. 后台：保存到 SP
        saveAppListToPref(newShowList);
        // 5. 后台：添加占位符
        addAppPlaceholderTo(newShowList);

        // 6. 统一替换 showAppInfoList
        showAppInfoList = newShowList;
    }
    private void loadDefaultAppsInto(List<AppInfo> target, List<AppInfo> allApps) {
        //String[] defaultApps = {"com.awell.navigation", "com.awell.bluetooth", "com.awell.carsetting"};
        for (String packName : Utils.getDefaultShowApp(this)) {
            AppInfo appInfo = Utils.getAppInfoFromPackage(packName, allApps);
            if (appInfo != null) target.add(appInfo);
        }
    }

    private void saveAppListToPref(List<AppInfo> list) {
        List<String> packageNames = new ArrayList<>();
        for (AppInfo app : list) {
            if (app != null) packageNames.add(app.package_name);
        }
        AppListStorage.save(this, packageNames);
    }

    private void addAppPlaceholderTo(List<AppInfo> list) {
        list.add(placehodlerInfo);
    }
    private void verifyInstalledApps() {
        if (showAppInfoList == null || showAppInfoList.isEmpty()) return;
        
        // 刷新应用列表
        if (appInfoAdapter != null) {
            appInfoAdapter.setContentList(showAppInfoList);
        }
        
        // 检查应用数量是否匹配
        //checkAppCountMatch();
        
        // 刷新适配器
        new Thread(() -> {
            // 后台加载所有已安装应用并更新 showAppInfoList
            refreshAppListAsync();
            runOnUiThread(() -> {
                if (appInfoAdapter != null) {
                    appInfoAdapter.setContentList(showAppInfoList);
                }
                LogUtil.i("end ,verifyInstalledApps");
            });
        }).start();
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private AppInfo createMismatchPlaceholder() {
        AppInfo mismatchPlaceholder = new AppInfo();
       // mismatchPlaceholder.setIcon(getResources().getDrawable(R.drawable.sf_app_add_icon));
        mismatchPlaceholder.setLabel(getString(R.string.add_app));
        mismatchPlaceholder.package_name = "placeholder_mismatch";
        return mismatchPlaceholder;
    }

}
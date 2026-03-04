package com.launcher.ui3;

import static com.awell.utils.Utils.startWallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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


import com.awell.addapp.MyDbHelper;
import com.awell.addapp.ShowPopupI;
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
    private MusicWidgetBinding musicWidgetBinding;
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

        musicWidgetBinding = binding.layoutMusicWidget;
        musicWidget = musicWidgetBinding.musicWidgetLayout;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);

        setContentView(binding.getRoot());

        initLongTouch();

        clickApp();

        initAddAppView();

        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(1);
    }

    private void initLongTouch() {
        contentView = findViewById(android.R.id.content);
        handler = new Handler(Looper.getMainLooper());
        viewConfiguration = ViewConfiguration.get(this);
    }

    private LinearLayoutManager linearLayoutManager;
    private AppInofAdapter appInfoAdapter;
    private List<AppInfo> allAppInfoList, showAppInfoList;
    private AppInfo placehodlerInfo;
    private MyDbHelper myDbHelper;
    private SQLiteDatabase sqLiteDatabase;

    private void initAddAppView() {
        showAppInfoList = new ArrayList<>();
        allAppInfoList = new ArrayList<>();

        placehodlerInfo = createMismatchPlaceholder();

        // 获取已保存需要显示的app包名，如果没有，则显示默认
        myDbHelper = new MyDbHelper(this, "show_app", null, 1);
        sqLiteDatabase = myDbHelper.getWritableDatabase();

        appInfoAdapter = new AppInofAdapter(this, showAppInfoList, showPopupI, addSelectAppCallback);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
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
        refreshAppList();
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                appInfoAdapter.setContentList(showAppInfoList);
                appInfoAdapter.notifyDataSetChanged();
            }
        });
    }

    private void refreshAppList() {
        // 获取所有已安装应用
        allAppInfoList = getAllAppInfo(this, false);
        // 清空当前显示列表
        showAppInfoList = new ArrayList<>();
        
        // 从数据库加载保存的应用
        List<String> storageAppList = loadAppListFromDatabase();
        
        // 添加保存的应用到显示列表
        for (String packageName : storageAppList) {
            AppInfo app = Utils.getAppInfoFromPackage(packageName, allAppInfoList);
            if (app != null) {
                showAppInfoList.add(app);
            }
        }
        
        // 如果没有保存的应用，加载默认应用
        if (showAppInfoList.isEmpty()) {
            loadDefaultApps();
        }
        
        // 保存应用到数据库
        saveAppListToDatabase();
        
        // 添加占位符
        addAppPlaceholder();
    }

    private List<String> loadAppListFromDatabase() {
        List<String> storageAppList = new ArrayList<>();
        Cursor cursor = myDbHelper.getWritableDatabase().query("showapp", null, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String packageName = cursor.getString(cursor.getColumnIndex("packagename"));
                storageAppList.add(packageName);
                // 删除记录
                sqLiteDatabase.delete("showapp", "packagename=?", new String[]{packageName});
            }
            cursor.close();
        }
        return storageAppList;
    }

    private void loadDefaultApps() {
        for (String packName : Utils.defaultShowApp) {
            AppInfo appInfo = Utils.getAppInfoFromPackage(packName, allAppInfoList);
            if (appInfo != null) {
                showAppInfoList.add(appInfo);
            }
        }
    }

    private void saveAppListToDatabase() {
        for (AppInfo storagePac : showAppInfoList) {
            if (storagePac != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packagename", storagePac.package_name);
                sqLiteDatabase.insert("showapp", null, contentValues);
            }
        }
    }

    private void addAppPlaceholder() {
        // 使用统一的占位符更新方法
        updateAppPlaceholder(false);
    }

    private ShowPopupI showPopupI = new ShowPopupI() {
        @Override
        public void showPopup() {
            showPopupAllapp();
            backgroundAlpha(0.2f);
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
            appInfoAdapter.notifyDataSetChanged();

            new Thread(() -> {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packagename", appInfo.package_name);
                sqLiteDatabase.insert("showapp", null, contentValues);
            }).start();

            showPopupI.hidePopup();
        }

        @Override
        public void removeAppInfo(String packageName) {
            sqLiteDatabase.delete("showapp", "packagename=?", new String[]{packageName});
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

    PopupWindow popupWindow;

    /**
     * popupwindow列表显示所有已安装应用
     */
    private void showPopupAllapp() {

        View view = LayoutInflater.from(this).inflate(R.layout.layout_allapp, null);
        popupWindow = new PopupWindow(view, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);

        popupWindow.setOutsideTouchable(true);
        //popupWindow.setFocusable(true);

        allAppInfoList = getAllAppInfo(this, false);

        RecyclerView rvPop = view.findViewById(R.id.rv_pop_allapp);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        gridLayoutManager.setSpanCount(2);
        gridLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        AppPopAdapter appInofAdapter = new AppPopAdapter(this, allAppInfoList, addSelectAppCallback);
        rvPop.setLayoutManager(gridLayoutManager);
        rvPop.setAdapter(appInofAdapter);

        popupWindow.setOnDismissListener(() -> {
            backgroundAlpha(1.0f);
        });

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
     * @param ctx
     * @param isFilterSystem 是否过滤系统应用
     * @return
     */
    private ArrayList<AppInfo> getAllAppInfo(Context ctx, boolean isFilterSystem) {

        ArrayList<AppInfo> appBeanList = new ArrayList<>();
        AppInfo bean = null;
        PackageManager packageManager = ctx.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        List<PackageInfo> appList = getAppList(packageManager, resolveInfos);

        for (PackageInfo p : appList) {
            bean = new AppInfo();
            bean.setIcon(p.applicationInfo.loadIcon(packageManager));
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString());
            String packName = p.applicationInfo.packageName;
            bean.setPackage_name(packName);
            int flags = p.applicationInfo.flags;
            bean.setFlags(flags);
            if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 && Utils.needToShowPackageName.contains(packName)) {
                appBeanList.add(bean);
            } else if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && !Utils.filterAppPackageName.contains(packName)) {
                appBeanList.add(bean);
            } else if ((flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 && Utils.otherNeedToShowPackageName.contains(packName)) {
                appBeanList.add(bean);
            }
        }

        for (AppInfo a : appBeanList) {
            String str = a.getPackage_name();
            for (int i = 0; i < Utils.mHomePackName.length; i++) {
                if (Utils.mHomePackName[i].equals(str)) {
                    a.setIcon(getResources().getDrawable(Utils.mHomeIcon[i]));
                    break;
                }
            }
        }
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
        binding.radioIv.setOnClickListener(this);
        binding.time.setOnClickListener(this);
        binding.page2DateM.setOnClickListener(this);
        binding.ampm.setOnClickListener(this);
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
        } else if (v.getId() == binding.radioIv.getId()) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
        }else if(v.getId() == binding.time.getId()|| v.getId() == binding.page2DateM.getId() ||v.getId() == binding.ampm.getId()){
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

        // Remove from database
        sqLiteDatabase.delete("showapp", "packagename=?", new String[]{packageName});

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
        registerPackageReceiver();
        // Verify all apps in the list are still installed
        verifyInstalledApps();
    }

    private void verifyInstalledApps() {
        if (showAppInfoList == null || showAppInfoList.isEmpty()) return;
        
        // 刷新应用列表
        refreshAppList();
        
        // 检查应用数量是否匹配
        checkAppCountMatch();
        
        // 刷新适配器
        if (appInfoAdapter != null) {
            appInfoAdapter.setContentList(showAppInfoList);
        }
    }

    private void checkAppCountMatch() {
        // 获取系统中实际应用列表
        ArrayList<AppInfo> actualAppList = getAllAppInfo(this, false);
        // 获取系统中实际应用数量
        int actualAppCount = actualAppList.size();
        // 获取当前应用列表中的应用数量
        int currentAppCount = allAppInfoList != null ? allAppInfoList.size() : 0;
        
        // 检查数量是否匹配
        boolean countMatches = actualAppCount == currentAppCount;
        // 检查包名是否匹配
        boolean packageNamesMatch = countMatches && comparePackageNames(actualAppList, allAppInfoList);
        
        // 检查是否完全匹配
        if (!countMatches || !packageNamesMatch) {
            // 数量或包名不匹配，显示不匹配占位符
            updateAppPlaceholder(true);
        } else {
            // 数量和包名都匹配，显示正常占位符
            updateAppPlaceholder(false);
        }
    }

    private boolean comparePackageNames(ArrayList<AppInfo> actualAppList, List<AppInfo> currentAppList) {
        if (actualAppList == null || currentAppList == null) {
            return false;
        }
        
        // 创建包名集合进行比较
        java.util.Set<String> actualPackages = new java.util.HashSet<>();
        for (AppInfo appInfo : actualAppList) {
            if (appInfo != null && appInfo.package_name != null) {
                actualPackages.add(appInfo.package_name);
            }
        }
        
        java.util.Set<String> currentPackages = new java.util.HashSet<>();
        for (AppInfo appInfo : currentAppList) {
            if (appInfo != null && appInfo.package_name != null) {
                currentPackages.add(appInfo.package_name);
            }
        }
        
        // 检查两个集合是否相等
        return actualPackages.equals(currentPackages);
    }

    private void updateAppPlaceholder(boolean isMismatch) {
        // 移除所有占位符
        removeAllPlaceholders();
        
        // 添加相应的占位符
        if (isMismatch) {
            // 添加不匹配占位符
            AppInfo mismatchPlaceholder = createMismatchPlaceholder();
            showAppInfoList.add(mismatchPlaceholder);
        } else {
            // 添加正常占位符
            showAppInfoList.add(placehodlerInfo);
        }
    }

    private void removeAllPlaceholders() {
        for (int i = showAppInfoList.size() - 1; i >= 0; i--) {
            AppInfo appInfo = showAppInfoList.get(i);
            if (appInfo != null) {
                // 移除正常占位符
                if (appInfo.getLabel() != null && appInfo.getLabel().equals(getString(R.string.add_app))) {
                    showAppInfoList.remove(i);
                }
                // 移除不匹配占位符
                else if ("placeholder_mismatch".equals(appInfo.package_name)) {
                    showAppInfoList.remove(i);
                }
            }
        }
    }

    private AppInfo createMismatchPlaceholder() {
        AppInfo mismatchPlaceholder = new AppInfo();
        mismatchPlaceholder.setIcon(getDrawable(R.drawable.sf_app_add_icon));
        mismatchPlaceholder.setLabel(getString(R.string.add_app));
        mismatchPlaceholder.package_name = "placeholder_mismatch";
        return mismatchPlaceholder;
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterPackageReceiver();
    }

}
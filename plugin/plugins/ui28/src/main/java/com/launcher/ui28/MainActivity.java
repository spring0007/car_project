package com.launcher.ui28;

import static com.awell.utils.Utils.startWallpaper;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.addapp.AddSelectAppCallback;
import com.awell.addapp.AppInfo;
import com.awell.addapp.AppListStorage;
import com.awell.addapp.ShowPopupI;
import com.awell.control.AppsCustomizeConfig;
import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.utils.CommonData;
import com.awell.utils.Utils;
import com.launcher.ui28.databinding.ActivityMainBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int MSG_UPDATE_SPEED = 1;
    private static final int MSG_CLEAR_SPEED = 2;

    // 首页宫格:固定 2 行 × 3 列,最多 6 格
    private static final int MAX_APP_COUNT = 6;
    private static final int GRID_COLUMNS = 3;
    private static final int GRID_ROWS = 2;
    private static final String PLACEHOLDER_PACKAGE = "placeholder_mismatch";

    private static final int ACC_RESET_DELAY_MS = 8 * 1000;
    private static final int SPEED_CLEAR_DELAY_MS = 3 * 1000;
    private static final int POPUP_GRID_COLUMNS = 2;
    private static final int SPEED_UNIT_META_ID = 0x84;
    private static final String SPEED_UNIT_KMH = "KM/h";
    private static final String SPEED_UNIT_MPH = "mph";

    private ActivityMainBinding binding;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private ViewGroup contentView;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler speedHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_SPEED:
                    updateSpeed(msg.arg1, msg.arg2);
                    break;
                case MSG_CLEAR_SPEED:
                    binding.carSpeedTv.setText("0");
                    break;
                default:
                    break;
            }
        }
    };

    private boolean accRecor = false;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private ViewConfiguration viewConfiguration;

    // 长按壁纸检测
    private boolean isEventConsumedByChild = false;
    private boolean isLongPressPossible = false;
    private float startX = 0f;
    private float startY = 0f;
    private Runnable longPressRunnable = null;

    // 首页应用宫格
    private AppInofAdapter appInfoAdapter;
    private List<AppInfo> showAppInfoList = new ArrayList<>();
    private final AppInfo placeholderInfo = createPlaceholder();
    private PopupWindow popupWindow;
    private int loadGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        contentView = findViewById(android.R.id.content);
        viewConfiguration = ViewConfiguration.get(this);

        initMedia();
        initReceiver();
        initAppGridView();
        initLocation();

        binding.timeLayout.setOnClickListener(this);
        updateSpeedUnitText();

        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(new AppsCustomizeConfig.Builder().setThemeMode(0).build());

        int fontSize = getResources().getDimensionPixelSize(R.dimen.font_size);
        int iconSize = getResources().getDimensionPixelSize(R.dimen.icon_size);
        //int cellHeight = getResources().getDimensionPixelSize(R.dimen.cell_height);
        int textPadding = getResources().getDimensionPixelSize(R.dimen.text_padding);

        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeConfig config = new AppsCustomizeConfig.Builder()
                .setThemeMode(0xff)
                .setIconMap(ImageManager.getPackageIconMap())
                .setPluginPackageName("com.launcher.ui28")
                .setRefresh(false)
                //.setIconSizeSame(true)
                //.setColumnCount(6)
                //.setRowCount(2)
                //.setFontSizeSp(fontSize)
                .setAutoWidthGap(true)
                .setAutoHeightGap(true)
                .setFontColor(0xff000000)
                .setIconTextPadding(textPadding)
                //.setTextLine(1)
                // .setCellWidthDp(cellWidth)
                //.setCellWidthDp(iconWidth)
                //.setCellHeightDp(cellHeight)
                .setPluginOtherBgName("sf_icon_bg")
                .setIconSize(iconSize).build();

        AppsCustomizeControl.INSTANCE.setPluginThemeMode(config);

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mainReceiver);
        AppsCustomizeControl.INSTANCE.setActivity(null);
        AppsCustomizeControl.INSTANCE.hideApps();

        mainHandler.removeCallbacksAndMessages(null);
        speedHandler.removeCallbacksAndMessages(null);
        stopLocationUpdates();
        hidePopup();

        mediaControl.unBindDataService(this);
        binding = null;
    }

    // ==================== 媒体 ====================

    private void initMedia() {
        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        musicWidget = binding.layoutMusicWidget.layoutMusicWidget;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.init();
        mediaControl.refreshCurrentMediaState();
    }

    private final AwellMediaControl.UpdateMediaDataToView mediaImpl = new AwellMediaControl.UpdateMediaDataToView() {
        @Override
        public void updateViewMusicPlay(@NotNull Bundle bundle, @NotNull String pkg, @NotNull String command, int mediaType, int currentMedia) {
            if (!"com.awell.radio".equals(pkg)) {
                musicWidget.switchMediaController(pkg, command, mediaType);
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
            musicWidget.setMusicNameTextView(mediaTextOrDefault(songName, R.string.click_play_music), type);
            musicWidget.setArtistNameTextView(mediaTextOrDefault(singerName, R.string.music_artist), type);
        }

        @Override
        public void updateViewPlayTime(@NotNull Bundle bundle, long currentTime, long totalTime, int type) {
            musicWidget.setMusicSeekBar((int) currentTime, (int) totalTime, type);
        }

        @Override
        public void updateViewRadioFreq(@NotNull Bundle bundle, @NotNull String fmOrAm, @NotNull String freq, @NotNull String unit) {
        }

        @Override
        public void handleOriginBundle(@NotNull Bundle bundle) {
        }
    };

    /**
     * 媒体信息里的 "NO_MUSIC_LIST" 和空串都按“无歌曲”处理,显示默认文案
     */
    private String mediaTextOrDefault(String text, int defaultResId) {
        if (TextUtils.isEmpty(text) || "NO_MUSIC_LIST".equals(text)) {
            return getString(defaultResId);
        }
        return text;
    }

    // ==================== 首页应用宫格 ====================

    private void initAppGridView() {
        appInfoAdapter = new AppInofAdapter(showAppInfoList, showPopupI, addSelectAppCallback);
        binding.rvAllApp.setLayoutManager(new GridLayoutManager(this, GRID_COLUMNS, RecyclerView.VERTICAL, false));
        int iconSize = getResources().getDimensionPixelSize(R.dimen.recycle_app_icon_size);
        binding.rvAllApp.addItemDecoration(new EvenSpaceItemDecoration(GRID_COLUMNS, GRID_ROWS, iconSize, iconSize));
        binding.rvAllApp.setAdapter(appInfoAdapter);
    }

    /**
     * 后台构建应用列表,完成后在主线程刷新
     */
    private void loadAppList() {
        final int generation = ++loadGeneration;
        new Thread(() -> {
            List<AppInfo> apps = buildAppList();
            runOnUiThread(() -> {
                // 只接受最后一次刷新结果,避免连续 onResume 时旧数据回写
                if (generation != loadGeneration) {
                    return;
                }
                showAppInfoList = apps;
                if (appInfoAdapter != null) {
                    appInfoAdapter.setContentList(showAppInfoList);
                }
            });
        }, "app-list-loader").start();
    }

    private List<AppInfo> buildAppList() {
        List<AppInfo> allApps = Utils.getAllAppInfo(this, false);
        List<AppInfo> list = new ArrayList<>();

        // 优先使用用户保存的列表,为空时回退到默认应用
        for (String packageName : AppListStorage.load(this)) {
            AppInfo app = Utils.getAppInfoFromPackage(packageName, allApps);
            if (app != null) {
                list.add(app);
            }
        }
        if (list.isEmpty()) {
            for (String packageName : Utils.getDefaultShowApp(this)) {
                AppInfo app = Utils.getAppInfoFromPackage(packageName, allApps);
                if (app != null) {
                    list.add(app);
                }
            }
        }

        // 最多 6 个应用,超出部分不显示也不再保存
        while (list.size() > MAX_APP_COUNT) {
            list.remove(list.size() - 1);
        }
        saveAppList(list);
        ensurePlaceholder(list);
        return list;
    }

    private void saveAppList(List<AppInfo> list) {
        List<String> packageNames = new ArrayList<>(list.size());
        for (AppInfo app : list) {
            if (app != null) {
                packageNames.add(app.package_name);
            }
        }
        AppListStorage.save(this, packageNames);
    }

    /**
     * 未满 6 个应用时补上“添加”按钮;满 6 个应用则隐藏
     */
    private void ensurePlaceholder(List<AppInfo> list) {
        list.remove(placeholderInfo);
        if (list.size() < MAX_APP_COUNT) {
            list.add(placeholderInfo);
        }
    }

    private void removeFromShowList(String packageName) {
        showAppInfoList.removeIf(info -> info != null && packageName.equals(info.package_name));
        ensurePlaceholder(showAppInfoList);
    }

    private int getShownAppCount() {
        int count = 0;
        for (AppInfo info : showAppInfoList) {
            if (info != null && !PLACEHOLDER_PACKAGE.equals(info.package_name)) {
                count++;
            }
        }
        return count;
    }

    private static AppInfo createPlaceholder() {
        AppInfo placeholder = new AppInfo();
        placeholder.package_name = PLACEHOLDER_PACKAGE;
        return placeholder;
    }

    private final ShowPopupI showPopupI = new ShowPopupI() {
        @Override
        public void showPopup() {
            showPopupAllapp();
        }

        @Override
        public void hidePopup() {
            MainActivity.this.hidePopup();
        }
    };

    private final AddSelectAppCallback addSelectAppCallback = new AddSelectAppCallback() {
        @Override
        public void addAppInfo(AppInfo appInfo) {
            if (appInfo == null) {
                return;
            }
            for (AppInfo shown : showAppInfoList) {
                if (shown != null && appInfo.package_name.equals(shown.package_name)) {
                    hidePopup();
                    return;
                }
            }
            if (getShownAppCount() >= MAX_APP_COUNT) {
                hidePopup();
                return;
            }

            loadGeneration++; // 使可能还在后台跑的旧刷新结果失效
            showAppInfoList.remove(placeholderInfo);
            showAppInfoList.add(appInfo);
            ensurePlaceholder(showAppInfoList);
            appInfoAdapter.setContentList(showAppInfoList);
            AppListStorage.addItem(MainActivity.this, appInfo.package_name);
            hidePopup();
        }

        @Override
        public void removeAppInfo(String packageName) {
            loadGeneration++; // 使可能还在后台跑的旧刷新结果失效
            AppListStorage.removeItem(MainActivity.this, packageName);
            removeFromShowList(packageName);
            if (appInfoAdapter != null) {
                appInfoAdapter.setContentList(showAppInfoList);
            }
        }
    };

    /**
     * 弹窗展示所有已安装应用
     */
    private void showPopupAllapp() {
        hidePopup();
        View view = LayoutInflater.from(this).inflate(R.layout.layout_allapp, binding.getRoot(), false);
        popupWindow = new PopupWindow(view, RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT, true);
        popupWindow.setOutsideTouchable(true);

        RecyclerView rvPop = view.findViewById(R.id.rv_pop_allapp);
        rvPop.setLayoutManager(new GridLayoutManager(this, POPUP_GRID_COLUMNS, RecyclerView.HORIZONTAL, false));
        int verticalSpacing = (int) (20 * getResources().getDisplayMetrics().density);
        rvPop.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View child,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = verticalSpacing;
                outRect.bottom = verticalSpacing;
            }
        });
        rvPop.setAdapter(new AppPopAdapter(Utils.getAllAppInfo(this, false), addSelectAppCallback));

        view.setOnClickListener(v -> hidePopup());
        popupWindow.showAtLocation(binding.getRoot(), Gravity.CENTER, 0, 10);
    }

    private void hidePopup() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    // ==================== 广播 ====================

    private void initReceiver() {
        IntentFilter actionFilter = new IntentFilter();
        actionFilter.addAction(CommonData.ACTION_ACC_ON);
        actionFilter.addAction(CommonData.ACTION_ACC_OFF);
        actionFilter.addAction(CommonData.ACTION_ZLINK);
        actionFilter.addAction(CommonData.ACTION_SHOW_ALL_APP);
        actionFilter.addAction(CommonData.ACTION_SPEED_UNIT_CHANGE);
        registerReceiver(mainReceiver, actionFilter, RECEIVER_EXPORTED);

        // 带 data scheme 的过滤器必须单独注册,否则会导致上面的普通广播全部匹配失败
        IntentFilter packageFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        registerReceiver(mainReceiver, packageFilter, RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case CommonData.ACTION_ACC_ON:
                    binding.carSpeedTv.postDelayed(() -> accRecor = false, ACC_RESET_DELAY_MS);
                    break;
                case CommonData.ACTION_ACC_OFF:
                    accRecor = true;
                    break;
                case CommonData.ACTION_ZLINK:
                    String zlinStatus = intent.getStringExtra("status");
                    if (zlinStatus != null) {
                        musicWidget.getCarPlayData(zlinStatus, intent.getStringExtra("phoneMode"));
                    }
                    break;
                case CommonData.ACTION_SHOW_ALL_APP:
                    AppsCustomizeControl.INSTANCE.showApps(contentView);
                    break;
                case CommonData.ACTION_SPEED_UNIT_CHANGE:
                    updateSpeedUnitText();
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    String data = intent.getDataString();
                    if (data != null) {
                        loadGeneration++; // 使可能还在后台跑的旧刷新结果失效
                        String removedPackage = data.replace("package:", "");
                        AppListStorage.removeItem(MainActivity.this, removedPackage);
                        removeFromShowList(removedPackage);
                        if (appInfoAdapter != null) {
                            appInfoAdapter.setContentList(showAppInfoList);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // ==================== 速度 ====================

    private void initLocation() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null || !location.hasSpeed()) {
                    return;
                }
                int speedKm = (int) (location.getSpeed() * 3.6f); // m/s -> km/h
                int speedMile = (int) (speedKm / 1.6093f);        // km/h -> mph
                Message msg = speedHandler.obtainMessage(MSG_UPDATE_SPEED, speedKm, speedMile);
                speedHandler.sendMessage(msg);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        if (mLocationManager == null || mLocationListener == null || !hasLocationPermission()) {
            return;
        }
        try {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    CommonData.MEMENTINE, CommonData.MAXINSTANCES, mLocationListener);
        } catch (SecurityException e) {
            Log.e(TAG, "requestLocationUpdates failed", e);
        }
    }

    private void stopLocationUpdates() {
        if (mLocationManager != null && mLocationListener != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (SecurityException ignored) {
            }
        }
        mLocationManager = null;
        mLocationListener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x10 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void updateSpeed(int speedKm, int speedMile) {
        if (accRecor) {
            return;
        }
        int unit = readSpeedUnit();
        if (unit == 0) {
            binding.carSpeedTv.setText(String.valueOf(speedKm));
            binding.carSpeedUnitTv.setText(SPEED_UNIT_KMH);
        } else if (unit == 1) {
            binding.carSpeedTv.setText(String.valueOf(speedMile));
            binding.carSpeedUnitTv.setText(SPEED_UNIT_MPH);
        }
        speedHandler.removeMessages(MSG_CLEAR_SPEED);
        speedHandler.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, SPEED_CLEAR_DELAY_MS);
    }

    private int readSpeedUnit() {
        byte[] unit = new byte[1];
        CommonData.readDataToMeta(unit, SPEED_UNIT_META_ID);
        return unit[0];
    }

    private void updateSpeedUnitText() {
        int unit = readSpeedUnit();
        if (unit == 0) {
            binding.carSpeedUnitTv.setText(SPEED_UNIT_KMH);
        } else if (unit == 1) {
            binding.carSpeedUnitTv.setText(SPEED_UNIT_MPH);
        }
    }

    // ==================== 点击 ====================

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.time_layout) {
            startActivity(new Intent(Settings.ACTION_DATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (id == R.id.bottom_apps_iv) {
            AppsCustomizeControl.INSTANCE.showApps(contentView);
        } else if (id == R.id.bottom_navigation_iv) {
            openNavigation();
        } else if (id == R.id.bottom_music_iv) {
            launchApp("com.awell.localmusic");
        } else if (id == R.id.bottom_phone_iv) {
            launchApp("com.awell.bluetooth");
        } else if (id == R.id.bottom_setting_iv) {
            launchApp("com.awell.carsetting");
        }
    }

    private void openNavigation() {
        String navPkg = SystemProperties.get("persist.sys.navi.packagename", "");
        Intent intent = TextUtils.isEmpty(navPkg) ? null : getPackageManager().getLaunchIntentForPackage(navPkg);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            launchApp("com.awell.navigation");
        }
    }

    private void launchApp(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            Log.w(TAG, "launchApp: not installed -> " + packageName);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        setBootPackage(packageName);
        startActivity(intent);
    }

    /**
     * 工作区默认应用点击时记录开机自启项
     */
    private void setBootPackage(String packageName) {
        if (!Arrays.asList(IconCache.WorkSpacePackageName).contains(packageName)) {
            return;
        }
        String key = packageName.contains("com.autonavi") ? "boot_apk1" : "boot_apk2";
        Settings.System.putString(getContentResolver(), key, packageName);
    }

    // ==================== 生命周期回调 ====================

    @Override
    public void onBackPressed() {
        AppsCustomizeControl.INSTANCE.hideApps();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppsCustomizeControl.INSTANCE.hideApps();
    }

    // ==================== 长按弹壁纸 ====================

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean consumed = super.dispatchTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isEventConsumedByChild = consumed;
                isLongPressPossible = true;
                startX = ev.getRawX();
                startY = ev.getRawY();
                startLongPressDetection();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getRawX() - startX);
                float dy = Math.abs(ev.getRawY() - startY);
                float touchSlop = viewConfiguration != null ? viewConfiguration.getScaledTouchSlop() : 0f;
                if (dx > touchSlop || dy > touchSlop) {
                    cancelLongPressDetection();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelLongPressDetection();
                break;
            default:
                break;
        }
        return consumed;
    }

    private void startLongPressDetection() {
        cancelLongPressDetection();
        longPressRunnable = () -> {
            if (!isEventConsumedByChild && isLongPressPossible) {
                startWallpaper();
            }
        };
        mainHandler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
    }

    private void cancelLongPressDetection() {
        if (longPressRunnable != null) {
            mainHandler.removeCallbacks(longPressRunnable);
            longPressRunnable = null;
        }
    }
}

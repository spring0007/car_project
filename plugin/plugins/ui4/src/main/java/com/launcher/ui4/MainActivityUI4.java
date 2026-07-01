package com.launcher.ui4;

import static com.awell.utils.Utils.startWallpaper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.addapp.AddSelectAppCallback;
import com.awell.addapp.AppInfo;
import com.awell.addapp.AppListStorage;
import com.awell.addapp.MyDbHelper;
import com.awell.addapp.ShowPopupI;
import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;
import com.awell.utils.CommonData;
import com.awell.utils.LogUtil;
import com.awell.utils.Utils;
import com.launcher.ui4.databinding.ActivityMainUi4NewBinding;
import com.launcher.ui4.databinding.MusicWidgetBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivityUI4 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI4.class.getSimpleName();
  //  private ActivityMainUi4Binding binding;
    private ActivityMainUi4NewBinding binding;
    //private MusicWidgetBinding musicWidgetBinding;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private final int MSG_UPDATE_SPEED = 1;
    private final int MSG_CLEAR_SPEED = 2;
    private Handler mHandlerSpeed = null;
    private boolean accRecor;
    private final int SPEEDHOME = 20;
    private View contentView;


    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // binding = ActivityMainUi4Binding.inflate(getLayoutInflater());
        binding = ActivityMainUi4NewBinding.inflate(getLayoutInflater());

        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        //musicWidgetBinding = binding.layoutMusicWidget;
        musicWidget = binding.layoutMusicWidget.layoutMusicWidget;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);

        setContentView(binding.getRoot());
        initClickEvent();
        initReceiver();
        initLongTouch();
        initAddAppView();

        initLocationListener();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            handler.removeMessages(SPEEDHOME);
            handler.sendEmptyMessageDelayed(SPEEDHOME, 1000);
        }
        updateSpeedUnitText();

        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(0);

    }

    private void initLocationListener() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onLocationChanged(Location location) {

                if (location != null && location.hasSpeed()) {

                    int speed = (int) (location.getSpeed() * 3.6);// m/s ---> km/h
                    int speedMile = (int) (speed / 1.6093);// km/h  ---> miles/h
                    Log.i(TAG, "onLocationChanged: float speed = " + speed);
                    Log.i(TAG, "onLocationChanged: float speedMile = " + speedMile);


                    Message msg = mHandlerSpeed.obtainMessage();
                    msg.what = MSG_UPDATE_SPEED;
                    msg.arg1 = speed;
                    msg.arg2 = speedMile;
                    mHandlerSpeed.sendMessage(msg);
                }
            }
        };
    }

    private void initClickEvent() {
        contentView = findViewById(android.R.id.content);
        binding.hotsetAllapp.setOnClickListener(this);
        binding.layoutRadioWidget.radioIvLayout.setOnClickListener(this);
        binding.layoutRadioWidget.tvRadioAmFm.setOnClickListener(this);
        binding.layoutRadioWidget.ivRadioPre.setOnClickListener(v -> {
            if (ClickUtils.isFastClick()) {
                return;
            }
            mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS);
        });
        binding.layoutRadioWidget.ivRadioNext.setOnClickListener(v -> {
            if (ClickUtils.isFastClick()) {
                return;
            }
            mediaControl.sendStrToHost(AwellTool.RADIO.NEXT);
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x10 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            handler.removeMessages(SPEEDHOME);
            handler.sendEmptyMessageDelayed(SPEEDHOME, 1000);
        }
    }

    private void initLongTouch() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.i(TAG, "handlerNew msg.what = " + msg.what);
                switch (msg.what) {
                    case 100:
                        break;
                    case SPEEDHOME:
                        speedhome();
                        break;
                    default:
                        break;
                }
            }
        };
        viewConfiguration = ViewConfiguration.get(this);
    }

    @SuppressLint("HandlerLeak")
    private void speedhome() {
        Log.i(TAG, "speed come in");
        mHandlerSpeed = new Handler() {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_SPEED:
                        int speed = msg.arg1;
                        binding.carSpeedTv.setText("" + speed);
                        /**
                         * gps车速
                         */
                        int speed_km = msg.arg1;
                        int speed_mile = msg.arg2;
                        Log.i(TAG, "accRecor = " + accRecor);
                        if (!accRecor) {
                            byte[] unit = new byte[1];
                            CommonData.readDataToMeta(unit, 0x84);
                            int unitData = unit[0];
                            Log.e(TAG, "unit Data = " + unitData);
                            if (unitData == 0) {
                                binding.carSpeedTv.setText(speed_km + "");
                                binding.carSpeedUnitTv.setText("KM/h");
                            } else if (unitData == 1) {
                                binding.carSpeedTv.setText(speed_mile + "");
                                binding.carSpeedUnitTv.setText("mph");
                            }
//                            if (animationDrawableTwo != null) {
//                                if (speed_km > 0 || speed_mile > 0) {
//                                    animationDrawableTwo.start();
//                                } else {
//                                    animationDrawableTwo.stop();
//                                }
//                            }
                        }
                        mHandlerSpeed.removeMessages(MSG_CLEAR_SPEED);
                        mHandlerSpeed.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 2000);
                        break;
                    case MSG_CLEAR_SPEED:
                        binding.carSpeedTv.setText("" + 0);
//                        if (animationDrawableTwo != null) {
//                            animationDrawableTwo.stop();
//                        }
                        break;
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mLocationManager != null) {
            mLocationManager.requestLocationUpdates("gps", 1000, 10, mLocationListener, mHandlerSpeed.getLooper());
        } else {
            Log.e(TAG, "LocationManager is null, cannot request location updates");
        }
    }

    private void updateSpeedUnitText() {
        byte[] unit = new byte[1];
        CommonData.readDataToMeta(unit, 0x84);
        int unitData = unit[0];
        Log.e(TAG, "unit Data = " + unitData);
        if (binding.carSpeedUnitTv != null) {
            if (unitData == 0) {
                binding.carSpeedUnitTv.setText("KM/h");
            } else if (unitData == 1) {
                binding.carSpeedUnitTv.setText("mph");
            }
        }
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(CommonData.BROADCAST_LAMP_SWITCH);
        filter.addAction(CommonData.ACTION_ACC_ON);
        filter.addAction(CommonData.ACTION_ACC_OFF);
        filter.addAction("com.zjinnova.zlink");
        filter.addAction("android.launcher.show.allApp");
        filter.addAction(CommonData.BROADCAST_MEDIA_EXIT);
        filter.addAction("CANBUS_CHANGE_SPEED_Unit");
        filter.addAction("top_session_package_change");
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(mainReceiver, filter, RECEIVER_EXPORTED);
//        updateTime();
    }

    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "mainReceiver:" + action);
            switch (action) {
                case CommonData.BROADCAST_LAMP_SWITCH:
//                    if (intent.getIntExtra("lamplet_state", 0) == 1)
//                        ivLampSwitchBg.setImageResource(com.awell.launcher.library.R.drawable.open);
//                    else ivLampSwitchBg.setImageResource(com.awell.launcher.library.R.drawable.off);
                    break;
                case CommonData.ACTION_ACC_ON:
//                    if (ivLampSwitchBg != null)
//                        ivLampSwitchBg.postDelayed(() -> accRecor = false, 8 * 1000);
                    break;
                case CommonData.ACTION_ACC_OFF:
                    accRecor = true;
                    break;
                case CommonData.BROADCAST_MEDIA_EXIT:
                    String packge = intent.getStringExtra("package");
                    if (packge != null && (packge.equals("cn.kuwo.kwmusiccar") || packge.equals("exitAll"))) {

                    }
                    break;
                case "com.zjinnova.zlink":
                    String zlinStatus = intent.getStringExtra("status");
                    String phoneMode = intent.getStringExtra("phoneMode");
                    Log.d(TAG, "zlinStatus:" + zlinStatus);
                    if (zlinStatus == null) {
                        return;
                    }
                    musicWidget.getCarPlayData(zlinStatus, phoneMode);
                    break;
                case "android.launcher.show.allApp":
                    Log.d(TAG, "mainReceiver:" + intent.getAction());
                    AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
                    break;
                case "CANBUS_CHANGE_SPEED_Unit":
                    updateSpeedUnitText();
                    break;
                case "top_session_package_change":
                    String sessionTopPkg = intent.getStringExtra("top_package");
                    handleMediaPlaybackResult(sessionTopPkg, "start", 3, 4);
                    Log.d(TAG, "88888-top_session_package_change:" + sessionTopPkg);
                    break;
				case  Intent.ACTION_PACKAGE_REMOVED :
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

    public void handleMediaPlaybackResult(String value1, String value2, int value3, int value4) {
        String oldPlayingPackage = mediaControl.getCurrentPkgName();
        boolean isStartCommand = "start".equals(value2);
        boolean isStopCommand = "stop".equals(value2);
        boolean isValidPackage = !TextUtils.isEmpty(value1);
        Log.i(TAG, "handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:value1=" + value1 + " --oldPlayingPackage=" + oldPlayingPackage + "--value2=" + value2);
        Log.i(TAG, "handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:isValidPackage=" + isValidPackage + " --isStartCommand=" + isStartCommand + "-isStopCommand=" + isStopCommand);


        // 处理停止播放的情况
        //if (isValidPackage && isStopCommand) {
        //    mMediaListener.setCurrentPlayingPackage(null);
        //    return;
        //}

        // 处理开始播放的情况
        if (isValidPackage && isStartCommand) {
            // 当前没有播放或切换到新包时，更新并启动回调
            if (oldPlayingPackage != null && !oldPlayingPackage.equals(value1)) {
            }
            //Log.i(TAG, "0000----Switched to new package: " + value1);
        }
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

//        findViewById(R.id.rl_content_view).setOnClickListener(v -> hidePopup());
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

//    private void refreshAppList() {
//        // 获取所有已安装应用
//        allAppInfoList = getAllAppInfo(this, false);
//        // 清空当前显示列表
//        showAppInfoList = new ArrayList<>();
//
//        // 从 SharedPreferences 加载保存的应用 — 替代 loadAppListFromDatabase()
//        List<String> storageAppList = AppListStorage.load(this);
//        // 添加保存的应用到显示列表
//        for (String packageName : storageAppList) {
//            AppInfo app = Utils.getAppInfoFromPackage(packageName, allAppInfoList);
//            if (app != null) {
//                showAppInfoList.add(app);
//            }
//        }
//        // 如果没有保存的应用，加载默认应用
//        if (showAppInfoList.isEmpty()) {
//            loadDefaultApps();
//        }
//        // 保存应用到 SharedPreferences — 替代 saveAppListToDatabase()
//        saveAppListToPref();
//        addAppPlaceholder();
//    }

    // 新增：写入 SP
//    private void saveAppListToPref() {
//        List<String> packageNames = new ArrayList<>();
//        for (AppInfo app : showAppInfoList) {
//            if (app != null) {
//                packageNames.add(app.package_name);
//            }
//        }
//        AppListStorage.save(this, packageNames);
//    }

//    private void loadDefaultApps() {
//        String [] defaultApps = {/*"com.autonavi.amapauto",*/ "com.awell.navigation","com.awell.bluetooth", "com.awell.carsetting"};//定制apk显示
//        for (String packName : defaultApps/*Utils.getDefaultShowApp(this)*/) {
//            AppInfo appInfo = Utils.getAppInfoFromPackage(packName, allAppInfoList);
//            if (appInfo != null) {
//                showAppInfoList.add(appInfo);
//            }
//        }
//    }

//    private void addAppPlaceholder() {
//        // 使用统一的占位符更新方法
//        updateAppPlaceholder(false);
//    }

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
            //appInfoAdapter.notifyDataSetChanged();

            // 替代 sqLiteDatabase.insert() — 无需新线程，SP.apply() 本身就是异步
            AppListStorage.addItem(MainActivityUI4.this, appInfo.package_name);

            showPopupI.hidePopup();
        }


        @Override
        public void removeAppInfo(String packageName) {
            // 替代 sqLiteDatabase.delete()
            AppListStorage.removeItem(MainActivityUI4.this, packageName);

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
        popupWindow = new PopupWindow(view, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);

        popupWindow.setOutsideTouchable(true);
        //popupWindow.setFocusable(true);

        allAppInfoList = getAllAppInfo(this, false);

        RecyclerView rvPop = view.findViewById(R.id.rv_pop_allapp);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        //gridLayoutManager.setSpanCount(2);
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
           // bean.setIcon(p.applicationInfo.loadIcon(packageManager));
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

        /*for (AppInfo a : appBeanList) {
            String str = a.getPackage_name();
            Integer iconRes = ImageManager.mHomePackName.getOrDefault(str ,0);

            if (iconRes != null && iconRes != 0) {
                try {
                    // 关键修改：使用主应用的 Context 加载资源
                    //@SuppressLint("UseCompatLoadingForDrawables") Drawable icon = getResources().getDrawable(iconRes,getTheme());
                    a.setIcon(getResources().getDrawable(iconRes,getTheme()));
                } catch (Resources.NotFoundException e) {
                    LogUtil.w( "加载自定义图标失败: " + str);
                }
            }
        }*/


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
        unregisterReceiver(mainReceiver);
        AppsCustomizeControl.INSTANCE.setActivity(null);
        AppsCustomizeControl.INSTANCE.hideApps();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        cleanListener();

        mediaControl.unBindDataService(this);
    }

    private void cleanListener() {
        if (mHandlerSpeed != null) {
            mHandlerSpeed.removeCallbacksAndMessages(null);
            mHandlerSpeed = null;
        }

        if (mLocationManager != null && mLocationListener != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            mLocationManager = null;
            mLocationListener = null;
        }

        if (binding != null) {
            binding = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.hotsetAllapp.getId()) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        } else if (v.getId() == binding.layoutRadioWidget.tvRadioAmFm.getId()) {
            mediaControl.sendStrToHost(AwellTool.RADIO.SET_FMAM);
        } else if (v.getId() == binding.layoutRadioWidget.radioIvLayout.getId()) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
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
                binding.layoutRadioWidget.tvRadioFreq.setText(freq);
                binding.layoutRadioWidget.tvRadioAmFm.setText(fmOrAm);
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
        String[] defaultApps = {"com.awell.navigation", "com.awell.bluetooth", "com.awell.carsetting"};
        for (String packName : defaultApps) {
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
            });
        }).start();
    }

//    private void checkAppCountMatch() {
//        // 获取系统中实际应用列表
//        ArrayList<AppInfo> actualAppList = getAllAppInfo(this, false);
//        // 获取系统中实际应用数量
//        int actualAppCount = actualAppList.size();
//        // 获取当前应用列表中的应用数量
//        int currentAppCount = allAppInfoList != null ? allAppInfoList.size() : 0;
//
//        // 检查数量是否匹配
//        boolean countMatches = actualAppCount == currentAppCount;
//        // 检查包名是否匹配
//        boolean packageNamesMatch = countMatches && comparePackageNames(actualAppList, allAppInfoList);
//
//        // 检查是否完全匹配
//        if (!countMatches || !packageNamesMatch) {
//            // 数量或包名不匹配，显示不匹配占位符
//            updateAppPlaceholder(true);
//        } else {
//            // 数量和包名都匹配，显示正常占位符
//            updateAppPlaceholder(false);
//        }
//    }

//    private boolean comparePackageNames(ArrayList<AppInfo> actualAppList, List<AppInfo> currentAppList) {
//        if (actualAppList == null || currentAppList == null) {
//            return false;
//        }
//
//        // 创建包名集合进行比较
//        java.util.Set<String> actualPackages = new java.util.HashSet<>();
//        for (AppInfo appInfo : actualAppList) {
//            if (appInfo != null && appInfo.package_name != null) {
//                actualPackages.add(appInfo.package_name);
//            }
//        }
//
//        java.util.Set<String> currentPackages = new java.util.HashSet<>();
//        for (AppInfo appInfo : currentAppList) {
//            if (appInfo != null && appInfo.package_name != null) {
//                currentPackages.add(appInfo.package_name);
//            }
//        }
//
//        // 检查两个集合是否相等
//        return actualPackages.equals(currentPackages);
//    }

//    private void updateAppPlaceholder(boolean isMismatch) {
//        // 移除所有占位符
//        removeAllPlaceholders();
//
//        // 添加相应的占位符
//        if (isMismatch) {
//            // 添加不匹配占位符
//            AppInfo mismatchPlaceholder = createMismatchPlaceholder();
//            showAppInfoList.add(mismatchPlaceholder);
//        } else {
//            // 添加正常占位符
//            showAppInfoList.add(placehodlerInfo);
//        }
//    }

//    private void removeAllPlaceholders() {
//        for (int i = showAppInfoList.size() - 1; i >= 0; i--) {
//            AppInfo appInfo = showAppInfoList.get(i);
//            if (appInfo != null) {
//                // 移除正常占位符
//                if (appInfo.getLabel() != null && appInfo.getLabel().equals(getString(R.string.add_app))) {
//                    showAppInfoList.remove(i);
//                }
//                // 移除不匹配占位符
//                else if ("placeholder_mismatch".equals(appInfo.package_name)) {
//                    showAppInfoList.remove(i);
//                }
//            }
//        }
//    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private AppInfo createMismatchPlaceholder() {
        AppInfo mismatchPlaceholder = new AppInfo();
        mismatchPlaceholder.setIcon(getResources().getDrawable(R.drawable.sf_app_add_icon));
        mismatchPlaceholder.setLabel(getString(R.string.add_app));
        mismatchPlaceholder.package_name = "placeholder_mismatch";
        return mismatchPlaceholder;
    }

}
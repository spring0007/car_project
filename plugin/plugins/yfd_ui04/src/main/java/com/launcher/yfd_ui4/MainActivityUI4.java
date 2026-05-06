package com.launcher.yfd_ui4;

import static com.awell.utils.Utils.startWallpaper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.awell.control.AppsCustomizeConfig;
import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.utils.CommonData;
import com.launcher.yfd_ui4.databinding.ActivityMainUi1Binding;
import com.launcher.yfd_ui4.utils.WeatherHelper;
import com.launcher.yfd_ui4.utils.WeatherIconLoader;
import com.launcher.yfd_ui4.utils.WeatherTextMapper;

import org.jetbrains.annotations.NotNull;



public class MainActivityUI4 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI4.class.getSimpleName();
    private ActivityMainUi1Binding binding;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private int PERMISSION_REQUEST_CODE = 100;
    private Boolean isWeatherTimerRunning = false;
    private boolean accRecor;
    private static final String BTSTATUS = "awell_bt_status";
    private Runnable  weatherRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadWeatherData();
            handler.postDelayed(this, (60 * 1000));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUi1Binding.inflate(getLayoutInflater());

        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        musicWidget = binding.layoutMusicWidget.layoutMusicWidget;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);

        setContentView(binding.getRoot());
        initLongTouch();
        binding.tvApp.setOnClickListener(this);
        binding.ivApp.setOnClickListener(this);
        binding.tvMusic.setOnClickListener(this);
        binding.ivMusic.setOnClickListener(this);
        binding.tvNav.setOnClickListener(this);
        binding.ivNav.setOnClickListener(this);
        binding.tvSetting.setOnClickListener(this);
        binding.ivSetting.setOnClickListener(this);
        binding.tvPhone.setOnClickListener(this);
        binding.ivPhone.setOnClickListener(this);


        binding.itemMusic.setOnClickListener(this);
        binding.itemCar.setOnClickListener(this);
        binding.itemNavi.setOnClickListener(this);
        binding.itemPhone.setOnClickListener(this);
        binding.itemRadio.setOnClickListener(this);
        binding.itemSetting.setOnClickListener(this);
        binding.itemVideo.setOnClickListener(this);
        binding.itemWeather.setOnClickListener(this);

        setBtstatus();
        initReceiver();
        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(4,false);
        AppsCustomizeConfig appsCustomizeConfig = new AppsCustomizeConfig.Builder()
                .setAutoWidthGap(true)
                .setColumnCount(5)
                .setIconTextPadding((int) getResources().getDimension(R.dimen.app_icon_padding))
                .build();
        AppsCustomizeControl.INSTANCE.applyAppsCustomizeConfig(appsCustomizeConfig);

        binding.getRoot().postDelayed(this::checkAndRequestPermission, 50);
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
        registerReceiver(mainReceiver, filter, RECEIVER_EXPORTED);
//        updateTime();
    }

    private void setBtstatus(){
        // flag 0 : BT close   1 : BT NoConnected    2 : BT Connected
        int mBTStatus = Settings.System.getInt(getContentResolver(), BTSTATUS, 0);
        Log.d(TAG, " mBTStatus = " + mBTStatus);

        if (mBTStatus == 0 || mBTStatus == 1){
            binding.itemPhone.setBackgroundResource(R.drawable.edit_icon_phone_unconnected);
            binding.tvPhoneSubTitle.setText(R.string.no_phone_connected);
        }else if (mBTStatus == 2){
            binding.itemPhone.setBackgroundResource(R.drawable.edit_icon_phone_connected);
            binding.tvPhoneSubTitle.setText(R.string.phone_connect);
        }
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
                    AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
                    break;
                case "top_session_package_change":
                    String sessionTopPkg = intent.getStringExtra("top_package");
                    handleMediaPlaybackResult(sessionTopPkg, "start", 3, 4);
                    Log.d(TAG, "88888-top_session_package_change:" + sessionTopPkg);
                    break;

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
    private void loadWeatherData() {
        Log.i(TAG, "loadWeatherData: huang info=>");
        new Thread(() -> {
            WeatherHelper.WeatherInfo weatherInfo = WeatherHelper.getCurrentWeather(MainActivityUI4.this);
            Log.i(TAG, "loadWeatherData: huang info=>" + weatherInfo);
            handler.post(() -> {
                if (weatherInfo != null) {
                    binding.weaterTvTemp.setText(weatherInfo.temperature + " ℃");
                    String weather = (WeatherTextMapper.description(MainActivityUI4.this, weatherInfo.condCode));
                    binding.weaterTvTemp.setVisibility(View.VISIBLE);
                    binding.weaterIvInfo.setVisibility(View.VISIBLE);
                    binding.weaterTvInfo.setText(weather + "  " + weatherInfo.tempMin + "/" + weatherInfo.tempMax + "℃");
                    WeatherIconLoader.getWeatherIcon(binding.weaterIvInfo, weatherInfo.condCode);
                }else {
                    binding.weaterTvInfo.setText(R.string.no_weather_information_available_at_the_moment);
                    binding.weaterTvTemp.setVisibility(View.GONE);
                    binding.weaterIvInfo.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, "com.awell.weather.permission.READ_WEATHER")
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{"com.awell.weather.permission.READ_WEATHER"},
                    PERMISSION_REQUEST_CODE
            );
        } else {
            // 权限已授予，开始查询
            loadWeatherData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWeatherData();
            } else {
                // 处理权限被拒绝
                //showPermissionDeniedMessage()
                Log.e(TAG, "onRequestPermissionsResult: not have permission==>");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isWeatherTimerRunning) {
            handler.postDelayed(weatherRefreshRunnable, 0);
            isWeatherTimerRunning = true;
        }
        setBtstatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(weatherRefreshRunnable);
        isWeatherTimerRunning = false;
        setBtstatus();
    }

    private void initLongTouch() {
        handler = new Handler(Looper.getMainLooper());
        viewConfiguration = ViewConfiguration.get(this);
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
        mediaControl.unBindDataService(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_music || v.getId() == R.id.tv_music || v.getId() == R.id.item_music ){
            startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
        } else if (v.getId() == R.id.iv_nav || v.getId() == R.id.tv_nav || v.getId() == R.id.item_navi) {
            String navPkg = SystemProperties.get("persist.sys.navi.packagename", "");
            //String navClazz = SystemProperties.get("persist.sys.navi.clazzname", "");
            if(TextUtils.isEmpty(navPkg) || !getNaviApps(navPkg))
                startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
            else{
                Intent intent = this.getPackageManager().getLaunchIntentForPackage(navPkg);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
                }
            }
        } else if (v.getId() == R.id.item_weather) {
            startActivity("com.awell.weather", "com.awell.weather.MainActivity");
        } else if (v.getId() == R.id.iv_app || v.getId() == R.id.tv_app) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        } else if (v.getId() == R.id.item_radio) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
       } else if (v.getId() == R.id.item_car) {
            startActivity("com.awell.backcar", "com.awell.backcar.MainActivity");
        } else if (v.getId() == R.id.iv_phone || v.getId() == R.id.tv_phone) {
            startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
        } else if (v.getId() == R.id.item_video) {
            startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
        } else if (v.getId() == R.id.iv_setting || v.getId() == R.id.tv_setting || v.getId() == R.id.item_setting) {
            startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
        } else if ( v.getId() == R.id.item_phone) {
            startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
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

    public boolean getNaviApps(String appName) {
        try {
            ApplicationInfo appinfo = this.getPackageManager().getApplicationInfo(appName, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            Log.i(TAG,"Exception = " + e.toString());
        }
        return false;
    }

}
package com.launcher.ui6;

import static com.awell.utils.Utils.startWallpaper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.utils.CommonData;
import com.launcher.ui6.databinding.ActivityMainUi6Binding;
import com.launcher.ui6.databinding.DialWidgetBinding;
import com.launcher.ui6.databinding.MusicWidgetBinding;
import com.launcher.ui6.view.DialWidget;

import org.jetbrains.annotations.NotNull;


public class MainActivityUI6 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI6.class.getSimpleName();
    private ActivityMainUi6Binding binding;
    private MusicWidgetBinding musicWidgetBinding;
    private MusicWidget musicWidget;
    private DialWidgetBinding dialWidgetBinding;
    private DialWidget dialWidget;
    private AwellMediaControl mediaControl;
    private final int MSG_UPDATE_SPEED = 1;
    private final int MSG_CLEAR_SPEED = 2;
    private Handler mHandlerSpeed = null;
    private boolean accRecor;
    private final int SPEEDHOME = 20;
    LocationManager mLocationManager = null;

    LocationListener mLocationListener = new LocationListener() {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUi6Binding.inflate(getLayoutInflater());

        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        musicWidgetBinding = binding.layoutMusicWidget;
        musicWidget = musicWidgetBinding.musicWidgetLayout;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);
        dialWidgetBinding = binding.layoutDialWidget;
        dialWidget = dialWidgetBinding.dialWidgetLayout;
        dialWidget.findViews(this, dialWidget);
        setContentView(binding.getRoot());

        initReceiver();
        initLongTouch();

        clickApp();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            handler.removeMessages(SPEEDHOME);
            handler.sendEmptyMessageDelayed(SPEEDHOME, 1000);
        }

        updateSpeedUnitText();
        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(1);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dialWidget != null)
            dialWidget.startAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialWidget != null)
            dialWidget.stopAnimation();
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
                                binding.carSpeedUnitTv.setText("km/h");
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
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates("gps", 1000, 10, mLocationListener, mHandlerSpeed.getLooper());
    }

    private void updateSpeedUnitText() {
        byte[] unit = new byte[1];
        CommonData.readDataToMeta(unit, 0x84);
        int unitData = unit[0];
        Log.e(TAG, "unit Data = " + unitData);
        if (binding.carSpeedUnitTv != null) {
            if (unitData == 0) {
                binding.carSpeedUnitTv.setText("km/h");
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
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
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
                case Intent.ACTION_TIME_CHANGED:
                    // 用户手动更改了时间
                case Intent.ACTION_TIMEZONE_CHANGED:
                    // 时区发生了变化
                case Intent.ACTION_DATE_CHANGED:
                    // 日期发生了变化
                case Intent.ACTION_TIME_TICK:
                    //系统时间变化
                    if (dialWidget != null)
                        dialWidget.updateTimeSysem();
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
        unregisterReceiver(mainReceiver);
        AppsCustomizeControl.INSTANCE.hideApps();
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

    private void clickApp() {
        binding.hotsetAllapp.setOnClickListener(this);
        binding.hotsetBtapp.setOnClickListener(this);
        binding.hotsetNavApp.setOnClickListener(this);
        binding.hotsetSettingsApp.setOnClickListener(this);
        binding.hotsetRadioApp.setOnClickListener(this);
        binding.hotsetVideoApp.setOnClickListener(this);
        binding.layoutDialWidget.analogClockView.setOnClickListener(this);
//        binding.layoutRadioWidget.tvRadioAmFm.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.hotsetAllapp.getId()) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        } else if (v.getId() == binding.hotsetBtapp.getId()) {
            startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
        } else if (v.getId() == binding.hotsetNavApp.getId()) {
            startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
        } else if (v.getId() == binding.hotsetSettingsApp.getId()) {
            startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
        } else if (v.getId() == binding.hotsetRadioApp.getId()) {
            startActivity("com.awell.radio", "com.awell.radio.AwellFmActivity");
        } else if (v.getId() == binding.hotsetVideoApp.getId()) {
            startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
        } else if (v.getId() == binding.layoutDialWidget.analogClockView.getId()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
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
//           runOnUiThread(() -> {
//                binding.layoutRadioWidget.tvRadioFreq.setText(freq);
//                binding.layoutRadioWidget.tvRadioAmFm.setText(fmOrAm);
//             });
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

    private void handleLongPressAction() {
        startWallpaper();
    }


}
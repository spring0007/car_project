package com.launcher.ui11;

import static com.awell.utils.Utils.startWallpaper;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;

import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;
import com.awell.utils.CommonData;
import com.launcher.ui11.databinding.ActivityMainUi11Binding;
import com.launcher.ui11.databinding.ActivityMainUi11NewBinding;
import com.launcher.ui11.databinding.MusicWidgetBinding;

import org.jetbrains.annotations.NotNull;


public class MainActivityUI11 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI11.class.getSimpleName();
   // private ActivityMainUi11Binding binding;
    private ActivityMainUi11NewBinding binding;
    private MusicWidgetBinding musicWidgetBinding;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private final int MSG_UPDATE_SPEED = 1;
    private final int MSG_CLEAR_SPEED = 2;
    private boolean accRecor;
    private final int SPEEDHOME = 20;
    private static final String BTSTATUS = "awell_bt_status";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // binding = ActivityMainUi11Binding.inflate(getLayoutInflater());
        binding = ActivityMainUi11NewBinding.inflate(getLayoutInflater());


        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        musicWidgetBinding = binding.layoutMusicWidget;
        musicWidget = musicWidgetBinding.musicWidgetLayout;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);
        setContentView(binding.getRoot());

        setBtstatus();
        initReceiver();
        initLongTouch();

        clickApp();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            handler.removeMessages(SPEEDHOME);
            handler.sendEmptyMessageDelayed(SPEEDHOME, 1000);
        }
        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(0);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "lifedate onResume");
        super.onResume();
        setBtstatus();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "lifedate onPause");
        super.onPause();
        setBtstatus();
    }


    @Override
    protected void onStop() {
        Log.d(TAG, "lifedate onStop");
        super.onStop();
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
                    default:
                        break;
                }
            }
        };
        viewConfiguration = ViewConfiguration.get(this);
    }

    private void setBtstatus(){
        // flag 0 : BT close   1 : BT NoConnected    2 : BT Connected
        int mBTStatus = Settings.System.getInt(getContentResolver(), BTSTATUS, 0);
        Log.d(TAG, " mBTStatus = " + mBTStatus);
        if (mBTStatus == 0){
            binding.btStatusTv.setText(getString(R.string.bt_close));
        }else if (mBTStatus == 1){
            binding.btStatusTv.setText(getString(R.string.bt_not_connected));
        }else if (mBTStatus == 2){
            binding.btStatusTv.setText(getString(R.string.bt_connected));
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
        unregisterReceiver(mainReceiver);
        mediaControl.unBindDataService(this);
        AppsCustomizeControl.INSTANCE.hideApps();
    }

    private void clickApp() {
        binding.hotsetAllapp.setOnClickListener(this);
        binding.hotsetNavApp.setOnClickListener(this);
        binding.hotsetMusicApp.setOnClickListener(this);
        binding.hotsetSettingApp.setOnClickListener(this);
		binding.hotsetVideoApp.setOnClickListener(this);
        binding.timeBgIv.setOnClickListener(this);
        binding.timeHour.setOnClickListener(this);
        binding.layoutRadioWidget.radioLayout.setOnClickListener(this);
        binding.layoutRadioWidget.ivRadioPre.setOnClickListener(this);
        binding.layoutRadioWidget.ivRadioNext.setOnClickListener(this);
        binding.layoutRadioWidget.tvRadioAmFm.setOnClickListener(this);
        binding.btBgIv.setOnClickListener(this);
  //      binding.layoutDialWidget.analogClockView.setOnClickListener(this);
//        binding.layoutRadioWidget.tvRadioAmFm.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.hotsetAllapp.getId()) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        }else if (v.getId() == binding.btBgIv.getId() ) {
            startActivity( "com.awell.bluetooth","com.awell.bluetooth.MainActivity");
        }else if (v.getId() == binding.hotsetNavApp.getId()) {
            startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
        }else if (v.getId() == binding.hotsetMusicApp.getId() ){
            startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
        }else if(v.getId() == binding.layoutRadioWidget.radioLayout.getId()){
            startActivity("com.awell.radio", "com.awell.radio.AwellFmActivity");
        }else if (v.getId() == binding.layoutRadioWidget.ivRadioNext.getId()){
            if (ClickUtils.isFastClick()) {
                return;
            }
            mediaControl.sendStrToHost(AwellTool.RADIO.NEXT);
        }else if(v.getId() == binding.layoutRadioWidget.ivRadioPre.getId()){
            if (ClickUtils.isFastClick()) {
                return;
            }
            mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS);
        }else if (v.getId() == binding.layoutRadioWidget.tvRadioAmFm.getId()){
            mediaControl.sendStrToHost(AwellTool.RADIO.SET_FMAM);
        } else if(v.getId() == binding.hotsetVideoApp.getId()){
            startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
        }else if (v.getId() == binding.hotsetSettingApp.getId()){
            startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
        }else if(v.getId() == binding.timeBgIv.getId()){
            Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
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
                binding.layoutRadioWidget.tvRadioFreq.setText(freq);
                binding.layoutRadioWidget.tvRadioAmFm.setText(fmOrAm);
                binding.layoutRadioWidget.tvRadioUnit.setText(unit);
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

    private void handleLongPressAction() {
        startWallpaper();
    }


}
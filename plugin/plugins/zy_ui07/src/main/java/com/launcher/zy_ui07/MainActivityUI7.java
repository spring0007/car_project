package com.launcher.zy_ui07;

import static com.awell.utils.Utils.startWallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
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

import com.awell.control.AppsCustomizeConfig;
import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;
import com.awell.utils.CommonData;
import com.launcher.zy_ui07.ClickUtils;
import com.launcher.zy_ui07.databinding.ActivityMainBinding;
import com.launcher.zy_ui07.databinding.MusicWidgetBinding;

import org.jetbrains.annotations.NotNull;


public class MainActivityUI7 extends Activity implements View.OnClickListener {
    private ActivityMainBinding binding;
    private MusicWidgetBinding musicWidgetBinding;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private View contentView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());


        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        musicWidgetBinding = binding.layoutMusicWidget;
        musicWidget = musicWidgetBinding.layoutMusicWidget;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);

        setContentView(binding.getRoot());

        initLongTouch();
        initReceiver();
        clickApp();

        int fontSize = getResources().getDimensionPixelSize(R.dimen.font_size);
        int iconSize = getResources().getDimensionPixelSize(R.dimen.icon_size);
        int cellWidth = getResources().getDimensionPixelSize(R.dimen.cell_width);
        int cellHeight = getResources().getDimensionPixelSize(R.dimen.cell_height);
        int textPadding = getResources().getDimensionPixelSize(R.dimen.text_padding);


        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeConfig config = new AppsCustomizeConfig.Builder()
                .setColumnCount(5)
                .setRowCount(2)
                .setFontSizeSp(fontSize)
                .setAutoWidthGap(true)
                .setAutoHeightGap(true)
                .setIconTextPadding(textPadding)
                .setTextLine(1)
                .setCellWidthDp(cellWidth)
                //.setCellWidthDp(iconWidth)
                .setCellHeightDp(cellHeight)
                .setThemeMode(0xff)
                .setRefresh(false)
                .setIconMap(IconManager.getPackageIconMap())
                .setPluginPackageName("com.launcher.zy_ui07")
                .setPluginOtherBgName("zy07_other_app")
                .setIconSize(iconSize).build();
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(config);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.setLogSwitch();
    }

    private void initLongTouch() {
        contentView = findViewById(android.R.id.content);
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
        try {
            mediaControl.unBindDataService(this);
        } catch (Exception e) {
            e.printStackTrace();
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
        registerReceiver(mainReceiver, filter, RECEIVER_EXPORTED);
//        updateTime();
    }

    private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.i( "mainReceiver:" + action);
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
                    //accRecor = true;
                    break;
                case CommonData.BROADCAST_MEDIA_EXIT:
                    String packge = intent.getStringExtra("package");
                    if (packge != null && (packge.equals("cn.kuwo.kwmusiccar") || packge.equals("exitAll"))) {

                    }
                    break;
                case "com.zjinnova.zlink":
                    String zlinStatus = intent.getStringExtra("status");
                    String phoneMode = intent.getStringExtra("phoneMode");
                    LogUtil.d( "zlinStatus:" + zlinStatus);
                    if (zlinStatus == null) {
                        return;
                    }
                    //musicWidget.getCarPlayData(zlinStatus, phoneMode);
                    break;
                case "android.launcher.show.allApp":
                    AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
                    break;
                case "top_session_package_change":
                    String sessionTopPkg = intent.getStringExtra("top_package");
                    handleMediaPlaybackResult(sessionTopPkg, "start", 3, 4);
                    LogUtil.d( "88888-top_session_package_change:" + sessionTopPkg);
                    break;

            }

        }
    };

    public void handleMediaPlaybackResult(String value1, String value2, int value3, int value4) {
        String oldPlayingPackage = mediaControl.getCurrentPkgName();
        boolean isStartCommand = "start".equals(value2);
        boolean isStopCommand = "stop".equals(value2);
        boolean isValidPackage = !TextUtils.isEmpty(value1);
        LogUtil.i("handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:value1=" + value1 + " --oldPlayingPackage=" + oldPlayingPackage + "--value2=" + value2);
        LogUtil.i("handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:isValidPackage=" + isValidPackage + " --isStartCommand=" + isStartCommand + "-isStopCommand=" + isStopCommand);


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

    private void clickApp() {
        binding.hotsetAllapp.setOnClickListener(this);
       // binding.radioLayout.radioIv.setOnClickListener(this);
//        binding.radioLayout.ivRadioPre.setOnClickListener(v -> {
//            if (ClickUtils.isFastClick()) {
//                return;
//            }
//            LogUtil.d( "quickclickApp");
//            mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS);
//        });
//        binding.radioLayout.ivRadioNext.setOnClickListener(v -> {
//            if (ClickUtils.isFastClick()) {
//                return;
//            }
//            mediaControl.sendStrToHost(AwellTool.RADIO.NEXT);
//        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        LogUtil.i("onClick : "+v.getId());
        switch(v.getId()){
            case R.id.hotset_allapp:
                AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
                break;
            case R.id.iv_navi_bg:
            case R.id.hotset_navi:
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
                break;
            case R.id.iv_radio_bg:
                startActivity("com.awell.radio", "com.awell.radio.MainActivity");
                break;
            case R.id.ll_time:
                Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.iv_youtube_bg:
                startActivity("com.google.android.youtube", "com.google.android.youtube.app.honeycomb.Shell$HomeActivity");
                break;
            case R.id.hotset_settings:
                startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
                break;
            case R.id.hotset_camera:
                startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
                break;
            case R.id.hotset_bt_music:
            case R.id.iv_bt_icon:
            case R.id.tv_bt_music:
                try {
                    Intent btIntent = new Intent("com.awell.bluetooth");
                    btIntent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
                    btIntent.putExtra("bt_preference_key", 3);
                    startActivity(btIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e("TAG", "Activity not found: " + e.getMessage());
                    // 可以提示用户安装目标应用
                }
                break;
            case R.id.hotset_bt_phone:
                try {
                    Intent btIntent = new Intent("com.awell.bluetooth");
                    btIntent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
                    btIntent.putExtra("bt_preference_key", 0);
                    startActivity(btIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e("TAG", "Activity not found: " + e.getMessage());
                    // 可以提示用户安装目标应用
                }
                break;
            case R.id.hotset_music:
            case R.id.iv_music_bg:
                startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
                break;
            case R.id.hotset_sound_eq:
                startActivity("com.awell.eqselect", "com.awell.eqselect.MainActivity");
                break;
            case R.id.bt_music_state:
                if (mediaControl.sendStrToHost(AwellTool.BT.GET_STATE).equals("true")) {
                    mediaControl.sendStrToHost(AwellTool.BT.PAUSE);
                    binding.btMusicState.setImageResource(R.drawable.sf_music_bofang_n);
                } else {
                    mediaControl.sendStrToHost(AwellTool.BT.PLAY);
                    binding.btMusicState.setImageResource(R.drawable.sf_music_zanting_n);
                }
                break;
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
                LogUtil.d("packagename11=" + packName);
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

            if ("com.awell.localmusic".equals(pkg) ) {
                musicWidget.switchMediaController(pkg, command, mediaType, currentMedia);
            }else if ((pkg.contains("com.awell.bluetooth") || pkg.contains("/system/bin/gocsdk") && mediaType == AudioManager.STREAM_MUSIC)) {
                //LogUtil.d("updateViewMusicPlay222 ,pkg=" + pkg + " command=" + command + " mediaType=" + mediaType + " currentMedia=" + currentMedia+" bundle="+bundle.toString());
                if ("start".equals(command)){
                    binding.btMusicState.setImageResource(R.drawable.sf_music_zanting_n);
                    binding.btMusicState.setTag(R.drawable.sf_music_zanting_n);
                }else{
                    binding.btMusicState.setImageResource(R.drawable.sf_music_bofang_n);
                    binding.btMusicState.setTag(R.drawable.sf_music_bofang_n);
                }
            }
               // musicWidget.switchMediaController(pkg, command, mediaType, currentMedia);
        }

        @Override
        public void updateViewPlayStatus(@NotNull Bundle bundle, boolean status, int type) {

            if (type == MusicWidget.MUSIC) {
                musicWidget.setCurMusicState(status, type);
            }else if(type == MusicWidget.BT){
                if (status) {
                    binding.btMusicState.setImageResource(R.drawable.sf_music_zanting_n);
                    binding.btMusicState.setTag(R.drawable.sf_music_zanting_n);
                } else {
                    binding.btMusicState.setImageResource(R.drawable.sf_music_bofang_n);
                    binding.btMusicState.setTag(R.drawable.sf_music_bofang_n);
                }
            }

        }

        @Override
        public void updateViewMusicPlayImage(@NotNull Bundle bundle) {
           /* runOnUiThread(() -> {
                String uriStr = bundle.getString(AwellTool.VALUE_M1, null);
                Uri uri = null;
                if (uriStr != null) {
                    Uri parsedUri = Uri.parse(uriStr);
                    if (parsedUri.getScheme() != null) {
                        uri = parsedUri;
                    }
                }
                LogUtil.i("updateViewMusicPlayImage: uri=>" + uri);
                musicWidget.loadAlbumArtByUri(uri);
            });*/

        }

        @Override
        public void updateViewPlayInfo(@NotNull Bundle bundle, @NotNull String songName, @NotNull String singerName, @NotNull String album, int type) {
            if ("NO_MUSIC_LIST".equals(songName)
                    && "NO_MUSIC_LIST".equals(singerName)
                    && "NO_MUSIC_LIST".equals(album)) {
                musicWidget.setMusicNameTextView(getResources().getString(R.string.music), MusicWidget.MUSIC);
               // musicWidget.setArtistNameTextView(getResources().getString(R.string.music_artist), MusicWidget.MUSIC);
                musicWidget.setArtistNameTextView("", MusicWidget.MUSIC);

            }else{
                musicWidget.setMusicNameTextView(songName, type);
                musicWidget.setArtistNameTextView(singerName, type);
                if(type == MusicWidget.BT) {
                    if (binding.btMusicState.getTag() != null && !binding.btMusicState.getTag().equals(R.drawable.sf_music_zanting_n)) {
                        binding.btMusicState.setImageResource(R.drawable.sf_music_zanting_n);
                        binding.btMusicState.setTag(R.drawable.sf_music_zanting_n);
                    }
                }
            }
        }

        @Override
        public void updateViewPlayTime(@NotNull Bundle bundle, long currentTime, long totalTime, int type) {
           // musicWidget.setMusicSeekBar((int) currentTime, (int) totalTime, type);
        }

        @Override
        public void updateViewRadioFreq(@NotNull Bundle bundle, @NotNull String fmOrAm, @NotNull String freq, @NotNull String unit) {
            runOnUiThread(() -> {
                binding.tvRadioFreq.setText(freq);
                binding.tvRadioFreqUnit.setText(unit);
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

    public boolean getNaviApps(String appName) {
        try {
            ApplicationInfo appinfo = this.getPackageManager().getApplicationInfo(appName, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            LogUtil.i("Exception = " + e.toString());
        }
        return false;
    }




}
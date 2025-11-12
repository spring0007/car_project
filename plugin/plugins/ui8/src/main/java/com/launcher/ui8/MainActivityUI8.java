package com.launcher.ui8;

import static com.awell.utils.Utils.startWallpaper;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.utils.CommonData;
import com.launcher.ui8.databinding.ActivityMainUi8Binding;
import com.launcher.ui8.databinding.MusicWidgetBinding;

import org.jetbrains.annotations.NotNull;


public class MainActivityUI8 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI8.class.getSimpleName();
    private ActivityMainUi8Binding binding;
    private MusicWidgetBinding musicWidgetBinding;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private View contentView;
    private final int INITVIEW = 111;
    private boolean accRecor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUi8Binding.inflate(getLayoutInflater());


        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        musicWidgetBinding = binding.layoutMusicWidget;
        musicWidget = musicWidgetBinding.musicWidgetLayout;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);

        setContentView(binding.getRoot());

        initReceiver();

        initLongTouch();

        clickApp();

        handler.removeMessages(INITVIEW);
        handler.sendEmptyMessageDelayed(INITVIEW, 0);
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
                    if (intent.getIntExtra("lamplet_state", 0) == 1)
                        binding.carIv.setImageResource(R.drawable.sf_car_on);
                    else binding.carIv.setImageResource(R.drawable.sf_car_off);
                    break;
                case CommonData.ACTION_ACC_ON:
                    if (binding.carIv != null)
                        binding.carIv.postDelayed(() -> accRecor = false, 8 * 1000);
                    break;
                case CommonData.ACTION_ACC_OFF:
                    accRecor = true;
                    break;
            }
        }
    };
    private void initLongTouch() {
        contentView = findViewById(android.R.id.content);
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.i(TAG, "handlerNew msg.what = " + msg.what);
                switch (msg.what) {
                    case INITVIEW:
                        initCarView();
                        break;
                }
            }
        };
        viewConfiguration = ViewConfiguration.get(this);
    }


    private void initCarView() {

        binding.carIv.postDelayed(() -> accRecor = false, 8 * 1000);

        if (Settings.System.getInt(getContentResolver(), "Headlamp", 0) == 1)
            binding.carIv.setImageResource(R.drawable.sf_car_on);
        else binding.carIv.setImageResource(R.drawable.sf_car_off);

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
        mediaControl.unBindDataService(this);
        AppsCustomizeControl.INSTANCE.hideApps();
    }

    private void clickApp() {
        binding.dateLayout.time.setOnClickListener(this);
        binding.dateLayout.date.setOnClickListener(this);
        binding.dateLayout.weekday.setOnClickListener(this);
        binding.bottomLayout.radioIv.setOnClickListener(this);
        binding.bottomLayout.radioTv.setOnClickListener(this);
        binding.bottomLayout.naviIv.setOnClickListener(this);
        binding.bottomLayout.naviTv.setOnClickListener(this);
        binding.bottomLayout.allIv.setOnClickListener(this);
        binding.bottomLayout.allTv.setOnClickListener(this);
        binding.bottomLayout.phoneIv.setOnClickListener(this);
        binding.bottomLayout.phoneTv.setOnClickListener(this);
        binding.bottomLayout.musicIv.setOnClickListener(this);
        binding.bottomLayout.musicTv.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.bottomLayout.radioIv.getId() || v.getId() == binding.bottomLayout.radioTv.getId()) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
        }else if (v.getId() == binding.bottomLayout.naviIv.getId() || v.getId() == binding.bottomLayout.naviTv.getId()) {
            startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
        } else if (v.getId() == binding.bottomLayout.allIv.getId() || v.getId() == binding.bottomLayout.allTv.getId()) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        } else if (v.getId() == binding.bottomLayout.phoneIv.getId() || v.getId() == binding.bottomLayout.phoneTv.getId()) {
            startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
        } else if (v.getId() == binding.bottomLayout.musicIv.getId() || v.getId() == binding.bottomLayout.musicTv.getId()) {
            startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
        }else if (v.getId() == binding.dateLayout.time.getId() || v.getId() == binding.dateLayout.date.getId() || v.getId() == binding.dateLayout.weekday.getId()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);
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
}
package com.launcher.zy_ui01;

import static com.awell.utils.Utils.startWallpaper;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.launcher.zy_ui01.databinding.ActivityMainUi1Binding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivityUI1 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI1.class.getSimpleName();
    private ActivityMainUi1Binding binding;
    private List<PageData> pageDataList;
    private TextView musicText;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUi1Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.ivNavi.setOnClickListener(this);
        binding.ivHome.setOnClickListener(this);
        binding.ivMusic.setOnClickListener(this);
        binding.ivRadio.setOnClickListener(this);
        binding.ivEq.setOnClickListener(this);
        initLongTouch();

        setUpViewPager();
//        updateTimeZoneByLocale();
        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);
        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(4);
    }

    public void setMusicWidget(MusicWidget musicWidget) {
        Log.d(TAG, "setMusicWidget: musicWidget = " + musicWidget);
        this.musicWidget = musicWidget;

        if (this.musicWidget != null && mediaControl != null) {
            this.musicWidget.setMediaLibrary(mediaControl);
            this.musicWidget.setActivity(this, this.musicWidget);
        }
    }

    public void setMusicText(TextView musicText) {
        this.musicText = musicText;
    }

    public void updateTimeZoneByLocale() {
        // 1. 获取当前系统的 Locale（语言+国家）
        Locale currentLocale = getResources().getConfiguration().locale;
        String country = currentLocale.getCountry(); // 例如 "CN", "US"

        // 2. 根据国家码获取对应的默认时区 ID
        // 注意：一个国家可能有多个时区（如美国），但我们可以取其核心时区
        if ("CN".equalsIgnoreCase(country)) {
            // 中国统一使用上海/北京时区
            setTimeZone("Asia/Shanghai");
        } else if ("US".equalsIgnoreCase(country)) {
            setTimeZone("America/New_York"); // 默认东部时间，或者根据业务逻辑细分
        }
    }

    private void setTimeZone(String timeZoneId) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        try {
            // 需要 android.permission.SET_TIME_ZONE 权限
            am.setTimeZone(timeZoneId);
            Log.i(TAG, "已同步时区至: " + timeZoneId);
        } catch (Exception e) {
            Log.e(TAG, "权限不足或设置失败: " + e.getMessage());
        }
    }

    private void setUpViewPager() {
        pageDataList = new ArrayList<>();
        pageDataList.add(new PageData(R.layout.viewpager_2));
        pageDataList.add(new PageData(R.layout.viewpager_1));
        ViewPagerAdapter adapter = new ViewPagerAdapter(pageDataList, this,this);
        binding.viewpager.setAdapter(adapter);
        binding.viewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
            }
        });
        updateIndicators(0);
    }


    private void updateIndicators(int position) {
        // 更新第一个指示点
        if (position == 0) {
            binding.indicator1.setBackgroundResource(R.drawable.indicator_selected);
            binding.indicator2.setBackgroundResource(R.drawable.indicator_unselect);
        } else {
            binding.indicator1.setBackgroundResource(R.drawable.indicator_unselect);
            binding.indicator2.setBackgroundResource(R.drawable.indicator_selected);

        }
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
        AppsCustomizeControl.INSTANCE.setActivity(null);
        AppsCustomizeControl.INSTANCE.hideApps();
        mediaControl.unBindDataService(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_music || v.getId() == R.id.layout_music){
            startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
        } else if (v.getId() == R.id.iv_navi) {
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
        } else if (v.getId() == R.id.iv_time) {
            Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (v.getId() == R.id.iv_home) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        } else if (v.getId() == R.id.iv_radio) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
        } else if (v.getId() == R.id.iv_interconnect || v.getId() == R.id.iv_bluetooth) {
            startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
        } else if (v.getId() == R.id.iv_video) {
            startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
        } else if (v.getId() == R.id.iv_album) {
            startActivity("com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");
        } else if (v.getId() == R.id.iv_eq) {
            startActivity("com.awell.dspeffect", "com.awell.dspeffect.DspActivity");
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
            if (musicText != null) {
                if (!TextUtils.isEmpty(songName)) {
                    musicText.setText(songName);
                } else {
                    musicText.setText(getResources().getString(R.string.click_play_music));
                }
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
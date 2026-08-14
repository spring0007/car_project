package com.launcher.yfd_ui5;

import static com.awell.utils.Utils.startWallpaper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.awell.control.AppsCustomizeConfig;
import com.awell.control.AppsCustomizeControl;
import com.awell.control.AwellMediaControl;
import com.awell.launcher2.IconCache;
import com.awell.library.AwellTool;
import com.awell.library.util.ClickUtils;
import com.awell.library.util.PageData;
import com.awell.utils.LogUtil;
import com.launcher.yfd_ui5.databinding.ActivityMainUi5Binding;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class MainActivityUI5 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI5.class.getSimpleName();
    private ActivityMainUi5Binding binding;
    private List<PageData> pageDataList;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;
    private static final String BTSTATUS = "awell_bt_status";
    private ImageView btNoIv;
    private ContentObserver btStatusObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUi5Binding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        initView();
        initLongTouch();

        setUpViewPager();

        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        // 注册蓝牙状态监听器
        registerBtStatusObserver();

        AppsCustomizeControl.INSTANCE.setActivity(this);
        SharedPreferences sharedPreferences = getSharedPreferences("styleMode", MODE_PRIVATE);
        int styleMode = sharedPreferences.getInt("styleMode", 1);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(new AppsCustomizeConfig.Builder().setThemeMode(styleMode).build());

    }

    private void initView() {
        binding.themeLayout.setOnClickListener(this);
    }

    public void setMusicWidget(MusicWidget musicWidget) {
        Log.d(TAG, "setMusicWidget: musicWidget = " + musicWidget);
        this.musicWidget = musicWidget;

        if (this.musicWidget != null && mediaControl != null) {
            this.musicWidget.setMediaLibrary(mediaControl);
            this.musicWidget.setActivity(this, this.musicWidget);
        }
    }
    private void setUpViewPager() {
        pageDataList = new ArrayList<>();
        pageDataList.add(new PageData(R.layout.viewpager_1));
        pageDataList.add(new PageData(R.layout.viewpager_2));
        ViewPagerAdapter adapter = new ViewPagerAdapter(pageDataList, this);
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
            binding.indicator1.setImageResource(R.drawable.indicato_sel);
            binding.indicator2.setImageResource(R.drawable.indicato_n);
        } else {
            binding.indicator2.setImageResource(R.drawable.indicato_sel);
            binding.indicator1.setImageResource(R.drawable.indicato_n);
        }
    }


    private void initLongTouch() {
        handler = new Handler(Looper.getMainLooper());
        viewConfiguration = ViewConfiguration.get(this);
    }

    /**
     * 注册蓝牙状态观察者
     */
    private void registerBtStatusObserver() {
        btStatusObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                Log.d(TAG, "Bluetooth status changed");
                setBtstatus();
            }
        };
        
        getContentResolver().registerContentObserver(
            Settings.System.getUriFor(BTSTATUS),
            false,
            btStatusObserver
        );
    }

    /**
     * 注销蓝牙状态观察者
     */
    private void unregisterBtStatusObserver() {
        if (btStatusObserver != null) {
            getContentResolver().unregisterContentObserver(btStatusObserver);
            btStatusObserver = null;
        }
    }

    public void setBtstatus(){
        // flag 0 : BT close   1 : BT NoConnected    2 : BT Connected
        int mBTStatus = Settings.System.getInt(getContentResolver(), BTSTATUS, 0);
        Log.d(TAG, " mBTStatus = " + mBTStatus);
        TextView btPhoneSubTv = findViewById(R.id.bt_phone_sub_tv);
        if (btPhoneSubTv != null) {
            Log.d(TAG, " mBTStatus ,btPhoneSubTv");
            if (mBTStatus == 0 || mBTStatus == 1){
                btPhoneSubTv.setText(R.string.no_phone_connected);
            }else if (mBTStatus == 2){
                btPhoneSubTv.setText(R.string.phone_connected);
            }
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
    protected void onResume() {
        super.onResume();
        setBtstatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //setBtstatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销蓝牙状态观察者
        unregisterBtStatusObserver();
        AppsCustomizeControl.INSTANCE.setActivity(null);
        AppsCustomizeControl.INSTANCE.hideApps();
    }


    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick: " + v.getId());
        /*if (v.getId() == R.id.music_iv){
            startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
        } else*/
        if (v.getId() == R.id.nav_icon) {
            String navPkg = SystemProperties.get("persist.sys.navi.packagename", "");
            //String navClazz = SystemProperties.get("persist.sys.navi.clazzname", "");
            if (TextUtils.isEmpty(navPkg) || !getNaviApps(navPkg))
                startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
            else {
                Intent intent = this.getPackageManager().getLaunchIntentForPackage(navPkg);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
                }
            }
        } else if (v.getId() == R.id.nav_setting_iv) {
            Intent mIntent = new Intent();
            mIntent.setPackage("com.awell.carsetting");
            mIntent.setComponent(new ComponentName("com.awell.carsetting", "com.awell.carsetting.MainActivity"));
            mIntent.putExtra("SelectDefaultId", 8);
            mIntent.putExtra("SelectDefaultFragment", 0);
            startActivityIntent(mIntent);
            //}else if (v.getId() == R.id.nav_app_iv) {

        } else if (v.getId() == R.id.theme_layout) {
            startActivity("com.awell.themesetting", "com.awell.themesetting.MainActivity");
        } else if (v.getId() == R.id.phone_book_iv || v.getId() == R.id.contact_iv) {
            // 打开电话簿，跳转到车机设置的电话簿页面
            Intent mIntent = new Intent();
            mIntent.setPackage("com.awell.bluetooth");
            mIntent.setComponent(new ComponentName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity"));

            if (v.getId() == R.id.phone_book_iv)
                mIntent.putExtra("bt_preference_key", 0);
            else
                mIntent.putExtra("bt_preference_key", 1);
            startActivityIntent(mIntent);

        } else if (v.getId() == R.id.iv_bt_phone_icon) {
            startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
        } else if (v.getId() == R.id.settings_icon) {
            startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
        } else if (v.getId() == R.id.setting_general_iv) {

            Intent mIntent = new Intent();
            mIntent.setPackage("com.awell.carsetting");
            mIntent.setComponent(new ComponentName("com.awell.carsetting", "com.awell.carsetting.MainActivity"));
            mIntent.putExtra("SelectDefaultId", 3);
            //mIntent.putExtra("SelectDefaultFragment", 30);
            startActivityIntent(mIntent);
        } else if (v.getId() == R.id.setting_sound_iv) {

            Intent mIntent = new Intent();
            mIntent.setPackage("com.awell.carsetting");
            mIntent.setComponent(new ComponentName("com.awell.carsetting", "com.awell.carsetting.MainActivity"));
            mIntent.putExtra("SelectDefaultId", 2);
            //mIntent.putExtra("SelectDefaultFragment", 30);
            startActivityIntent(mIntent);

        } else if (v.getId() == R.id.iv_video_icon) {
            startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
        } else if (v.getId() == R.id.video_file_iv || v.getId() == R.id.video_search_iv) {
            Intent mIntent = new Intent();
            mIntent.setPackage("com.awell.localvideo");
            mIntent.setComponent(new ComponentName("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity"));
            if (v.getId() == R.id.video_search_iv)
                mIntent.putExtra("default_frament", 3);
            else
                mIntent.putExtra("default_frament", 0);
            startActivityIntent(mIntent);
        }else if(v.getId() == R.id.iv_app_icon){
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        }else if(v.getId() == R.id.app_iv ){
            if (ClickUtils.isFastClick()) {
                return;
            }
            SharedPreferences sharedPreferences = getSharedPreferences("styleMode", MODE_PRIVATE);
            int styleMode = sharedPreferences.getInt("styleMode", 1);
            if(styleMode >= 3){
                styleMode = 1;
            }else{
                styleMode++;
            }
            sharedPreferences.edit().putInt("styleMode", styleMode).apply();
            AppsCustomizeControl.INSTANCE.setPluginThemeMode(new AppsCustomizeConfig.Builder().setThemeMode(styleMode).build());
            Toast.makeText(this, "应用图标已切换成功", Toast.LENGTH_SHORT).show();
        }else if (v.getId() == R.id.iv_radio_icon) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
        }else if (v.getId() == R.id.iv_file_icon) {
            startActivity("com.mediatek.filemanager", "com.mediatek.filemanager.FileManagerOperationActivity");
       // }else if (v.getId() == R.id.file_searchs) {
        //    startActivity("com.mediatek.filemanager", "com.mediatek.filemanager.FileManagerSearchActivity");
        }else if (v.getId() == R.id.file_gallery) {
            startActivity("com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");
        }else if (v.getId() == R.id.radio_pre) {
            if (ClickUtils.isFastClick()) {
                return;
            }
            mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS);
        }else if (v.getId() == R.id.radio_next){
            if (ClickUtils.isFastClick()) {
                return;
            }
            mediaControl.sendStrToHost(AwellTool.RADIO.NEXT);
        /*}else if (v.getId() == R.id.time_iv) {
                Log.d(TAG, "onClick: " + v.getId());
            Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

             Intent mIntent = new Intent();
            mIntent.setPackage("com.awell.carsetting");
            mIntent.setComponent(new ComponentName("com.awell.carsetting", "com.awell.carsetting.MainActivity"));
            mIntent.putExtra("SelectDefaultId", 3);
            mIntent.putExtra("SelectDefaultFragment", 30);
            startActivityIntent(mIntent);

            */
        }
    }

    /**
     * 启动Activity并处理启动标记
     *
     * @param intent Intent对象
     */
    private void startActivityIntent(Intent intent) {
        LogUtil.i("startActivityIntent");
        String packName = intent.getPackage();
        boolean isboot = true;
        
        // 检查是否为工作区应用
        for (int index = 0; index < IconCache.WorkSpacePackageName.length; index++) {
            if (packName == null || !packName.equals(IconCache.WorkSpacePackageName[index])) {
                isboot = false;
                break;
            }
        }
        
        // 根据包名设置不同的启动标记
        if (packName != null && packName.contains("com.autonavi")) {
            if (isboot) {
                Settings.System.putString(getContentResolver(), "boot_apk1", packName);
            }
        } else {
            if (isboot) {
                Settings.System.putString(getContentResolver(), "boot_apk2", packName);
            }
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
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
                musicWidget.setMusicNameTextView(getResources().getString(R.string.unknown_song), MusicWidget.MUSIC);
                musicWidget.setArtistNameTextView(getResources().getString(R.string.music_artist), MusicWidget.MUSIC);
            }

            if (MusicWidget.OTHER_MUSIC == type) {
                if (!TextUtils.isEmpty(songName)) {
                    musicWidget.setMusicNameTextView(songName, MusicWidget.OTHER_MUSIC);
                } else {
                    musicWidget.setMusicNameTextView(getResources().getString(R.string.unknown_song), MusicWidget.OTHER_MUSIC);
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
                ((TextView)findViewById(R.id.radio_sub_tv)).setText(String.format("%s %s%s", fmOrAm, freq, unit));
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
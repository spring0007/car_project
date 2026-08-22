package com.launcher.zy_ui04;

import static com.awell.utils.Utils.startWallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import android.widget.ImageView;
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
import com.launcher.zy_ui04.databinding.ActivityMainBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class MainActivityUI4 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI4.class.getSimpleName();
    private ActivityMainBinding binding;
    private List<PageData> pageDataList;
    private MusicWidget musicWidget;
    private AwellMediaControl mediaControl;

    // 页面组常量
    private static final int PAGE_GROUP_DEFAULT = 0;  // 默认组（viewpager_1, viewpager_2）
    private static final int PAGE_GROUP_SWITCHED = 1; // 切换组（viewpager_5）
    private static final String PREFS_NAME = "zy_ui04_page_group";
    private static final String KEY_PAGE_GROUP = "page_group";
    private int currentPageGroup = PAGE_GROUP_DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        mediaControl = new AwellMediaControl();
        mediaControl.bindDataService(this);
        mediaControl.setUpdateMusicView(mediaImpl);

        musicWidget = binding.layoutMusicWidget.layoutMusicWidget;
        musicWidget.setMediaLibrary(mediaControl);
        musicWidget.setActivity(this, musicWidget);
        mediaControl.refreshCurrentMediaState();

        setContentView(binding.getRoot());
        initLongTouch();

        // 读取保存的页面组状态
        loadPageGroupState();
        setUpViewPager();

        AppsCustomizeControl.INSTANCE.setActivity(this);

        // 设置主题模式,并同步到 IconManager
        int  iconSize = getResources().getDimensionPixelSize(R.dimen.cell_icon_size); //126
        int fontSize = getResources().getDimensionPixelSize(R.dimen.cell_font_size);
        int cellWidth = getResources().getDimensionPixelSize(R.dimen.cell_width);

        AppsCustomizeConfig config = new AppsCustomizeConfig.Builder()
                .setThemeMode(0xff)
                .setRefresh(false)
                .setIconMap(IconManager.INSTANCE.getPackageIconMap())
                .setPluginPackageName("com.launcher.zy_ui04")
                .setColumnCount(5)
                .setRowCount(2)
                .setFontSizeSp(fontSize)
                .setAutoHeightGap(true)
                .setCellWidthDp(cellWidth)
                //.setCellWidthDp(iconWidth)
                //.setCellHeightDp(iconHeight)
                .setIconSize(iconSize).build();

        AppsCustomizeControl.INSTANCE.setPluginThemeMode(config);

    }

    /**
     * 加载页面组状态
     */
    private void loadPageGroupState() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentPageGroup = sharedPreferences.getInt(KEY_PAGE_GROUP, PAGE_GROUP_DEFAULT);
        Log.d(TAG, "Loaded page group state: " + currentPageGroup);
    }

    /**
     * 保存页面组状态
     */
    private void savePageGroupState(int pageGroup) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sharedPreferences.edit().putInt(KEY_PAGE_GROUP, pageGroup).apply();
        Log.d(TAG, "Saved page group state: " + pageGroup);
    }

    /**
     * 设置ViewPager
     */
    private void setUpViewPager() {
        pageDataList = new ArrayList<>();

        // 根据当前页面组加载不同的页面配置
        if (currentPageGroup == PAGE_GROUP_DEFAULT) {
            // 默认组：包含 viewpager_5
            pageDataList.add(new PageData(R.layout.viewpager_5));
            pageDataList.add(new PageData(R.layout.viewpager_6));

        } else {
            // 切换组：包含 viewpager_1 和 viewpager_2
            pageDataList.add(new PageData(R.layout.viewpager_1));
            pageDataList.add(new PageData(R.layout.viewpager_2));
            pageDataList.add(new PageData(R.layout.viewpager_3));
            pageDataList.add(new PageData(R.layout.viewpager_4));
        }

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


    /**
     * 更新指示器状态
     */
    private void updateIndicators(int position) {
        // 根据当前页面组的页面数量更新指示器
        int pageCount = pageDataList.size();

        // 获取所有指示器数组，方便统一管理
        ImageView[] indicators = {
                binding.indicator1,
                binding.indicator2,
                binding.indicator3,
                binding.indicator4
        };

        // 隐藏所有指示器
        for (ImageView indicator : indicators) {
            indicator.setVisibility(View.GONE);
        }

        // 根据页面数量显示相应的指示器
        for (int i = 0; i < pageCount && i < indicators.length; i++) {
            indicators[i].setVisibility(View.VISIBLE);
        }

        // 更新选中状态：将当前位置的指示器设为选中，其他设为未选中
        for (int i = 0; i < pageCount && i < indicators.length; i++) {
            if (i == position) {
                // 当前页面对应的指示器设为选中状态
                indicators[i].setImageResource(R.drawable.indicato_sel);
            } else {
                // 其他指示器设为未选中状态
                indicators[i].setImageResource(R.drawable.indicato_n);
            }
        }

        Log.d(TAG, "updateIndicators: position=" + position + ", pageCount=" + pageCount);
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
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {

        Log.i(TAG, "onClick: " + v.getId());

        switch (v.getId()) {
            case R.id.iv_music_big:
            case R.id.tv_music_big:
            case R.id.iv_music_small:
            case R.id.tv_music_small:
                startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
                break;

            case R.id.iv_video_big:
            case R.id.tv_video_big:
            case R.id.iv_video_small:
            case R.id.tv_video_small:
                startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
                break;
            case R.id.iv_radio_big:
            case R.id.tv_radio_big:
            case R.id.iv_radio_small:
            case R.id.tv_radio_small:
                startActivity("com.awell.radio", "com.awell.radio.MainActivity");
                break;
            case R.id.iv_btphone_big:
            case R.id.tv_btphone_big:
            case R.id.iv_btphone_small:
            case R.id.tv_btphone_small:
            case R.id.host_bt_phone_iv:
            case R.id.host_bt_phone_tv:
                Intent mIntent = new Intent();
                mIntent.setPackage("com.awell.bluetooth");
                mIntent.setComponent(new ComponentName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity"));
                mIntent.putExtra("bt_preference_key", 0);
                startActivityIntent(mIntent);
                break;

            case R.id.iv_setting_big:
            case R.id.tv_setting_big:
            case R.id.iv_setting_small:
            case R.id.tv_setting_small:
                startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
                break;

            case R.id.iv_btmusic_big:
            case R.id.tv_btmusic_big:
            case R.id.iv_btmusic_small:
            case R.id.tv_btmusic_small:
                Intent intent = new Intent();
                intent.setPackage("com.awell.bluetooth");
                intent.setComponent(new ComponentName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity"));
                intent.putExtra("bt_preference_key", 3);
                startActivityIntent(intent);
                break;

            case R.id.iv_gallery_big:
            case R.id.tv_gallery_big:
            case R.id.iv_gallery_small:
            case R.id.tv_gallery_small:
                startActivity("com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity");
                break;
            case R.id.iv_filemanager_big:
            case R.id.tv_filemanager_big:
            case R.id.iv_filemanager_small:
            case R.id.tv_filemanager_small:
                startActivity("com.mediatek.filemanager", "com.mediatek.filemanager.FileManagerOperationActivity");
                break;
            case R.id.iv_chrome_big:
            case R.id.tv_chrome_big:
            case R.id.iv_chrome_small:
            case R.id.tv_chrome_small:
                String url = SystemProperties.get("persist.sys.lz.HOME_PAGE", "http://m.baidu.com");
                Intent chromeIntent = new Intent(Intent.ACTION_VIEW);
                chromeIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                chromeIntent.setData(Uri.parse(url));
                // 检查是否有应用可以处理此Intent
                if (chromeIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(chromeIntent);
                }
                break;
            case R.id.iv_audio_input_big:
            case R.id.tv_audio_input_big:
            case R.id.iv_audio_input_small:
            case R.id.tv_audio_input_small:
                startActivity("com.awell.eqselect", "com.awell.eqselect.MainActivity");
                break;
            case R.id.tv_video_input_big:
            case R.id.iv_video_input_big:
            case R.id.tv_video_input_small:
            case R.id.iv_video_input_small:
                startActivity("com.awell.backcar", "com.awell.backcar.MainActivity");
                break;
            case R.id.iv_easy_connect_big:
            case R.id.iv_easy_connect_small:
            case R.id.tv_easy_connect_big:
            case R.id.tv_easy_connect_small:
                startActivity("com.zjinnova.zlink","com.zjinnova.android.zlink.features.main.MainActivity");

                break;
            case R.id.host_nav_iv:
            case R.id.host_nav_tv: {
                String navPkg = SystemProperties.get("persist.sys.navi.packagename", "");
                //String navClazz = SystemProperties.get("persist.sys.navi.clazzname", "");
                if (TextUtils.isEmpty(navPkg) || !getNaviApps(navPkg))
                    startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
                else {
                    Intent intent2 = getPackageManager().getLaunchIntentForPackage(navPkg);
                    if (intent2 != null) {
                        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent2);
                    } else {
                        startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
                    }
                }
                break;
            }
            case R.id.host_apps_iv:
            case R.id.host_apps_tv:
                AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
                break;
            case R.id.host_switch_iv:
            case R.id.host_switch_tv: {
                if (ClickUtils.isFastClick()) {
                    return;
                }
                // 切换页面组
                switchPageGroup();
                break;
            }
            case R.id.left_pointer_iv:
                if (ClickUtils.isFastClick()) {
                    return;
                }
                // 切换到上一页
                int currentItem = binding.viewpager.getCurrentItem();
                if (currentItem > 0) {
                    binding.viewpager.setCurrentItem(currentItem - 1, true);
                    Log.d(TAG, "Switch to previous page: " + (currentItem - 1));
                } else {
                    Log.d(TAG, "Already at first page, cannot go previous");
                }
                break;
            case R.id.right_pointer_iv:
                if (ClickUtils.isFastClick()) {
                    return;
                }
                // 切换到下一页
                int currentPage = binding.viewpager.getCurrentItem();
                int totalPages = pageDataList != null ? pageDataList.size() : 0;
                if (currentPage < totalPages - 1) {
                    binding.viewpager.setCurrentItem(currentPage + 1, true);
                    Log.d(TAG, "Switch to next page: " + (currentPage + 1));
                } else {
                    Log.d(TAG, "Already at last page, cannot go next");
                }
                break;
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
               // musicWidget.setArtistNameTextView(getResources().getString(R.string.music_artist), MusicWidget.MUSIC);
            }

            if (MusicWidget.OTHER_MUSIC == type) {
                if (!TextUtils.isEmpty(songName)) {
                    musicWidget.setMusicNameTextView(songName, MusicWidget.OTHER_MUSIC);
                } else {
                    musicWidget.setMusicNameTextView(getResources().getString(R.string.unknown_song), MusicWidget.OTHER_MUSIC);
                }
               /* if (!TextUtils.isEmpty(singerName)) {
                    musicWidget.setArtistNameTextView(singerName, MusicWidget.OTHER_MUSIC);
                } else {
                    musicWidget.setArtistNameTextView(getResources().getString(R.string.music_artist), MusicWidget.OTHER_MUSIC);
                }*/
            }
        }

        @Override
        public void updateViewPlayTime(@NotNull Bundle bundle, long currentTime, long totalTime, int type) {
            musicWidget.setMusicSeekBar((int) currentTime, (int) totalTime, type);
        }

        @Override
        public void updateViewRadioFreq(@NotNull Bundle bundle, @NotNull String fmOrAm, @NotNull String freq, @NotNull String unit) {
            runOnUiThread(() -> {
                // ((TextView)findViewById(R.id.radio_sub_tv)).setText(String.format("%s %s%s", fmOrAm, freq, unit));
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

    /**
     * 切换页面组
     */
    private void switchPageGroup() {
        // 切换页面组状态
        currentPageGroup = (currentPageGroup == PAGE_GROUP_DEFAULT) ? PAGE_GROUP_SWITCHED : PAGE_GROUP_DEFAULT;

        // 保存状态
        savePageGroupState(currentPageGroup);

        // 重新设置ViewPager
        setUpViewPager();

        // 重置到第一页
        binding.viewpager.setCurrentItem(0, false);

        // 更新指示器
        updateIndicators(0);

//        // 显示提示信息
//        String message = (currentPageGroup == PAGE_GROUP_DEFAULT) ?
//                "已切换到默认布局" : "已切换到切换布局";
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//
//        Log.d(TAG, "Switched to page group: " + currentPageGroup);
    }

    public boolean getNaviApps(String appName) {
        try {
            ApplicationInfo appinfo = this.getPackageManager().getApplicationInfo(appName, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            Log.i(TAG, "Exception = " + e.toString());
        }
        return false;
    }
}
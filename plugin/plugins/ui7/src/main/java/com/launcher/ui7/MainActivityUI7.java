package com.launcher.ui7;

import static com.awell.utils.Utils.startWallpaper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.viewpager2.widget.ViewPager2;

import com.awell.control.AppsCustomizeControl;
import com.awell.launcher2.IconCache;
import com.launcher.ui7.databinding.ActivityMainUi7Binding;

import java.util.ArrayList;
import java.util.List;


public class MainActivityUI7 extends Activity implements View.OnClickListener {
    private final String TAG = MainActivityUI7.class.getSimpleName();
    private ActivityMainUi7Binding binding;
    private List<PageData> pageDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUi7Binding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        initLongTouch();

        setUpViewPager();

        binding.timeLayout.dateLayout.setOnClickListener(this);
        AppsCustomizeControl.INSTANCE.setActivity(this);
        AppsCustomizeControl.INSTANCE.setPluginThemeMode(2);

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

        // 设置ViewPager2滑动更灵敏
        setViewPager2Sensitivity(binding.viewpager);

        // 添加翻页动画效果
        // binding.viewpager.setPageTransformer(new DepthPageTransformer()); // 使用深度动画
        // 或使用缩放动画
         binding.viewpager.setPageTransformer(new ZoomOutPageTransformer());
        // 或使用旋转动画
        // binding.viewpager.setPageTransformer(new RotatePageTransformer());
        // 或使用淡入淡出动画
        //binding.viewpager.setPageTransformer(new FadePageTransformer());
        // 或使用立方体动画
       // binding.viewpager.setPageTransformer(new CubePageTransformer());
    }

    /**
     * 设置ViewPager2滑动灵敏度
     * @param viewPager2 ViewPager2实例
     */
    private void setViewPager2Sensitivity(ViewPager2 viewPager2) {
        try {
            // 1. 获取ViewPager2内部的RecyclerView
            java.lang.reflect.Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
            recyclerViewField.setAccessible(true);
            androidx.recyclerview.widget.RecyclerView recyclerView = (androidx.recyclerview.widget.RecyclerView) recyclerViewField.get(viewPager2);

            // 2. 设置滑动阈值更小，使滑动更灵敏
            java.lang.reflect.Field mTouchSlopField = androidx.recyclerview.widget.RecyclerView.class.getDeclaredField("mTouchSlop");
            mTouchSlopField.setAccessible(true);
            // 将默认的滑动阈值减小一半
            int originalTouchSlop = (int) mTouchSlopField.get(recyclerView);
            mTouchSlopField.set(recyclerView, originalTouchSlop /3);

            // 3. 设置fling速度限制，使滑动更灵敏
            setFlingFactor(recyclerView, 0.3f); // 减小fling系数，使滑动更灵敏

            Log.d(TAG, "ViewPager2 sensitivity set to more sensitive");
        } catch (Exception e) {
            Log.e(TAG, "Error setting ViewPager2 sensitivity: " + e.getMessage());
        }
    }

    /**
     * 设置RecyclerView的fling速度系数
     * @param recyclerView RecyclerView实例
     * @param factor 系数，小于1使滑动更灵敏
     */
    private void setFlingFactor(androidx.recyclerview.widget.RecyclerView recyclerView, float factor) {
        try {
            // 设置最大fling速度
            java.lang.reflect.Field flingField = androidx.recyclerview.widget.RecyclerView.class.getDeclaredField("mMaxFlingVelocity");
            flingField.setAccessible(true);
            int originalMaxFling = (int) flingField.get(recyclerView);
            flingField.set(recyclerView, (int) (originalMaxFling * factor));

            // 同时调整最小fling速度
            java.lang.reflect.Field minFlingField = androidx.recyclerview.widget.RecyclerView.class.getDeclaredField("mMinFlingVelocity");
            minFlingField.setAccessible(true);
            int originalMinFling = (int) minFlingField.get(recyclerView);
            minFlingField.set(recyclerView, (int) (originalMinFling * factor));
        } catch (Exception e) {
            Log.e(TAG, "Error setting fling factor: " + e.getMessage());
        }
    }


    private void updateIndicators(int position) {
        // 更新第一个指示点
        if (position == 0) {
            binding.indicator1.setBackgroundResource(R.drawable.indicator_selected);
            binding.indicator2.setBackgroundResource(R.drawable.indicator_unselect);
        } else {
            binding.indicator2.setBackgroundResource(R.drawable.indicator_selected);
            binding.indicator1.setBackgroundResource(R.drawable.indicator_unselect);
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
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.music_iv){
            startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity");
        } else if (v.getId() == R.id.navi_iv) {
            startActivity("com.awell.navigation", "com.awell.navigation.MainActivity");
        }else if (v.getId() == R.id.all_iv) {
            AppsCustomizeControl.INSTANCE.showApps(findViewById(android.R.id.content));
        }else if (v.getId() == R.id.radio_iv) {
            startActivity("com.awell.radio", "com.awell.radio.MainActivity");
        }else if (v.getId() == R.id.phone_iv) {
            startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity");
        }else if (v.getId() == R.id.aux_iv) {
            startActivity("com.awell.backcar", "com.awell.backcar.MainActivity");
        } else if (v.getId() == R.id.browser_iv) {
            startActivity("com.android.chrome", "com.google.android.apps.chrome.Main");
        }else if (v.getId() == R.id.setting_iv) {
            startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity");
        }else if (v.getId() == R.id.dsp_iv) {
            startActivity("com.awell.eqselect", "com.awell.eqselect.MainActivity");
        }else if (v.getId() == R.id.video_iv) {
            startActivity("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity");
        }else if (v.getId() == R.id.time_layout || v.getId() == R.id.date_layout) {
                Log.d(TAG, "onClick: " + v.getId());
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

    // 深度动画效果
    private static class DepthPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        @Override
        public void transformPage(View page, float position) {
            int pageWidth = page.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // 页面已经在屏幕左侧完全不可见
                page.setAlpha(0f);
            } else if (position <= 0) { // [-1,0]
                // 页面从左侧进入或向左侧离开
                page.setAlpha(1f);
                page.setTranslationX(0f);
                page.setScaleX(1f);
                page.setScaleY(1f);
            } else if (position <= 1) { // (0,1]
                // 页面从右侧进入或向右侧离开
                // 淡出效果
                page.setAlpha(1 - position);
                // 向左滑动
                page.setTranslationX(pageWidth * -position);
                // 缩小效果
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);
            } else { // (1,+Infinity]
                // 页面已经在屏幕右侧完全不可见
                page.setAlpha(0f);
            }
        }
    }

    // 缩放淡出动画效果
    private static class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        @Override
        public void transformPage(View page, float position) {
            int pageWidth = page.getWidth();
            int pageHeight = page.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // 页面已经在屏幕左侧完全不可见
                page.setAlpha(0f);
            } else if (position <= 1) { // [-1,1]
                // 计算缩放和透明度
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float verticalMargin = pageHeight * (1 - scaleFactor) / 2;
                float horizontalMargin = pageWidth * (1 - scaleFactor) / 2;

                if (position < 0) {
                    page.setTranslationX(horizontalMargin - verticalMargin / 2);
                } else {
                    page.setTranslationX(-horizontalMargin + verticalMargin / 2);
                }

                // 应用缩放
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);

                // 应用透明度
                page.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            } else { // (1,+Infinity]
                // 页面已经在屏幕右侧完全不可见
                page.setAlpha(0f);
            }
        }
    }

    // 旋转动画效果
    private static class RotatePageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            int pageWidth = page.getWidth();
            int pageHeight = page.getHeight();
            int rotation = (int) (position * 30); // 旋转30度

            if (position > 1) {
                // 页面在右侧
                page.setRotation(30);
                page.setPivotX(pageWidth);
                page.setPivotY(pageHeight / 2);
                page.setAlpha(0f);
            } else if (position < -1) {
                // 页面在左侧
                page.setRotation(-30);
                page.setPivotX(0);
                page.setPivotY(pageHeight / 2);
                page.setAlpha(0f);
            } else {
                // 页面在屏幕上
                page.setRotation(rotation);
                page.setPivotX(pageWidth / 2);
                page.setPivotY(pageHeight / 2);
                page.setAlpha(1f);
            }
        }
    }

    // 淡入淡出动画效果
    private static class FadePageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            page.setAlpha(1 - Math.abs(position));
        }
    }

    // 立方体动画效果
    private static class CubePageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            int pageWidth = page.getWidth();

            if (position < -1) { // [-Infinity,-1)
                page.setAlpha(0f);
            } else if (position <= 0) { // [-1,0]
                page.setAlpha(1f);
                page.setPivotX(pageWidth);
                page.setRotationY(90 * Math.abs(position));
            } else if (position <= 1) { // (0,1]
                page.setAlpha(1f);
                page.setPivotX(0);
                page.setRotationY(-90 * Math.abs(position));
            } else { // (1,+Infinity]
                page.setAlpha(0f);
            }
        }
    }
}
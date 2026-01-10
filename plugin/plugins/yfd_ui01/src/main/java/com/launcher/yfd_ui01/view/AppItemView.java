package com.launcher.yfd_ui01.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.launcher.yfd_ui01.R;
import com.launcher.yfd_ui01.utils.LogUtil;

public class AppItemView extends RelativeLayout implements View.OnTouchListener {
    private static final String TAG = "AppItemView";
    
    private ImageView appIcon;
    private TextView appName;
    private ImageView appBg;


    public AppItemView(Context context) {
        super(context);
        init(context, null);
    }

    public AppItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AppItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//        // 获取测量模式和尺寸
//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
//
//        LogUtil.d( "onMeasure - 宽度模式: " + getModeString(widthMode) +
//                    ", 尺寸: " + widthSize +
//                    ", 高度模式: " + getModeString(heightMode) +
//                    ", 尺寸: " + heightSize);
//
//        // 如果布局参数是wrap_content，设置默认尺寸
//        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
//            // 转换为像素
//            int width = dpToPx(defaultWidth);
//            int height = dpToPx(defaultHeight);
//            setMeasuredDimension(width, height);
//            LogUtil.d( "设置默认尺寸: " + width + "x" + height);
//        }
//    }
    
//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        super.onLayout(changed, l, t, r, b);
//        LogUtil.d( "onLayout - 位置: [" + l + ", " + t + ", " + r + ", " + b + "]");
//    }
//
//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        super.onSizeChanged(w, h, oldw, oldh);
//        LogUtil.d( "onSizeChanged - 新尺寸: " + w + "x" + h);
//    }

    
//    private int dpToPx(int dp) {
//        return (int) (dp * getResources().getDisplayMetrics().density);
//    }

    private void init(Context context, AttributeSet attrs) {
        try {
            // 加载布局
            LayoutInflater.from(context).inflate(R.layout.app_item_layout, this, true);
            
            // 查找子View
            appIcon = (ImageView) findViewById(R.id.app_item_icon);
            appName = (TextView) findViewById(R.id.app_item_name);
            appBg = (ImageView) findViewById(R.id.app_item_bg);

            // 处理自定义属性
            if (attrs != null) {
                TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AppItemView);

                // 设置图标
                Drawable iconDrawable = typedArray.getDrawable(R.styleable.AppItemView_appIcon);
                if (iconDrawable != null && appIcon != null) {
                    appIcon.setImageDrawable(iconDrawable);
                }

                // 设置名称
                String name = typedArray.getString(R.styleable.AppItemView_appName);
                if (name != null && appName != null) {
                    appName.setText(name);
                }

                // 设置背景，只有appBg不为null时才设置
                Drawable bgDrawable = typedArray.getDrawable(R.styleable.AppItemView_appBg);
                if (bgDrawable != null) {
                    if (appBg != null) {
                        appBg.setBackground(bgDrawable);
                    } else {
                        LogUtil.e( "appBg is null, cannot set background drawable");
                    }
                }

                typedArray.recycle();
            }
        } catch (Exception e) {
            LogUtil.e("AppItemView初始化失败", e);
        }
    }

    // 设置应用图标
    public void setAppIcon(Drawable drawable) {
        if (appIcon != null) {
            appIcon.setImageDrawable(drawable);
        } else {
            LogUtil.e( "setAppIcon: appIcon为null");
        }
    }

    public void setAppIcon(int resId) {
        if (appIcon != null) {
            appIcon.setImageResource(resId);
        } else {
            LogUtil.e("setAppIcon: appIcon为null, resId: " + resId);
        }
    }

    // 设置应用名称
    public void setAppName(String name) {
        if (appName != null) {
            appName.setText(name);
        } else {
            LogUtil.e("setAppName: appName为null, name: " + name);
        }
    }

    public void setAppName(int resId) {
        if (appName != null) {
            appName.setText(resId);
        } else {
            LogUtil.e( "setAppName: appName为null, resId: " + resId);
        }
    }

    // 设置背景
    public void setAppBg(Drawable drawable) {
        if (appBg != null) {
            appBg.setImageDrawable(drawable);
        } else {
            LogUtil.e( "setAppBg: appBg为null");
        }
    }

    public void setAppBg(int resId) {
        if (appBg != null) {
            appBg.setImageResource(resId);
        } else {
            LogUtil.e("setAppBg: appBg为null, resId: " + resId);
        }
    }

    // 获取当前应用名称
    public String getAppName() {
        return appName != null ? appName.getText().toString() : "";
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setAlpha(0.5f); // 按下时变暗
                return true; // 返回true才能监听到ACTION_UP

            case MotionEvent.ACTION_UP:
                v.setAlpha(1.0f); // 抬起时恢复
                // 执行点击事件
                v.performClick();
                return true;

            case MotionEvent.ACTION_CANCEL:
                v.setAlpha(1.0f); // 取消时恢复
                return true;
        }
        return false;
    }
}
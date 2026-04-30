package com.launcher.ui21;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.Nullable;

/**
 * 自定义 ImageView,支持点击时的透明度变化效果
 * 注意:已改用原生 ImageView,无需 AppCompat 主题
 */
@SuppressLint("AppCompatCustomView")
public class ImageViewTouch extends ImageView {

    public ImageViewTouch(Context context) {
        this(context, null);
    }

    public ImageViewTouch(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageViewTouch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        setClickable(true);
        setFocusable(true);
        setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setAlpha(0.6f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    setAlpha(1.0f);
                    break;
            }
            return false; // 不消费,让点击事件正常传递
        });
    }
}

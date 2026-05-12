package com.launcher.zy_ui04;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class TouchEffectUtils {

    /**
     * 为 ImageView 添加触摸效果
     * 点击时透明度变为 0.6，松开时恢复 1.0
     *
     * @param imageView 需要添加触摸效果的 ImageView
     */
    public static void addTouchEffect(ImageView imageView) {
        imageView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    imageView.setAlpha(0.6f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    imageView.setAlpha(1.0f);
                    break;
            }
            // 不消费事件，让点击事件正常传递
            return false;
        });
    }

    /**
     * 为 View 添加触摸效果
     * 点击时透明度变为 0.6，松开时恢复 1.0
     *
     * @param view 需要添加触摸效果的 View
     */
    public static void addTouchEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.setAlpha(0.6f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.setAlpha(1.0f);
                    break;
            }
            // 不消费事件，让点击事件正常传递
            return false;
        });
    }
}
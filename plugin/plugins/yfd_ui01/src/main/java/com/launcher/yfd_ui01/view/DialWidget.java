package com.launcher.yfd_ui01.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.launcher.yfd_ui01.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DialWidget extends LinearLayout {

    private Context mContext;
    private AnalogClockView analogClockView;
    private Handler timeHandler;
    private Runnable timeRunnable;
    private TextView tv_am_pm, tv_date, tv_week;

    private boolean isAnimationRunning = false;
    
    // 日期格式化
    private SimpleDateFormat dateFormat;
    private Calendar calendar;

    public DialWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    private void init() {
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
        timeHandler = new Handler(Looper.getMainLooper());
    }

    public void findViews(Context context, View view) {
        analogClockView = view.findViewById(R.id.analogClockView);
        tv_am_pm = view.findViewById(R.id.dial_am_pm);
        tv_date = view.findViewById(R.id.dial_date);
        tv_week = view.findViewById(R.id.dial_week);
        
        setupDigitalTimeUpdater();
        updateTime();
    }

    @SuppressLint("DefaultLocale")
    private void updateTime() {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        } else {
            calendar.setTimeInMillis(System.currentTimeMillis());
        }

        // 获取时间信息
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        //int minute = calendar.get(Calendar.MINUTE);
        //int second = calendar.get(Calendar.SECOND);
        int week = calendar.get(Calendar.DAY_OF_WEEK);

        // 更新AM/PM显示
        if (tv_am_pm != null) {
            tv_am_pm.setText(hour < 12 ? R.string.am : R.string.pm);
        }

        // 更新日期显示
        if (tv_date != null) {
            tv_date.setText(String.format("%02d-%02d", month, day));
        }

        // 更新星期显示
        if (tv_week != null) {
            String weekString = getWeekString(week);
            tv_week.setText(weekString);
        }
    }

    private String getWeekString(int week) {
        String[] formats;
        try {
            formats = getResources().getStringArray(R.array.week_str);
        } catch (Resources.NotFoundException e) {
            // 备用英文星期数组
            formats = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        }
        
        if (formats != null && formats.length >= 7) {
            // Calendar.DAY_OF_WEEK 返回 1 (Sunday) .. 7 (Saturday)
            int idx = (week - 1) % formats.length;
            return formats[idx];
        }
        return "";
    }

    private void setupDigitalTimeUpdater() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                timeHandler.postDelayed(this, 1000); // 每秒更新一次
            }
        };
        timeHandler.post(timeRunnable);
    }

    public void startAnimation() {
        if (!isAnimationRunning) {
            if (analogClockView != null) {
                analogClockView.startAnimation();
            }
            isAnimationRunning = true;
        }
        
        // 确保时间更新器运行
        if (timeHandler != null) {
            timeHandler.removeCallbacks(timeRunnable);
            timeHandler.post(timeRunnable);
        }
    }

    public void updateTimeSysem() {
        updateTime();
        if (analogClockView != null) {
            analogClockView.resetToCurrentTime();
        }
    }

    public void stopAnimation() {
        if (isAnimationRunning) {
            if (analogClockView != null) {
                analogClockView.stopAnimation();
            }
            isAnimationRunning = false;
        }
        
        if (timeHandler != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
        if (timeHandler != null) {
            timeHandler.removeCallbacksAndMessages(null);
        }
    }
}

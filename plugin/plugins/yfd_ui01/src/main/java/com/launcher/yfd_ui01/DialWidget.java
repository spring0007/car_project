package com.launcher.yfd_ui01;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.launcher.yfd_ui01.AnalogClockView;

import java.util.Calendar;

public class DialWidget extends LinearLayout {

    private Context mContext;
    private AnalogClockView analogClockView;
    private Handler timeHandler;
    private Runnable timeRunnable;
    private TextView tv_am_pm ,tv_date,tv_week;

    private boolean isAnimationRunning = true;

    public DialWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public void findViews(Context context, View view) {
        analogClockView= view.findViewById(R.id.analogClockView);
        tv_am_pm = view.findViewById(R.id.dial_am_pm);
        tv_date = view.findViewById(R.id.dial_date);
        tv_week = view.findViewById(R.id.dial_week);
        setupDigitalTimeUpdater();
        updateTime();
    }

    @SuppressLint("DefaultLocale")
    private void updateTime(){
        // 获取当前时间
        Calendar calendar = Calendar.getInstance();
        // 获取系统的日期
        //int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // 月份从0开始，需要加1
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        // 获取系统时间
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        //int minute = calendar.get(Calendar.MINUTE);
        //int second = calendar.get(Calendar.SECOND);
        int week = calendar.get(Calendar.DAY_OF_WEEK);
        // 判断是上午还是下午
        if (tv_am_pm != null) {
            if(hour < 12){
                tv_am_pm.setText(R.string.am);
            } else {
                tv_am_pm.setText(R.string.pm);
            }
        }

        if (tv_date != null) {
            tv_date.setText(String.format("%02d-%02d", month, day));
        }

        if (tv_week != null) {
            String[] formats = null;
            try {
                formats = getResources().getStringArray(R.array.week_str);
            } catch (Resources.NotFoundException e) {
                // fallback to a default English week array
                formats = new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
            }
            if (formats != null && formats.length > 0) {
                // Calendar.DAY_OF_WEEK returns 1 (Sunday) .. 7 (Saturday)
                int idx = (week - 1) % formats.length;
                if (idx < 0) idx = 0;
                tv_week.setText(formats[idx]);
            }
        }

    }

    private void setupDigitalTimeUpdater() {
        timeHandler = new Handler();
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                //updateDigitalTime();
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    public void startAnimation(){
        if (isAnimationRunning) {
            analogClockView.startAnimation();
        }
        timeHandler.post(timeRunnable);

    }
    public void updateTimeSysem(){
        updateTime();
        if(analogClockView!=null)
            analogClockView.resetToCurrentTime();
    }

    public void stopAnimation(){
        if (isAnimationRunning) {
            analogClockView.stopAnimation();
        }
        timeHandler.removeCallbacks(timeRunnable);

    }
}

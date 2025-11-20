package com.launcher.ui6.view;

import static java.util.Locale.getDefault;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.launcher.ui6.R;

import java.util.Calendar;

public class DialWidget extends LinearLayout {

    private Context mContext;
    private AnalogClockView analogClockView;
    private Handler timeHandler;
    private Runnable timeRunnable;
    private TextView tv_time ,tv_date,tv_week;

    private boolean isAnimationRunning = true;

    public DialWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public void findViews(Context context, View view) {
        analogClockView= view.findViewById(R.id.analogClockView);
        tv_time = view.findViewById(R.id.dial_time);
        tv_date = view.findViewById(R.id.dial_date);
        tv_week = view.findViewById(R.id.dial_week);
        setupDigitalTimeUpdater();
        updateTime();
    }


    private void updateTime(){
        // 获取当前时间
        Calendar calendar = Calendar.getInstance();
        // 获取系统的日期
        //int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // 月份从0开始，需要加1
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        // 获取系统时间
       // int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int hour = calendar.get(Calendar.HOUR);
        if (hour == 0) {
            hour = 12;
        }
        int minute = calendar.get(Calendar.MINUTE);
        //int second = calendar.get(Calendar.SECOND);
        int week = calendar.get(Calendar.DAY_OF_WEEK);
//        // 判断是上午还是下午
//        if(hour < 12){
//            //tv_time.setText(R.string.am);
//        }else{
//            //tv_time.setText(R.string.pm);
//        }
        tv_time.setText(String.format(getDefault(), "%2d:%02d", hour, minute));
        tv_date.setText(String.format(getDefault(), "%2d-%02d", month, day));
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

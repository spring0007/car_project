package com.awell.ctrlview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.awell.launcher.R;

import java.util.Calendar;

/**
 * Created 20210715
 * 分针、秒针、时针初始状态必现指向12点钟方向
 */
public class MyQAnalogClock extends View {
    //时钟盘，分针、秒针、时针对象

    Bitmap mBmpDial;
    Bitmap mBmpHour;
    Bitmap mBmpHourNight;
    Bitmap mBmpMinute;
    Bitmap mBmpMinuteNight;
//    Bitmap mBmpSecond;

    BitmapDrawable bmdHour;
    BitmapDrawable bmdHourNight;
    BitmapDrawable bmdMinute;
    BitmapDrawable bmdMinuteNight;
    //    BitmapDrawable bmdSecond;
    BitmapDrawable bmdDial;

    Paint mPaint;

    Handler tickHandler;

    int mWidth;
    int mHeigh;
    int centerX;
    int centerY;

    /*int availableWidth = 100;
    int availableHeight = 100;*/

    private String sTimeZoneString;

    private int value;

    public MyQAnalogClock(Context context) {
        this(context, null);
    }

    public MyQAnalogClock(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public MyQAnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        MyQAnalogClock1(context, "GMT+8：00");
    }

    public void MyQAnalogClock1(Context context, String sTime_Zone) {
        value = Settings.System.getInt(context.getContentResolver(), "launcherTypeDN", 0);
        Log.i("MyQAnalogClock", "value==" + value);
        sTimeZoneString = sTime_Zone;

//        mBmpHour = BitmapFactory.decodeResource(getResources(),
//                R.drawable.shi_day_clock);
//        bmdHour = new BitmapDrawable(mBmpHour);

        bmdHour = ((BitmapDrawable) getResources().getDrawable(R.drawable.shi_day_clock));

//        mBmpHourNight = BitmapFactory.decodeResource(getResources(),
//                R.drawable.shi_night_clock);
//        bmdHourNight = new BitmapDrawable(mBmpHourNight);

        bmdHourNight = ((BitmapDrawable) getResources().getDrawable(R.drawable.shi_night_clock));

//        mBmpMinute = BitmapFactory.decodeResource(getResources(),
//                R.drawable.fen_day_clock);
//        bmdMinute = new BitmapDrawable(mBmpMinute);

        bmdMinute = ((BitmapDrawable) getResources().getDrawable(R.drawable.fen_day_clock));

//        mBmpMinuteNight = BitmapFactory.decodeResource(getResources(),
//                R.drawable.fen_night_clock);
//        bmdMinuteNight = new BitmapDrawable(mBmpMinuteNight);

        bmdMinuteNight = ((BitmapDrawable) getResources().getDrawable(R.drawable.fen_night_clock));

//        mBmpSecond = BitmapFactory.decodeResource(getResources(),
//                R.drawable.fen);
//        bmdSecond = new BitmapDrawable(mBmpSecond);

//        mBmpDial = BitmapFactory.decodeResource(getResources(),
//                R.drawable.biaopan);
//        bmdDial = new BitmapDrawable(mBmpDial);

        bmdDial = ((BitmapDrawable) getResources().getDrawable(R.drawable.biaopan));
        mBmpDial = bmdDial.getBitmap();

        mWidth = mBmpDial.getWidth();
        mHeigh = mBmpDial.getHeight();

        /*availableWidth = mWidth / 2;
        availableHeight = mHeigh / 2;*/

        centerX = mWidth / 2;
        centerY = mHeigh / 2;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLUE);
        run();
    }

    public void run() {
        tickHandler = new Handler();
        tickHandler.post(tickRunnable);
    }

    public void setValue(int value) {
        this.value = value;
        invalidate();
    }

    private final Runnable tickRunnable = new Runnable() {
        public void run() {
            postInvalidate();
            tickHandler.postDelayed(tickRunnable, 1000);
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        mHeigh = MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            //默认大小 200*200
            // setMeasuredDimension(100, 100);
            setMeasuredDimension(280, 280);
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mHeigh, mHeigh);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mWidth, mWidth);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        float hourRotate = hour * 30.0f + minute / 60.0f * 30.0f;

        float minuteRotate = minute * 6.0f;
        float secondRotate = second * 6.0f;
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        PaintFlagsDrawFilter pfd = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.setDrawFilter(pfd);
        int diameter = Math.min(getWidth(), getHeight());
        int radiuos = diameter / 2;
//        bmdDial.setBounds(0,
//                0,
//                diameter,
//                diameter);
//        bmdDial.draw(canvas);

        if (value == 0) {
            canvas.save();
            canvas.rotate(hourRotate, radiuos, radiuos);
            bmdHour.setBounds(0
                    , 0
                    , diameter
                    , diameter);
            bmdHour.draw(canvas);
            canvas.restore();
        } else if (value == 1) {
            canvas.save();
            canvas.rotate(hourRotate, radiuos, radiuos);
            bmdHourNight.setBounds(0
                    , 0
                    , diameter
                    , diameter);
            bmdHourNight.draw(canvas);
            canvas.restore();
        }

        if (value == 0) {
            canvas.save();
            canvas.rotate(minuteRotate, radiuos, radiuos);
            bmdMinute.setBounds(0
                    , 0
                    , diameter
                    , diameter);
            bmdMinute.draw(canvas);
            canvas.restore();
        } else if (value == 1) {
            canvas.save();
            canvas.rotate(minuteRotate, radiuos, radiuos);
            bmdMinuteNight.setBounds(0
                    , 0
                    , diameter
                    , diameter);
            bmdMinuteNight.draw(canvas);
            canvas.restore();
        }


//        canvas.save();
//        canvas.rotate(secondRotate, radiuos, radiuos);
//        bmdSecond.setBounds(0
//                , 0
//                , diameter
//                , diameter);
//        bmdSecond.draw(canvas);
//        canvas.restore();
    }
}

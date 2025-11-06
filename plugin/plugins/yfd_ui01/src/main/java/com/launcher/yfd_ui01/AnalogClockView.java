package com.launcher.yfd_ui01;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Calendar;

public class AnalogClockView extends View {
    private Bitmap clockBackground;
    private Bitmap hourHand;
    private Bitmap minuteHand;
    private Bitmap secondHand;
    private Bitmap centerDot;

    private Paint paint;
    private Matrix matrix;

    private int centerX, centerY;
    private float scaleFactor = 1.0f;

    private ValueAnimator secondAnimator;
    private float secondRotation = 0;
    private float minuteRotation = 0;
    private float hourRotation = 0;

    private boolean isRunning = false;
    public AnalogClockView(Context context) {
        super(context);
        init();
    }

    public AnalogClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalogClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 加载图片资源
        clockBackground = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_dail_bg);
        hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_dail_hour);
        minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_dail_minute);
        secondHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_dail_second);
        centerDot = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_dial_dot);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        matrix = new Matrix();

        // 设置秒针动画
        setupSecondAnimation();
    }

    private void setupSecondAnimation() {
        secondAnimator = ValueAnimator.ofFloat(0, 360);
        secondAnimator.setDuration(60000); // 60秒完成一圈
        secondAnimator.setRepeatCount(ValueAnimator.INFINITE);
        secondAnimator.setInterpolator(new LinearInterpolator());
        secondAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                secondRotation = (float) animation.getAnimatedValue();
                updateTimeFromSystem();
                invalidate();
            }
        });
    }

    private void updateTimeFromSystem() {
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        // 计算时针角度（考虑分钟的影响）
        hourRotation = (hours * 30) + (minutes * 0.5f);

        // 计算分针角度（考虑秒针的影响）
        minuteRotation = (minutes * 6) + (seconds * 0.1f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2;
        centerY = h / 2;

        // 计算缩放因子，使时钟适应View大小
        int minSize = Math.min(w, h);
        float bgWidth = clockBackground.getWidth();
        //scaleFactor = (minSize * 0.8f) / bgWidth;
        
        // 如果没有运行动画，则开始动画
        if (!isRunning) {
            resetToCurrentTime();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 保存画布状态
        canvas.save();

        // 移动到中心点
        canvas.translate(centerX, centerY);
        canvas.scale(scaleFactor, scaleFactor);

        // 绘制表盘背景
        drawBitmapCentered(canvas, clockBackground, 0, 0, 0);

        // 绘制时针
        drawBitmapCenteredPoint(canvas, hourHand, 0, 0, (float) -hourHand.getWidth() / 2,  (float)-hourHand.getHeight(), hourRotation);

        // 绘制分针
        drawBitmapCenteredPoint(canvas, minuteHand, 0, 0, (float) -minuteHand.getWidth() / 2,  (float)-minuteHand.getHeight(), minuteRotation);

        // 绘制秒针
        drawBitmapCenteredPoint(canvas, secondHand, 0, 0, (float) -secondHand.getWidth() / 2,  (float)-secondHand.getHeight()+15,secondRotation);

        // 绘制中心原点
        drawBitmapCentered(canvas, centerDot, 0, 0, 0);

        // 恢复画布状态
        canvas.restore();
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float x, float y, float rotation) {
        matrix.reset();

        // 移动到中心点
        matrix.postTranslate(-bitmap.getWidth() / 2, -bitmap.getHeight() / 2);

        // 应用旋转
        matrix.postRotate(rotation);

        // 移动到指定位置
        matrix.postTranslate(x, y);

        // 绘制位图
        canvas.drawBitmap(bitmap, matrix, paint);
    }

    private void drawBitmapCenteredPoint(Canvas canvas, Bitmap bitmap, float x, float y,float dx, float dy, float rotation) {
        matrix.reset();

        // 移动到中心点
        matrix.postTranslate(dx, dy);

        // 应用旋转
        matrix.postRotate(rotation);

        // 移动到指定位置
        matrix.postTranslate(x, y);

        // 绘制位图
        canvas.drawBitmap(bitmap, matrix, paint);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isRunning) {
            startAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    public void startAnimation() {
        if (secondAnimator != null && !secondAnimator.isStarted()) {
            secondAnimator.start();
            isRunning = true;
        }
    }

    public void stopAnimation() {
        if (secondAnimator != null) {
            secondAnimator.cancel();
            isRunning = false;
        }
    }
    
    // 重置到当前系统时间
    public void resetToCurrentTime() {
        if (secondAnimator != null) {
            secondAnimator.cancel();
        }
        // 更新时间为当前系统时间
        updateTimeFromSystem();
        Calendar calendar = Calendar.getInstance();
        int seconds = calendar.get(Calendar.SECOND);
        int milliseconds = calendar.get(Calendar.MILLISECOND);
        secondRotation = seconds * 6 + milliseconds * 0.006f;
        
        if (isRunning) {
            secondAnimator.start();
        }
        invalidate();
    }
    public boolean isRunning() {
        return isRunning;
    }
}
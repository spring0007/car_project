package com.launcher.ui6.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.launcher.ui6.R;

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
        clockBackground = BitmapFactory.decodeResource(getResources(), R.drawable.ui6_dial_bg);
        hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui6_dial_hour);
        minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui6_dial_minute);
        secondHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui6_dial_second);
        centerDot = BitmapFactory.decodeResource(getResources(), R.drawable.ui6_dial_dot);

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
        int mill_seconds = calendar.get(Calendar.MILLISECOND);

        // 计算时针角度（考虑分钟的影响）
        hourRotation = (hours * 30) + (minutes * 0.5f);

        // 计算分针角度（考虑秒针的影响）
        minuteRotation = (minutes * 6) + (seconds * 0.1f);

        secondRotation = (seconds * 6) + (mill_seconds * 0.006f);
    }

    // 添加分辨率适配方法
    private float getOptimalScaleFactor() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        
        float scaleFactor;
        
        if (screenWidth <= 1024 && screenHeight <= 600) {
            scaleFactor = 0.75f;
            Log.d("AnalogClockView", "1024x600分辨率，使用缩放因子: " + scaleFactor);
        } else if (screenWidth <= 1280 && screenHeight <= 720) {
            scaleFactor = 1.0f;
            Log.d("AnalogClockView", "1280x720分辨率，使用缩放因子: " + scaleFactor);
        }  else if (screenWidth <= 2400 && screenHeight <= 896) {
            scaleFactor = 1.5f;
            Log.d("AnalogClockView", "2400x896分辨率，使用缩放因子: " + scaleFactor);
        }else if (screenWidth <= 2000 && screenHeight <= 1200) {
                scaleFactor = 1.5f;
                Log.d("AnalogClockView", "2000x1200分辨率，使用缩放因子: " + scaleFactor);

        } else {
            scaleFactor = 1.0f;
            Log.d("AnalogClockView", "其他分辨率(" + screenWidth + "x" + screenHeight + ")，使用缩放因子: " + scaleFactor);
        }
        
        return scaleFactor;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2;
        centerY = h / 2;

        // 根据分辨率计算缩放因子
        scaleFactor = getOptimalScaleFactor();
        
        // 如果没有运行动画，则开始动画
        if (!isRunning) {
            resetToCurrentTime();
        }
    }

    // 添加 onMeasure 方法确保正确测量
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 获取基础尺寸（基于原始图片大小）
        int baseSize = Math.max(clockBackground.getWidth(), clockBackground.getHeight());
        
        // 根据缩放因子计算最终尺寸
        float scale = getOptimalScaleFactor();
        int desiredSize = (int) (baseSize * scale);
        
        // 考虑 padding
        desiredSize += getPaddingLeft() + getPaddingRight();
        
        // 设置测量尺寸
        setMeasuredDimension(desiredSize, desiredSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    
        // 保存画布状态
        canvas.save();
    
        // 移动到中心点
        canvas.translate(centerX, centerY);
        
        // 不缩放整个画布，改为单独缩放每个图片
    
        // 绘制表盘背景（应用缩放）
        drawScaledBitmapCentered(canvas, clockBackground, 0, 0, 0, scaleFactor);
    
        // 绘制时针（应用缩放）
        drawScaledBitmapCenteredPoint(canvas, hourHand, 0, 0, 
            (float) -hourHand.getWidth() / 2, (float)-hourHand.getHeight()-10, 
            hourRotation, scaleFactor);
    
        // 绘制分针（应用缩放）
        drawScaledBitmapCenteredPoint(canvas, minuteHand, 0, 0, 
            (float) -minuteHand.getWidth() / 2, (float)-minuteHand.getHeight()-7, 
            minuteRotation, scaleFactor);
    
        // 绘制秒针（应用缩放）
        drawScaledBitmapCenteredPoint(canvas, secondHand, 0, 0, 
            (float) -secondHand.getWidth() / 2, (float)-secondHand.getHeight()-5, 
            secondRotation, scaleFactor);
    
        // 绘制中心原点（应用缩放）
        drawScaledBitmapCentered(canvas, centerDot, 0, 0, 0, scaleFactor);
    
        // 恢复画布状态
        canvas.restore();
    }
    
    // 新的绘制方法：单独缩放每个图片，保持位置不变
    private void drawScaledBitmapCentered(Canvas canvas, Bitmap bitmap, float x, float y, float rotation, float scale) {
        matrix.reset();
    
        // 先移动到中心点（不缩放）
        matrix.postTranslate(-bitmap.getWidth() / 2, -bitmap.getHeight() / 2);
    
        // 应用旋转
        matrix.postRotate(rotation);
    
        // 应用缩放（在旋转之后，这样缩放不会影响位置）
        matrix.postScale(scale, scale);
    
        // 移动到指定位置
        matrix.postTranslate(x, y);
    
        // 绘制位图
        canvas.drawBitmap(bitmap, matrix, paint);
    }
    
    private void drawScaledBitmapCenteredPoint(Canvas canvas, Bitmap bitmap, float x, float y, 
                                             float dx, float dy, float rotation, float scale) {
        matrix.reset();
    
        // 先移动到偏移点（不缩放）
        matrix.postTranslate(dx, dy);
    
        // 应用旋转
        matrix.postRotate(rotation);
    
        // 应用缩放（在旋转之后，这样缩放不会影响位置）
        matrix.postScale(scale, scale);
    
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
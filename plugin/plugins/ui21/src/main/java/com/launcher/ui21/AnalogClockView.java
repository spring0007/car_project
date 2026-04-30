package com.launcher.ui21;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import com.launcher.ui21.R;

import java.util.Calendar;

public class AnalogClockView extends View {
    private static final String TAG = "AnalogClockView";
    // 位图资源
    private Bitmap clockBackground;
    private Bitmap hourHand;
    private Bitmap minuteHand;
    private Bitmap secondHand;
    private Bitmap centerDot;

    // 绘图工具
    private Paint paint;
    private Matrix matrix;

    // 视图尺寸
    private int centerX, centerY;
    private float scaleFactor = 1.0f;

    // 时间相关
    private float secondRotation = 0;
    private float minuteRotation = 0;
    private float hourRotation = 0;

    // 动画控制
    private ValueAnimator secondAnimator;
    private Handler timeHandler;
    private Runnable timeUpdater;
    private boolean isRunning = false;

    // 性能优化：缓存计算值
    private float hourHandCenterX, hourHandCenterY;
    private float minuteHandCenterX, minuteHandCenterY;
    private float secondHandCenterX, secondHandCenterY;

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
        // 初始化绘图工具
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        matrix = new Matrix();
        
        // 加载位图资源（优化内存使用）
        loadBitmaps();
        
        // 初始化时间处理器
        timeHandler = new Handler(Looper.getMainLooper());
        
        // 设置时间更新器
        setupTimeUpdater();
        
        // 预计算中心点偏移量
        preCalculateHandCenters();
    }

    private void loadBitmaps() {
        try {
            // 使用 BitmapFactory.Options 优化位图加载
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 减少内存占用
            
            clockBackground = BitmapFactory.decodeResource(getResources(), R.drawable.time_watch_bg, options);
            hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.hand_hour, options);
            minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.hand_minute, options);
            secondHand = BitmapFactory.decodeResource(getResources(), R.drawable.hand_second, options);
           // centerDot = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_dial_dot, options);
            
        } catch (Exception e) {
            // 处理资源加载异常
            e.printStackTrace();
        }
    }

    private void preCalculateHandCenters() {
        if (hourHand != null) {
            hourHandCenterX = -hourHand.getWidth() / 2.0f;
            hourHandCenterY = -hourHand.getHeight();
        }
        if (minuteHand != null) {
            minuteHandCenterX = -minuteHand.getWidth() / 2.0f;
            minuteHandCenterY = -minuteHand.getHeight();
        }
        if (secondHand != null) {
            secondHandCenterX = -secondHand.getWidth() / 2.0f;
            secondHandCenterY = -secondHand.getHeight() + 15;
        }
    }

    private void setupTimeUpdater() {
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    updateTimeFromSystem();
                    invalidate(); // 请求重绘
                    timeHandler.postDelayed(this, 16); // 约60FPS
                }
            }
        };
    }

    private void updateTimeFromSystem() {
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int milliseconds = calendar.get(Calendar.MILLISECOND);

        // 计算时针角度（考虑分钟的影响，更精确）
        hourRotation = (hours * 30.0f) + (minutes * 0.5f) + (seconds * 0.00833f);

        // 计算分针角度（考虑秒针的影响，更精确）
        minuteRotation = (minutes * 6.0f) + (seconds * 0.1f) + (milliseconds * 0.0001667f);

        // 计算秒针角度（包含毫秒，实现平滑移动）
        secondRotation = (seconds * 6.0f) + (milliseconds * 0.006f);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2;
        centerY = h / 2;

        if (clockBackground != null) {
            // 计算缩放因子，让表盘背景适应视图大小
            float viewAspect = (float) w / h;
            float bgAspect = (float) clockBackground.getWidth() / clockBackground.getHeight();

            if (viewAspect > bgAspect) {
                // 视图更宽，以高度为基准缩放
                scaleFactor = (float) h / clockBackground.getHeight();
            } else {
                // 视图更高，以宽度为基准缩放
                scaleFactor = (float) w / clockBackground.getWidth();
            }

            // 留出5%的边距
            scaleFactor *= 0.95f;
        }

        // 如果没有运行，则重置到当前时间
        if (!isRunning) {
            resetToCurrentTime();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 检查位图是否加载成功
        if (clockBackground == null || hourHand == null || 
            minuteHand == null || secondHand == null || centerDot == null) {
            return;
        }

        // 保存画布状态
        canvas.save();

        // 移动到中心点并缩放
        canvas.translate(centerX, centerY);
        canvas.scale(scaleFactor, scaleFactor);

        try {
            // 绘制表盘背景
            drawBitmapCentered(canvas, clockBackground, 0, 0, 0);

            // 绘制时针
            drawBitmapCenteredPoint(canvas, hourHand, 0, 0, hourHandCenterX, hourHandCenterY, hourRotation);

            // 绘制分针
            drawBitmapCenteredPoint(canvas, minuteHand, 0, 0, minuteHandCenterX, minuteHandCenterY, minuteRotation);

            // 绘制秒针
            drawBitmapCenteredPoint(canvas, secondHand, 0, 0, secondHandCenterX, secondHandCenterY, secondRotation);

            // 绘制中心原点
            drawBitmapCentered(canvas, centerDot, 0, 0, 0);
            
        } catch (Exception e) {
            // 捕获绘制过程中的异常，避免应用崩溃
            e.printStackTrace();
        }

        // 恢复画布状态
        canvas.restore();
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float x, float y, float rotation) {
        if (bitmap == null || bitmap.isRecycled()) return;
        
        matrix.reset();
        matrix.postTranslate(-bitmap.getWidth() / 2.0f, -bitmap.getHeight() / 2.0f);
        matrix.postRotate(rotation);

        // 移动到指定位置
        matrix.postTranslate(x, y);

        // 绘制位图
        canvas.drawBitmap(bitmap, matrix, paint);
    }

    private void drawBitmapCenteredPoint(Canvas canvas, Bitmap bitmap, float x, float y, float dx, float dy, float rotation) {
        if (bitmap == null || bitmap.isRecycled()) return;
        
        matrix.reset();

        // 移动到中心点
        matrix.postTranslate(dx, dy);

        // 应用旋转
        matrix.postRotate(rotation); // 围绕指定点旋转

        // 移动到指定位置
        matrix.postTranslate(x, y);

        // 绘制位图
        canvas.drawBitmap(bitmap, matrix, paint);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
        recycleBitmaps(); // 释放位图资源
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            if (!isRunning) {
                startAnimation();
            }
        } else {
            stopAnimation();
        }
    }

    public void startAnimation() {
        if (!isRunning) {
            isRunning = true;
            resetToCurrentTime();
            timeHandler.post(timeUpdater);
        }
    }

    public void stopAnimation() {
        if (isRunning) {
            isRunning = false;
            timeHandler.removeCallbacks(timeUpdater);
        }
    }
    
    public void resetToCurrentTime() {
        updateTimeFromSystem();
        invalidate();
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void recycleBitmaps() {
        // 回收位图资源
        recycleBitmap(clockBackground);
        recycleBitmap(hourHand);
        recycleBitmap(minuteHand);
        recycleBitmap(secondHand);
        recycleBitmap(centerDot);
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
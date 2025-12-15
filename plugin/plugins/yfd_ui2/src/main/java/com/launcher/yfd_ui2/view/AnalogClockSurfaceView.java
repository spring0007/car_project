package com.launcher.yfd_ui2.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.launcher.yfd_ui2.R;

import java.util.Calendar;

public class AnalogClockSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "AnalogClockSurfaceView";

    // Surface相关
    private SurfaceHolder mHolder;
    private Thread mDrawThread;
    private volatile boolean mIsRunning = false;

    // 位图资源
    private Bitmap clockBackground;
    private Bitmap hourHand;
    private Bitmap minuteHand;
    private Bitmap secondHand;

    // 绘图工具
    private Paint paint;
    private Paint clearPaint;
    private Matrix matrix;

    // 视图尺寸
    private int centerX, centerY;
    private int viewWidth, viewHeight;

    // 时间相关
    private float secondRotation = 0;
    private float minuteRotation = 0;
    private float hourRotation = 0;

    // 帧率控制
    private static final long FRAME_DELAY = 42; // 约24FPS

    // 性能优化
    private float hourHandCenterX, hourHandCenterY;
    private float minuteHandCenterX, minuteHandCenterY;
    private float secondHandCenterX, secondHandCenterY;

    // 缩放因子
    private float scaleFactor = 1.0f;

    // 用于清除画布的标志
    private boolean needClearCanvas = true;

    public AnalogClockSurfaceView(Context context) {
        super(context);
        init();
    }

    public AnalogClockSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalogClockSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始化SurfaceHolder
        mHolder = getHolder();
        mHolder.addCallback(this);

        // 设置SurfaceView为透明
        setZOrderOnTop(true);

        // 设置SurfaceView透明背景
        mHolder.setFormat(PixelFormat.TRANSPARENT);

        // 初始化绘图工具
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        // 专门用于清除画布的Paint
        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        matrix = new Matrix();

        // 加载位图资源
        loadBitmaps();

        // 预计算中心点偏移量（现在图片是正方形，中心在正中间）
        preCalculateHandCenters();

        // 设置焦点和触摸，避免阻塞Surface更新
        setFocusable(true);
        setFocusableInTouchMode(true);

        // 确保视图背景透明
        setBackgroundColor(Color.TRANSPARENT);
    }

    private void loadBitmaps() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888; // 改为ARGB_8888支持透明

            clockBackground = BitmapFactory.decodeResource(getResources(), R.drawable.dial, options);
            hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.hour, options);
            minuteHand = BitmapFactory.decodeResource(getResources(), R.drawable.minute, options);
            secondHand = BitmapFactory.decodeResource(getResources(), R.drawable.second, options);

            // 检查位图是否包含alpha通道
            if (secondHand != null && !secondHand.hasAlpha()) {
                // 如果秒针没有alpha通道，需要处理
                convertToARGB(secondHand);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void convertToARGB(Bitmap bitmap) {
        // 如果有必要，可以创建一个新的ARGB_8888位图
    }

    private void preCalculateHandCenters() {
        // 现在图片是正方形，旋转中心在正中间
        // 所以偏移量都是负的图片宽度/2和高度/2
        if (hourHand != null) {
            hourHandCenterX = -hourHand.getWidth() / 2.0f;
            hourHandCenterY = -hourHand.getHeight() / 2.0f;
        }
        if (minuteHand != null) {
            minuteHandCenterX = -minuteHand.getWidth() / 2.0f;
            minuteHandCenterY = -minuteHand.getHeight() / 2.0f;
        }
        if (secondHand != null) {
            secondHandCenterX = -secondHand.getWidth() / 2.0f;
            secondHandCenterY = -secondHand.getHeight() / 2.0f;
        }
    }

    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int milliseconds = calendar.get(Calendar.MILLISECOND);

        // 计算时针角度（考虑分钟和秒的影响，更精确）
        hourRotation = (hours * 30.0f) + (minutes * 0.5f) + (seconds * 0.00833f);

        // 计算分针角度（考虑秒的影响，更精确）
        minuteRotation = (minutes * 6.0f) + (seconds * 0.1f) + (milliseconds * 0.0001667f);

        // 计算秒针角度（包含毫秒，实现平滑移动）
        secondRotation = (seconds * 6.0f) + (milliseconds * 0.006f);
    }

    private void drawClock(Canvas canvas) {
        if (canvas == null) return;

        // 完全清除画布，使用透明色
        // 这是解决残影问题的关键
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // 检查位图是否加载成功
        if (clockBackground == null || hourHand == null ||
                minuteHand == null || secondHand == null) {
            return;
        }

        // 保存画布状态
        canvas.save();

        // 移动到中心点并应用缩放
        canvas.translate(centerX, centerY);
        canvas.scale(scaleFactor, scaleFactor);

        try {
            // 绘制表盘背景
            drawBitmapCentered(canvas, clockBackground, 0, 0, 0);

            // 绘制时针（现在图片中心就是旋转中心）
            drawBitmapWithRotation(canvas, hourHand, 0, 0, hourRotation);

            // 绘制分针
            drawBitmapWithRotation(canvas, minuteHand, 0, 0, minuteRotation);

            // 绘制秒针 - 注意绘制顺序，秒针在最上面
            drawBitmapWithRotation(canvas, secondHand, 0, 0, secondRotation);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 恢复画布状态
        canvas.restore();
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float x, float y, float rotation) {
        if (bitmap == null || bitmap.isRecycled()) return;

        matrix.reset();
        // 先移动到中心
        matrix.postTranslate(-bitmap.getWidth() / 2.0f, -bitmap.getHeight() / 2.0f);

        // 应用旋转
        matrix.postRotate(rotation);

        // 移动到指定位置
        matrix.postTranslate(x, y);

        canvas.drawBitmap(bitmap, matrix, paint);
    }

    private void drawBitmapWithRotation(Canvas canvas, Bitmap bitmap, float x, float y, float rotation) {
        if (bitmap == null || bitmap.isRecycled()) return;

        matrix.reset();

        // 现在图片是正方形，旋转中心在正中间
        // 所以偏移量是负的图片宽度/2和高度/2
        float offsetX = -bitmap.getWidth() / 2.0f;
        float offsetY = -bitmap.getHeight() / 2.0f;

        // 先应用偏移，然后旋转，再移动到指定位置
        matrix.postTranslate(offsetX, offsetY);
        matrix.postRotate(rotation);
        matrix.postTranslate(x, y);

        canvas.drawBitmap(bitmap, matrix, paint);
    }

    // 绘制线程的run方法
    @Override
    public void run() {
        while (mIsRunning) {
            Canvas canvas = null;
            try {
                // 锁定Canvas开始绘制
                canvas = mHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (mHolder) {
                        // 更新时间
                        updateTime();

                        // 绘制时钟
                        drawClock(canvas);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 解锁Canvas并提交绘制
                if (canvas != null) {
                    try {
                        mHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // 控制帧率
            try {
                Thread.sleep(FRAME_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // 开始绘制线程
    private void startDrawing() {
        if (!mIsRunning) {
            mIsRunning = true;
            mDrawThread = new Thread(this, "AnalogClockDrawThread");
            mDrawThread.start();
        }
    }

    // 停止绘制线程
    private void stopDrawing() {
        mIsRunning = false;
        if (mDrawThread != null) {
            try {
                mDrawThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mDrawThread = null;
        }
    }

    // SurfaceHolder.Callback接口方法
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 当Surface创建时开始绘制
        startDrawing();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Surface尺寸变化时更新参数
        viewWidth = width;
        viewHeight = height;
        centerX = width / 2;
        centerY = height / 2;

        // 计算缩放因子
        if (clockBackground != null) {
            int minSize = Math.min(width, height);
            int bgSize = Math.min(clockBackground.getWidth(), clockBackground.getHeight());
            scaleFactor = (minSize * 1f) / bgSize;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 当Surface销毁时停止绘制
        stopDrawing();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            // 如果Surface已经创建，重新开始绘制
            if (mHolder.getSurface().isValid()) {
                needClearCanvas = true; // 需要清除画布
                startDrawing();
            }
        } else {
            stopDrawing();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopDrawing();
        recycleBitmaps();
    }

    // 公开的控制方法
    public void startAnimation() {
        if (!mIsRunning && mHolder.getSurface().isValid()) {
            needClearCanvas = true;
            startDrawing();
        }
    }

    public void stopAnimation() {
        stopDrawing();
    }

    public void resetToCurrentTime() {
        synchronized (mHolder) {
            updateTime();
            needClearCanvas = true; // 重置时需要清除画布
        }
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    // 释放位图资源
    private void recycleBitmaps() {
        recycleBitmap(clockBackground);
        recycleBitmap(hourHand);
        recycleBitmap(minuteHand);
        recycleBitmap(secondHand);
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    // 清除画布上的所有内容
    private void clearCanvas(Canvas canvas) {
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }

    // 设置位图的方法，方便动态更换皮肤
    public void setClockBackground(Bitmap background) {
        synchronized (mHolder) {
            recycleBitmap(clockBackground);
            clockBackground = background;
            preCalculateHandCenters();
        }
    }

    public void setHourHand(Bitmap hourHand) {
        synchronized (mHolder) {
            recycleBitmap(this.hourHand);
            this.hourHand = hourHand;
            preCalculateHandCenters();
        }
    }

    public void setMinuteHand(Bitmap minuteHand) {
        synchronized (mHolder) {
            recycleBitmap(this.minuteHand);
            this.minuteHand = minuteHand;
            preCalculateHandCenters();
        }
    }

    public void setSecondHand(Bitmap secondHand) {
        synchronized (mHolder) {
            recycleBitmap(this.secondHand);
            this.secondHand = secondHand;
            preCalculateHandCenters();
        }
    }
}
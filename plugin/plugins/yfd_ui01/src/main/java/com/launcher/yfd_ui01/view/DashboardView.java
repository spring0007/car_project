package com.launcher.yfd_ui01.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.awell.library.util.LogUtil;
import com.launcher.yfd_ui01.R;

/**
 * 汽车仪表盘
 * 优化点：
 * 1. 修复 recycleBitmap 回收 clockBackground 时误置空 hourHand 的 Bug
 * 2. View detach 时不再回收位图(避免页面切换后白屏),改为仅停止动画;
 *    资源释放统一走 releaseResources(),由 Fragment 在 onDestroyView 调用
 * 3. onDraw 前自动检测并恢复失效位图(attach 后自愈)
 * 4. setPointerBitmap 不再 recycle 外部传入的位图(避免误伤共享引用)
 * 5. 删除死代码:calculateAnimationDuration/isAttachedToWindow/空实现的 setMaxSpeed
 */
public class DashboardView extends View {
    private static final int SWEEP_ANGLE = 240;
    private static final int MAX_SPEED = 240;
    private static final int ANIMATION_DURATION = 500;
    private static final float START_ANGLE = 240f;

    private Bitmap clockBackground;
    private Bitmap hourHand;

    private int centerX, centerY;
    private float scaleFactor = 1.0f;
    private boolean scaleReady = false;   // 位图或视图尺寸变化后需重新计算缩放

    private ValueAnimator mAnim;
    private float currentDegree = START_ANGLE;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix matrix = new Matrix();

    private float handCenterX, handCenterY;

    public DashboardView(Context context) {
        super(context);
        init();
    }

    public DashboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DashboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 关闭硬件加速,避免低端车机 GPU 上旋转大图异常
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    // ==================== 资源管理 ====================

    /**
     * 确保位图有效,失效时自动重载(attach 后再次绘制可自愈)
     */
    private void ensureBitmaps() {
        if (clockBackground == null || clockBackground.isRecycled()
                || hourHand == null || hourHand.isRecycled()) {
            loadBitmapResource();
            preCalculateHandCenter();
            scaleReady = false; // 位图尺寸可能变化,需要重新计算缩放
        }
    }

    private void loadBitmapResource() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 减少内存占用
            options.inSampleSize = 1;
            clockBackground = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_car_peed_bg, options);
            hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_car_speed_point, options);
            if (hourHand == null) {
                LogUtil.e("Failed to load bitmap resource");
            }
        } catch (Exception e) {
            LogUtil.e("Error loading bitmap", e);
        }
    }

    private void preCalculateHandCenter() {
        if (hourHand != null) {
            handCenterX = -hourHand.getWidth() / 2.0f;
            handCenterY = -hourHand.getHeight();
        }
    }

    /**
     * 释放资源(Fragment onDestroyView 时调用,与 detach 区分开)
     */
    public void releaseResources() {
        closeAnimation();
        recycleBitmap(clockBackground);
        clockBackground = null;
        recycleBitmap(hourHand);
        hourHand = null;
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    // ==================== 布局/缩放 ====================

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        scaleReady = false;
    }

    private void updateScaleFactor() {
        if (clockBackground == null || clockBackground.isRecycled()
                || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float viewAspect = (float) getWidth() / getHeight();
        float bgAspect = (float) clockBackground.getWidth() / clockBackground.getHeight();
        if (viewAspect > bgAspect) {
            scaleFactor = (float) getHeight() / clockBackground.getHeight();
        } else {
            scaleFactor = (float) getWidth() / clockBackground.getWidth();
        }
        scaleFactor *= 0.95f; // 留 5% 边距
        scaleReady = true;
    }

    // ==================== 绘制 ====================

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // 位图缺失/被回收时自动恢复
        ensureBitmaps();
        if (clockBackground == null || hourHand == null
                || clockBackground.isRecycled() || hourHand.isRecycled()) {
            return;
        }
        if (!scaleReady) {
            updateScaleFactor();
        }

        canvas.save();
        try {
            canvas.translate(centerX, centerY);
            canvas.scale(scaleFactor, scaleFactor);
            drawBitmapCentered(canvas, clockBackground, 0, 0, 0);
            drawBitmapCenteredPoint(canvas, hourHand, 0, 0, handCenterX, handCenterY, currentDegree);
        } catch (Exception e) {
            LogUtil.e("Error in onDraw", e);
        } finally {
            canvas.restore();
        }
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float x, float y, float rotation) {
        if (bitmap == null || bitmap.isRecycled()) return;
        matrix.reset();
        matrix.postTranslate(-bitmap.getWidth() / 2.0f, -bitmap.getHeight() / 2.0f);
        matrix.postRotate(rotation);
        matrix.postTranslate(x, y);
        canvas.drawBitmap(bitmap, matrix, paint);
    }

    private void drawBitmapCenteredPoint(Canvas canvas, Bitmap bitmap, float x, float y, float dx, float dy, float rotation) {
        if (bitmap == null || bitmap.isRecycled()) return;
        matrix.reset();
        matrix.postTranslate(dx, dy);
        matrix.postRotate(rotation);
        matrix.postTranslate(x, y);
        canvas.drawBitmap(bitmap, matrix, paint);
    }

    // ==================== 对外接口 ====================

    /**
     * 外部更新速度(0-240),越界自动 clamp
     */
    public void udDataSpeed(int car_speed) {
        if (car_speed < 0) car_speed = 0;
        if (car_speed > MAX_SPEED) car_speed = MAX_SPEED;

        float targetAngle = START_ANGLE + ((float) car_speed / MAX_SPEED) * SWEEP_ANGLE;
        LogUtil.i("udDataSpeed: " + car_speed + ", targetAngle-> " + targetAngle);
        startAnimation(currentDegree, targetAngle);
    }

    private void startAnimation(float start, float end) {
        // 角度无变化则跳过动画
        if (Math.abs(start - end) < 0.1f) {
            currentDegree = end;
            invalidate();
            return;
        }
        cancelCurrentAnimation();
        mAnim = ValueAnimator.ofFloat(start, end);
        mAnim.setDuration(ANIMATION_DURATION);
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                currentDegree = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        mAnim.start();
    }

    private void cancelCurrentAnimation() {
        if (mAnim != null) {
            if (mAnim.isRunning()) {
                mAnim.cancel();
            }
            mAnim.removeAllUpdateListeners();
        }
    }

    /**
     * 重置到指定速度(无动画)
     */
    public void setSpeedImmediate(int speed) {
        if (speed < 0) speed = 0;
        if (speed > MAX_SPEED) speed = MAX_SPEED;
        cancelCurrentAnimation();
        currentDegree = START_ANGLE + ((float) speed / MAX_SPEED) * SWEEP_ANGLE;
        invalidate();
    }

    /**
     * 获取当前速度
     */
    public int getCurrentSpeed() {
        float progress = (currentDegree - START_ANGLE) / SWEEP_ANGLE;
        return (int) (progress * MAX_SPEED);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 位图若在后台被回收,attach 后自动恢复
        ensureBitmaps();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 只停动画不回收位图:View detach 后可能再次 attach(页面切换/重建),
        // 此时回收位图会导致仪表盘白屏。真正释放由 releaseResources() 负责
        closeAnimation();
    }

    /**
     * 停止动画(界面不可见时调用,如 onPause)
     */
    public void closeAnimation() {
        cancelCurrentAnimation();
    }
}
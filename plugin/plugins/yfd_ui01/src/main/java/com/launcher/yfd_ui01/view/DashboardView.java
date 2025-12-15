package com.launcher.yfd_ui01.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.launcher.yfd_ui01.R;

/**
 * Date :2021/12/24
 * Time :21:24
 * Author:moyihen
 * Description: 仪表盘
 * * 注意:因为是在固定开发板用,没怎么考虑适配情况.
 * 1.表盘大小是根据控件xml布局文件的width决定
 * 2.控件宽度太小可能刻度文字显示不全,可以看情况调整下文字大小.
 */
public class DashboardView extends View {
    private static final String TAG = "DashboardView";
    private Context mContext;
    
    // 常量定义
    private static final int SWEEP_ANGLE = 240;
    private static final int MAX_SPEED = 240;
    private static final int ANIMATION_DURATION = 500;
    private static final float START_ANGLE = 240f;
    private static final float END_ANGLE = 120f;
    
    // 位图资源
    private Bitmap hourHand;
    
    // 视图参数
    private int centerX, centerY;
    private float scaleFactor = 1.0f;
    
    // 动画控制
    private ValueAnimator mAnim;
    private float currentDegree = START_ANGLE;
    
    // 绘图工具
    private Paint paint;
    private Matrix matrix;
    
    // 性能优化：预计算的值
    private float handCenterX, handCenterY;
    private boolean isAttachedToWindow = false;

    public DashboardView(Context context) {
        super(context);
        init(context);
    }


    public DashboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DashboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        // 优化位图加载
        loadBitmapResource();
        
        // 初始化绘图工具
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        matrix = new Matrix();
        
        // 预计算中心点偏移
        preCalculateHandCenter();

        // 关闭硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private void loadBitmapResource() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 减少内存占用
            options.inSampleSize = 1; // 可根据需要调整采样率
            
            hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_car_speed_point, options);
            
            if (hourHand == null) {
                Log.e(TAG, "Failed to load bitmap resource");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap", e);
        }
    }

    private void preCalculateHandCenter() {
        if (hourHand != null) {
            handCenterX = (float) -hourHand.getWidth() / 2;
            handCenterY = (float) -hourHand.getHeight() - 30;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2;
        centerY = h / 2;

        // 计算合适的缩放因子
       // int minSize = Math.min(w, h);
        //if (hourHand != null) {
        //    float handSize = Math.max(hourHand.getWidth(), hourHand.getHeight());
       //     scaleFactor = (minSize * 1.0f) / handSize; // 调整缩放比例
        //}
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 检查位图是否有效
        if (hourHand == null || hourHand.isRecycled()) {
            return;
        }

        // 保存画布状态
        canvas.save();
        
        try {
            // 移动到中心点并缩放
            canvas.translate(centerX, centerY);
            canvas.scale(scaleFactor, scaleFactor);
            
            // 绘制指针
            drawBitmapCenteredPoint(canvas, hourHand, 0, 0, handCenterX, handCenterY, currentDegree);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDraw", e);
        } finally {
            // 恢复画布状态
            canvas.restore();
        }
    }

    private void drawBitmapCenteredPoint(Canvas canvas, Bitmap bitmap, float x, float y, float dx, float dy, float rotation) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        
        matrix.reset();

        // 移动到偏移点
        matrix.postTranslate(dx, dy);

        // 应用旋转
        matrix.postRotate(rotation);

        // 移动到指定位置
        matrix.postTranslate(x, y);
        // 绘制位图
        canvas.drawBitmap(bitmap, matrix, paint);
    }



    /**
     * 外部更新速度
     * @param car_speed 速度值 (0-240)
     */
    public void udDataSpeed(int car_speed) {
        if (car_speed < 0) {
            throw new IllegalArgumentException("速度不能小于0");
        }
        
        if (car_speed > MAX_SPEED) {
            car_speed = MAX_SPEED;
        }
        
        // 计算目标角度
        float targetAngle = START_ANGLE + ((float) car_speed / MAX_SPEED) * SWEEP_ANGLE;
        
        // 启动动画
        startAnimation(currentDegree, targetAngle);
    }

    /**
     * 指针动画
     */
    private void startAnimation(float start, float end) {
        // 如果目标角度与当前角度相同，不需要动画
        if (Math.abs(start - end) < 0.1f) {
            currentDegree = end;
            invalidate();
            return;
        }
        
        // 取消之前的动画
        cancelCurrentAnimation();
        
        // 创建新动画
        mAnim = ValueAnimator.ofFloat(start, end);
        mAnim.setDuration(500);
		//mAnim.setDuration(calculateAnimationDuration(start, end));
        //anim.setRepeatCount(ValueAnimator.INFINITE);//设置无限重复
        //anim.setRepeatMode(ValueAnimator.REVERSE);//设置重复模式

        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                currentDegree = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        
        mAnim.start();
    }
    
    /**
     * 根据角度变化计算动画持续时间（更平滑的速度变化）
     */
    private int calculateAnimationDuration(float start, float end) {
        float angleDiff = Math.abs(end - start);
        // 基础时间 + 根据角度差调整的时间
        return (int) (ANIMATION_DURATION * (angleDiff / SWEEP_ANGLE));
    }

    /**
     * 取消当前动画
     */
    private void cancelCurrentAnimation() {
        if (mAnim != null) {
            if (mAnim.isRunning()) {
                mAnim.cancel();
            }
            mAnim.removeAllUpdateListeners();
        }
    }

    /**
     * 重置到指定速度（无动画）
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
        isAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        closeAnimation();
        recycleBitmap();
    }

    /**
     * 关闭动画并释放资源
     */
    public void closeAnimation() {
        cancelCurrentAnimation();
    }

    /**
     * 回收位图资源
     */
    private void recycleBitmap() {
        if (hourHand != null && !hourHand.isRecycled()) {
            hourHand.recycle();
            hourHand = null;
        }
    }

    /**
     * 重新加载资源（在资源可能被回收后调用）
     */
    public void reloadResources() {
        if (hourHand == null || hourHand.isRecycled()) {
            loadBitmapResource();
            preCalculateHandCenter();
            invalidate();
        }
    }
    
    /**
     * 设置自定义指针图片
     */
    public void setPointerBitmap(Bitmap bitmap) {
        // 回收旧位图
        if (hourHand != null && !hourHand.isRecycled()) {
            hourHand.recycle();
        }
        
        hourHand = bitmap;
        preCalculateHandCenter();
        invalidate();
    }
    
    /**
     * 设置最大速度值
     */
    public void setMaxSpeed(int maxSpeed) {
        if (maxSpeed <= 0) {
            throw new IllegalArgumentException("最大速度必须大于0");
        }
        // 这里需要更新相关计算，但注意SWEEP_ANGLE是常量
        // 如果需要动态调整，可以将SWEEP_ANGLE改为成员变量
    }
}
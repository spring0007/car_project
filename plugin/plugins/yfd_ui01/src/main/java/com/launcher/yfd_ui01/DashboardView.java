package com.launcher.yfd_ui01;

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
    //圆环的总角度
    private int SWEEPANGLE = 240;

    private ValueAnimator mAnim;


    private Bitmap hourHand;
    private int centerX, centerY;
    private float scaleFactor = 1.0f;
    //指针开始的角度.
    private float startAngele = 240;
    //指针结束的角度.
    private float endAngele = 120;
    //最大速度
    private int maxSpeed = 240;
    //当前的角度
    private float currentDegree = startAngele;
    private Paint paint;
    private Matrix matrix;

    public DashboardView(Context context) {
        super(context);
        init(context);
    }


    public DashboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        hourHand = BitmapFactory.decodeResource(getResources(), R.drawable.ui7_car_speed_point);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        matrix = new Matrix();
        // 关闭硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        centerX = w / 2;
        centerY = h / 2;

        // 计算缩放因子，使时钟适应View大小
        //int minSize = Math.min(w, h);
        //float bgWidth = getWidth();
        //scaleFactor = (minSize * 0.8f) / bgWidth;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 保存画布状态
        canvas.save();
        // 移动到中心点
        canvas.translate(centerX, centerY);
        canvas.scale(scaleFactor, scaleFactor);
        // 绘制时针
        drawBitmapCenteredPoint(canvas, hourHand, 0, 0, (float) -hourHand.getWidth() / 2,  (float)-hourHand.getHeight()-30, currentDegree);
        // 恢复画布状态
        canvas.restore();
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



    /**
     * 外部更新刻度.
     *
     * @param car_speed .
     */
    public void udDataSpeed(int car_speed) {

        if (car_speed < 0) throw new IllegalArgumentException("----speed不能小于0----");
        if (car_speed > maxSpeed)
            car_speed = maxSpeed;
        float angle = (startAngele + ((float) car_speed / maxSpeed)*SWEEPANGLE);
        //Log.i(TAG, "udDataSpeed: currentDegree="+currentDegree+", angle ="+angle+", car_speed:"+car_speed);
        startAnimation(currentDegree, angle);
    }

    //指针+阴影偏移动画.
    private void startAnimation(float start, float end) {
        if (mAnim != null) {
            if (mAnim.isRunning() || mAnim.isStarted()) {
                mAnim.cancel();
                mAnim.removeAllUpdateListeners();
            }
            boolean running = mAnim.isRunning();
            boolean started = mAnim.isStarted();
            Log.i(TAG, "startAnimation: running:" + running + "--started" + started);
        }
        mAnim = ValueAnimator.ofFloat(start, end);
        //anim.setRepeatCount(ValueAnimator.INFINITE);//设置无限重复
        //anim.setRepeatMode(ValueAnimator.REVERSE);//设置重复模式
        mAnim.setDuration(500);
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (float) mAnim.getAnimatedValue();
                //Log.i(TAG, "onAnimationUpdate: " + value);
                currentDegree = value;
                invalidate();
            }
        });
        mAnim.start();
    }

    /**
     * 退出动画.
     */
    public void closeAnimation() {
        if (mAnim != null) {
            mAnim.cancel();
            mAnim.removeAllUpdateListeners();
        }
    }


}




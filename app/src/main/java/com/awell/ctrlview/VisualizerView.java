package com.awell.ctrlview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class VisualizerView extends View {

    private Handler mHandler;
    private Paint mPaint;
    private Random random;
    private RectF rect;
    private int width;
    private int height;
    private int rectColorUp = 0x426fbbce;
    private int rectColorDown = 0xbf2f7f94;
    private int COLUME_COUNTS = 9;  //行数
    private int ROWS_COUNTS = 10;   //列数
    private int rectWidth = 10;     // 小矩形宽
    private int rectHeight = 4;     // 小矩形高
    private boolean mAnimate = false;
    private int mFrequency = 500;   //刷新率（毫秒）

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            invalidate();
            handler.sendEmptyMessageDelayed(0, 400); //每间隔200毫秒发送消息刷新
        }
    };

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
//        handler.sendEmptyMessageDelayed(0, 400); //每间隔200毫秒发送消息刷新
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(rectColorDown);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2f);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mHandler = new Handler();
        random = new Random();
        rect = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        width = widthSize;
        height = heightSize;
        setMeasuredDimension(widthSize, heightSize);
    }


    /*  private int colorGroup[] = {0xFFBB86FC,0xFF6200EE,0xFF3700B3,0xFF03DAC5,0xFF018786,
              0xF0457800,0xEE060368,0xEE680603,0xEE68,0xED6894,0xED689400,};*/
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        rect.left = (width - rectWidth * 16) / 2f;
        rect.top = height - rectHeight;
        rect.right = (width - rectWidth * 16) / 2f + rectWidth;
        rect.bottom = height;
        int i, j, r;
        for (i = 0; i < COLUME_COUNTS; i++) {
            canvas.save();
            r = random.nextInt(ROWS_COUNTS - 1) + 2;
            mPaint.setColor(rectColorDown);
            for (j = 0; j < r; j++) {
                if (j == (r / 2)) mPaint.setColor(rectColorUp);
//                mPaint.setColor(colorGroup[random.nextInt(ROWS_COUNTS - 1)]);
                canvas.drawRect(rect, mPaint);
                canvas.translate(0, -rectHeight * 2);
            }
            canvas.restore();
            canvas.translate(rectWidth * 2, 0);
        }
        canvas.restore();

        if (mAnimate) {
            mHandler.removeCallbacks(runnable);
            mHandler.postDelayed(runnable, mFrequency);
        }

    }

    private Runnable runnable = new Runnable() {
        public void run() {
            repaint(mAnimate);
        }
    };

    private void repaint(boolean anim) {
        mAnimate = anim;
        invalidate();
    }

    public void setRectColor(int rectColorUp, int rectColorDown) {
        this.rectColorUp = rectColorUp;
        this.rectColorDown = rectColorDown;
    }

    public void startAnimation() {
//        repaint(true);
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, 400); //每间隔200毫秒发送消息刷新
    }

    public void stopAnimation() {
//        repaint(false);
        handler.removeMessages(0);
    }

    public boolean isAnimated() {
        return mAnimate;
    }

    public void setAnimateFrequency(int freguency) {
        mFrequency = freguency;
    }
}

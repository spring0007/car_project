package com.launcher.yfd_ui3.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import com.launcher.yfd_ui3.R;

/**
 * FM/AM 刻度尺View
 * 实现了FM和AM频段的切换、频率显示、滑动调整频率等功能
 * 支持自定义刻度颜色、中心线颜色、刻度大小等属性
 */
public class FMScaleView extends View {

    /** 默认FM频段最小值 (87.0 MHz) */
    private static final int DEFAULT_MIN_FM = 870;
    /** 默认FM频段最大值 (108.0 MHz) */
    private static final int DEFAULT_MAX_FM = 1080;
    /** 默认AM频段最小值 (530 KHz) */
    private static final int DEFAULT_MIN_AM = 530;
    /** 默认AM频段最大值 (1610 KHz) */
    private static final int DEFAULT_MAX_AM = 1610;

    /** 当前频段最小值 */
    private int minFrequency;
    /** 当前频段最大值 */
    private int maxFrequency;
    /** 当前频率值 */
    private int currentFrequency;
    /** 是否为FM频段 */
    private boolean isFM = true;

    /** 刻度线画笔 */
    private Paint scalePaint;
    /** 文本画笔 */
    private Paint textPaint;
    /** 中心线画笔 */
    private Paint centerLinePaint;
    /** 渐变遮罩画笔 */
    private Paint gradientPaint;

    /** 刻度线宽度 */
    private float scaleWidth;
    /** 刻度线高度 */
    private float scaleHeight;
    /** 文本大小 */
    private float textSize;
    /** 中心线宽度 */
    private float centerLineWidth;
    /** 中心线高度 */
    private float centerLineHeight;

    /** 触摸起始X坐标 */
    private float touchStartX;
    /** 触摸起始频率 */
    private int startFrequency;
    /** 是否正在拖动 */
    private boolean isDragging = false;

    /** 频率变化监听器 */
    private OnFrequencyChangedListener frequencyChangedListener;

    /**
     * 频率变化监听器接口
     */
    public interface OnFrequencyChangedListener {
        /**
         * 当频率发生变化时回调
         * @param frequency 当前频率值
         * @param isFM 是否为FM频段
         */
        void onFrequencyChanged(int frequency, boolean isFM);
    }

    /**
     * 构造方法
     * @param context 上下文
     */
    public FMScaleView(Context context) {
        super(context);
        init(null);
    }

    /**
     * 构造方法
     * @param context 上下文
     * @param attrs 属性集
     */
    public FMScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    /**
     * 构造方法
     * @param context 上下文
     * @param attrs 属性集
     * @param defStyleAttr 样式属性
     */
    public FMScaleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /**
     * 初始化方法
     * @param attrs 属性集
     */
    private void init(AttributeSet attrs) {
        // 设置默认频率范围
        setFMRange(DEFAULT_MIN_FM, DEFAULT_MAX_FM);
        setAMRange(DEFAULT_MIN_AM, DEFAULT_MAX_AM);
        currentFrequency = (minFrequency + maxFrequency) / 2;

        // 初始化画笔
        scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scalePaint.setColor(0xFFFFFFFF);
        scalePaint.setStrokeWidth(2f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        centerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerLinePaint.setColor(0xFFFF0000);
        centerLinePaint.setStrokeWidth(4f);

        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 设置默认尺寸
        scaleWidth = 2f;
        scaleHeight = 40f;
        textSize = 30f;
        centerLineWidth = 4f;
        centerLineHeight = 60f;

        // 读取自定义属性
        if (attrs != null) {
            readAttributes(attrs);
        }
    }

    /**
     * 读取自定义属性
     * @param attrs 属性集
     */
    private void readAttributes(AttributeSet attrs) {
        // 读取自定义属性
        android.content.res.TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.FMScaleView);

        try {
            // 刻度相关属性
            int scaleColor = typedArray.getColor(R.styleable.FMScaleView_scaleColor, 0xFFFFFFFF);
            float scaleWidth = typedArray.getDimension(R.styleable.FMScaleView_scaleWidth, 2f);
            float scaleHeight = typedArray.getDimension(R.styleable.FMScaleView_scaleHeight, 40f);

            // 文本相关属性
            int textColor = typedArray.getColor(R.styleable.FMScaleView_textColor, 0xFFFFFFFF);
            float textSize = typedArray.getDimension(R.styleable.FMScaleView_textSize, 30f);

            // 中心线相关属性
            int centerLineColor = typedArray.getColor(R.styleable.FMScaleView_centerLineColor, 0xFFFF0000);
            float centerLineWidth = typedArray.getDimension(R.styleable.FMScaleView_centerLineWidth, 4f);
            float centerLineHeight = typedArray.getDimension(R.styleable.FMScaleView_centerLineHeight, 60f);

            // 频段相关属性
            boolean isFM = typedArray.getBoolean(R.styleable.FMScaleView_isFM, true);
            int minFrequency = typedArray.getInt(R.styleable.FMScaleView_minFrequency, isFM ? DEFAULT_MIN_FM : DEFAULT_MIN_AM);
            int maxFrequency = typedArray.getInt(R.styleable.FMScaleView_maxFrequency, isFM ? DEFAULT_MAX_FM : DEFAULT_MAX_AM);
            int currentFrequency = typedArray.getInt(R.styleable.FMScaleView_currentFrequency, (minFrequency + maxFrequency) / 2);

            // 设置属性
            setScaleColor(scaleColor);
            setScaleWidth(scaleWidth);
            setScaleHeight(scaleHeight);
            setTextColor(textColor);
            setTextSize(textSize);
            setCenterLineColor(centerLineColor);
            setCenterLineWidth(centerLineWidth);
            setCenterLineHeight(centerLineHeight);

            if (isFM) {
                switchToFM();
            } else {
                switchToAM();
            }

            setCurrentFrequency(currentFrequency);
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * 当View尺寸变化时调用
     * @param w 新宽度
     * @param h 新高度
     * @param oldw 旧宽度
     * @param oldh 旧高度
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient();
    }

    /**
     * 更新渐变效果
     */
    private void updateGradient() {
        if (getWidth() > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, 0, getWidth(), 0,
                    0x00FFFFFF, 0xFFFFFFFF, Shader.TileMode.CLAMP
            );
            gradientPaint.setShader(gradient);
        }
    }

    /**
     * 绘制View
     * @param canvas 画布
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float centerY = height / 2f;

        // 绘制渐变遮罩
        canvas.drawRect(0, 0, width * 0.1f, height, gradientPaint);
        canvas.drawRect(width * 0.9f, 0, width, height, gradientPaint);

        // 计算刻度范围
        int frequencyRange = maxFrequency - minFrequency;
        float pixelsPerFrequency = (width * 0.8f) / frequencyRange;
        float startX = width * 0.1f;

        // 绘制刻度线和数字
        for (int freq = minFrequency; freq <= maxFrequency; freq += 10) {
            float x = startX + (freq - minFrequency) * pixelsPerFrequency;

            // 绘制刻度线
            if (freq % 100 == 0) {
                // 主刻度
                canvas.drawLine(x, centerY - scaleHeight, x, centerY + scaleHeight, scalePaint);
                // 绘制数字
                String text = String.valueOf(freq / 10f);
                canvas.drawText(text, x, centerY + scaleHeight + textSize + 10, textPaint);
            } else if (freq % 50 == 0) {
                // 次刻度
                canvas.drawLine(x, centerY - scaleHeight * 0.7f, x, centerY + scaleHeight * 0.7f, scalePaint);
            } else {
                // 小刻度
                canvas.drawLine(x, centerY - scaleHeight * 0.5f, x, centerY + scaleHeight * 0.5f, scalePaint);
            }
        }

        // 绘制中心线
        float centerX = startX + (currentFrequency - minFrequency) * pixelsPerFrequency;
        canvas.drawLine(centerX, centerY - centerLineHeight, centerX, centerY + centerLineHeight, centerLinePaint);
    }

    /**
     * 处理触摸事件
     * @param event 触摸事件
     * @return 是否消费事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录触摸起始位置和当前频率
                touchStartX = event.getX();
                startFrequency = currentFrequency;
                isDragging = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    // 计算滑动距离对应的频率变化
                    float deltaX = event.getX() - touchStartX;
                    int frequencyRange = maxFrequency - minFrequency;
                    float pixelsPerFrequency = (getWidth() * 0.8f) / frequencyRange;
                    int deltaFreq = (int) (deltaX / pixelsPerFrequency);
                    int newFrequency = startFrequency + deltaFreq;

                    // 限制频率范围
                    newFrequency = Math.max(minFrequency, Math.min(maxFrequency, newFrequency));

                    // 如果频率发生变化，更新并通知监听器
                    if (newFrequency != currentFrequency) {
                        currentFrequency = newFrequency;
                        invalidate();
                        if (frequencyChangedListener != null) {
                            frequencyChangedListener.onFrequencyChanged(currentFrequency, isFM);
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 结束拖动
                isDragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 设置FM频段范围
     * @param min 最小值
     * @param max 最大值
     */
    public void setFMRange(int min, int max) {
        this.minFrequency = min;
        this.maxFrequency = max;
        if (isFM) {
            currentFrequency = Math.max(min, Math.min(max, currentFrequency));
            invalidate();
        }
    }

    /**
     * 设置AM频段范围
     * @param min 最小值
     * @param max 最大值
     */
    public void setAMRange(int min, int max) {
        this.minFrequency = min;
        this.maxFrequency = max;
        if (!isFM) {
            currentFrequency = Math.max(min, Math.min(max, currentFrequency));
            invalidate();
        }
    }

    /**
     * 切换到FM频段
     */
    public void switchToFM() {
        isFM = true;
        setFMRange(DEFAULT_MIN_FM, DEFAULT_MAX_FM);
    }

    /**
     * 切换到AM频段
     */
    public void switchToAM() {
        isFM = false;
        setAMRange(DEFAULT_MIN_AM, DEFAULT_MAX_AM);
    }

    /**
     * 设置当前频率
     * @param frequency 频率值
     */
    public void setCurrentFrequency(int frequency) {
        currentFrequency = Math.max(minFrequency, Math.min(maxFrequency, frequency));
        invalidate();
        if (frequencyChangedListener != null) {
            frequencyChangedListener.onFrequencyChanged(currentFrequency, isFM);
        }
    }

    /**
     * 获取当前频率
     * @return 当前频率值
     */
    public int getCurrentFrequency() {
        return currentFrequency;
    }

    /**
     * 判断是否为FM频段
     * @return 是否为FM频段
     */
    public boolean isFM() {
        return isFM;
    }

    /**
     * 设置频率变化监听器
     * @param listener 监听器
     */
    public void setOnFrequencyChangedListener(OnFrequencyChangedListener listener) {
        this.frequencyChangedListener = listener;
    }

    /**
     * 设置刻度线宽度
     * @param width 宽度
     */
    public void setScaleWidth(float width) {
        this.scaleWidth = width;
        scalePaint.setStrokeWidth(width);
        invalidate();
    }

    /**
     * 设置刻度线高度
     * @param height 高度
     */
    public void setScaleHeight(float height) {
        this.scaleHeight = height;
        invalidate();
    }

    /**
     * 设置文本大小
     * @param size 大小
     */
    public void setTextSize(float size) {
        this.textSize = size;
        textPaint.setTextSize(size);
        invalidate();
    }

    /**
     * 设置文本颜色
     * @param color 颜色
     */
    public void setTextColor(int color) {
        textPaint.setColor(color);
        invalidate();
    }

    /**
     * 设置中心线宽度
     * @param width 宽度
     */
    public void setCenterLineWidth(float width) {
        this.centerLineWidth = width;
        centerLinePaint.setStrokeWidth(width);
        invalidate();
    }

    /**
     * 设置中心线高度
     * @param height 高度
     */
    public void setCenterLineHeight(float height) {
        this.centerLineHeight = height;
        invalidate();
    }

    /**
     * 设置刻度颜色
     * @param color 颜色
     */
    public void setScaleColor(int color) {
        scalePaint.setColor(color);
        invalidate();
    }

    /**
     * 设置中心线颜色
     * @param color 颜色
     */
    public void setCenterLineColor(int color) {
        centerLinePaint.setColor(color);
        invalidate();
    }
}
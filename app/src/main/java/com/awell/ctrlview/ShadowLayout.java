package com.awell.ctrlview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.awell.launcher.R;

public class ShadowLayout extends LinearLayout {

    private boolean showShadow = false;
    private int shadowColor = Color.BLACK;
    private int bgColor = Color.WHITE;
    private float shadowDx = 0f;
    private float shadowDy = 0f;
    private float shadowRadius = 0f;
    private float cornerRadius = 0f;

    private final Paint paint = new Paint();

    public ShadowLayout(Context context) {
        this(context, null);
    }

    public ShadowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttribute(attrs);
        setLayoutAndPaint();
    }

    private void parseAttribute(AttributeSet attrs) {
        if (attrs == null) return;

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ShadowLayout);
        showShadow = typedArray.getBoolean(R.styleable.ShadowLayout_showShadow, false);
        shadowColor = typedArray.getColor(R.styleable.ShadowLayout_shadowColor, Color.BLACK);
        bgColor = typedArray.getColor(R.styleable.ShadowLayout_bgColor, Color.WHITE);
        shadowDx = typedArray.getDimension(R.styleable.ShadowLayout_shadowDx, 0f);
        shadowDy = typedArray.getDimension(R.styleable.ShadowLayout_shadowDy, 0f);
        shadowRadius = typedArray.getDimension(R.styleable.ShadowLayout_shadowRadius, 0f);
        cornerRadius = typedArray.getDimension(R.styleable.ShadowLayout_cornerRadius, 0f);
        typedArray.recycle();
    }

    private void setLayoutAndPaint() {
        float paddingLeft = shadowRadius - shadowDx;
        float paddingTop = shadowRadius - shadowDy;
        float paddingRight = shadowRadius + shadowDx;
        float paddingBottom = shadowRadius + shadowDy;
        paddingLeft = Math.max(paddingLeft, 0f);
        paddingTop = Math.max(paddingTop, 0f);
        paddingRight = Math.max(paddingRight, 0f);
        paddingBottom = Math.max(paddingBottom, 0f);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);


        if (showShadow) {
            setWillNotDraw(false);
            paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
            setPadding((int) paddingLeft, (int) paddingTop, (int) paddingRight, (int) paddingBottom);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int[] i;

        canvas.drawRoundRect(
                getPaddingLeft(),
                getPaddingTop(),
                getMeasuredWidth() - getPaddingRight(),
                getMeasuredHeight() - getPaddingBottom(),
                cornerRadius,
                cornerRadius,
                paint
        );
    }
}
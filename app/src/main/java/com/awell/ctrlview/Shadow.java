package com.awell.ctrlview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.awell.launcher.R;

public class Shadow extends FrameLayout {

    private boolean showShadow;
    private int shadowColor = Color.BLACK;
    private float shadowDx = 0f;
    private float shadowDy = 0f;
    private float shadowRadius = 0f;
    private float cornerRadius = 0f;
    float paddingLeft = 0f;
    float paddingTop = 0f;
    float paddingRight = 0f;
    float paddingBottom = 0f;
    private Paint paint = new Paint();

    public Shadow(Context context) {
        this(context, null);
    }

    public Shadow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Shadow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttributes(attrs);
        setLayoutAndPaint();
    }

    private void parseAttributes(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ShadowLayout);
        showShadow = typedArray.getBoolean(R.styleable.ShadowLayout_showShadow, false);
        shadowColor = typedArray.getColor(R.styleable.ShadowLayout_shadowColor, Color.BLACK);
        shadowDx = typedArray.getDimension(R.styleable.ShadowLayout_shadowDx, 0f);
        shadowDy = typedArray.getDimension(R.styleable.ShadowLayout_shadowDy, 0f);
        shadowRadius = typedArray.getDimension(R.styleable.ShadowLayout_shadowRadius, 0f);
        cornerRadius = typedArray.getDimension(R.styleable.ShadowLayout_cornerRadius, 0f);
        typedArray.recycle();
    }

    private void setLayoutAndPaint() {
        float paddingLeft = Math.max(0f, shadowRadius - shadowDx);
        float paddingTop = Math.max(0f, shadowRadius - shadowDy);
        float paddingRight = Math.max(0f, shadowRadius + shadowDx);
        float paddingBottom = Math.max(0f, shadowRadius + shadowDy);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

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
        float left = paddingLeft;
        float top = paddingTop;
        float right = getMeasuredWidth() - paddingRight;
        float bottom = getMeasuredHeight() - paddingBottom;
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, paint);
    }
}


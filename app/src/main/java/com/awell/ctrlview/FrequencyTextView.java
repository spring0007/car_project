package com.awell.ctrlview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.TextView;

@SuppressLint("AppCompatCustomView")
public class FrequencyTextView extends TextView {

    private Rect mTextBound;
    private int flag = 0;
    private boolean implement = false;
    private LinearGradient mLinearGradient;

    public FrequencyTextView(Context context) {
        this(context, null);
    }

    public FrequencyTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrequencyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mTextBound = new Rect();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!implement) return;
        implement = false;

        int[] i;
        /*if (flag == 1) {
            i = new int[]{0xFF1277BE, 0xFFff8c00};
        }else {
            i = new int[]{0xFFff8c00, 0xFF1277BE};
        }*/
        i = new int[]{0x99775ee6,0xaaffffff, 0x99c9beff};
        mLinearGradient = new LinearGradient(0, 0, getMeasuredWidth(), getMeasuredHeight(),
                i, null, Shader.TileMode.REPEAT);
        Paint mPaint = getPaint();
        String mTipText = getText().toString();
        mPaint.getTextBounds(mTipText, 0, mTipText.length(), mTextBound);
        mPaint.setShader(mLinearGradient);
        canvas.drawText(mTipText, getMeasuredWidth() / 2 - mTextBound.width() / 2, getMeasuredHeight() / 2 + mTextBound.height() / 2, mPaint);
        setText(mTipText);
    }

    public void setFlag(int flag) {
        this.flag = flag;
        implement = true;
        invalidate();
    }
}

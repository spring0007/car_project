package com.awell.ctrlview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class mySeekBar extends SeekBar {

    public mySeekBar(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public mySeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.seekBarStyle);
        // TODO Auto-generated constructor stub
    }

    public mySeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
//		return super.onTouchEvent(event);
        return false;
    }


}

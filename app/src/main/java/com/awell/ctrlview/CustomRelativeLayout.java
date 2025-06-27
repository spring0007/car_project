package com.awell.ctrlview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class CustomRelativeLayout extends RelativeLayout {
    public CustomRelativeLayout(Context context) {
        this(context, null);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        if(ev.getAction() == MotionEvent.ACTION_DOWN){
            setAlpha(0.4f);
            return true;
        }
        if(ev.getAction() == MotionEvent.ACTION_UP){
            setAlpha(1f);
        }
        if(ev.getAction() == MotionEvent.ACTION_CANCEL){
            setAlpha(1f);
        }
        return false;
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
//        if(event.getAction() == MotionEvent.ACTION_DOWN){
//            setAlpha(0.4f);
//            return true;
//        }
//        if(event.getAction() == MotionEvent.ACTION_UP){
//            setAlpha(1f);
//        }
//        return false;
//    }
}

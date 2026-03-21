package com.launcher.zy_ui01;

import android.content.Context;
import android.util.AttributeSet;

public class AlwaysMarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {
    public AlwaysMarqueeTextView(Context context) { super(context); }
    public AlwaysMarqueeTextView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override
    public boolean isFocused() {
        return true; // 欺骗系统，永远认为我有焦点
    }
}

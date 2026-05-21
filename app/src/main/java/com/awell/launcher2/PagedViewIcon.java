/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.awell.launcher2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import com.awell.launcher.library.R;
import com.awell.utils.LogUtil;

/**
 * An icon on a PagedView, specifically for items in the launcher's paged view
 * (with compound drawables on the top).
 */
public class PagedViewIcon extends TextView {
    /**
     * A simple callback interface to allow a PagedViewIcon to notify when it
     * has been pressed
     */
    public static interface PressedCallback {
        void iconPressed(PagedViewIcon icon);
    }

    @SuppressWarnings("unused")
    private static final String TAG = "PagedViewIcon";
    private static final float PRESS_ALPHA = 0.4f;

    private PagedViewIcon.PressedCallback mPressedCallback;
    private boolean mLockDrawableState = false;

    private Bitmap mIcon;
    private Context mContext;
    
    // 图标偏移量（像素）
    private int mIconOffsetX = 0;  // 水平偏移，正值向右，负值向左
    private int mIconOffsetY = 0;  // 垂直偏移，正值向下，负值向上

    public PagedViewIcon(Context context) {
        this(context, null);
    }

    public PagedViewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mIconOffsetX = 0;
        mIconOffsetY = 0;
    }

    @SuppressLint("ResourceType")
    public void applyFromApplicationInfo(ApplicationInfo info, boolean scaleUp,
                                         PagedViewIcon.PressedCallback cb, int drawablePadding) {
        mIcon = info.iconBitmap;
        mPressedCallback = cb;
        setText(info.title);
//        if(Launcher.getSettingOrAndroidPage()){
//        	setTextSize(20);        	
//        	setBackground(new FastBitmapDrawable(mIcon));
//        	setGravity(Gravity.CENTER_HORIZONTAL);
//        	setPaddingRelative(5,110, 20, 0);
//        	if(info.title.length()>6){
//        		String myTitle=info.title.subSequence(0, 6).toString();            		
//        		setText(myTitle);            		
//        	}   
//        }else{
        setTextSize(mContext.getResources().getInteger(R.integer.pageview_app_icon_text_size));
        setCompoundDrawablePadding(drawablePadding);
        setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(mIcon), null, null);
        // 去掉阴影：半径设为0，颜色设为透明
        setShadowLayer(0, 0, 0, Color.TRANSPARENT);
//        }
        setTag(info);

    }

    /**
     * 使用自定义缩故后的 Bitmap 设置图标
     * @param info 应用信息
     * @param scaleUp 是否允许放大
     * @param cb 按下回调
     * @param drawablePadding 图标和文字的间距
     * @param scaledBitmap 已经缩故好的 Bitmap
     * @param textSize 文字大小
     * @param textOrientation 文字方向
     * @param textGravity 文字重力方向，默认水平居中
     */
    @SuppressLint("ResourceType")
    public void applyFromApplicationInfoWithBitmap(ApplicationInfo info, boolean scaleUp,
                                                    PagedViewIcon.PressedCallback cb, int drawablePadding,
                                                    Bitmap scaledBitmap ,int textSize ,int textColor, int textLine,int textOrientation ,int textGravity) {
        mIcon = scaledBitmap != null ? scaledBitmap : info.iconBitmap;
        mPressedCallback = cb;
        setText(info.title);
        

//            // 默认布局：图标在上，文字在下
//            setTextSize(mContext.getResources().getInteger(R.integer.pageview_app_icon_text_size));
//            setCompoundDrawablePadding(drawablePadding);
//            setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(mIcon), null, null);
//            setGravity(Gravity.CENTER_HORIZONTAL);
//        

        if(textSize > 0)
            setTextSize(textSize);
        else
            setTextSize(mContext.getResources().getInteger(R.integer.pageview_app_icon_text_size));

        if(textColor != Color.TRANSPARENT) {
            setTextColor(textColor);
            // 去掉阴影：半径设为0，颜色设为透明
            setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        }else {
            setTextColor(Color.WHITE);
        }

        if(textLine>0)
            setMaxLines(textLine);
        else
            setMaxLines(2);

        //LogUtil.i("drawablePadding ="+ drawablePadding+",textOrientation="+textOrientation);

        setCompoundDrawablePadding(drawablePadding);
        if(textOrientation==1)
            setCompoundDrawablesWithIntrinsicBounds(null,null, null,  new FastBitmapDrawable(mIcon));
        else if(textOrientation==2)
            setCompoundDrawablesWithIntrinsicBounds(new FastBitmapDrawable(mIcon), null, null, null);
        else if(textOrientation==3)
            setCompoundDrawablesWithIntrinsicBounds(null,  null, new FastBitmapDrawable(mIcon),null);
        else
            setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(mIcon), null, null);

        if(textGravity<=0)
            setGravity(Gravity.CENTER_HORIZONTAL);
        else
            setGravity(textGravity);

        
        setTag(info);
    }

    /**
     * 设置图标的水平偏移量（像素）
     * @param offsetX 偏移量，正值向右偏移，负值向左偏移，0表示不偏移（默认）
     */
    public void setIconOffsetX(int offsetX) {
        this.mIconOffsetX = offsetX;
        invalidate();  // 重绘以应用偏移
    }
    public void setIconOffsetY(int offsetY) {
        this.mIconOffsetY = offsetY;
        invalidate();
    }

    public void setIconOffsetXY(int offsetX, int offsetY) {
        this.mIconOffsetX = offsetX;
        this.mIconOffsetY = offsetY;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mIconOffsetX != 0 || mIconOffsetY != 0) {
            // 保存画布状态
            canvas.save();
            // 平移画布以实现图标偏移效果
            canvas.translate(mIconOffsetX, mIconOffsetY);
            // 调用父类的 onDraw 来绘制图标和文字
            super.onDraw(canvas);
            // 恢复画布状态
            canvas.restore();
        } else {
            // 没有偏移时，直接调用父类方法
            super.onDraw(canvas);
        }
    }

    public void lockDrawableState() {
        mLockDrawableState = true;
    }

    public void resetDrawableState() {
        mLockDrawableState = false;
        post(new Runnable() {
            @Override
            public void run() {
                refreshDrawableState();
            }
        });
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();

        // We keep in the pressed state until resetDrawableState() is called to
        // reset the press
        // feedback
        if (isPressed()) {
            setAlpha(PRESS_ALPHA);
            if (mPressedCallback != null) {
                mPressedCallback.iconPressed(this);
            }
        } else if (!mLockDrawableState) {
            setAlpha(1f);
        }
    }
}

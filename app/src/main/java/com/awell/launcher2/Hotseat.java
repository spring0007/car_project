/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.awell.launcher.library.R;

public class Hotseat extends FrameLayout {
    @SuppressWarnings("unused")
    private static final String TAG = "Hotseat";

    private Launcher mLauncher;
    private CellLayout mContent;

    private int mCellCountX;
    private int mCellCountY;
    private int mAllAppsButtonRank;
    private int mSettingsButtonRank;
    private int mAutoNaviButtonRank;
    private SparseArray<Integer> iconPaddingArray = new SparseArray<>();
    private boolean mTransposeLayoutWithOrientation;
    private boolean mIsLandscape;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Hotseat, defStyle, 0);
        Resources r = context.getResources();
        mCellCountX = a.getInt(R.styleable.Hotseat_cellCountX, -1);
        mCellCountY = a.getInt(R.styleable.Hotseat_cellCountY, -1);
        mAllAppsButtonRank = r.getInteger(R.integer.hotseat_all_apps_index);
        mSettingsButtonRank = r.getInteger(R.integer.hotseat_settings_index);
        mAutoNaviButtonRank = r.getInteger(R.integer.hotseat_auto_navi_index);
        mTransposeLayoutWithOrientation =
                r.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        mIsLandscape = context.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
//        mIsLandscape = true;
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        setOnKeyListener(new HotseatIconKeyEventListener());
    }

    CellLayout getLayout() {
        return mContent;
    }

    private boolean hasVerticalHotseat() {
        return (mIsLandscape && mTransposeLayoutWithOrientation);
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    int getOrderInHotseat(int x, int y) {
        return hasVerticalHotseat() ? (mContent.getCountY() - y - 1) : x;
    }

    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    int getCellXFromOrder(int rank) {
        return hasVerticalHotseat() ? 0 : rank;
    }

    int getCellYFromOrder(int rank) {
        return hasVerticalHotseat() ? (mContent.getCountY() - (rank + 1)) : 0;
    }

    public boolean isAllAppsButtonRank(int rank) {
        return rank == mAllAppsButtonRank;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mCellCountX < 0) mCellCountX = LauncherModel.getCellCountX();
        if (mCellCountY < 0) mCellCountY = LauncherModel.getCellCountY();
        mContent = (CellLayout) findViewById(R.id.layout);
        mContent.setGridSize(mCellCountX, mCellCountY);
        mContent.setIsHotseat(true);

        resetLayout();
    }

    void resetLayout() {
        mContent.removeAllViewsInLayout();
        int x, y;

        // Add the Apps button
        Context context = getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        BubbleTextView allAppsButton = (BubbleTextView)
                inflater.inflate(R.layout.application, mContent, false);
        allAppsButton.setCompoundDrawablesWithIntrinsicBounds(null,
                context.getResources().getDrawable(R.drawable.all_apps_button_icon), null, null);
//        allAppsButton.setPadding(0, 40, 0, 0);

        allAppsButton.setContentDescription(context.getString(R.string.all_apps_button_label));
//        allAppsButton.setText(context.getString(R.string.all_apps_button_label));
        allAppsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mLauncher != null &&
                        (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    mLauncher.onTouchDownAllAppsButton(v);
                }
                return false;
            }
        });

        allAppsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if (mLauncher != null) {

                    mLauncher.onClickAllAppsButton(v);

                }
            }
        });

        // Note: We do this to ensure that the hotseat is always laid out in the orientation of
        // the hotseat in order regardless of which orientation they were added
        x = getCellXFromOrder(mAllAppsButtonRank);
        y = getCellYFromOrder(mAllAppsButtonRank);
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x, y, 1, 1);
        lp.canReorder = false;
        mContent.addViewToCellLayout(allAppsButton, -1, 0, lp, true);

        //add other  by rtd
//        resetLayout_add(context);

        Log.e(TAG, "resetLayout() add allAppsButton at [" + x + ", " + y + "]");
    }


    void resetLayout_add(Context context) {
        // Add the Apps button
        int x, y;

        LayoutInflater inflater = LayoutInflater.from(context);
        BubbleTextView settingsButton = (BubbleTextView) inflater.inflate(R.layout.hotseat_rtd, mContent, false);
        BubbleTextView autoNaviButton = (BubbleTextView) inflater.inflate(R.layout.hotseat_rtd, mContent, false); 
        
  /*      settingsButton.setCompoundDrawablesWithIntrinsicBounds(null,
                context.getResources().getDrawable(R.drawable.settings_button), null, null);*/
//        settingsButton.setPadding(0, 42, 0, 0);
//        settingsButton.setContentDescription(context.getString(R.string.settings_button_label));
//        settingsButton.setText(context.getString(R.string.settings_button_label));
        settingsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mLauncher != null &&
                        (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    mLauncher.onTouchDownAllAppsButton(v);
                }
                return false;
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if (mLauncher != null) {

                    mLauncher.onClickSettingsButton(v);

                }
            }
        });

        //add other
        x = getCellXFromOrder(mSettingsButtonRank);
        y = getCellYFromOrder(mSettingsButtonRank);
        CellLayout.LayoutParams lp1 = new CellLayout.LayoutParams(x, y, 1, 1);
        lp1.canReorder = false;
        mContent.addViewToCellLayout(settingsButton, -1, 0, lp1, true);
        
      /*  autoNaviButton.setCompoundDrawablesWithIntrinsicBounds(null,
                context.getResources().getDrawable(R.drawable.auto_navi), null, null);*/
//        settingsButton.setPadding(0, 42, 0, 0);
        /*autoNaviButton.setContentDescription(context.getString(R.string.auto_navi_button_label)+"");
        autoNaviButton.setText(context.getString(R.string.auto_navi_button_label)+"");
       */
        autoNaviButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mLauncher != null &&
                        (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    mLauncher.onTouchDownAllAppsButton(v);
                }
                return false;
            }
        });

        autoNaviButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if (mLauncher != null) {

                    mLauncher.onClickAutoNaviButton(v);

                }
            }
        });

        x = getCellXFromOrder(mAutoNaviButtonRank);
        y = getCellYFromOrder(mAutoNaviButtonRank);
        CellLayout.LayoutParams lp2 = new CellLayout.LayoutParams(x, y, 1, 1);
        lp2.canReorder = false;
        mContent.addViewToCellLayout(autoNaviButton, -1, 0, lp2, true);

    }

}

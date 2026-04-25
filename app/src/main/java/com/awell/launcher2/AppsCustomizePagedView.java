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

import static com.awell.launcher2.LauncherApplication.getmAppContext;
import static com.awell.utils.Utils.startActivitySafely;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.awell.control.AppsCustomizeConfig;
import com.awell.launcher.library.R;
import com.awell.launcher2.DropTarget.DragObject;
import com.awell.control.AppsCustomizeControl;
import com.awell.utils.LogUtil;
import com.awell.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * A simple callback interface which also provides the results of the task.
 */
interface AsyncTaskCallback {
    void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data);
}

/**
 * The data needed to perform either of the custom AsyncTasks.
 */
class AsyncTaskPageData {
    enum Type {
        LoadWidgetPreviewData
    }

    AsyncTaskPageData(int p, ArrayList<Object> l, ArrayList<Bitmap> si, AsyncTaskCallback bgR, AsyncTaskCallback postR) {
        page = p;
        items = l;
        sourceImages = si;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = maxImageHeight = -1;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }

    AsyncTaskPageData(int p, ArrayList<Object> l, int cw, int ch, AsyncTaskCallback bgR, AsyncTaskCallback postR) {
        page = p;
        items = l;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = cw;
        maxImageHeight = ch;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }

    void cleanup(boolean cancelled) {
        // Clean up any references to source/generated bitmaps
        if (sourceImages != null) {
            if (cancelled) {
                for (Bitmap b : sourceImages) {
                    b.recycle();
                }
            }
            sourceImages.clear();
        }
        if (generatedImages != null) {
            if (cancelled) {
                for (Bitmap b : generatedImages) {
                    b.recycle();
                }
            }
            generatedImages.clear();
        }
    }

    int page;
    ArrayList<Object> items;
    ArrayList<Bitmap> sourceImages;
    ArrayList<Bitmap> generatedImages;
    int maxImageWidth;
    int maxImageHeight;
    AsyncTaskCallback doInBackgroundCallback;
    AsyncTaskCallback postExecuteCallback;
}

/**
 * A generic template for an async task used in AppsCustomize.
 */
class AppsCustomizeAsyncTask extends AsyncTask<AsyncTaskPageData, Void, AsyncTaskPageData> {
    AppsCustomizeAsyncTask(int p, AsyncTaskPageData.Type ty) {
        page = p;
        threadPriority = Process.THREAD_PRIORITY_DEFAULT;
        dataType = ty;
    }

    @Override
    protected AsyncTaskPageData doInBackground(AsyncTaskPageData... params) {
        if (params.length != 1) return null;
        // Load each of the widget previews in the background
        params[0].doInBackgroundCallback.run(this, params[0]);
        return params[0];
    }

    @Override
    protected void onPostExecute(AsyncTaskPageData result) {
        // All the widget previews are loaded, so we can just callback to
        // inflate the page
        result.postExecuteCallback.run(this, result);
    }

    void setThreadPriority(int p) {
        threadPriority = p;
    }

    void syncThreadPriority() {
        Process.setThreadPriority(threadPriority);
    }

    // The page that this async task is associated with
    AsyncTaskPageData.Type dataType;
    int page;
    int threadPriority;
}

abstract class WeakReferenceThreadLocal<T> {
    private ThreadLocal<WeakReference<T>> mThreadLocal;

    public WeakReferenceThreadLocal() {
        mThreadLocal = new ThreadLocal<WeakReference<T>>();
    }

    abstract T initialValue();

    public void set(T t) {
        mThreadLocal.set(new WeakReference<T>(t));
    }

    public T get() {
        WeakReference<T> reference = mThreadLocal.get();
        T obj;
        if (reference == null) {
            obj = initialValue();
            mThreadLocal.set(new WeakReference<T>(obj));
            return obj;
        } else {
            obj = reference.get();
            if (obj == null) {
                obj = initialValue();
                mThreadLocal.set(new WeakReference<T>(obj));
            }
            return obj;
        }
    }
}

class CanvasCache extends WeakReferenceThreadLocal<Canvas> {
    @Override
    protected Canvas initialValue() {
        return new Canvas();
    }
}

class PaintCache extends WeakReferenceThreadLocal<Paint> {
    @Override
    protected Paint initialValue() {
        return null;
    }
}

class BitmapCache extends WeakReferenceThreadLocal<Bitmap> {
    @Override
    protected Bitmap initialValue() {
        return null;
    }
}

class RectCache extends WeakReferenceThreadLocal<Rect> {
    @Override
    protected Rect initialValue() {
        return new Rect();
    }
}

/**
 * The Apps/Customize page that displays all the applications, widgets, and
 * shortcuts.
 */
public class AppsCustomizePagedView extends PagedViewWithDraggableItems implements View.OnClickListener, View.OnKeyListener, DragSource, PagedViewIcon.PressedCallback, PagedViewWidget.ShortPressListener, LauncherTransitionable {
    static final String TAG = "AppsCustomizePagedView";

    /**
     * The different content types that this paged view can show.
     */
    public enum ContentType {
        Applications, Widgets
    }

    // Refs
    private Launcher mLauncher;
    private DragController mDragController;
    private final LayoutInflater mLayoutInflater;
    private final PackageManager mPackageManager;

    // Save and Restore
    private int mSaveInstanceStateItemIndex = -1;
    private PagedViewIcon mPressedIcon;

    // Content
    private ArrayList<ApplicationInfo> mApps;
    private ArrayList<Object> mWidgets;

    // Cling
    private boolean mHasShownAllAppsCling;
    private int mClingFocusedX;
    private int mClingFocusedY;

    // Caching
    private Canvas mCanvas;
    private IconCache mIconCache;

    // Dimens
    private int mContentWidth;
    private int mAppIconSize;
    private int mMaxAppCellCountX, mMaxAppCellCountY;
    private int mWidgetCountX, mWidgetCountY;
    private int mWidgetWidthGap, mWidgetHeightGap;
    private final float sWidgetPreviewIconPaddingPercentage = 0.25f;
    private PagedViewCellLayout mWidgetSpacingLayout;
    private int mNumAppsPages = 0;
    private int mNumWidgetPages;

    private int mAppIconPadding = 0;
    private int mPageBackgroundResId = 0;

    // 保存从 XML 读取的原始页面间距默认值，用于重置
    private int mCustomPageLayoutWidthGap = -1;
    private int mCustomPageLayoutHeightGap = -1;

    // Relating to the scroll and overscroll effects
    Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
    private static float CAMERA_DISTANCE = 6500;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);

    // Previews & outlines
    ArrayList<AppsCustomizeAsyncTask> mRunningTasks;
    private static final int sPageSleepDelay = 200;
    private static final int defaultAppColumnCount = 7;//默认每行显示的应用图标数量
    private static final int defaultAppRowCount = 2;//默认每列显示的应用图标数量
    private Runnable mInflateWidgetRunnable = null;
    private Runnable mBindWidgetRunnable = null;
    static final int WIDGET_NO_CLEANUP_REQUIRED = -1;
    static final int WIDGET_PRELOAD_PENDING = 0;
    static final int WIDGET_BOUND = 1;
    static final int WIDGET_INFLATED = 2;
    int mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
    int mWidgetLoadingId = -1;
    PendingAddWidgetInfo mCreateWidgetInfo = null;
    private boolean mDraggingWidget = false;

    private Toast mWidgetInstructionToast;

    // Deferral of loading widget previews during launcher transitions
    private boolean mInTransition;
    private ArrayList<AsyncTaskPageData> mDeferredSyncWidgetPageItems = new ArrayList<AsyncTaskPageData>();
    private ArrayList<Runnable> mDeferredPrepareLoadWidgetPreviewsTasks = new ArrayList<Runnable>();

    private Rect mTmpRect = new Rect();

    private static final String LAUNCHER_PLUGIN_CLAZZ = "persist.sys.launcher.clazz";
    private AppsCustomizeConfig mAppsCustomizeConfig = null;

    // 防抖机制：防止重复调用 invalidatePageData 导致多次 syncPages
    private boolean mPendingInvalidatePageData = false;
    private Runnable mPendingInvalidatePageDataRunnable = null;
    private boolean mIsSyncingPages = false; // 标记是否正在执行 syncPages


    /**
     * 批量重置所有图标属性（只在最后刷新一次布局）
     * @return 是否重置成功
     */
    public void resetAllAppAttributes() {
        LogUtil.d("resetAllAppAttributes: resetting all app attributes...");
        this.mAppsCustomizeConfig = null;
        mAppIconSize =  getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        mPageBackgroundResId = 0;
        
        // 重置 mWidgetSpacingLayout 到默认值，防止之前 plugin 的设置影响当前 plugin
        if (mWidgetSpacingLayout != null) {
            mWidgetSpacingLayout.resetToDefaultCellDimensions();
          //  LogUtil.d("resetAllAppAttributes: mWidgetSpacingLayout reset to default dimensions");
        }
        
        // 重置页面间距为从 XML 读取的原始默认值
        // 不同屏幕尺寸有不同的默认值（如 sw600dp-land-mdpi 是 45dp）
        mCustomPageLayoutWidthGap = mPageLayoutWidthGap ;
        mCustomPageLayoutHeightGap = mPageLayoutHeightGap ;
        //LogUtil.d("resetAllAppAttributes: reset gaps to original defaults - width=" + mCustomPageLayoutWidthGap
        //        + ", height=" + mCustomPageLayoutHeightGap +",isDataReady="+isDataReady());
        
        // 统一刷新布局（只调用一次）
        if (isDataReady()) {
            // 关键修复：使用父容器的可用高度，而不是当前已缩小的测量值
            // 避免 wrap_content 导致的恶性循环：570dp -> 441px -> 更小 -> ...
            int availableWidth = getMeasuredWidth();
            int availableHeight = getMeasuredHeight();
            
            // 如果当前高度异常小（可能是上次切换导致的），使用父容器高度
            ViewParent parent = getParent();
            if (parent instanceof View) {
                View parentView = (View) parent;
                int parentHeight = parentView.getMeasuredHeight();
                // 如果父容器高度合理且当前高度明显偏小，使用父容器高度
                if (parentHeight > 0 && availableHeight < parentHeight) {
                    availableHeight = parentHeight - getPaddingTop() - getPaddingBottom();
                   // LogUtil.d("resetAllAppAttributes: using parent height instead of current - parent=" + parentHeight
                   //         + ", current=" + getMeasuredHeight() + ", adjusted=" + availableHeight);
                }
            }
            
            onDataReady(availableWidth, availableHeight);
          //  LogUtil.d("resetAllAppAttributes: layout refreshed once, width=" + availableWidth + ", height=" + availableHeight);
        }
    }

    /**
     * 处理来自 AppsCustomizeControl 的配置对象
     * 此方法负责数据的解析和应用，实现数据处理逻辑
     * @param configObj 配置对象（Kotlin data class）
     */
    public void processAppsCustomizeConfig(AppsCustomizeConfig configObj) {
        if (configObj == null) {
            LogUtil.w("processAppsCustomizeConfig: config is null, resetting to default");
            resetAllAppAttributes();
            return;
        }
        mAppsCustomizeConfig = configObj;
        if(configObj.getBackgroundTheme() <=0){
            mPageBackgroundResId = 0;
        }else{
            mPageBackgroundResId =R.drawable.rect_svg;
        }
        if (mWidgetSpacingLayout != null) {
            mWidgetSpacingLayout.resetToDefaultCellDimensions();
        //    LogUtil.d("resetAllAppAttributes: mWidgetSpacingLayout reset to default dimensions");
        }
        if (isDataReady()) {
            // 关键修复：使用父容器的可用高度，避免累积缩小
            int availableWidth = getMeasuredWidth();
            int availableHeight = getMeasuredHeight();
            
            ViewParent parent = getParent();
            if (parent instanceof View) {
                View parentView = (View) parent;
                int parentHeight = parentView.getMeasuredHeight();
                if (parentHeight > 0 && availableHeight < parentHeight ) {
                    availableHeight = parentHeight - getPaddingTop() - getPaddingBottom();
                //    LogUtil.d("processAppsCustomizeConfig: using parent height - parent=" + parentHeight
                //            + ", current=" + getMeasuredHeight() + ", adjusted=" + availableHeight);
                }
            }
            
            onDataReady(availableWidth, availableHeight);
         //   LogUtil.d("processAppsCustomizeConfig: layout refreshed once, width=" + availableWidth + ", height=" + availableHeight);
        }
       // LogUtil.d("processAppsCustomizeConfig: processing configuration...");
    }

    public void setActivity(Context context) {
        mActivity = new WeakReference<>(context);
    }

    public Context getUiActivity() {
        if (mActivity != null) {
            return mActivity.get();
        }
        return null;
    }

    private WeakReference<Context> mActivity = null;

    // Used for drawing shortcut previews
    BitmapCache mCachedShortcutPreviewBitmap = new BitmapCache();
    PaintCache mCachedShortcutPreviewPaint = new PaintCache();
    CanvasCache mCachedShortcutPreviewCanvas = new CanvasCache();

    // Used for drawing widget previews
    CanvasCache mCachedAppWidgetPreviewCanvas = new CanvasCache();
    RectCache mCachedAppWidgetPreviewSrcRect = new RectCache();
    RectCache mCachedAppWidgetPreviewDestRect = new RectCache();
    PaintCache mCachedAppWidgetPreviewPaint = new PaintCache();

    public AppsCustomizePagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mPackageManager = context.getPackageManager();
        mApps = new ArrayList<ApplicationInfo>();
        mWidgets = new ArrayList<Object>();
        mIconCache = ((LauncherApplication) context.getApplicationContext()).getIconCache();
        mCanvas = new Canvas();
        mRunningTasks = new ArrayList<AppsCustomizeAsyncTask>();

        // Save the default widget preview background
        Resources resources = context.getResources();
        // 初始化默认图标大小，如果 plugin 未调用 setAppIconSize，将使用此默认值
        mAppIconSize = resources.getDimensionPixelSize(R.dimen.app_icon_size);
      //  LogUtil.d("AppsCustomizePagedView: mAppIconSize = " + mAppIconSize);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0, 0);
        mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountX, -1);
        mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountY, -1);
        mWidgetWidthGap = a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellWidthGap, 0);
        mWidgetHeightGap = a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellHeightGap, 0);
        mWidgetCountX = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 7);
        mWidgetCountY = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 2);
        mClingFocusedX = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedX, 0);
        mClingFocusedY = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedY, 0);
        a.recycle();
        mWidgetSpacingLayout = new PagedViewCellLayout(getContext());

        // 保存从父类构造函数中读取的原始页面间距默认值
        // 这些值是从 XML 布局属性中读取的（如 values-sw600dp-land-mdpi-v13 中的 45dp）
        mCustomPageLayoutWidthGap = mPageLayoutWidthGap;
        mCustomPageLayoutHeightGap = mPageLayoutHeightGap;
       // LogUtil.d("AppsCustomizePagedView: saved default gaps - width=" + mCustomPageLayoutWidthGap
       //         + ", height=" + mCustomPageLayoutHeightGap);

        // The padding on the non-matched dimension for the default widget
        // preview icons
        // (top + bottom)
        mFadeInAdjacentScreens = false;

        // Unless otherwise specified this view is important for accessibility.
        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    @Override
    protected void init() {
        super.init();
        mCenterPagesVertically = false;

        Context context = getContext();
        Resources r = context.getResources();
        setDragSlopeThreshold(r.getInteger(R.integer.config_appsCustomizeDragSlopeThreshold) / 100f);
    }

    /**
     * Returns the item index of the center item on this page so that we can
     * restore to this item index when we rotate.
     */
    private int getMiddleComponentIndexOnCurrentPage() {
        int i = -1;
        if (getPageCount() > 0) {
            int currentPage = getCurrentPage();
            if (currentPage < mNumAppsPages) {
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountY;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = (currentPage * numItemsPerPage) + (childCount / 2);
                }
            } else {
                int numApps = mApps.size();
                PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(currentPage);
                int numItemsPerPage = mWidgetCountX * mWidgetCountY;
                int childCount = layout.getChildCount();
                if (childCount > 0) {
                    i = numApps + ((currentPage - mNumAppsPages) * numItemsPerPage) + (childCount / 2);
                }
            }
        }
        return i;
    }

    /**
     * Get the index of the item to restore to if we need to restore the current
     * page.
     */
    int getSaveInstanceStateIndex() {
        if (mSaveInstanceStateItemIndex == -1) {
            mSaveInstanceStateItemIndex = getMiddleComponentIndexOnCurrentPage();
        }
        return mSaveInstanceStateItemIndex;
    }

    /**
     * Returns the page in the current orientation which is expected to contain
     * the specified item index.
     */
    int getPageForComponent(int index) {
        if (index < 0) return 0;

        if (index < mApps.size()) {
            int numItemsPerPage = mCellCountX * mCellCountY;
            return (index / numItemsPerPage);
        } else {
            int numItemsPerPage = mWidgetCountX * mWidgetCountY;
            return mNumAppsPages + ((index - mApps.size()) / numItemsPerPage);
        }
    }

    /**
     * Restores the page for an item at the specified index
     */
    void restorePageForIndex(int index) {
        if (index < 0) return;
        mSaveInstanceStateItemIndex = index;
    }

    private void updatePageCounts() {

//		mNumWidgetPages = (int) Math.ceil(mWidgets.size() / (mWidgetCountX * mWidgetCountY)); // by jxy (float)
        mNumAppsPages = (int) Math.ceil((float) mApps.size() / (mCellCountX * mCellCountY));
    //    LogUtil.i("updatePageCounts: huang mNumAppsPages=>" + mNumAppsPages + " mApps.size()=>" + mApps.size() + " count=>" + (mCellCountX * mCellCountY));
        if (mNumAppsPages == 1) {
            if (getPageIndication() != null) {
                getPageIndication().setVisibility(View.GONE);
            }
        } else {
            if (getPageIndication() != null) {
                getPageIndication().setVisibility(View.VISIBLE);
            }
        }
    }

    protected void onDataReady(int width, int height) {
        // Note that we transpose the counts in portrait so that we get a
        // similar layout
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int maxCellCountX = Integer.MAX_VALUE;
        int maxCellCountY = Integer.MAX_VALUE;
        if (LauncherApplication.isScreenLarge()) {
            maxCellCountX = (isLandscape ? LauncherModel.getCellCountX() : LauncherModel.getCellCountY());
            maxCellCountY = (isLandscape ? LauncherModel.getCellCountY() : LauncherModel.getCellCountX());
        }


        if (mMaxAppCellCountX > -1) {
            maxCellCountX = Math.min(maxCellCountX, mMaxAppCellCountX);
        }

        // 使用默认布局或 setAppCountPerRow 设置的值
        if (mAppsCustomizeConfig!=null && mAppsCustomizeConfig.getColumnCount() > 0) {
            maxCellCountX = mAppsCustomizeConfig.getColumnCount(); // 根据提供的接口数量显示
        } else {
            maxCellCountX = defaultAppColumnCount;
        }
        if (mAppsCustomizeConfig!=null && mAppsCustomizeConfig.getRowCount() > 0) {
            maxCellCountY = mAppsCustomizeConfig.getRowCount(); // 根据提供的接口数量显示
        }else {
            maxCellCountY = defaultAppRowCount;
        }
       // LogUtil.d("onDataReady: using DEFAULT layout, columnCount=" + maxCellCountX + ", rowCount=" + maxCellCountY);


        // Temp hack for now: only use the max cell count Y for widget layout
        int maxWidgetCellCountY = maxCellCountY;

        /*if (mMaxAppCellCountY > -1) {
            maxWidgetCellCountY = Math.min(maxWidgetCellCountY, mMaxAppCellCountY);
        }
        LogUtil.d("onDataReady: using DEFAULT layout, maxCellCountY=" + maxCellCountY + ", mMaxAppCellCountY=" + mMaxAppCellCountY);*/
        // Now that the data is ready, we can calculate the content width, the
        // number of cells to
        // use for each page
        // 处理页面间距：如果配置对象存在但未设置，则使用从 XML 读取的原始默认值
        if (mAppsCustomizeConfig != null && mAppsCustomizeConfig.getAutoWidthGap()) {
            mCustomPageLayoutWidthGap = -1;
        } else {
            mCustomPageLayoutWidthGap = mPageLayoutWidthGap;
        }

        if (mAppsCustomizeConfig != null && mAppsCustomizeConfig.getAutoHeightGap()) {
            mCustomPageLayoutHeightGap = -1;
        } else {
            mCustomPageLayoutHeightGap = mPageLayoutHeightGap;
        }
       // LogUtil.d("onDataReady: config applied - mCustomPageLayoutWidthGap=" + mCustomPageLayoutWidthGap
        //        + ", mCustomPageLayoutHeightGap=" + mCustomPageLayoutHeightGap);

        mWidgetSpacingLayout.setGap(mCustomPageLayoutWidthGap, mCustomPageLayoutHeightGap);
        mWidgetSpacingLayout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop, mPageLayoutPaddingRight, mPageLayoutPaddingBottom);
        
        // 如果设置了自定义 Cell 尺寸，则直接设置到 PagedViewCellLayout
        if (mAppsCustomizeConfig!=null  && mAppsCustomizeConfig.getCellWidthDp() > 0 ) {
            int cellWidthPx = (int) (mAppsCustomizeConfig.getCellWidthDp());
            mWidgetSpacingLayout.setCustomCellWidth(cellWidthPx);
        }

        if (mAppsCustomizeConfig!=null  && mAppsCustomizeConfig.getCellHeightDp() > 0 ) {
            int cellHeightPx = (int) (mAppsCustomizeConfig.getCellHeightDp());
            mWidgetSpacingLayout.setCustomCellHeight(cellHeightPx);
        }

        mWidgetSpacingLayout.calculateCellCount(width, height, maxCellCountX, maxWidgetCellCountY);
        mCellCountX = mWidgetSpacingLayout.getCellCountX();
        mCellCountY = mWidgetSpacingLayout.getCellCountY();
        updatePageCounts();

        // Force a measure to update recalculate the gaps
        // 优化：只在必要时才进行二次测量和计算
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if (measuredWidth > 0 && measuredHeight > 0) {
            int widthSpec = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.AT_MOST);
            int heightSpec = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST);
            // 避免重复计算，直接测量获取最终结果
            mWidgetSpacingLayout.measure(widthSpec, heightSpec);
            mContentWidth = mWidgetSpacingLayout.getContentWidth();
        }

        AppsCustomizeTabHost host = (AppsCustomizeTabHost) getTabHost();
        final boolean hostIsTransitioning = host != null && host.isTransitioning();

        // Restore the page
        int page = getPageForComponent(mSaveInstanceStateItemIndex);
        
        // 优化：立即更新页面指示器，延迟重量级的页面数据同步
        // 先更新页面计数到指示器
        if (getPageIndication() != null) {
            getPageIndication().setMTotalPages(mNumAppsPages);
            getPageIndication().setMCurrentPage(0);
            getPageIndication().invalidate();
        }
        
        // 防抖：取消之前的 pending 任务，只保留最新的调用
        if (mPendingInvalidatePageData && mPendingInvalidatePageDataRunnable != null) {
            removeCallbacks(mPendingInvalidatePageDataRunnable);
         //   LogUtil.d("onDataReady: cancelled previous pending invalidatePageData to avoid duplicate syncPages");
        }
        
        // 延迟页面数据失效，避免在测量阶段触发重量级操作
        final int finalPage = Math.max(0, page);
        final boolean finalHostIsTransitioning = hostIsTransitioning;
        
        mPendingInvalidatePageDataRunnable = new Runnable() {
            @Override
            public void run() {
                mPendingInvalidatePageData = false;
             //   LogUtil.d("onDataReady: executing deferred invalidatePageData - page=" + finalPage);
                invalidatePageData(finalPage, finalHostIsTransitioning);
            }
        };
        
        mPendingInvalidatePageData = true;
        postDelayed(mPendingInvalidatePageDataRunnable, 50); // 增加延迟到50ms以合并多次调用

        // Show All Apps cling if we are finished transitioning, otherwise, we
        // will try again when
        // the transition completes in AppsCustomizeTabHost (otherwise the wrong
        // offsets will be
        // returned while animating)
        if (!hostIsTransitioning) {
            post(new Runnable() {
                @Override
                public void run() {
                    showAllAppsCling();
                }
            });
        }
    }

    void showAllAppsCling() {
        if (!mHasShownAllAppsCling && isDataReady()) {
            mHasShownAllAppsCling = true;
            // Calculate the position for the cling punch through
            int[] offset = new int[2];
            int[] pos = mWidgetSpacingLayout.estimateCellPosition(mClingFocusedX, mClingFocusedY);
            if (mLauncher != null)
                mLauncher.getDragLayer().getLocationInDragLayer(this, offset);
            // PagedViews are centered horizontally but top aligned
            pos[0] += (getMeasuredWidth() - mWidgetSpacingLayout.getMeasuredWidth()) / 2 + offset[0];
            pos[1] += offset[1];
            if (mLauncher != null)
                mLauncher.showFirstRunAllAppsCling(pos);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!isDataReady()) {
            if (!mApps.isEmpty()/* && !mWidgets.isEmpty()*/) {
                setDataIsReady();
                setMeasuredDimension(width, height);
                onDataReady(width, height);
            }
        } else {
            // 数据已就绪时，避免重复测量，直接使用缓存的尺寸
            if (getMeasuredWidth() == width && getMeasuredHeight() == height) {
                // 尺寸未变化，跳过父类的完整测量流程
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void onPackagesUpdated() {
        // Get the list of widgets and shortcuts
        mWidgets.clear();
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(getContext()).getInstalledProviders();
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = mPackageManager.queryIntentActivities(shortcutsIntent, 0);
        for (AppWidgetProviderInfo widget : widgets) {
            if (widget.minWidth > 0 && widget.minHeight > 0) {
                // Ensure that all widgets we show can be added on a workspace
                // of this size
                int[] spanXY = Launcher.getSpanForWidget(getContext(), widget);
                int[] minSpanXY = Launcher.getMinSpanForWidget(getContext(), widget);
                int minSpanX = Math.min(spanXY[0], minSpanXY[0]);
                int minSpanY = Math.min(spanXY[1], minSpanXY[1]);
                if (minSpanX <= LauncherModel.getCellCountX() && minSpanY <= LauncherModel.getCellCountY()) {
                    mWidgets.add(widget);
                } else {
                    //Log.e(TAG, "Widget " + widget.provider + " can not fit on this device (" + widget.minWidth + ", " + widget.minHeight + ")");
                }
            } else {
                //Log.e(TAG, "Widget " + widget.provider + " has invalid dimensions (" + widget.minWidth + ", " + widget.minHeight + ")");
            }
        }
        mWidgets.addAll(shortcuts);
        Collections.sort(mWidgets, new LauncherModel.WidgetAndShortcutNameComparator(mPackageManager));
        updatePageCounts();
        invalidateOnDataChange();
    }

    @Override
    public void onClick(View v) {
        // When we have exited all apps or are in transition, disregard clicks
    //    LogUtil.i("onClick: huang v=>" + v);

        //if (!mLauncher.isAllAppsVisible() || mLauncher.getWorkspace().isSwitchingState()) return;

        if (v instanceof PagedViewIcon) {
            // Animate some feedback to the click
            final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();

            // Lock the drawable state to pressed until we return to Launcher
            //huangxw 2025/7/1 not set the effect
            //if (mPressedIcon != null) {
            //    mPressedIcon.lockDrawableState();
            //}

            // NOTE: We want all transitions from launcher to act as if the
            // wallpaper were enabled
            // to be consistent. So re-enable the flag here, and we will
            // re-disable it as necessary
            // when Launcher resumes and we are still in AllApps.
            //mLauncher.updateWallpaperVisibility(true);
            String currentPluginUi = SystemProperties.get(LAUNCHER_PLUGIN_CLAZZ, "com.example.plugin1.UIActivity");
            Context context = getmAppContext();
            if (getUiActivity() != null && currentPluginUi != null &&
                    getUiActivity().toString().contains(currentPluginUi)) {
                //使用Plugin的Activity上下文
                context = getUiActivity();
               // LogUtil.i("onClick: huang use plugin context=>" + context);
            }

            startActivitySafely(v, context, appInfo.intent, appInfo);
          //  LogUtil.i("appinfo huang packageName = " + appInfo.componentName.getPackageName() + ",className = " + appInfo.componentName.getClassName());
        } else if (v instanceof PagedViewWidget) {
            // Let the user know that they have to long press to add a widget
            if (mWidgetInstructionToast != null) {
                mWidgetInstructionToast.cancel();
            }
            mWidgetInstructionToast = Toast.makeText(getContext(), R.string.long_press_widget_to_add, Toast.LENGTH_SHORT);
            mWidgetInstructionToast.show();

            // Create a little animation to show that the widget can move
            float offsetY = getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);
            final ImageView p = (ImageView) v.findViewById(R.id.widget_preview);
            AnimatorSet bounce = LauncherAnimUtils.createAnimatorSet();
            ValueAnimator tyuAnim = LauncherAnimUtils.ofFloat(p, "translationY", offsetY);
            tyuAnim.setDuration(125);
            ValueAnimator tydAnim = LauncherAnimUtils.ofFloat(p, "translationY", 0f);
            tydAnim.setDuration(100);
            bounce.play(tyuAnim).before(tydAnim);
            bounce.setInterpolator(new AccelerateInterpolator());
            bounce.start();
        }
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeKeyEvent(v, keyCode, event);
    }

    /*
     * PagedViewWithDraggableItems implementation
     */
    @Override
    protected void determineDraggingStart(android.view.MotionEvent ev) {
        // Disable dragging by pulling an app down for now.
    }

    private void beginDraggingApplication(View v) {
        mLauncher.getWorkspace().onDragStartedWithItem(v);
        mLauncher.getWorkspace().beginDragShared(v, this);
    }

    Bundle getDefaultOptionsForWidget(Launcher launcher, PendingAddWidgetInfo info) {
        Bundle options = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            AppWidgetResizeFrame.getWidgetSizeRanges(mLauncher, info.spanX, info.spanY, mTmpRect);
            Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(mLauncher, info.componentName, null);

            float density = getResources().getDisplayMetrics().density;
            int xPaddingDips = (int) ((padding.left + padding.right) / density);
            int yPaddingDips = (int) ((padding.top + padding.bottom) / density);

            options = new Bundle();
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, mTmpRect.left - xPaddingDips);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, mTmpRect.top - yPaddingDips);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, mTmpRect.right - xPaddingDips);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, mTmpRect.bottom - yPaddingDips);
        }
        return options;
    }

    private void preloadWidget(final PendingAddWidgetInfo info) {
        final AppWidgetProviderInfo pInfo = info.info;
        final Bundle options = getDefaultOptionsForWidget(mLauncher, info);

        if (pInfo.configure != null) {
            info.bindOptions = options;
            return;
        }

        mWidgetCleanupState = WIDGET_PRELOAD_PENDING;
        mBindWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                mWidgetLoadingId = mLauncher.getAppWidgetHost().allocateAppWidgetId();
                // Options will be null for platforms with JB or lower, so this
                // serves as an
                // SDK level check.
                if (options == null) {
                    if (AppWidgetManager.getInstance(mLauncher).bindAppWidgetIdIfAllowed(mWidgetLoadingId, info.componentName)) {
                        mWidgetCleanupState = WIDGET_BOUND;
                    }
                } else {
                    if (AppWidgetManager.getInstance(mLauncher).bindAppWidgetIdIfAllowed(mWidgetLoadingId, info.componentName, options)) {
                        mWidgetCleanupState = WIDGET_BOUND;
                    }
                }
            }
        };
        post(mBindWidgetRunnable);

        mInflateWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                if (mWidgetCleanupState != WIDGET_BOUND) {
                    return;
                }
                AppWidgetHostView hostView = mLauncher.getAppWidgetHost().createView(getContext(), mWidgetLoadingId, pInfo);
                info.boundWidget = hostView;
                mWidgetCleanupState = WIDGET_INFLATED;
                hostView.setVisibility(INVISIBLE);
                int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(info.spanX, info.spanY, info, false);

                // We want the first widget layout to be the correct size. This
                // will be important
                // for width size reporting to the AppWidgetManager.
                DragLayer.LayoutParams lp = new DragLayer.LayoutParams(unScaledSize[0], unScaledSize[1]);
                lp.x = lp.y = 0;
                lp.customPosition = true;
                hostView.setLayoutParams(lp);
                mLauncher.getDragLayer().addView(hostView);
            }
        };
        post(mInflateWidgetRunnable);
    }

    @Override
    public void onShortPress(View v) {
        // We are anticipating a long press, and we use this time to load bind
        // and instantiate
        // the widget. This will need to be cleaned up if it turns out no long
        // press occurs.
        if (mCreateWidgetInfo != null) {
            // Just in case the cleanup process wasn't properly executed. This
            // shouldn't happen.
            cleanupWidgetPreloading(false);
        }
        mCreateWidgetInfo = new PendingAddWidgetInfo((PendingAddWidgetInfo) v.getTag());

        //preloadWidget(mCreateWidgetInfo);
    }

    private void cleanupWidgetPreloading(boolean widgetWasAdded) {
        if (!widgetWasAdded) {
            // If the widget was not added, we may need to do further cleanup.
            PendingAddWidgetInfo info = mCreateWidgetInfo;
            mCreateWidgetInfo = null;

            if (mWidgetCleanupState == WIDGET_PRELOAD_PENDING) {
                // We never did any preloading, so just remove pending callbacks
                // to do so
                removeCallbacks(mBindWidgetRunnable);
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_BOUND) {
                // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // We never got around to inflating the widget, so remove the
                // callback to do so.
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_INFLATED) {
                // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // The widget was inflated and added to the DragLayer -- remove
                // it.
                AppWidgetHostView widget = info.boundWidget;
                mLauncher.getDragLayer().removeView(widget);
            }
        }
        mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
        mWidgetLoadingId = -1;
        mCreateWidgetInfo = null;
        PagedViewWidget.resetShortPressTarget();
    }

    @Override
    public void cleanUpShortPress(View v) {
        if (!mDraggingWidget) {
            cleanupWidgetPreloading(false);
        }
    }

    private boolean beginDraggingWidget(View v) {
        mDraggingWidget = true;
        // Get the widget preview as the drag representation
        ImageView image = (ImageView) v.findViewById(R.id.widget_preview);
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();

        // If the ImageView doesn't have a drawable yet, the widget preview
        // hasn't been loaded and
        // we abort the drag.
        if (image.getDrawable() == null) {
            mDraggingWidget = false;
            return false;
        }

        // Compose the drag image
        Bitmap preview;
        Bitmap outline;
        float scale = 1f;
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            // This can happen in some weird cases involving multi-touch. We
            // can't start dragging
            // the widget if this is null, so we break out.
            if (mCreateWidgetInfo == null) {
                return false;
            }

            PendingAddWidgetInfo createWidgetInfo = mCreateWidgetInfo;
            createItemInfo = createWidgetInfo;
            int spanX = createItemInfo.spanX;
            int spanY = createItemInfo.spanY;
            int[] size = mLauncher.getWorkspace().estimateItemSize(spanX, spanY, createWidgetInfo, true);

            FastBitmapDrawable previewDrawable = (FastBitmapDrawable) image.getDrawable();
            float minScale = 1.25f;
            int maxWidth, maxHeight;
            maxWidth = Math.min((int) (previewDrawable.getIntrinsicWidth() * minScale), size[0]);
            maxHeight = Math.min((int) (previewDrawable.getIntrinsicHeight() * minScale), size[1]);
            preview = getWidgetPreview(createWidgetInfo.componentName, createWidgetInfo.previewImage, createWidgetInfo.icon, spanX, spanY, maxWidth, maxHeight);

            // Determine the image view drawable scale relative to the preview
            float[] mv = new float[9];
            Matrix m = new Matrix();
            m.setRectToRect(new RectF(0f, 0f, (float) preview.getWidth(), (float) preview.getHeight()), new RectF(0f, 0f, (float) previewDrawable.getIntrinsicWidth(), (float) previewDrawable.getIntrinsicHeight()), Matrix.ScaleToFit.START);
            m.getValues(mv);
            scale = (float) mv[0];
        } else {
            PendingAddShortcutInfo createShortcutInfo = (PendingAddShortcutInfo) v.getTag();
            Drawable icon = mIconCache.getFullResIcon(createShortcutInfo.shortcutActivityInfo);
            preview = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            mCanvas.setBitmap(preview);
            mCanvas.save();
            renderDrawableToBitmap(icon, preview, 0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            mCanvas.restore();
            mCanvas.setBitmap(null);
            createItemInfo.spanX = createItemInfo.spanY = 1;
        }

        // Don't clip alpha values for the drag outline if we're using the
        // default widget preview
        boolean clipAlpha = !(createItemInfo instanceof PendingAddWidgetInfo && (((PendingAddWidgetInfo) createItemInfo).previewImage == 0));

        // Save the preview for the outline generation, then dim the preview
        outline = Bitmap.createScaledBitmap(preview, preview.getWidth(), preview.getHeight(), false);

        // Start the drag
        mLauncher.lockScreenOrientation();
        mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, outline, clipAlpha);
        if (mDragController != null) {
            mDragController.startDrag(image, preview, this, createItemInfo, DragController.DRAG_ACTION_COPY, null, scale);
        }
        outline.recycle();
        preview.recycle();
        return true;
    }

    @Override
    protected boolean beginDragging(final View v) {
        if (!super.beginDragging(v)) return false;

        if (v instanceof PagedViewIcon) {
            beginDraggingApplication(v);
        } else if (v instanceof PagedViewWidget) {
            if (!beginDraggingWidget(v)) {
                return false;
            }
        }

        // We delay entering spring-loaded mode slightly to make sure the UI
        // thready is free of any work.
        postDelayed(new Runnable() {
            @Override
            public void run() {
                // We don't enter spring-loaded mode if the drag has been
                // cancelled
                if (mLauncher.getDragController().isDragging()) {
                    // Dismiss the cling
                    mLauncher.dismissAllAppsCling(null);

                    // Reset the alpha on the dragged icon before we drag
                    resetDrawableState();

                    // Go into spring loaded mode (must happen before we
                    // startDrag())
                    mLauncher.enterSpringLoadedDragMode();
                }
            }
        }, 150);

        return true;
    }

    /**
     * Clean up after dragging.
     *
     * @param target where the item was dragged to (can be null if the item was
     *               flung)
     */
    private void endDragging(View target, boolean isFlingToDelete, boolean success) {
        if (isFlingToDelete || !success || (target != mLauncher.getWorkspace() && !(target instanceof DeleteDropTarget))) {
            // Exit spring loaded mode if we have not successfully dropped or
            // have not handled the
            // drop in Workspace
            mLauncher.exitSpringLoadedDragMode();
        }
        mLauncher.unlockScreenOrientation(false);
    }

    @Override
    public View getContent() {
        return null;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        mInTransition = true;
        if (toWorkspace) {
            cancelAllTasks();
        }
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        mInTransition = false;
        for (AsyncTaskPageData d : mDeferredSyncWidgetPageItems) {
            onSyncWidgetPageItems(d);
        }
        mDeferredSyncWidgetPageItems.clear();
        for (Runnable r : mDeferredPrepareLoadWidgetPreviewsTasks) {
            r.run();
        }
        mDeferredPrepareLoadWidgetPreviewsTasks.clear();
        mForceDrawAllChildrenNextFrame = !toWorkspace;
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete, boolean success) {
        // Return early and wait for onFlingToDeleteCompleted if this was the
        // result of a fling
        if (isFlingToDelete) return;

        endDragging(target, false, success);

        // Display an error message if the drag failed due to there not being
        // enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    layout.calculateSpans(itemInfo);
                    showOutOfSpaceMessage = !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage(false);
            }

            d.deferDragViewCleanupPostAnimation = false;
        }
        cleanupWidgetPreloading(success);
        mDraggingWidget = false;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // We just dismiss the drag when we fling, so cleanup here
        endDragging(null, true, true);
        cleanupWidgetPreloading(false);
        mDraggingWidget = false;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAllTasks();
    }

    public void clearAllWidgetPages() {
        cancelAllTasks();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getPageAt(i);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            }
        }
    }

    private void cancelAllTasks() {
        // Clean up all the async tasks
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            task.cancel(false);
            iter.remove();
            mDirtyPageContent.set(task.page, true);

            // We've already preallocated the views for the data to load into,
            // so clear them as well
            View v = getPageAt(task.page);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
            }
        }
        mDeferredSyncWidgetPageItems.clear();
        mDeferredPrepareLoadWidgetPreviewsTasks.clear();
    }

    public void setContentType(ContentType type) {
        if (type == ContentType.Widgets) {
            invalidatePageData(mNumAppsPages, true);
        } else if (type == ContentType.Applications) {
            invalidatePageData(0, true);
        }
    }

    protected void snapToPage(int whichPage, int delta, int duration) {
        super.snapToPage(whichPage, delta, duration);
        updateCurrentTab(whichPage);

        // Update the thread priorities given the direction lookahead
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int pageIndex = task.page;
            if ((mNextPage > mCurrentPage && pageIndex >= mCurrentPage) || (mNextPage < mCurrentPage && pageIndex <= mCurrentPage)) {
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            } else {
                task.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            }
        }
    }

    private void updateCurrentTab(int currentPage) {
        AppsCustomizeTabHost tabHost = getTabHost();
        if (tabHost != null) {
            String tag = tabHost.getCurrentTabTag();
            if (tag != null) {
                if (currentPage >= mNumAppsPages && !tag.equals(tabHost.getTabTagForContentType(ContentType.Widgets))) {
                    tabHost.setCurrentTabFromContent(ContentType.Widgets);
                } else if (currentPage < mNumAppsPages && !tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
                    tabHost.setCurrentTabFromContent(ContentType.Applications);
                }
            }
        }
    }

    /*
     * Apps PagedView implementation
     */
    private void setVisibilityOnChildren(ViewGroup layout, int visibility) {
        int childCount = layout.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            layout.getChildAt(i).setVisibility(visibility);
        }
    }

    private void setupPage(PagedViewCellLayout layout) {
        if (mAppsCustomizeConfig != null && mAppsCustomizeConfig.getAutoWidthGap()) {
            mCustomPageLayoutWidthGap = -1;
        } else {
            mCustomPageLayoutWidthGap = mPageLayoutWidthGap;
        }

        if (mAppsCustomizeConfig != null && mAppsCustomizeConfig.getAutoHeightGap()) {
            mCustomPageLayoutHeightGap = -1;
        } else {
            mCustomPageLayoutHeightGap = mPageLayoutHeightGap;
        }
      //  LogUtil.d("setupPage: mCustomWidthGap=" + mCustomPageLayoutWidthGap + ", mCustomHeightGap=" + mCustomPageLayoutHeightGap);

        // 如果是自定义样式且设置了自定义 Cell 尺寸，则应用到每个页面
        if (mAppsCustomizeConfig!=null  && mAppsCustomizeConfig.getCellWidthDp() > 0 ) {
            int cellWidthPx = (int) (mAppsCustomizeConfig.getCellWidthDp());
            mWidgetSpacingLayout.setCustomCellWidth(cellWidthPx);
        }
        if (mAppsCustomizeConfig!=null  && mAppsCustomizeConfig.getCellHeightDp() > 0 ) {
            int cellHeightPx = (int) (mAppsCustomizeConfig.getCellHeightDp());
            mWidgetSpacingLayout.setCustomCellHeight(cellHeightPx);
        }

        layout.setPageViewIconBackgroundResource(mPageBackgroundResId);

        
        layout.setCellCount(mCellCountX, mCellCountY);
        layout.setGap(mCustomPageLayoutWidthGap, mCustomPageLayoutHeightGap);
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop, mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do
        // layout calculations
        // immediately after syncing, we don't have a proper width. That said,
        // we already know the
        // expected page width, so we can actually optimize by hiding all the
        // TextView-based
        // children that are expensive to measure, and let that happen naturally
        // later.
        setVisibilityOnChildren(layout, View.GONE);
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        
        // 优化：只在尺寸真正变化时才 measure，避免重复测量
        if (layout.getMeasuredWidth() != getMeasuredWidth() || 
            layout.getMeasuredHeight() != getMeasuredHeight() ||
            !layout.isLayoutRequested()) {
            layout.measure(widthSpec, heightSpec);
        }
        setVisibilityOnChildren(layout, View.VISIBLE);
    }

    public void syncAppsPageItems(int page, boolean immediate) {
        // ensure that we have the right number of items on the pages
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mApps.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);

        layout.removeAllViewsOnPage();
        ArrayList<Object> items = new ArrayList<Object>();
        ArrayList<Bitmap> images = new ArrayList<Bitmap>();
        // 根据 mAppIconSize 缩放图标
        if(mAppsCustomizeConfig !=null && mAppsCustomizeConfig.getIconSize()>0)
            mAppIconSize = mAppsCustomizeConfig.getIconSize();

      //  LogUtil.i("syncAppsPageItems: scaling icon to " + mAppIconSize + "px");

        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mApps.get(i);
            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(R.layout.apps_customize_application, layout, false);

            Bitmap scaledIcon = scaleBitmapIfNeeded(info.iconBitmap, mAppIconSize);
            if(mAppsCustomizeConfig!=null)
                icon.applyFromApplicationInfoWithBitmap(info, true, this, mAppsCustomizeConfig.getIconTextPadding(), scaledIcon, mAppsCustomizeConfig.getFontSizeSp(), mAppsCustomizeConfig.getFontColor(), mAppsCustomizeConfig.getTextOrientation(), mAppsCustomizeConfig.getTextGravity());
            else
                icon.applyFromApplicationInfoWithBitmap(info, true, this, mAppIconPadding, scaledIcon, 0, 0,0, 0);
            
            // 应用图标水平偏移量配置
            if(mAppsCustomizeConfig != null && (mAppsCustomizeConfig.getIconOffsetX() != 0 || mAppsCustomizeConfig.getIconOffsetY() !=0)) {
                icon.setIconOffsetXY(mAppsCustomizeConfig.getIconOffsetX() , mAppsCustomizeConfig.getIconOffsetY());
            }
            
            icon.setOnClickListener(this);
            icon.setOnLongClickListener(this);
            icon.setOnTouchListener(this);
            icon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i, new PagedViewCellLayout.LayoutParams(x, y, 1, 1));

            items.add(info);
            images.add(info.iconBitmap);
        }

        layout.createHardwareLayers();
        
        // 在添加完所有图标后，应用背景到每个图标
        if (mPageBackgroundResId != 0) {
            layout.setPageViewIconBackgroundResource(mPageBackgroundResId);
            //LogUtil.d("syncAppsPageItems: applied background after adding icons: resId=" + mPageBackgroundResId);
        }else {
            layout.setPageViewIconBackgroundResource(0);
            //LogUtil.d("syncAppsPageItems: no background applied");
        }
    }

    /**
     * A helper to return the priority for loading of the specified widget page.
     */
    private int getWidgetPageLoadPriority(int page) {
        // If we are snapping to another page, use that index as the target page
        // index
        int toPage = mCurrentPage;
        if (mNextPage > -1) {
            toPage = mNextPage;
        }

        // We use the distance from the target page as an initial guess of
        // priority, but if there
        // are no pages of higher priority than the page specified, then bump up
        // the priority of
        // the specified page.
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        int minPageDiff = Integer.MAX_VALUE;
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            minPageDiff = Math.abs(task.page - toPage);
        }

        int rawPageDiff = Math.abs(page - toPage);
        return rawPageDiff - Math.min(rawPageDiff, minPageDiff);
    }

    /**
     * Return the appropriate thread priority for loading for a given page (we
     * give the current page much higher priority)
     */
    private int getThreadPriorityForPage(int page) {
        // TODO-APPS_CUSTOMIZE: detect number of cores and set thread priorities
        // accordingly below
        int pageDiff = getWidgetPageLoadPriority(page);
        if (pageDiff <= 0) {
            return Process.THREAD_PRIORITY_LESS_FAVORABLE;
        } else if (pageDiff <= 1) {
            return Process.THREAD_PRIORITY_LOWEST;
        } else {
            return Process.THREAD_PRIORITY_LOWEST;
        }
    }

    private int getSleepForPage(int page) {
        int pageDiff = getWidgetPageLoadPriority(page);
        return Math.max(0, pageDiff * sPageSleepDelay);
    }

    /**
     * Creates and executes a new AsyncTask to load a page of widget previews.
     */
    private void prepareLoadWidgetPreviewsTask(int page, ArrayList<Object> widgets, int cellWidth, int cellHeight, int cellCountX) {

        // Prune all tasks that are no longer needed
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int taskPage = task.page;
            if (taskPage < getAssociatedLowerPageBound(mCurrentPage) || taskPage > getAssociatedUpperPageBound(mCurrentPage)) {
                task.cancel(false);
                iter.remove();
            } else {
                task.setThreadPriority(getThreadPriorityForPage(taskPage));
            }
        }

        // We introduce a slight delay to order the loading of side pages so
        // that we don't thrash
        final int sleepMs = getSleepForPage(page);
        AsyncTaskPageData pageData = new AsyncTaskPageData(page, widgets, cellWidth, cellHeight, new AsyncTaskCallback() {
            @Override
            public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                try {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (Exception e) {
                    }
                    loadWidgetPreviewsInBackground(task, data);
                } finally {
                    if (task.isCancelled()) {
                        data.cleanup(true);
                    }
                }
            }
        }, new AsyncTaskCallback() {
            @Override
            public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                mRunningTasks.remove(task);
                if (task.isCancelled()) return;
                // do cleanup inside onSyncWidgetPageItems
                onSyncWidgetPageItems(data);
            }
        });

        // Ensure that the task is appropriately prioritized and runs in
        // parallel
        AppsCustomizeAsyncTask t = new AppsCustomizeAsyncTask(page, AsyncTaskPageData.Type.LoadWidgetPreviewData);
        t.setThreadPriority(getThreadPriorityForPage(page));
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pageData);
        mRunningTasks.add(t);
    }

    /*
     * Widgets PagedView implementation
     */
    private void setupPage(PagedViewGridLayout layout) {
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop, mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do
        // layout calculations
        // immediately after syncing, we don't have a proper width.
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
    }

    private void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h) {
        renderDrawableToBitmap(d, bitmap, x, y, w, h, 1f);
    }

    private void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h, float scale) {
        if (bitmap != null) {
            Canvas c = new Canvas(bitmap);
            c.scale(scale, scale);
            Rect oldBounds = d.copyBounds();
            d.setBounds(x, y, x + w, y + h);
            d.draw(c);
            d.setBounds(oldBounds); // Restore the bounds
            c.setBitmap(null);
        }
    }

    private Bitmap getShortcutPreview(ResolveInfo info, int maxWidth, int maxHeight) {
        Bitmap tempBitmap = mCachedShortcutPreviewBitmap.get();
        final Canvas c = mCachedShortcutPreviewCanvas.get();
        if (tempBitmap == null || tempBitmap.getWidth() != maxWidth || tempBitmap.getHeight() != maxHeight) {
            tempBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
            mCachedShortcutPreviewBitmap.set(tempBitmap);
        } else {
            c.setBitmap(tempBitmap);
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            c.setBitmap(null);
        }
        // Render the icon
        Drawable icon = mIconCache.getFullResIcon(info);

        int paddingTop = getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_top);
        int paddingLeft = getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_left);
        int paddingRight = getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_right);

        int scaledIconWidth = (maxWidth - paddingLeft - paddingRight);

        renderDrawableToBitmap(icon, tempBitmap, paddingLeft, paddingTop, scaledIconWidth, scaledIconWidth);

        Bitmap preview = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
        c.setBitmap(preview);
        Paint p = mCachedShortcutPreviewPaint.get();
        if (p == null) {
            p = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            p.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            p.setAlpha((int) (255 * 0.06f));
            // float density = 1f;
            // p.setMaskFilter(new BlurMaskFilter(15*density,
            // BlurMaskFilter.Blur.NORMAL));
            mCachedShortcutPreviewPaint.set(p);
        }
        c.drawBitmap(tempBitmap, 0, 0, p);
        c.setBitmap(null);

        renderDrawableToBitmap(icon, preview, 0, 0, mAppIconSize, mAppIconSize);

        return preview;
    }

    /**
     * 根据需要缩放 Bitmap 到指定尺寸
     * @param bitmap 原始 Bitmap
     * @param targetSize 目标尺寸（宽高相同）
     * @return 缩放后的 Bitmap，如果不需要缩放则返回原图
     */
    private Bitmap scaleBitmapIfNeeded(Bitmap bitmap, int targetSize) {
        if (bitmap == null || targetSize <= 0) {
            return bitmap;
        }
        
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        
        // 如果已经是目标尺寸，直接返回
        if (originalWidth == targetSize && originalHeight == targetSize) {
            return bitmap;
        }
        
        // 计算缩放比例
        float scale = Math.min((float) targetSize / originalWidth, (float) targetSize / originalHeight);
        
        // 如果缩故比例接近 1，直接返回原图
        if (Math.abs(scale - 1.0f) < 0.01f) {
            return bitmap;
        }
        
        int scaledWidth = Math.round(originalWidth * scale);
        int scaledHeight = Math.round(originalHeight * scale);
        
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
    }

    private Bitmap getWidgetPreview(ComponentName provider, int previewImage, int iconId, int cellHSpan, int cellVSpan, int maxWidth, int maxHeight) {
        // Load the preview image if possible
        String packageName = provider.getPackageName();
        if (maxWidth < 0) maxWidth = Integer.MAX_VALUE;
        if (maxHeight < 0) maxHeight = Integer.MAX_VALUE;

        Drawable drawable = null;
        if (previewImage != 0) {
            drawable = mPackageManager.getDrawable(packageName, previewImage, null);
//            if (drawable == null) {
//                LogUtil.w("Can't load widget preview drawable 0x" + Integer.toHexString(previewImage) + " for provider: " + provider);
//            }
        }

        int bitmapWidth;
        int bitmapHeight;
        Bitmap defaultPreview = null;
        boolean widgetPreviewExists = (drawable != null);
        if (widgetPreviewExists) {
            bitmapWidth = drawable.getIntrinsicWidth();
            bitmapHeight = drawable.getIntrinsicHeight();
        } else {
            // Generate a preview image if we couldn't load one
            if (cellHSpan < 1) cellHSpan = 1;
            if (cellVSpan < 1) cellVSpan = 1;

            BitmapDrawable previewDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.widget_preview_tile);
            final int previewDrawableWidth = previewDrawable.getIntrinsicWidth();
            final int previewDrawableHeight = previewDrawable.getIntrinsicHeight();
            bitmapWidth = previewDrawableWidth * cellHSpan; // subtract 2 dips
            bitmapHeight = previewDrawableHeight * cellVSpan;

            defaultPreview = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);
            final Canvas c = mCachedAppWidgetPreviewCanvas.get();
            c.setBitmap(defaultPreview);
            previewDrawable.setBounds(0, 0, bitmapWidth, bitmapHeight);
            previewDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            previewDrawable.draw(c);
            c.setBitmap(null);

            // Draw the icon in the top left corner
            int minOffset = (int) (mAppIconSize * sWidgetPreviewIconPaddingPercentage);
            int smallestSide = Math.min(bitmapWidth, bitmapHeight);
            float iconScale = Math.min((float) smallestSide / (mAppIconSize + 2 * minOffset), 1f);
            //LogUtil.d(" mAppIconSize = " + mAppIconSize+", iconScale = " + iconScale+", bitmapWidth = " + bitmapWidth+" bitmapHeight = " + bitmapHeight);
            try {
                Drawable icon = null;
                int hoffset = (int) ((previewDrawableWidth - mAppIconSize * iconScale) / 2);
                int yoffset = (int) ((previewDrawableHeight - mAppIconSize * iconScale) / 2);
                if (iconId > 0) icon = mIconCache.getFullResIcon(packageName, iconId);
                if (icon != null) {
                    renderDrawableToBitmap(icon, defaultPreview, hoffset, yoffset, (int) (mAppIconSize * iconScale), (int) (mAppIconSize * iconScale));
                }
            } catch (Resources.NotFoundException e) {
            }
        }

        // Scale to fit width only - let the widget preview be clipped in the
        // vertical dimension
        float scale = 1f;
        if (bitmapWidth > maxWidth) {
            scale = maxWidth / (float) bitmapWidth;
        }
        if (scale != 1f) {
            bitmapWidth = (int) (scale * bitmapWidth);
            bitmapHeight = (int) (scale * bitmapHeight);
        }

        Bitmap preview = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);

        // Draw the scaled preview into the final bitmap
        if (widgetPreviewExists) {
            renderDrawableToBitmap(drawable, preview, 0, 0, bitmapWidth, bitmapHeight);
        } else {
            final Canvas c = mCachedAppWidgetPreviewCanvas.get();
            final Rect src = mCachedAppWidgetPreviewSrcRect.get();
            final Rect dest = mCachedAppWidgetPreviewDestRect.get();
            c.setBitmap(preview);
            src.set(0, 0, defaultPreview.getWidth(), defaultPreview.getHeight());
            dest.set(0, 0, preview.getWidth(), preview.getHeight());

            Paint p = mCachedAppWidgetPreviewPaint.get();
            if (p == null) {
                p = new Paint();
                p.setFilterBitmap(true);
                mCachedAppWidgetPreviewPaint.set(p);
            }
            c.drawBitmap(defaultPreview, src, dest, p);
            c.setBitmap(null);
        }
        return preview;
    }

    public void syncWidgetPageItems(final int page, final boolean immediate) {
        int numItemsPerPage = mWidgetCountX * mWidgetCountY;

        // Calculate the dimensions of each cell we are giving to each widget
        final ArrayList<Object> items = new ArrayList<Object>();
        int contentWidth = mWidgetSpacingLayout.getContentWidth();
        final int cellWidth = ((contentWidth - mPageLayoutPaddingLeft - mPageLayoutPaddingRight - ((mWidgetCountX - 1) * mWidgetWidthGap)) / mWidgetCountX);
        int contentHeight = mWidgetSpacingLayout.getContentHeight();
        final int cellHeight = ((contentHeight - mPageLayoutPaddingTop - mPageLayoutPaddingBottom - ((mWidgetCountY - 1) * mWidgetHeightGap)) / mWidgetCountY);

        // Prepare the set of widgets to load previews for in the background
        int offset = (page - mNumAppsPages) * numItemsPerPage;
        for (int i = offset; i < Math.min(offset + numItemsPerPage, mWidgets.size()); ++i) {
            items.add(mWidgets.get(i));
        }

        // Prepopulate the pages with the other widget info, and fill in the
        // previews later
        final PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);
        layout.setColumnCount(layout.getCellCountX());
        for (int i = 0; i < items.size(); ++i) {
            Object rawInfo = items.get(i);
            PendingAddItemInfo createItemInfo = null;
            PagedViewWidget widget = (PagedViewWidget) mLayoutInflater.inflate(R.layout.apps_customize_widget, layout, false);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                // Fill in the widget information
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                createItemInfo = new PendingAddWidgetInfo(info, null, null);

                // Determine the widget spans and min resize spans.
                int[] spanXY = Launcher.getSpanForWidget(mLauncher, info);
                createItemInfo.spanX = spanXY[0];
                createItemInfo.spanY = spanXY[1];
                int[] minSpanXY = Launcher.getMinSpanForWidget(mLauncher, info);
                createItemInfo.minSpanX = minSpanXY[0];
                createItemInfo.minSpanY = minSpanXY[1];

                widget.applyFromAppWidgetProviderInfo(info, -1, spanXY);
                widget.setTag(createItemInfo);
                widget.setShortPressListener(this);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                createItemInfo = new PendingAddShortcutInfo(info.activityInfo);
                createItemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
                createItemInfo.componentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                widget.applyFromResolveInfo(mPackageManager, info);
                widget.setTag(createItemInfo);
            }
            widget.setOnClickListener(this);
            widget.setOnLongClickListener(this);
            widget.setOnTouchListener(this);
            widget.setOnKeyListener(this);

            // Layout each widget
            int ix = i % mWidgetCountX;
            int iy = i / mWidgetCountX;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(iy, GridLayout.LEFT), GridLayout.spec(ix, GridLayout.TOP));
            lp.width = cellWidth;
            lp.height = cellHeight;
            lp.setGravity(Gravity.TOP | Gravity.LEFT);
            if (ix > 0) lp.leftMargin = mWidgetWidthGap;
            if (iy > 0) lp.topMargin = mWidgetHeightGap;
            layout.addView(widget, lp);
        }

        // wait until a call on onLayout to start loading, because
        // PagedViewWidget.getPreviewSize() will return 0 if it hasn't been laid
        // out
        // TODO: can we do a measure/layout immediately?
        layout.setOnLayoutListener(new Runnable() {
            public void run() {
                // Load the widget previews
                int maxPreviewWidth = cellWidth;
                int maxPreviewHeight = cellHeight;
                if (layout.getChildCount() > 0) {
                    PagedViewWidget w = (PagedViewWidget) layout.getChildAt(0);
                    int[] maxSize = w.getPreviewSize();
                    maxPreviewWidth = maxSize[0];
                    maxPreviewHeight = maxSize[1];
                }
                if (immediate) {
                    AsyncTaskPageData data = new AsyncTaskPageData(page, items, maxPreviewWidth, maxPreviewHeight, null, null);
                    loadWidgetPreviewsInBackground(null, data);
                    onSyncWidgetPageItems(data);
                } else {
                    if (mInTransition) {
                        mDeferredPrepareLoadWidgetPreviewsTasks.add(this);
                    } else {
                        prepareLoadWidgetPreviewsTask(page, items, maxPreviewWidth, maxPreviewHeight, mWidgetCountX);
                    }
                }
            }
        });
    }

    private void loadWidgetPreviewsInBackground(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
        // loadWidgetPreviewsInBackground can be called without a task to load a
        // set of widget
        // previews synchronously
        if (task != null) {
            // Ensure that this task starts running at the correct priority
            task.syncThreadPriority();
        }

        // Load each of the widget/shortcut previews
        ArrayList<Object> items = data.items;
        ArrayList<Bitmap> images = data.generatedImages;
        int count = items.size();
        for (int i = 0; i < count; ++i) {
            if (task != null) {
                // Ensure we haven't been cancelled yet
                if (task.isCancelled()) break;
                // Before work on each item, ensure that this task is running at
                // the correct
                // priority
                task.syncThreadPriority();
            }

            Object rawInfo = items.get(i);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                int[] cellSpans = Launcher.getSpanForWidget(mLauncher, info);

                int maxWidth = Math.min(data.maxImageWidth, mWidgetSpacingLayout.estimateCellWidth(cellSpans[0]));
                int maxHeight = Math.min(data.maxImageHeight, mWidgetSpacingLayout.estimateCellHeight(cellSpans[1]));
                Bitmap b = getWidgetPreview(info.provider, info.previewImage, info.icon, cellSpans[0], cellSpans[1], maxWidth, maxHeight);
                images.add(b);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                images.add(getShortcutPreview(info, data.maxImageWidth, data.maxImageHeight));
            }
        }
    }

    private void onSyncWidgetPageItems(AsyncTaskPageData data) {
        if (mInTransition) {
            mDeferredSyncWidgetPageItems.add(data);
            return;
        }
        try {
            int page = data.page;
            PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);

            ArrayList<Object> items = data.items;
            int count = items.size();
            for (int i = 0; i < count; ++i) {
                PagedViewWidget widget = (PagedViewWidget) layout.getChildAt(i);
                if (widget != null) {
                    Bitmap preview = data.generatedImages.get(i);
                    widget.applyPreview(new FastBitmapDrawable(preview), i);
                }
            }

            layout.createHardwareLayer();
            invalidate();

            // Update all thread priorities
            Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
            while (iter.hasNext()) {
                AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
                int pageIndex = task.page;
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            }
        } finally {
            data.cleanup(false);
        }
    }

    @Override
    public void syncPages() {
        //LogUtil.i("syncPages: huang pages mNumWidgetPages=>" + mNumWidgetPages + " mNumAppsPages=>" + mNumAppsPages +",mWidgetCountX="+mWidgetCountX+",mWidgetCountY="+mWidgetCountY);
            
        mIsSyncingPages = true; // 标记开始同步页面
        try {
            removeAllViews();
            cancelAllTasks();

        Context context = getContext();
        for (int j = 0; j < mNumWidgetPages; ++j) {
            PagedViewGridLayout layout = new PagedViewGridLayout(context, mWidgetCountX, mWidgetCountY);
            setupPage(layout);
            addView(layout, new PagedView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        // 保存自定义 Cell 尺寸信息（如果有的话）
        int customCellWidth = 0;
        int customCellHeight = 0;
        if (mAppsCustomizeConfig != null && mAppsCustomizeConfig.getCellWidthDp() > 0 && mAppsCustomizeConfig.getCellHeightDp() > 0) {
            Resources res = getResources();
            float density = res.getDisplayMetrics().density;
            customCellWidth = (int) (mAppsCustomizeConfig.getCellWidthDp() * density);
            customCellHeight = (int) (mAppsCustomizeConfig.getCellHeightDp() * density);
         //   LogUtil.d("syncPages: will apply custom cell dimensions - width=" + customCellWidth + ", height=" + customCellHeight);
        }

        for (int i = 0; i < mNumAppsPages; ++i) {
            PagedViewCellLayout layout = new PagedViewCellLayout(context);
            
            // 如果有自定义 Cell 尺寸，立即应用到新创建的页面上
            if (customCellWidth > 0 && customCellHeight > 0) {
                layout.setCustomCellDimensions(customCellWidth, customCellHeight);
            //    LogUtil.d("syncPages: applied custom dimensions to page " + i);
            }
            
            setupPage(layout);
            addView(layout);
        }
        } finally {
            mIsSyncingPages = false; // 标记同步页面完成
         //   LogUtil.d("syncPages: completed, mIsSyncingPages=false");
        }
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
        if (page < mNumAppsPages) {
            syncAppsPageItems(page, immediate);
        } else {
            syncWidgetPageItems(page, immediate);
        }
    }

    // We want our pages to be z-ordered such that the further a page is to the
    // left, the higher
    // it is in the z-order. This is important to insure touch events are
    // handled correctly.
    View getPageAt(int index) {

        return getChildAt(indexToPage(index));
    }

    @Override
    protected int indexToPage(int index) {
        return getChildCount() - index - 1;
    }

    // In apps customize, we have a scrolling effect which emulates pulling
    // cards off of a stack.
    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        for (int i = 0; i < getChildCount(); i++) {
            View v = getPageAt(i);
            if (v != null) {
                float scrollProgress = getScrollProgress(screenCenter, v, i);

                float interpolatedProgress = mZInterpolator.getInterpolation(Math.abs(Math.min(scrollProgress, 0)));
                float scale = (1 - interpolatedProgress) + interpolatedProgress * TRANSITION_SCALE_FACTOR;
                float translationX = Math.min(0, scrollProgress) * v.getMeasuredWidth();

                float alpha;

                if (scrollProgress < 0) {
                    alpha = scrollProgress < 0 ? mAlphaInterpolator.getInterpolation(1 - Math.abs(scrollProgress)) : 1.0f;
                } else {
                    // On large screens we need to fade the page as it nears its
                    // leftmost position
                    alpha = mLeftScreenAlphaInterpolator.getInterpolation(1 - scrollProgress);
                }

                v.setCameraDistance(mDensity * CAMERA_DISTANCE);
                int pageWidth = v.getMeasuredWidth();
                int pageHeight = v.getMeasuredHeight();

                if (PERFORM_OVERSCROLL_ROTATION) {
                    if (i == 0 && scrollProgress < 0) {
                        // Overscroll to the left
                        v.setPivotX(TRANSITION_PIVOT * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the first page, we don't want the page to have any
                        // lateral motion
                        translationX = 0;
                    } else if (i == getChildCount() - 1 && scrollProgress > 0) {
                        // Overscroll to the right
                        v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the last page, we don't want the page to have any
                        // lateral motion.
                        translationX = 0;
                    } else {
                        v.setPivotY(pageHeight / 2.0f);
                        v.setPivotX(pageWidth / 2.0f);
                        v.setRotationY(0f);
                    }
                }

                v.setTranslationX(translationX);
                v.setScaleX(scale);
                v.setScaleY(scale);
                v.setAlpha(alpha);

                // If the view has 0 alpha, we set it to be invisible so as to
                // prevent
                // it from accepting touches
                if (alpha == 0) {
                    v.setVisibility(INVISIBLE);
                } else if (v.getVisibility() != VISIBLE) {
                    v.setVisibility(VISIBLE);
                }
            }
        }
    }

    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    /**
     * Used by the parent to get the content width to set the tab bar to
     *
     * @return
     */
    public int getPageContentWidth() {
        return mContentWidth;
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        mForceDrawAllChildrenNextFrame = true;
        // We reset the save index when we change pages so that it will be
        // recalculated on next
        // rotation
        mSaveInstanceStateItemIndex = -1;
    }

    /*
     * AllAppsView implementation
     */
    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        mDragController = dragController;
    }

    /**
     * We should call thise method whenever the core data changes (mApps,
     * mWidgets) so that we can appropriately determine when to invalidate the
     * PagedView page data. In cases where the data has yet to be set, we can
     * requestLayout() and wait for onDataReady() to be called in the next
     * onMeasure() pass, which will trigger an invalidatePageData() itself.
     */
    private void invalidateOnDataChange() {
        if (!isDataReady()) {
            // The next layout pass will trigger data-ready if both widgets and
            // apps are set, so
            // request a layout to trigger the page data when ready.
            requestLayout();
        } else {
            // 防抖1:如果正在执行 syncPages，跳过此次调用
            if (mIsSyncingPages) {
              //  LogUtil.d("invalidateOnDataChange: skipped because syncPages is currently executing");
                return;
            }
            
            // 防抖2:如果已经有 pending 的 invalidatePageData，跳过此次调用
            // 因为 onDataReady 中的 postDelayed 会稍后执行
            if (mPendingInvalidatePageData) {
              //  LogUtil.d("invalidateOnDataChange: skipped because there's already a pending invalidatePageData");
                return;
            }
            
            cancelAllTasks();
            invalidatePageData();
        }
    }

    public void setAppsAndroidPage(ArrayList<ApplicationInfo> list) {
        mApps.clear();
        for (ApplicationInfo a : list) {
            try {
                String str = a.componentName.getPackageName();
                if (str.equals("com.android.xysysteminfo")
                        || str.equals("com.pve.xysecurity")
                        || str.equals("com.pve.steering")
                        || str.equals("com.xy.brightsetting")
                        || str.equals("com.android.xy.volumesetting")
                        || str.equals("com.acloud.stub.onekeyclean")
                        || str.equals("com.pve.aps")
                        || str.equals("com.pve.gpsinfo")
                        || str.equals("com.pve.wifi")
                        || str.equals("com.pve.logoselector")
                        || str.equals("com.pve.wallpaper")
                        || str.equals("com.pve.sysrestore")
                        || str.equals("com.pve.time")
                        || str.equals("com.pve.language")
//						|| str.equals("com.autonavi.xmgd.navigator")
//						|| str.equals("cld.navi.c2739.mainframe")
//						|| str.equals("com.acloud.stub.news")
//						|| str.equals("com.acloud.stub.cdplay")
//						|| str.equals("com.acloud.stub.newonlineradio")
//						|| str.equals("com.acloud.stub.newonlinemusic")
//						|| str.equals("com.acloud.stub.video")
//						|| str.equals("com.acloud.stub.localmusic")
                        || str.equals("com.pve.onekeynavi")
//						|| str.equals("com.autochips.avin")
//						|| str.equals("com.autochips.HDMI")
//						|| str.equals("com.acloud.stub.localradio")
//						|| str.equals("com.android.browser")
//						|| str.equals("com.autochips.bluetooth")
//						|| str.equals("com.pve.onekeysos")
                        || str.equals("com.android.xy.floatbar")
                        || str.equals("com.iflytek.speechcloud")
                        || str.equals("com.iflytek.inputmethod")
                        || str.equals("com.android.settings")
                        || str.equals("com.example.logosetter")
                        || str.equals("com.android.speechrecorder")
                        || str.equals("com.awell.pvcanset")
                        || str.equals("com.android.gallery3d")
                        || str.equals("com.android.deskclock")
                        || str.equals("com.wifi.pnp.player")
                        || str.equals("com.awell.canbus")
//						||str.equals("com.pve.naviguide")
                        || str.equals("com.xy.usbsettings")
                        || str.equals("com.xy.screensettings")
                        || str.equals("com.xy.avoutsettings")
                        || str.equals("com.bizhi.mogudongtai")
                        || str.equals("com.pve.pveparkingbrake")
                        || str.equals("com.gpspve.pvefloatbar")
                        || str.equals("com.pve.navisetting")
                        || str.equals("com.fourtech.logosettings")
                        || str.equals("com.aispeech.aios")
                    /*		|| str.equals("com.acloud.stub.manual")*/) {

                } else {
                    if (str.equals("com.acloud.stub.news")
                            || str.equals("com.acloud.stub.cdplay")
                            || str.equals("com.acloud.stub.newonlineradio")
                            || str.equals("com.acloud.stub.newonlinemusic")
                            || str.equals("com.acloud.stub.video")
                            || str.equals("com.acloud.stub.localmusic")
                            || str.equals("com.autochips.avin")
                            || str.equals("com.acloud.stub.localradio")
                            || str.equals("com.android.browser")
                            || str.equals("com.autochips.bluetooth")
                            || str.equals("com.pve.onekeysos")
                            || str.equals("com.pve.naviguide")
                            || str.equals("com.autochips.HDMI")) {
                        a.setLevel(2);
                        mApps.add(a);
                    } else if (str.equals("com.xyauto.xysettings")) {
                        a.setLevel(1);
                        mApps.add(a);
                    } else {
                        a.setLevel(3);
                        mApps.add(a);
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        list = mApps;
        mApps.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return lhs.getLevel().compareTo(rhs.getLevel());
            }

        });
        updatePageCounts();
        invalidateOnDataChange();
        /*
         * mApps = list; Collections.sort(mApps,
         * LauncherModel.getAppNameComparator()); updatePageCounts();
         * invalidateOnDataChange();
         */
    }

    public void setAppsAndroidPageHaveCanBus(ArrayList<ApplicationInfo> list) {
        String language = Locale.getDefault().getLanguage();
        mApps.clear();
        for (ApplicationInfo a : list) {
            try {
                String str = a.componentName.getPackageName();
                if (str.equals("com.android.xysysteminfo")
                        || str.equals("com.pve.xysecurity")
                        || str.equals("com.pve.steering")
                        || str.equals("com.acloud.stub.onekeyclean")
                        || str.equals("com.pve.aps")
                        || str.equals("com.pve.gpsinfo")
                        || str.equals("com.pve.wifi")
                        || str.equals("com.pve.logoselector")
                        || str.equals("com.pve.wallpaper")
                        || str.equals("com.pve.sysrestore")
                        || str.equals("com.pve.time")
                        || str.equals("com.pve.language")
                        || str.equals("com.pve.onekeynavi")
                        || str.equals("com.android.xy.floatbar")
                        || str.equals("com.iflytek.speechcloud")
                        || str.equals("com.iflytek.inputmethod")
                        || str.equals("com.android.settings")
                        || str.equals("com.example.logosetter")
                        || str.equals("com.android.speechrecorder")
                        || str.equals("com.awell.pvcanset")
                        || str.equals("com.android.gallery3d")
                        || str.equals("com.android.deskclock")
                        || str.equals("com.wifi.pnp.player")
                        || str.equals("com.bizhi.mogudongtai")
                        || str.equals("com.pve.pveparkingbrake")
                        || str.equals("com.gpspve.pvefloatbar")
                        || str.equals("com.pve.navisetting")
                        || str.equals("com.fourtech.logosettings")
                        || str.equals("com.aispeech.aios")) {

                } else if (str.contains("com.awell.launcher")) {

                } else {
                    if (str.equals("com.awell.localmusic")) {
                        a.setLevel(1);
                        mApps.add(a);
                    } else if (str.equals("com.awell.bluetooth")) {
                        a.setLevel(2);
                        mApps.add(a);
                    } else if (str.equals("com.awell.radio")) {
                        a.setLevel(3);
                        mApps.add(a);
                    } else if (str.equals("com.awell.localvideo")) {
                        a.setLevel(4);
                        mApps.add(a);
                    } else if (str.equals("com.awell.carsetting")) {
                        a.setLevel(5);
                        mApps.add(a);
                    } else if (str.equals("cn.kuwo.kwmusiccar")) {
                        a.setLevel(6);
                        mApps.add(a);
                    } else if (str.equals("com.autochips.bluetooth")
                            || str.equals("com.pve.onekeysos")
                            || str.equals("com.pve.naviguide")
                            || str.equals("com.autochips.HDMI")
                            || str.equals("com.awell.soundeffect")
                            || str.equals("com.awell.eqselect")
                            || str.equals("com.android.chrome")
                            || str.equals("com.android.browser")
                            || str.equals("com.mediatek.filemanager")
                            || str.equals("com.awell.navigation")
                            || str.equals("com.autonavi.amapauto")
                            || str.equals("com.autonavi.minimap")
                            || str.equals("com.google.android.apps.maps")) {
                        a.setLevel(7);
                        mApps.add(a);
                    } else if (str.equals("com.tima.carnet.vt")
                            || str.equals("net.easyconn")
                            || str.equals("com.zjinnova.zlink")
                            || str.equals("com.awell.backcar")
                            || str.equals("com.android.vending")
                            || str.equals("com.google.android.youtube")) {
                        a.setLevel(8);
                        mApps.add(a);
                    } else if (str.equals("com.android.documentsui")
                            || str.equals("com.android.dialer")
                            || str.equals("com.android.mms")
                            || str.equals("com.awell.canbus")
                            || str.equals("com.android.calculator2")
                            || str.equals("com.android.soundrecorder")) {
                        a.setLevel(9);
                        mApps.add(a);
                    } else {
                        a.setLevel(10);
                        mApps.add(a);
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        list = mApps;
        Collections.sort(mApps, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return lhs.getLevel().compareTo(rhs.getLevel());
            }
        });
        updatePageCounts();
        invalidateOnDataChange();
        /*
         * mApps = list; Collections.sort(mApps,
         * LauncherModel.getAppNameComparator()); updatePageCounts();
         * invalidateOnDataChange();
         */
    }

    /**
     * 浠巃pps涓壘鍒版寚瀹氱殑鍖呭悕锛屽鏋滃瓨鍦紝鍒欑洿鎺ユ坊鍔犲埌apps涓�
     *
     * @param apps
     * @param packName
     * @param listRunApps
     */
    private void addApps(ArrayList<ApplicationInfo> apps, String packName, ArrayList<ApplicationInfo> listRunApps) {
        for (ApplicationInfo a : listRunApps) {
            try {
                String str = a.componentName.getPackageName();
                if (str.equals(packName)) {
                    apps.add(a);
                    break;
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    public void setAppsSettingPage(ArrayList<ApplicationInfo> list) {
        mApps.clear();
        String[] displayApps = new String[]{"com.android.xysysteminfo", "com.pve.xysecurity", "com.pve.steering", "com.xy.brightsetting", "com.android.xy.volumesetting", "com.acloud.stub.onekeyclean", "com.pve.aps", "com.pve.gpsinfo", "com.pve.wifi", "com.pve.wallpaper", "com.pve.logoselector", "com.pve.sysrestore", "com.pve.time", // "com.pve.language",
                "com.awell.pvcanset", "com.xy.usbsettings", "com.xy.screensettings", "com.xy.avoutsettings"};

        for (int i = 0; i < displayApps.length; i++) {
            addApps(mApps, displayApps[i], list);
        }

        list = mApps;
        // Collections.sort(mApps, LauncherModel.getAppNameComparator());
        updatePageCounts();
        invalidateOnDataChange();
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        mApps.clear();
        String[] excludePackageArray = getContext().getResources().getStringArray(R.array.excludePackageList);
        List<String> excludePackageList = Arrays.asList(excludePackageArray);
        for (ApplicationInfo a : list) {
            //Log.d(TAG, "setApps--getPackageName=" + a.componentName.getPackageName());
            //Log.d(TAG, "setApps--getClassName=" + a.componentName.getClassName());
            if (excludePackageList != null && (excludePackageList.contains(a.componentName.getPackageName())
                    || excludePackageList.contains(a.componentName.getClassName()))) {
                continue;
            }
            setAppShowLevel(a);
            mApps.add(a);
        }

        //mApps.sort(LauncherModel.getAppNameComparator());
        mApps.sort(LauncherModel.getAppLevelComparator());
        updatePageCounts();
        invalidateOnDataChange();
    }

    private void setAppShowLevel(ApplicationInfo app) {
        if (Utils.mAppLevel_1.contains(app.componentName.getPackageName())) {
            app.setLevel(1);
        } else if (Utils.mAppLevel_2.contains(app.componentName.getPackageName())) {
            app.setLevel(2);
        } else if (Utils.mAppLevel_3.contains(app.componentName.getPackageName())) {
            app.setLevel(3);
        } else if (Utils.mAppLevel_4.contains(app.componentName.getPackageName())) {
            app.setLevel(4);
        } else if (Utils.mAppLevel_5.contains(app.componentName.getPackageName())) {
            app.setLevel(5);
        } else if (Utils.mAppLevel_6.contains(app.componentName.getPackageName())) {
            app.setLevel(6);
        } else if (Utils.mAppLevel_7.contains(app.componentName.getPackageName())) {
            app.setLevel(7);
        } else if (Utils.mAppLevel_8.contains(app.componentName.getPackageName())) {
            app.setLevel(8);
        } else if (Utils.mAppLevel_9.contains(app.componentName.getPackageName())) {
            app.setLevel(9);
        } else {
            app.setLevel(10);
        }
    }

    private void addAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // We add it in place, in alphabetical order
        int count = list.size();
        for (int i = 0; i < count; ++i) {
            ApplicationInfo info = list.get(i);
            int index = Collections.binarySearch(mApps, info, LauncherModel.getAppNameComparator());
            if (index < 0) {
                //Log.i(TAG, "addAppsWithoutInvalidate: huang add info =>" + info);
                mApps.add(-(index + 1), info);
            }
        }
    }

    public void addApps(ArrayList<ApplicationInfo> list) {
        addAppsWithoutInvalidate(list);
        updatePageCounts();
        invalidateOnDataChange();
    }

    private int findAppByComponent(List<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            if (info.intent.getComponent().equals(removeComponent)) {
                return i;
            }
        }
        return -1;
    }

    private int findAppByPackage(List<ApplicationInfo> list, String packageName) {
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            if (ItemInfo.getPackageName(info.intent).equals(packageName)) {
                return i;
            }
        }
        return -1;
    }

    private void removeAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // loop through all the apps and remove apps that have the same
        // component
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            int removeIndex = findAppByComponent(mApps, info);
            if (removeIndex > -1) {
            //    LogUtil.i("removeAppsWithoutInvalidate: huang remove info=>" + info);
                mApps.remove(removeIndex);
            }
        }
    }

    private void removeAppsWithPackageNameWithoutInvalidate(ArrayList<String> packageNames) {
        // loop through all the package names and remove apps that have the same
        // package name
        for (String pn : packageNames) {
            int removeIndex = findAppByPackage(mApps, pn);
            while (removeIndex > -1) {
            //    LogUtil.i("removeAppsWithPackageNameWithoutInvalidate: huang remove pm =>" + pn);
                mApps.remove(removeIndex);
                removeIndex = findAppByPackage(mApps, pn);
            }
        }
    }

    public void removeApps(ArrayList<String> packageNames) {
        removeAppsWithPackageNameWithoutInvalidate(packageNames);
        updatePageCounts();
        invalidateOnDataChange();
    }

    public void updateApps(ArrayList<ApplicationInfo> list) {
        // We remove and re-add the updated applications list because it's
        // properties may have
        // changed (ie. the title), and this will ensure that the items will be
        // in their proper
        // place in the list.
        removeAppsWithoutInvalidate(list);
        addAppsWithoutInvalidate(list);
        updatePageCounts();
        invalidateOnDataChange();
    }

    public void reset() {
        // If we have reset, then we should not continue to restore the previous
        // state
        mSaveInstanceStateItemIndex = -1;

        AppsCustomizeTabHost tabHost = getTabHost();
        String tag = tabHost.getCurrentTabTag();
        if (tag != null) {
            if (!tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
                tabHost.setCurrentTabFromContent(ContentType.Applications);
            }
        }

        if (mCurrentPage != 0) {
            invalidatePageData(0);
        }
    }

    private AppsCustomizeTabHost getTabHost() {
        //todo huangxw need to
        if (true) {
            return AppsCustomizeControl.INSTANCE.getMAppsCustomizeTabHost();
        }

        return (AppsCustomizeTabHost) mLauncher.findViewById(R.id.apps_customize_pane);
    }

    public void dumpState() {
        // TODO: Dump information related to current list of Applications,
        // Widgets, etc.
        ApplicationInfo.dumpApplicationInfoList(TAG, "mApps", mApps);
        dumpAppWidgetProviderInfoList(TAG, "mWidgets", mWidgets);
    }

    private void dumpAppWidgetProviderInfoList(String tag, String label, ArrayList<Object> list) {
        LogUtil.d(label + " size=" + list.size());
        for (Object i : list) {
            if (i instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) i;
                LogUtil.d( "tag"+tag+"   label=\"" + info.label + "\" previewImage=" + info.previewImage + " resizeMode=" + info.resizeMode + " configure=" + info.configure + " initialLayout=" + info.initialLayout + " minWidth=" + info.minWidth + " minHeight=" + info.minHeight);
            } else if (i instanceof ResolveInfo) {
                ResolveInfo info = (ResolveInfo) i;
                LogUtil.d( "tag"+tag+"   label=\"" + info.loadLabel(mPackageManager) + "\" icon=" + info.icon);
            }
        }
    }

    public void surrender() {
        // TODO: If we are in the middle of any process (ie. for holographic
        // outlines, etc) we
        // should stop this now.

        // Stop all background tasks
        cancelAllTasks();
    }

    @Override
    public void iconPressed(PagedViewIcon icon) {
        // Reset the previously pressed icon and store a reference to the
        // pressed icon so that
        // we can reset it on return to Launcher (in Launcher.onResume())
        if (mPressedIcon != null) {
            mPressedIcon.resetDrawableState();
        }
        mPressedIcon = icon;
    }

    public void resetDrawableState() {
        if (mPressedIcon != null) {
            mPressedIcon.resetDrawableState();
            mPressedIcon = null;
        }
    }

    /*
     * We load an extra page on each side to prevent flashes from scrolling and
     * loading of the widget previews in the background with the AsyncTasks.
     */
    final static int sLookBehindPageCount = 2;
    final static int sLookAheadPageCount = 2;

    protected int getAssociatedLowerPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMinIndex = Math.max(Math.min(page - sLookBehindPageCount, count - windowSize), 0);
        return windowMinIndex;
    }

    protected int getAssociatedUpperPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMaxIndex = Math.min(Math.max(page + sLookAheadPageCount, windowSize - 1), count - 1);
        return windowMaxIndex;
    }

    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int stringId = R.string.default_scroll_format;
        int count = 0;

        if (page < mNumAppsPages) {
            stringId = R.string.apps_customize_apps_scroll_format;
            count = mNumAppsPages;
        } else {
            page -= mNumAppsPages;
            stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWidgetPages;
        }

        return String.format(getContext().getString(stringId), page + 1, count);
    }
}

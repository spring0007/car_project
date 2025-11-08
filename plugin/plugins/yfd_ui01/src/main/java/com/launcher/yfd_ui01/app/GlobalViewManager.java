package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.awell.launcher2.LauncherModel;
import com.launcher.yfd_ui01.app.AppGridView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class GlobalViewManager {
    private static final String TAG = "GlobalViewManager";
    private static final boolean DEBUG = true;

    private static GlobalViewManager sInstance;

    private AppGridView gridView;

    private WeakReference<ViewGroup> currentViewGroup;
    // Store visibility maps per container to avoid id collisions across different activities/views
    private WeakHashMap<ViewGroup, Map<Integer, Integer>> originalViewsVisibility = new WeakHashMap<>();

    private final Context mAppContext;
    private boolean mAllIsShowing = false;

    private GlobalViewManager(Context context) {
        mAppContext = context;
    }

    public static GlobalViewManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (GlobalViewManager.class) {
                if (sInstance == null) {
                    sInstance = new GlobalViewManager(context);
                }
            }
        }
        return sInstance;
    }

    public void showApps(ViewGroup viewGroup ,int pageIndex) {
        synchronized (this) {
            // Defensive check: if flag says shown, verify actual attachment
            if (mAllIsShowing) {
                boolean attached = (gridView != null && gridView.getParent() instanceof ViewGroup);
                if (attached) {
                    // Already attached somewhere. If it's the same container, nothing to do.
                    ViewGroup attachedParent = (ViewGroup) gridView.getParent();
                    if (attachedParent == viewGroup) {
                        if (DEBUG) Log.i(TAG, "showApps: already showing in the same viewGroup");
                        return;
                    }

                    // Attached to a different parent: move it to the requested parent.
                    if (DEBUG) Log.i(TAG, "showApps: gridView attached to different parent, moving to new viewGroup");
                    try {
                        removeFromParent();
                    } catch (Exception e) {
                        Log.w(TAG, "showApps: failed removing from previous parent", e);
                        // continue and attempt to add to the new parent
                    }
                } else {
                    // flag stale: reset and continue
                    if (DEBUG) Log.w(TAG, "showApps: mAllIsShowing was true but gridView not attached, rectifying flag");
                    mAllIsShowing = false;
                }
            }

            currentViewGroup = new WeakReference<>(viewGroup);
            saveActivityState(viewGroup);

            // 隐藏所有内容视图
            hideAllContentViews(viewGroup);

            // ensure previous instance is removed
            removeFromParent();

            if (gridView == null) {
                // Use the viewGroup's context (likely the Activity) so that resources/layouts
                // resolve correctly in plugin/host environments instead of using the stored
                // application context which may not have plugin resources.
                gridView = new AppGridView(viewGroup.getContext());
            }

            try {
                viewGroup.addView(
                        gridView,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                if (DEBUG) {
                    Log.i(TAG, "showApps: huang show all apps=>");
                }
                if (gridView != null) gridView.requestFocus();
                mAllIsShowing = true;
            } catch (Exception e) {
                Log.w(TAG, "showApps: failed to add gridView to viewGroup", e);
                mAllIsShowing = false;
            }
        }
    }

    private void hideAllContentViews(ViewGroup viewGroup) {
        ViewGroup contentView = viewGroup;
        for (int i = 0; i < contentView.getChildCount(); i++) {
            View child = contentView.getChildAt(i);
            if (child != gridView) {
                child.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 再添加全部app列表显示前确保先移除该View
     */
    private void removeFromParent() {
        if (gridView != null) {
            try {
                // Let AppGridView perform its internal cleanup first
                gridView.closeView(mAppContext);
            } catch (Exception e) {
                Log.w(TAG, "gridView.closeView failed", e);
            }

            if (gridView.getParent() != null && gridView.getParent() instanceof ViewGroup) {
                try {
                    ((ViewGroup) gridView.getParent()).removeView(gridView);
                } catch (Exception e) {
                    Log.w(TAG, "removeView failed", e);
                }
            }
            // clear reference so a new AppGridView will be created next time if needed
            try {
                gridView = null;
            } catch (Exception e) {
                Log.w(TAG, "clearing gridView reference failed", e);
            }
        }
        mAllIsShowing = false;
    }

    /**
     * 在显示全部app列表时需要隐藏当前Activity的全部子view，否则会显示在全部app列表后面
     */
    private void saveActivityState(ViewGroup viewGroup) {
        // 保存视图可见性状态
        ViewGroup contentView = viewGroup;
        Map<Integer, Integer> visMap = new HashMap<>();
        for (int i = 0; i < contentView.getChildCount(); i++) {
            View child = contentView.getChildAt(i);
            if (child != gridView) {
                int key = System.identityHashCode(child);
                visMap.put(key, child.getVisibility());
            }
        }
        originalViewsVisibility.put(viewGroup, visMap);
    }

    /**
     * 隐藏全部app时显示当前Activity的子view
     */
    private void restoreActivityState() {
        if (currentViewGroup != null && currentViewGroup.get() != null) {
            ViewGroup viewGroup = currentViewGroup.get();

            // 恢复视图可见性
            ViewGroup contentView = viewGroup;
            Map<Integer, Integer> visMap = originalViewsVisibility.get(viewGroup);
            if (visMap != null) {
                for (int i = 0; i < contentView.getChildCount(); i++) {
                    View child = contentView.getChildAt(i);
                    if (child != gridView) {
                        int key = System.identityHashCode(child);
                        Integer visibility = visMap.get(key);
                        if (visibility != null) {
                            child.setVisibility(visibility);
                        }
                    }
                }
                // remove stored map for this viewGroup
                originalViewsVisibility.remove(viewGroup);
            }
        }
    }

    public void hideApps() {
        synchronized (this) {
            if (!mAllIsShowing) {
                if (DEBUG) Log.i(TAG, "hideApps: huang already hide all apps==>");
                return;
            }

            removeFromParent();
            restoreActivityState();

            mAllIsShowing = false;
        }
    }

    public boolean isAllShowing() {
        // Consider view actually showing only if flag true and view is attached
        return mAllIsShowing && gridView != null && gridView.getParent() instanceof ViewGroup;
    }

    /**
     * Preload the AppGridView (create and let it start loading data asynchronously)
     * Use an Activity context if possible so resources resolve correctly.
     */
    public void preload(Context context) {
        try {
            if (gridView == null) {
                gridView = new AppGridView(context);
            }
        } catch (Exception e) {
            Log.w(TAG, "preload failed", e);
        }
    }
}
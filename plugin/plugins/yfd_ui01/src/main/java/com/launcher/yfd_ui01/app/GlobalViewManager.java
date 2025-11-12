package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class GlobalViewManager {
    private static final String TAG = "GlobalViewManager";
    private static final boolean DEBUG = true;

    private static volatile GlobalViewManager sInstance;

    private AppGridView gridView;
    private WeakReference<ViewGroup> currentViewGroup;
    private final WeakHashMap<ViewGroup, Map<Integer, Integer>> originalViewsVisibility = new WeakHashMap<>();
    private final Context mAppContext;
    private boolean mAllIsShowing = false;

    private GlobalViewManager(Context context) {
        mAppContext = context.getApplicationContext();
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

    public void showApps(ViewGroup viewGroup, int pageIndex) {
        synchronized (this) {
			if (gridView != null) {
                // 重新激活 AppGridView
                gridView.setViewActive(true);
            }
            if (mAllIsShowing && gridView != null) {
                ViewGroup parent = (ViewGroup) gridView.getParent();
                if (parent == viewGroup) {
                    return; // 已经显示在同一个容器中
                }
                if (parent != null) {
                    removeFromParent(); // 从其他容器移除
                }
            }

            currentViewGroup = new WeakReference<>(viewGroup);
            saveActivityState(viewGroup);
            hideAllContentViews(viewGroup);

            if (gridView == null) {
                gridView = new AppGridView(viewGroup.getContext());
            }

            try {
                // 添加淡入动画
                gridView.setAlpha(0f);
                viewGroup.addView(gridView, 
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.MATCH_PARENT);
                
                // 执行淡入动画
                gridView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setListener(null)
                    .start();
                
                gridView.requestFocus();
                mAllIsShowing = true;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "Failed to add gridView", e);
                mAllIsShowing = false;
            }
        }
    }

    private void hideAllContentViews(ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child != gridView) {
                child.setVisibility(View.GONE);
            }
        }
    }

    private void removeFromParent() {
        if (gridView != null) {
            // 先清理内部资源
            gridView.closeView(mAppContext);
            
            // 从父视图移除
            ViewGroup parent = (ViewGroup) gridView.getParent();
            if (parent != null) {
                parent.removeView(gridView);
            }
            
            gridView = null;
        }
        mAllIsShowing = false;
    }

    private void saveActivityState(ViewGroup viewGroup) {
        Map<Integer, Integer> visMap = new HashMap<>();
        int childCount = viewGroup.getChildCount();
        
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child != gridView) {
                visMap.put(System.identityHashCode(child), child.getVisibility());
            }
        }
        originalViewsVisibility.put(viewGroup, visMap);
    }

    private void restoreActivityState() {
        ViewGroup viewGroup = currentViewGroup != null ? currentViewGroup.get() : null;
        if (viewGroup == null) return;

        Map<Integer, Integer> visMap = originalViewsVisibility.get(viewGroup);
        if (visMap != null) {
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i);
                if (child != gridView) {
                    Integer visibility = visMap.get(System.identityHashCode(child));
                    if (visibility != null) {
                        child.setVisibility(visibility);
                    }
                }
            }
            originalViewsVisibility.remove(viewGroup);
        }
    }

    public void hideApps() {
        synchronized (this) {
            if (!mAllIsShowing) return;
            
            removeFromParent();
            restoreActivityState();
        }
    }

    public boolean isAllShowing() {
        return mAllIsShowing && gridView != null && gridView.getParent() instanceof ViewGroup;
    }

    /**
     * 清理所有资源
     */
    public void destroy() {
        synchronized (this) {
            hideApps();
            originalViewsVisibility.clear();
            currentViewGroup = null;
            sInstance = null;
        }
    }
}
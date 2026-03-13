package com.launcher.yfd_ui01.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;


import com.awell.addapp.AppInfo;
import com.launcher.yfd_ui01.utils.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 优化的PagerAdapter，为每个页面创建RecyclerView（网格布局）
 */
public class AppPagerAdapter extends PagerAdapter {
    private static final int ITEMS_PER_PAGE = 18;
    private static final int ITEMS_VER = 6;
    private static final int ITEMS_HOR = 3;
    private final Context context;
    private final List<List<AppInfo>> pages;
    // 保存已创建的页面 RecyclerView 引用，便于更新数据而不重建视图
    private final android.util.SparseArray<RecyclerView> pageViews = new android.util.SparseArray<>();
    
    // 预创建的共享对象，避免重复创建
    private final GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            return 1;
        }
    };
    
    // 共享的ItemAnimator，禁用变化动画
    private final DefaultItemAnimator sharedAnimator = createSharedItemAnimator();

    public AppPagerAdapter(Context context, List<List<AppInfo>> pages, int itemsPerPage) {
        this.context = context;
        this.pages = pages;
    }

    @Override
    public int getCount() {
        return pages == null ? 0 : pages.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        RecyclerView recyclerView = createRecyclerView(container.getContext());
        //LogUtil.i("container,height="+container.getHeight()+",width="+container.getWidth());
        setupRecyclerView(recyclerView, position ,container.getWidth(),container.getHeight());
        // 保存引用
        pageViews.put(position, recyclerView);
        container.addView(recyclerView);
        return recyclerView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) object;
            // 清理资源
            recyclerView.setAdapter(null);
            recyclerView.setLayoutManager(null);
            container.removeView(recyclerView);
            // 移除保存的引用
            pageViews.remove(position);
        }
    }

    /**
     * 创建并配置RecyclerView
     */
    private RecyclerView createRecyclerView(Context ctx) {
        RecyclerView recyclerView = new RecyclerView(ctx);
        
        // 使用预创建的GridLayoutManager
        GridLayoutManager glm = new GridLayoutManager(ctx, 6, RecyclerView.VERTICAL, false);
        glm.setSpanSizeLookup(spanSizeLookup);
        recyclerView.setLayoutManager(glm);
        recyclerView.setPadding(0,0,0,0);
        
        // 性能优化配置
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        // 设置缓存大小
        recyclerView.setItemViewCacheSize(ITEMS_PER_PAGE);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        return recyclerView;
    }

    /**
     * 配置 RecyclerView
     */
    private void setupRecyclerView(RecyclerView recyclerView, int position ,int width, int height) {
        if (pages == null || position < 0 || position >= pages.size()) return;

        // 先移除所有已有的 ItemDecoration
        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }

        // 设置适配器
        AppGridRecyclerAdapter adapter = new AppGridRecyclerAdapter(context, pages.get(position));

        // 设置尺寸计算监听器 - 优化版本
        adapter.setOnItemSizeCalculatedListener(new AppGridRecyclerAdapter.OnItemSizeCalculatedListener() {
            @Override
            public void onItemSizeCalculated(int itemWidth, int itemHeight) {
                // 确保在主线程
                if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            onItemSizeCalculated(itemWidth, itemHeight));
                    return;
                }

                // 检查视图是否还可用
                if (!recyclerView.isAttachedToWindow() || recyclerView.getAdapter() == null ||
                        recyclerView.getHeight()==0 || recyclerView.getWidth()==0) {
                    return;
                }
                
                // 计算间距
                int leftSpacing = Math.max(10, (width - itemWidth * ITEMS_VER) / (ITEMS_VER + 1));
                int topSpacing = Math.max(10, (height - itemHeight * ITEMS_HOR) / (ITEMS_HOR + 1));

                // 添加 ItemDecoration（确保只添加一次）
                if (recyclerView.getItemDecorationCount() == 0) {
                    recyclerView.addItemDecoration(
                            AppGridRecyclerAdapter.createGridSpacingItemDecoration(
                                    6,
                                    leftSpacing,
                                    topSpacing,
                                    true
                            )
                    );
                }

                // 请求重新布局
                recyclerView.post(() -> {
                    if (recyclerView.getLayoutManager() != null) {
                        recyclerView.getLayoutManager().requestLayout();
                    }
                });
            }
        });

        // 设置适配器前先禁用动画
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        recyclerView.setItemAnimator(null);

        recyclerView.setAdapter(adapter);

        // 延迟恢复动画
        recyclerView.post(() -> {
            if (recyclerView.getAdapter() == adapter) {
                recyclerView.setItemAnimator(animator);
            }
        });
    }

    /**
     * 创建共享的ItemAnimator
     */
    private DefaultItemAnimator createSharedItemAnimator() {
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        animator.setAddDuration(100);    // 减少动画时间
        animator.setRemoveDuration(100);
        animator.setMoveDuration(100);
        animator.setChangeDuration(0);   // 禁用变化动画
        return animator;
    }

    /**
     * 数据更新方法
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<List<AppInfo>> newPages) {
        // 确保在主线程执行
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            new Handler(Looper.getMainLooper()).post(() -> updateData(newPages));
            return;
        }

        int oldCount = getCount();

        // 1. 先暂停所有动画
        for (int i = 0; i < pageViews.size(); i++) {
            RecyclerView rv = pageViews.valueAt(i);
            if (rv != null && rv.getItemAnimator() != null) {
                rv.getItemAnimator().endAnimations();
            }
        }

        // 2. 更新数据源
        this.pages.clear();
        if (newPages != null) {
            this.pages.addAll(newPages);
        }

        int newCount = getCount();

        // 3. 安全更新每个页面的适配器
        for (int i = 0; i < pageViews.size(); i++) {
            int key = pageViews.keyAt(i);
            RecyclerView rv = pageViews.get(key);
            if (rv == null) continue;

            RecyclerView.Adapter adapter = rv.getAdapter();
            if (adapter instanceof AppGridRecyclerAdapter) {
                List<AppInfo> listForPage = (key >= 0 && key < pages.size())
                        ? pages.get(key)
                        : Collections.emptyList();

                // 使用安全的数据更新方法
                updateAdapterSafely((AppGridRecyclerAdapter) adapter, listForPage);
            }
        }

        // 4. 恢复动画（如果需要）
        for (int i = 0; i < pageViews.size(); i++) {
            RecyclerView rv = pageViews.valueAt(i);
            if (rv != null) {
                // 延迟执行以确保布局完成
                rv.post(() -> {
                    if (rv.getAdapter() != null) {
                        rv.getAdapter().notifyDataSetChanged();
                    }
                });
            }
        }

        // 5. 页数变化时通知
        if (oldCount != newCount) {
            notifyDataSetChanged();
        }
    }

    /**
     * 安全更新适配器数据
     */
    private void updateAdapterSafely(AppGridRecyclerAdapter adapter, List<AppInfo> newList) {
        try {
            adapter.setAppList(newList);
        } catch (Exception e) {
            // 发生异常时重新创建适配器
            LogUtil.e("Error updating adapter data", e);
            recreateAdapter(adapter, newList);

        }
    }

    /**
     * 重新创建适配器（容错处理）
     */
    private void recreateAdapter(AppGridRecyclerAdapter oldAdapter, List<AppInfo> newList) {
        // 获取旧的 RecyclerView
        RecyclerView recyclerView = null;
        for (int i = 0; i < pageViews.size(); i++) {
            if (pageViews.valueAt(i).getAdapter() == oldAdapter) {
                recyclerView = pageViews.valueAt(i);
                break;
            }
        }

        if (recyclerView != null) {
            // 临时禁用动画
            RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
            recyclerView.setItemAnimator(null);

            // 创建新适配器
            AppGridRecyclerAdapter newAdapter = new AppGridRecyclerAdapter(
                    context,
                    new ArrayList<>(newList)  // 使用副本避免引用问题
            );
            recyclerView.setAdapter(newAdapter);

            // 延迟恢复动画
            RecyclerView finalRecyclerView = recyclerView;
            recyclerView.postDelayed(() -> {
                if (finalRecyclerView.getAdapter() == newAdapter) {
                    finalRecyclerView.setItemAnimator(animator);
                }
            }, 100);
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        // 返回 POSITION_UNCHANGED 以保持现有页面实例，避免频繁重建导致的状态丢失
        // 只有在真正需要重建页面时才返回 POSITION_NONE
        return POSITION_UNCHANGED;
    }

    /**
     * 更新所有页面的布局间距
     * @param containerWidth ViewPager 的当前宽度
     * @param containerHeight ViewPager 的当前高度
     */
    public void updateAllPageLayouts(int containerWidth, int containerHeight) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            new Handler(Looper.getMainLooper()).post(() ->
                    updateAllPageLayouts(containerWidth, containerHeight));
            return;
        }
    
        for (int i = 0; i < pageViews.size(); i++) {
            int position = pageViews.keyAt(i);
            RecyclerView recyclerView = pageViews.valueAt(i);
            updatePageLayout(recyclerView, position, containerWidth, containerHeight);
        }
    }

    /**
     * 更新单个页面的布局
     */
    private void updatePageLayout(RecyclerView recyclerView, int position,
                                  int containerWidth, int containerHeight) {
        if (recyclerView == null) return;

        // 获取屏幕密度
        float density = recyclerView.getContext().getResources().getDisplayMetrics().density;
        
        // 基于屏幕密度和容器尺寸动态计算item尺寸
        int itemWidth = calculateItemWidth(containerWidth, density);
        int itemHeight = calculateItemHeight(containerHeight, density);

        // 尝试从适配器获取item尺寸
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (adapter instanceof AppGridRecyclerAdapter) {
            AppGridRecyclerAdapter gridAdapter = (AppGridRecyclerAdapter) adapter;
            // 优先使用实际测量的item尺寸
            int measuredWidth = gridAdapter.getItemWidth();
            int measuredHeight = gridAdapter.getItemHeight();
            if (measuredWidth > 0) itemWidth = measuredWidth;
            if (measuredHeight > 0) itemHeight = measuredHeight;
        }
        LogUtil.i("containerWidth= "+containerWidth+",containerHeight="+containerHeight+
                ", itemWidth= "+itemWidth+", itemHeight="+itemHeight+", density="+density);

        int leftSpacing = Math.max((int)(10 * density), (containerWidth - itemWidth * ITEMS_VER) / (ITEMS_VER + 1));
        int topSpacing = Math.max((int)(10 * density), (containerHeight - itemHeight * ITEMS_HOR) / (ITEMS_HOR + 1));
        LogUtil.i("leftSpacing= "+leftSpacing+",topSpacing="+topSpacing);
        // 更新ItemDecoration
        updateItemDecoration(recyclerView, leftSpacing, topSpacing);

        // 请求重新布局
        recyclerView.post(() -> {
            if (recyclerView.getLayoutManager() != null) {
                recyclerView.getLayoutManager().requestLayout();
            }
        });
    }
    
    /**
     * 基于容器宽度和屏幕密度计算item宽度
     */
    private int calculateItemWidth(int containerWidth, float density) {
        // 基于屏幕密度和容器宽度动态计算
        int baseWidth = (int)(134 * density);
        // 确保在不同分辨率下都能适应
        int maxWidth = containerWidth / (ITEMS_VER + 1);
        int minWidth = (int)(80 * density);
        return Math.min(Math.max(baseWidth, minWidth), maxWidth);
    }
    
    /**
     * 基于容器高度和屏幕密度计算item高度
     */
    private int calculateItemHeight(int containerHeight, float density) {
        // 基于屏幕密度和容器高度动态计算
        int baseHeight = (int)(152 * density);
        // 确保在不同分辨率下都能适应
        int maxHeight = containerHeight / (ITEMS_HOR + 1);
        int minHeight = (int)(100 * density);
        return Math.min(Math.max(baseHeight, minHeight), maxHeight);
    }

    /**
     * 更新ItemDecoration
     */
    private void updateItemDecoration(RecyclerView recyclerView,
                                      int leftSpacing, int topSpacing) {
        // 移除旧的ItemDecoration
        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }

        // 添加新的ItemDecoration
        recyclerView.addItemDecoration(
                AppGridRecyclerAdapter.createGridSpacingItemDecoration(
                        6,
                        leftSpacing,
                        topSpacing,
                        true
                )
        );
    }

    /**
     * 重新计算并更新指定页面的布局
     */
    public void recalculatePageLayout(int position) {
        if (position < 0 || position >= getCount()) return;

        RecyclerView recyclerView = pageViews.get(position);
        if (recyclerView == null) return;

        // 获取当前尺寸
        ViewParent parent = recyclerView.getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) parent;
            updatePageLayout(recyclerView, position,
                    container.getWidth(), container.getHeight());
        }
    }
}

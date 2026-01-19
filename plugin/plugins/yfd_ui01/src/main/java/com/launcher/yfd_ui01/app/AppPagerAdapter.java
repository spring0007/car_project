package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;


import com.awell.addapp.AppInfo;
import com.launcher.yfd_ui01.R;
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
     * 配置RecyclerView
     */
    private void setupRecyclerView(RecyclerView recyclerView, int position ,int width, int height) {
        if (pages == null || position < 0 || position >= pages.size()) return;

        // 先移除所有已有的 ItemDecoration
        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }

        // 设置适配器
        AppGridRecyclerAdapter adapter = new AppGridRecyclerAdapter(context, pages.get(position));
        int maxLeftSpacing =  context.getResources().getDimensionPixelSize(R.dimen.menu_max_left_space);
        int maxTopSpacing = context.getResources().getDimensionPixelSize(R.dimen.menu_max_top_space);

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
                if (!recyclerView.isAttachedToWindow() || recyclerView.getAdapter() == null) {
                    return;
                }
                //LogUtil.i("itemWidth= "+itemWidth+",itemHeight="+itemHeight);
                // 计算间距720:54,40 800: 54,57

                int leftSpacing = Math.max(maxLeftSpacing, (width - itemWidth * ITEMS_VER) / (ITEMS_VER + 1));
                int topSpacing = Math.max(maxTopSpacing, (height - itemHeight * ITEMS_HOR) / (ITEMS_HOR + 1));
                //LogUtil.i("leftSpacing= "+leftSpacing+",topSpacing="+topSpacing);

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

        // 移除旧的全局布局监听器，使用更安全的方式
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            boolean isFirstLayout = true;

            @Override
            public void onGlobalLayout() {
                if (isFirstLayout) {
                    isFirstLayout = false;
                    recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // 延迟重置尺寸计算
                    recyclerView.postDelayed(() -> {
                        if (adapter != null && recyclerView.getAdapter() == adapter) {
                            adapter.resetSizeCalculation();
                        }
                    }, 50);
                }
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
        // 默认不强制重建页面，避免由于 notifyDataSetChanged 导致的页面闪烁
        return POSITION_UNCHANGED;
    }
}

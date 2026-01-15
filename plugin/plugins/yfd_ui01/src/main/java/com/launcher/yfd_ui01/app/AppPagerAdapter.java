package com.launcher.yfd_ui01.app;

import android.content.Context;
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
        
        // 设置适配器
        AppGridRecyclerAdapter adapter = new AppGridRecyclerAdapter(context, pages.get(position));

        // 设置尺寸计算监听器
        adapter.setOnItemSizeCalculatedListener(new AppGridRecyclerAdapter.OnItemSizeCalculatedListener() {
            @Override
            public void onItemSizeCalculated(int itemWidth, int itemHeight) {
                // 根据 item 尺寸计算间距
                //int horizontalSpacing = calculateHorizontalSpacing(itemWidth);
                //int verticalSpacing = calculateVerticalSpacing(itemHeight);

                int leftSpacing = (width - itemWidth*ITEMS_VER)/(ITEMS_VER+1);
                int topSpacing = (height - itemHeight*ITEMS_HOR)/(ITEMS_HOR+1);
               // LogUtil.i("recyclerView,topSpacing="+topSpacing+",leftSpacing="+leftSpacing);
                if(leftSpacing<0)
                    leftSpacing = 10;
                if(topSpacing<0)
                    topSpacing = 10;



                // 移除旧的 ItemDecoration
                if (recyclerView.getItemDecorationCount() > 0) {
                    recyclerView.removeItemDecorationAt(0);
                }

                // 添加新的 ItemDecoration
                recyclerView.addItemDecoration(
                        AppGridRecyclerAdapter.createGridSpacingItemDecoration(
                                6,
                                leftSpacing,
                                topSpacing,
                                true
                        )
                );
            }
        });

        recyclerView.setAdapter(adapter);
        // 153, 150
        // 监听 RecyclerView 尺寸变化（如旋转屏幕）
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // 重置尺寸计算，以便重新获取
                adapter.resetSizeCalculation();
            }
        });


        // 共享的ItemDecoration
        //RecyclerView.ItemDecoration sharedItemDecoration = AppGridRecyclerAdapter.createGridSpacingItemDecoration(6, leftSpacing, topSpacing, true);
        // 使用共享的ItemDecoration和ItemAnimator
        //recyclerView.addItemDecoration(sharedItemDecoration);

        recyclerView.setItemAnimator(sharedAnimator);
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
        int oldCount = getCount();
        this.pages.clear();
        if (newPages != null) {
            this.pages.addAll(newPages);
        }

        int newCount = getCount();

        // 更新已创建页面中 RecyclerView 的 adapter 数据，避免重建页面导致闪烁
        for (int i = 0; i < pageViews.size(); i++) {
            int key = pageViews.keyAt(i);
            RecyclerView rv = pageViews.get(key);
            if (rv == null) continue;
            RecyclerView.Adapter adapter = rv.getAdapter();
            if (adapter instanceof AppGridRecyclerAdapter) {
                List<AppInfo> listForPage = (key >= 0 && key < pages.size()) ? pages.get(key) : java.util.Collections.emptyList();
                ((AppGridRecyclerAdapter) adapter).setAppList(listForPage);
            }
        }

        // 仅在页数发生变化时通知 Pager 重新布局
        if (oldCount != newCount) {
            notifyDataSetChanged();
        }
    }
    @Override
    public int getItemPosition(@NonNull Object object) {
        // 默认不强制重建页面，避免由于 notifyDataSetChanged 导致的页面闪烁
        return POSITION_UNCHANGED;
    }
}

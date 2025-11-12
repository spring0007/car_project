package com.launcher.yfd_ui01.app;

import static com.launcher.yfd_ui01.app.AppGridView.ITEMS_PER_PAGE;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;


import com.awell.addapp.AppInfo;

import java.util.List;

/**
 * 优化的PagerAdapter，为每个页面创建RecyclerView（网格布局）
 */
public class AppPagerAdapter extends PagerAdapter {
    private final Context context;
    private final List<List<AppInfo>> pages;
    
    // 预创建的共享对象，避免重复创建
    private final GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            return 1;
        }
    };
    
    // 共享的ItemAnimator，禁用变化动画
    private final DefaultItemAnimator sharedAnimator = createSharedItemAnimator();
    
    // 共享的ItemDecoration
    private final RecyclerView.ItemDecoration sharedItemDecoration =
        AppGridRecyclerAdapter.createGridSpacingItemDecoration(6, 56, 30, true);

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
        setupRecyclerView(recyclerView, position);
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
    private void setupRecyclerView(RecyclerView recyclerView, int position) {
        if (pages == null || position < 0 || position >= pages.size()) return;
        
        // 设置适配器
        AppGridRecyclerAdapter adapter = new AppGridRecyclerAdapter(context, pages.get(position));
        recyclerView.setAdapter(adapter);
        
        // 使用共享的ItemDecoration和ItemAnimator
        recyclerView.addItemDecoration(sharedItemDecoration);
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
        this.pages.clear();
        if (newPages != null) {
            this.pages.addAll(newPages);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        // 强制刷新所有页面，确保数据一致性
        return POSITION_NONE;
    }
}

package com.launcher.yfd_ui01.app;

import static com.launcher.yfd_ui01.app.AppGridView.ITEMS_PER_PAGE;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;


import com.awell.addapp.AppInfo;

import java.util.List;

/**
 * PagerAdapter that creates a RecyclerView (GridLayout) per page. Each page is a RecyclerView
 * with a GridLayoutManager and uses AppGridRecyclerAdapter for items.
 */
public class AppPagerAdapter extends PagerAdapter {
    private Context context;
    private List<List<AppInfo>> pages;

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
        Context ctx = container.getContext();

        RecyclerView recyclerView = new RecyclerView(ctx);
        // Grid with 6 columns to mimic previous GridView (6 columns x 3 rows)
        GridLayoutManager glm = new GridLayoutManager(ctx, 6, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(glm);
        recyclerView.setHasFixedSize(true);
        // Cache enough child views for one or two pages to reduce rebinds while paging
        try {
            recyclerView.setItemViewCacheSize(ITEMS_PER_PAGE * 2);
        } catch (Exception ignored) {}
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        AppGridRecyclerAdapter adapter = new AppGridRecyclerAdapter(ctx, pages.get(position));
        recyclerView.setAdapter(adapter);

        // Improve animations and avoid change animations which can be heavy during updates
        try {
            androidx.recyclerview.widget.DefaultItemAnimator animator = new androidx.recyclerview.widget.DefaultItemAnimator();
            animator.setSupportsChangeAnimations(false);
            recyclerView.addItemDecoration(AppGridRecyclerAdapter.getGridItemDecoration(ctx, 6, 25, true)); //false 去掉首行top
        } catch (Exception ignored) {
        }

        // Some spacing can be added via ItemDecoration if desired (omitted for brevity)

        container.addView(recyclerView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return recyclerView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}

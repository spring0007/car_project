package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.awell.addapp.AppInfo;

import java.util.List;

public class AppPagerAdapter extends PagerAdapter {
    private Context context;
    private List<List<AppInfo>> pages;
    private int itemsPerPage; // 每页显示的应用数量

    public AppPagerAdapter(Context context, List<List<AppInfo>> pages, int itemsPerPage) {
        this.context = context;
        this.pages = pages;
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        GridView gridView = new GridView(context);
        gridView.setNumColumns(6); // 每行6个
        gridView.setColumnWidth(0);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gridView.setGravity(Gravity.CENTER);

        gridView.setVerticalSpacing(40);
       // gridView.setHorizontalSpacing(20);
       // gridView.setPadding(20, 20, 20, 20);
        gridView.setAdapter(new AppGridAdapter(context, pages.get(position)));
        
        container.addView(gridView);
        return gridView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}

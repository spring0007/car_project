package com.launcher.zy_ui04;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {

    private List<PageData> pageDataList;
    private MainActivityUI4 activity;

    public ViewPagerAdapter(List<PageData> pageDataList, MainActivityUI4 activity) {
        this.pageDataList = pageDataList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ViewPagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewPagerViewHolder holder, int position) {
        // 为页面中的所有 ImageView 添加触摸效果
        addTouchEffectToImageViews(holder.itemView);
    }
    /**
     * 递归查找并为所有 ImageView 添加触摸效果
     */
    private void addTouchEffectToImageViews(View view) {
        if (view instanceof ImageView) {
            TouchEffectUtils.addTouchEffect((ImageView) view);
        }
        
        // 如果是 ViewGroup，递归处理子视图
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                addTouchEffectToImageViews(viewGroup.getChildAt(i));
            }
        }
    }
    


    @Override
    public int getItemViewType(int position) {
        return pageDataList.get(position).getLayoutResId();
    }

    @Override
    public int getItemCount() {
        return pageDataList.size();
    }

    static class ViewPagerViewHolder extends RecyclerView.ViewHolder {
        public ViewPagerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}

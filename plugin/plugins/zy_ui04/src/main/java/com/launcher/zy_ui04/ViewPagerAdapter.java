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

import com.awell.library.util.PageData;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {

    /** ZLink(EasyConnect) 入口在页面里的 View，viewpager_4 大图 + viewpager_6 小图 */
    private static final int[] ZLINK_VIEW_IDS = {
            R.id.iv_easy_connect_big,
            R.id.tv_easy_connect_big,
            R.id.iv_easy_connect_small,
            R.id.tv_easy_connect_small
    };

    /** 导航入口，和 ZLink 入口占同一个位置，两者互斥显示 */
    private static final int[] NAVI_VIEW_IDS = {
            R.id.iv_navi_big,
            R.id.tv_navi_big,
            R.id.iv_navi_small,
            R.id.tv_navi_small
    };

    private List<PageData> pageDataList;
    private MainActivityUI4 activity;
    /** ZLink 是否可用：可用显示 ZLink 入口、隐藏导航入口；不可用则相反 */
    private boolean zlinkUsable = true;

    public ViewPagerAdapter(List<PageData> pageDataList, MainActivityUI4 activity) {
        this.pageDataList = pageDataList;
        this.activity = activity;
    }

    /**
     * 设置 ZLink 是否可用，状态变化时刷新已绑定的页面
     * 可用：显示 ZLink(EasyConnect) 入口、隐藏导航入口；不可用：显示导航入口、隐藏 ZLink 入口
     */
    public void setZlinkUsable(boolean usable) {
        if (this.zlinkUsable == usable) {
            return;
        }
        this.zlinkUsable = usable;
        notifyDataSetChanged();
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
        // 页面是懒加载的，新创建的页面也要应用当前的入口显隐状态
        applyEntryVisibility(holder.itemView);
    }

    /**
     * 递归查找 ZLink/导航入口 View 并按互斥规则设置显隐
     */
    private void applyEntryVisibility(View view) {
        if (contains(ZLINK_VIEW_IDS, view.getId())) {
            view.setVisibility(zlinkUsable ? View.VISIBLE : View.GONE);
        } else if (contains(NAVI_VIEW_IDS, view.getId())) {
            view.setVisibility(zlinkUsable ? View.GONE : View.VISIBLE);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyEntryVisibility(viewGroup.getChildAt(i));
            }
        }
    }

    private static boolean contains(int[] ids, int id) {
        for (int item : ids) {
            if (item == id) {
                return true;
            }
        }
        return false;
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

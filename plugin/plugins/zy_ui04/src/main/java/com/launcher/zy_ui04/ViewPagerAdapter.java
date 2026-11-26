package com.launcher.zy_ui04;

import android.os.Build;
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

    /**
     * mt6755 平台专用入口：
     * 抖音与图库、汽水音乐与浏览器分别占用同一个位置，互斥显示。
     * 抖音/汽水音乐的应用存在时显示前者，否则回退显示图库/浏览器。
     */
    private static final String HARDWARE_MT6755 = "mt6755";

    private static final int[] DOUYIN_VIEW_IDS = {
            R.id.iv_douyin_small,
            R.id.tv_douyin_small
    };

    private static final int[] QISHUI_VIEW_IDS = {
            R.id.iv_qishui_small,
            R.id.tv_qishui_small
    };

    private static final int[] GALLERY_SMALL_VIEW_IDS = {
            R.id.iv_gallery_small,
            R.id.tv_gallery_small
    };

    private static final int[] CHROME_SMALL_VIEW_IDS = {
            R.id.iv_chrome_small,
            R.id.tv_chrome_small
    };

    /** 第二页（viewpager_6）上的图库/浏览器入口，id 不能与第一页重复 */
    private static final int[] GALLERY_SMALL_PAGE2_VIEW_IDS = {
            R.id.iv_gallery_small_page2,
            R.id.tv_gallery_small_page2
    };

    private static final int[] CHROME_SMALL_PAGE2_VIEW_IDS = {
            R.id.iv_chrome_small_page2,
            R.id.tv_chrome_small_page2
    };

    private List<PageData> pageDataList;
    private MainActivityUI4 activity;
    /** ZLink 是否可用：可用显示 ZLink 入口、隐藏导航入口；不可用则相反 */
    private boolean zlinkUsable = true;
    /** 抖音应用是否存在（仅在 mt6755 上生效） */
    private boolean douyinUsable;
    /** 汽水音乐应用是否存在（仅在 mt6755 上生效） */
    private boolean qishuiUsable;

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

    /**
     * 设置抖音/汽水音乐应用是否可用，状态变化时刷新已绑定的页面。
     * 仅 mt6755 平台参与显隐：可用时用抖音/汽水音乐替换图库/浏览器入口，
     * 并被替换的图库/浏览器入口挪到第二页（viewpager_6）。
     */
    public void setDouyinQishuiUsable(boolean douyinUsable, boolean qishuiUsable) {
        if (this.douyinUsable == douyinUsable && this.qishuiUsable == qishuiUsable) {
            return;
        }
        this.douyinUsable = douyinUsable;
        this.qishuiUsable = qishuiUsable;
        notifyDataSetChanged();
    }

    private boolean isMt6755() {
        return HARDWARE_MT6755.equals(Build.HARDWARE);
    }

    /** 抖音入口是否显示（mt6755 且应用存在） */
    private boolean showDouyinEntry() {
        return isMt6755() && douyinUsable;
    }

    /** 汽水音乐入口是否显示（mt6755 且应用存在） */
    private boolean showQishuiEntry() {
        return isMt6755() && qishuiUsable;
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
        applyEntryVisibility(holder.itemView, pageDataList.get(position).getLayoutResId());
    }

    /**
     * 递归查找 ZLink/导航/抖音/汽水音乐入口 View 并按互斥规则设置显隐
     */
    private void applyEntryVisibility(View view, int layoutResId) {
        int id = view.getId();
        if (contains(ZLINK_VIEW_IDS, id)) {
            view.setVisibility(zlinkUsable ? View.VISIBLE : View.GONE);
        } else if (contains(NAVI_VIEW_IDS, id)) {
            view.setVisibility(zlinkUsable ? View.GONE : View.VISIBLE);
        } else if (layoutResId == R.layout.viewpager_5) {
            // 第一页：抖音/汽水音乐与图库/浏览器互斥
            if (contains(DOUYIN_VIEW_IDS, id)) {
                view.setVisibility(showDouyinEntry() ? View.VISIBLE : View.GONE);
            } else if (contains(GALLERY_SMALL_VIEW_IDS, id)) {
                view.setVisibility(showDouyinEntry() ? View.GONE : View.VISIBLE);
            } else if (contains(QISHUI_VIEW_IDS, id)) {
                view.setVisibility(showQishuiEntry() ? View.VISIBLE : View.GONE);
            } else if (contains(CHROME_SMALL_VIEW_IDS, id)) {
                view.setVisibility(showQishuiEntry() ? View.GONE : View.VISIBLE);
            }
        } else if (layoutResId == R.layout.viewpager_6) {
            // 第二页：第一页入口被抖音/汽水音乐占用时，图库/浏览器挪到这里显示
            if (contains(GALLERY_SMALL_PAGE2_VIEW_IDS, id)) {
                view.setVisibility(showDouyinEntry() ? View.VISIBLE : View.GONE);
            } else if (contains(CHROME_SMALL_PAGE2_VIEW_IDS, id)) {
                view.setVisibility(showQishuiEntry() ? View.VISIBLE : View.GONE);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyEntryVisibility(viewGroup.getChildAt(i), layoutResId);
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

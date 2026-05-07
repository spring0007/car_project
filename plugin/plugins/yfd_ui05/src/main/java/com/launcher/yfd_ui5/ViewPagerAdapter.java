package com.launcher.yfd_ui5;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {

    private List<PageData> pageDataList;
    private View.OnClickListener listener;
    private MainActivityUI5 activity;
    private static final String BTSTATUS = "awell_bt_status";

    public ViewPagerAdapter(List<PageData> pageDataList, View.OnClickListener listener, MainActivityUI5 activity) {
        this.pageDataList = pageDataList;
        this.listener = listener;
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
        // 根据位置设置不同页面的控件监听器
        if (position == 0) {
            // 第一页的控件 - 不再给总item设置点击事件，而是分别处理各子控件
            setupFirstPageListeners(holder);
        } else if (position == 1) {
            // 第二页的控件
//            setListener(holder.itemView, R.id.video_iv);
//            setListener(holder.itemView, R.id.setting_iv);
//            setListener(holder.itemView, R.id.radio_iv);
//            setListener(holder.itemView, R.id.all_iv);
        }
    }

    private void setupFirstPageListeners(ViewPagerViewHolder holder) {
        // 音乐组件内部已有完整的点击处理逻辑，无需额外设置
        MusicWidget musicWidget = holder.itemView.findViewById(R.id.music_widget_layout);
        if (musicWidget != null && activity != null) {
            activity.setMusicWidget(musicWidget);
        }

        // 为其他独立控件设置点击监听
        //setListener(holder.itemView, R.id.time_iv);
        //setListener(holder.itemView, R.id.bt_phone);
        //setListener(holder.itemView, R.id.navi_iv);
        
        // 注意：bt_phone内部的子控件(phone_book_iv, contact_iv)如果需要单独点击事件
        // 应该在对应的Activity或Fragment中处理，或者通过接口回调方式处理
        setupBtPhoneSubViews(holder);
    }

    private void setupBtPhoneSubViews(ViewPagerViewHolder holder) {
        // 处理bt_phone内部的子控件点击事件
        
        // bt_phone图标点击事件
        ImageView btPhoneIcon = holder.itemView.findViewById(R.id.iv_bt_phone_icon);
        if (btPhoneIcon != null) {
            btPhoneIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }
        
        // 电话簿点击事件
        ImageView phoneBookIv = holder.itemView.findViewById(R.id.phone_book_iv);
        if (phoneBookIv != null) {
            phoneBookIv.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }

        // 联系人点击事件
        ImageView contactIv = holder.itemView.findViewById(R.id.contact_iv);
        if (contactIv != null) {
            contactIv.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }
    }

    private void setListener(View parent, int viewId) {
        View view = parent.findViewById(viewId);
        if (view != null && listener != null) {
            // 确保控件可触摸和可点击，以显示点击效果
            view.setOnClickListener(listener);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return  pageDataList.get(position).getLayoutResId();
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

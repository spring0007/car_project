package com.launcher.zy_ui01;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {

    private List<PageData> pageDataList;

    private View.OnClickListener listener;

    private MainActivityUI1 activity;



    public ViewPagerAdapter(List<PageData> pageDataList, View.OnClickListener listener,MainActivityUI1 activity) {
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
            // 第二页的控件
            setListener(holder.itemView, R.id.iv_navi);
            setListener(holder.itemView, R.id.layout_music);
            setListener(holder.itemView, R.id.iv_radio);
            setListener(holder.itemView, R.id.iv_bluetooth);
            TextView textView = holder.itemView.findViewById(R.id.tv_music);
            activity.setMusicText(textView);
            MusicWidget musicWidget = holder.itemView.findViewById(R.id.music_widget_layout);
            activity.setMusicWidget(musicWidget);
        } else if (position == 1) {
            // 第一页的控件
            setListener(holder.itemView, R.id.iv_video);
            setListener(holder.itemView, R.id.iv_time);
            setListener(holder.itemView, R.id.iv_album);
            setListener(holder.itemView, R.id.iv_interconnect);
        }
    }

    private void setListener(View parent, int viewId) {
        View view = parent.findViewById(viewId);
        if (view != null && listener != null) {
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

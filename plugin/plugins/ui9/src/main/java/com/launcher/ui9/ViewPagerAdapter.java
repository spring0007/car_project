package com.launcher.ui9;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.library.util.PageData;

import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {

    private List<PageData> pageDataList;

    private View.OnClickListener listener;

    private MainActivityUI9 activity;



    public ViewPagerAdapter(List<PageData> pageDataList, View.OnClickListener listener,MainActivityUI9 activity) {
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
            // 第一页的控件
            setListener(holder.itemView, R.id.music_iv);
            setListener(holder.itemView, R.id.navi_iv);
            setListener(holder.itemView, R.id.radio_iv);
            setListener(holder.itemView, R.id.bt_iv);
            setListener(holder.itemView,R.id.music_widget_layout);
            MusicWidget musicWidget = holder.itemView.findViewById(R.id.music_widget_layout);
            activity.setMusicWidget(musicWidget);
        } else if (position == 1) {
            // 第二页的控件
            setListener(holder.itemView, R.id.setting_iv);
            setListener(holder.itemView, R.id.folder_iv);
            setListener(holder.itemView, R.id.video_iv);
            setListener(holder.itemView, R.id.all_iv);
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

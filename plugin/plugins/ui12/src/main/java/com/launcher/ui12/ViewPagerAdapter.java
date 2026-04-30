package com.launcher.ui12;

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
    private MainActivityUI12 activity;
    private static final String BTSTATUS = "awell_bt_status";

    public ViewPagerAdapter(List<PageData> pageDataList, View.OnClickListener listener, MainActivityUI12 activity) {
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
            setListener(holder.itemView, R.id.time_iv);
            setListener(holder.itemView, R.id.music_widget_layout);
            setListener(holder.itemView, R.id.bt_iv);
            setListener(holder.itemView, R.id.navi_iv);
            MusicWidget musicWidget = holder.itemView.findViewById(R.id.music_widget_layout);
            activity.setMusicWidget(musicWidget);
            ImageView btNoIv = holder.itemView.findViewById(R.id.bt_no_iv);
            int mBTStatus = Settings.System.getInt(activity.getContentResolver(), BTSTATUS, 0);
            if (mBTStatus == 0 || mBTStatus == 1){
                btNoIv.setVisibility(View.VISIBLE);
            }else if (mBTStatus == 2){
                btNoIv.setVisibility(View.GONE);
            }
            activity.setBtNoIv(btNoIv);
        } else if (position == 1) {
            // 第二页的控件
            setListener(holder.itemView, R.id.video_iv);
            setListener(holder.itemView, R.id.setting_iv);
            setListener(holder.itemView, R.id.radio_iv);
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

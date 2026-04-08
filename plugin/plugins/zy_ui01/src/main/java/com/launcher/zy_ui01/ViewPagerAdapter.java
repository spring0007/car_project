package com.launcher.zy_ui01;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.launcher.zy_ui01.utils.WeatherHelper;
import com.launcher.zy_ui01.utils.WeatherIconLoader;
import com.launcher.zy_ui01.utils.WeatherTextMapper;

import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {
    private List<PageData> pageDataList;

    private View.OnClickListener listener;

    private MainActivityUI1 activity;

    private WeatherHelper.WeatherInfo weatherInfo;

    @SuppressLint("NotifyDataSetChanged")
    public void setWeatherInfo(WeatherHelper.WeatherInfo weatherInfo) {
        this.weatherInfo = weatherInfo;
        notifyDataSetChanged();
    }

    public ViewPagerAdapter(List<PageData> pageDataList, View.OnClickListener listener, MainActivityUI1 activity) {
        this.pageDataList = pageDataList;
        this.listener = listener;
        this.activity = activity;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public ViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ViewPagerViewHolder(itemView);

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewPagerViewHolder holder, int position) {
        // 根据位置设置不同页面的控件监听器
        if (position == 0) {
            // 第二页的控件
            setListener(holder.itemView, R.id.iv_navi);
            setListener(holder.itemView, R.id.layout_music_widget);
            setListener(holder.itemView, R.id.iv_radio);
            setListener(holder.itemView, R.id.iv_bluetooth);
            MusicWidget musicWidget = holder.itemView.findViewById(R.id.layout_music_widget);
            activity.setMusicWidget(musicWidget);
        } else if (position == 1) {
            // 第一页的控件
            setListener(holder.itemView, R.id.iv_video);
            setListener(holder.itemView, R.id.iv_time);
            setListener(holder.itemView, R.id.iv_album);
            setListener(holder.itemView, R.id.iv_interconnect);
            TextView tvTemper = holder.itemView.findViewById(R.id.tv_temper);
            TextView tvTemperScope = holder.itemView.findViewById(R.id.tv_temper_scope);
            ImageView ivWeather = holder.itemView.findViewById(R.id.iv_weather);
            if (weatherInfo != null) {
                tvTemper.setText(weatherInfo.temperature + " ℃");
                String weather = (WeatherTextMapper.description(holder.itemView.getContext(), weatherInfo.condCode));
                tvTemperScope.setText(weather + " " + weatherInfo.tempMax + "/" + weatherInfo.tempMin + "℃");
                WeatherIconLoader.load(ivWeather, weatherInfo.condCode);
            } else {
                tvTemper.setText("");
                tvTemperScope.setText("");
                ivWeather.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
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

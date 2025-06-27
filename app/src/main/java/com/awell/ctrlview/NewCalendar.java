package com.awell.ctrlview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.awell.launcher.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/*星期日:Calendar.SUNDAY=1
 *星期一:Calendar.MONDAY=2
 *星期二:Calendar.TUESDAY=3
 *星期三:Calendar.WEDNESDAY=4
 *星期四:Calendar.THURSDAY=5
 *星期五:Calendar.FRIDAY=6
 *星期六:Calendar.SATURDAY=7 */
public class NewCalendar extends LinearLayout {
    private ImageView lastTv, nextTv;
    private TextView dateTv, tv1, tv2, tv3, tv4, tv5, tv6, tv7;
    private GridView calendarGv;
    private int months;

    private TextClock weekdayTc;

    private Calendar calendar = Calendar.getInstance();  //日历控件初始化

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {           //监听日期改变
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null || action.isEmpty()) return;
            if (action.equals(Intent.ACTION_DATE_CHANGED)) {
                calendar = Calendar.getInstance();
                renderCalendar();
            } else if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                //系统手动更改时间发送广播
                calendar = Calendar.getInstance();
                renderCalendar();
            }
        }
    };

    //重写三个构造方法
    public NewCalendar(Context context) {
        super(context);
    }

    public NewCalendar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initControl(context);  //绑定控件
    }

    public NewCalendar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initControl(context);  //绑定控件

    }

    private void initControl(Context context) {
        bindControl(context);  //绑定控件
        bindControlEvent();   //绑定控件事件
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        //context.registerReceiver(mBroadcastReceiver, filter);
    }

    public void unRegisterReceiver() {
        getContext().unregisterReceiver(mBroadcastReceiver);
    }


    //绑定控件事件方法
    private void bindControlEvent() {
        renderCalendar();
        //“下一月”点击事件
        nextTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.MONTH, +1);   //月份+1
                renderCalendar();
            }
        });
        //“上一个”点击事件
        lastTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("lastTv", "setOnClickListener");
                calendar.add(Calendar.MONTH, -1);   //月份-1
                renderCalendar();
            }
        });
    }

    public void renderCalendar(int value) {
        this.value = value;
        renderCalendar();
    }

    private void renderCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM");  //日期格式化
        dateTv.setText(sdf.format(calendar.getTime()));  //设置月份
        ArrayList<Date> cells = new ArrayList<>();
        Calendar calendar1 = (Calendar) calendar.clone();  //克隆日历对象
        calendar1.set(Calendar.DATE, 1);//把日期设置为当月第一天
        calendar1.roll(Calendar.DATE, -1);//日期回滚一天，也就是最后一天
        int maxCount = calendar1.get(Calendar.DATE);
        ;  //设置每个月最大天数
        months = calendar1.get(Calendar.MONTH) + 1;
        calendar1.set(Calendar.DATE, 1);
        int firstWeekDay = 6;
        calendar1.set(Calendar.DAY_OF_MONTH, 1);  //置于当月第一天;
        if (calendar1.get(Calendar.DAY_OF_WEEK) == 1) {
            calendar1.add(Calendar.DAY_OF_MONTH, -6);  //第一天
        } else {
            int prevDays = calendar1.get(Calendar.DAY_OF_WEEK) - 2;  //获取上个月最后一天是星期几
            calendar1.add(Calendar.DAY_OF_MONTH, -prevDays);  //第一天
            firstWeekDay = prevDays;
        }
        //循环存入集合中
        while (cells.size() < maxCount + firstWeekDay) {
            cells.add(calendar1.getTime());
            calendar1.add(Calendar.DAY_OF_MONTH, 1);  //日期+1
        }
        //设置适配器
        calendarGv.setAdapter(new CalendarAdapter(getContext(), cells));
    }

    private int value = 0;

    //适配器
    private class CalendarAdapter extends ArrayAdapter<Date> {
        LayoutInflater layoutInflater;
        TextView textView;

        public CalendarAdapter(Context context, ArrayList<Date> days) {
            super(context, R.layout.calendar_item_layout, days);
            layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(R.layout.calendar_item_layout, null, false);
            textView = view.findViewById(R.id.itemTv);
            Log.i("Calendar", "value==" + value);
            if (value == 0) {
                textView.setTextColor(Color.parseColor("#0D193C"));
                tv1.setTextColor(Color.parseColor("#0D193C"));
                tv2.setTextColor(Color.parseColor("#0D193C"));
                tv3.setTextColor(Color.parseColor("#0D193C"));
                tv4.setTextColor(Color.parseColor("#0D193C"));
                tv5.setTextColor(Color.parseColor("#0D193C"));
                tv6.setTextColor(Color.parseColor("#0D193C"));
                tv7.setTextColor(Color.parseColor("#0D193C"));
                dateTv.setTextColor(Color.parseColor("#0D193C"));
                weekdayTc.setTextColor(Color.parseColor("#9B2FFD"));
                lastTv.setImageResource(R.drawable.sf_radio_pre);
                nextTv.setImageResource(R.drawable.sf_radio_next);
            } else if (value == 1) {
                textView.setTextColor(Color.parseColor("#FFFFFF"));
                tv1.setTextColor(Color.parseColor("#FFFFFF"));
                tv2.setTextColor(Color.parseColor("#FFFFFF"));
                tv3.setTextColor(Color.parseColor("#FFFFFF"));
                tv4.setTextColor(Color.parseColor("#FFFFFF"));
                tv5.setTextColor(Color.parseColor("#FFFFFF"));
                tv5.setTextColor(Color.parseColor("#FFFFFF"));
                tv6.setTextColor(Color.parseColor("#FFFFFF"));
                tv7.setTextColor(Color.parseColor("#FFFFFF"));
                dateTv.setTextColor(Color.parseColor("#FFFFFF"));
                weekdayTc.setTextColor(Color.parseColor("#FFFFFF"));
                lastTv.setImageResource(R.drawable.sf_radio_pre_n);
                nextTv.setImageResource(R.drawable.sf_radio_next_n);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Date date = getItem(position);
            ViewHolder viewHolder;
            if (convertView == null) {  //初始化绑定
                convertView = layoutInflater.inflate(R.layout.calendar_item_layout, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.itemTv = convertView.findViewById(R.id.itemTv);
                convertView.setTag(viewHolder);
            }
            viewHolder = (ViewHolder) convertView.getTag();
            int day = date.getDate();
            int mot = date.getMonth() + 1;
            if (mot == months) {
                viewHolder.itemTv.setText(String.valueOf(day));  //赋值
            } else {
                viewHolder.itemTv.setText("");  //赋值
            }
            viewHolder.itemTv.setTextColor(value == 1 ? Color.WHITE : Color.BLACK);

            Date now = new Date();
            if (now.getDate() == date.getDate() && now.getMonth() == date.getMonth() && now.getYear() == date.getYear()) {
                viewHolder.itemTv.setTextColor(getResources().getColor(R.color.delete_target_hover_tint));
            }
            return convertView;
        }

        class ViewHolder {
            TextView itemTv;
        }
    }

    private void bindControl(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.calendar_layout, this);

        lastTv = findViewById(R.id.lastTv);
        Log.d("lastTv", "findViewById");
        tv1 = findViewById(R.id.Tv1);
        tv2 = findViewById(R.id.Tv2);
        tv3 = findViewById(R.id.Tv3);
        tv4 = findViewById(R.id.Tv4);
        tv5 = findViewById(R.id.Tv5);
        tv6 = findViewById(R.id.Tv6);
        tv7 = findViewById(R.id.Tv7);
        weekdayTc = findViewById(R.id.weekday);
        nextTv = findViewById(R.id.nextTv);
        dateTv = findViewById(R.id.monthTv);
        calendarGv = findViewById(R.id.cv_calendar);
    }

}

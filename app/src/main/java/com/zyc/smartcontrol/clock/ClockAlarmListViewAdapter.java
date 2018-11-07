/*
 * Copyright 2014 trinea.cn All right reserved. This software is the confidential and proprietary information of
 * trinea.cn ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into with trinea.cn.
 */
package com.zyc.smartcontrol.clock;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.zyc.smartcontrol.R;

import java.util.List;
import java.util.Map;


public class ClockAlarmListViewAdapter extends BaseAdapter {

    private Activity context;
    private List mdata;
    private LayoutInflater inflater;
    private int last_year = 0, last_month = 0;
    final static String[] week = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    public ClockAlarmListViewAdapter(Activity context, List data) {
        this.context = context;
        this.mdata = data;
        initLayoutInflater();
    }

    void initLayoutInflater() {
        inflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return mdata.size();
    }

    public Object getItem(int position) {
        return mdata.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position1, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        View view = null;
        final int position = position1;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_alarm_list, null);
            holder = new ViewHolder();

            holder.tv_time = (TextView) convertView.findViewById(R.id.tv_time);
            holder.tv_repeat = (TextView) convertView.findViewById(R.id.tv_repeat);
            holder.sw_on = (Switch) convertView.findViewById(R.id.sw_on);

            convertView.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
//        item.put("on", false);
//        item.put("repeat",0);
//        item.put("hour", 0);
//        item.put("minute", 0);

        holder.tv_time.setText(((Map<String, Object>) mdata.get(position)).get("time").toString());
        holder.sw_on.setChecked((boolean) ((Map<String, Object>) mdata.get(position)).get("on"));
        int repeat = (int) ((Map<String, Object>) mdata.get(position)).get("repeat");
        if (repeat == 0) holder.tv_repeat.setText("一次");
        else if (repeat >= 0x7f) holder.tv_repeat.setText("每天");
        else {
            String s="";
            for (int i = 0; i < 7; i++) {
                if ((repeat & (1 << i)) != 0) {
                    s+=","+week[i];
                }
            }
            holder.tv_repeat.setText(s.replaceFirst(",",""));
        }
//        holder.tv_repeat.setText(((Map<String, Object>) mdata.get(position)).get("reward").toString());
//        holder.sw_on.setText(
//                ((Map<String, Object>) mdata.get(position)).get("val").toString()
//                        + "/"
//                        + ((Map<String, Object>) mdata.get(position)).get("total").toString()
//        );

        return convertView;
    }

    private class ViewHolder {
        TextView tv_time;
        TextView tv_repeat;
        Switch sw_on;
    }
}

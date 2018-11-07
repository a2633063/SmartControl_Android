package com.zyc.smartcontrol.clock;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.zyc.MyFunction;
import com.zyc.smartcontrol.R;
import com.zyc.smartcontrol.socket.TCPSocketClient;
import com.zyc.smartcontrol.socket.UDPSocketClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.zyc.MyFunction.DoubletoInt;

@SuppressLint("ValidFragment")
public class ClockFragment extends Fragment {
    final String Tag = "ClockFragment_Tag";
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;

    //region 控件
    TextView log;
    Switch AlarmOn;
    Switch sw_show_opposite;
    SeekBar sb_brightness;
    CheckBox ck_auto_brightness;

    private ProgressDialog pd;
    //endregion

    Map<String, Object> item = new HashMap<>();
    List<Map<String, Object>> data = new ArrayList<>();
    ClockAlarmListViewAdapter adapter;

    //UDP
    UDPSocketClient UdpSocketClient;
    String IP;

    int deviceNum;

    public ClockFragment() {
    }

    public ClockFragment(int x) {
        deviceNum = x;
    }


    //region handler
    @SuppressLint("HandlerLeak")    //Warning:忽略此警告可能会导致严重的内存泄露
    //TODO 处理handler可能会导致的内存泄露问题
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                //region UDP广播获取设备IP地址成功
                case 1:
                    IP = mSharedPreferences.getString("LAN_IP", null);
                    log.setText(log.getText() + "\n自动获取设备IP地址成功:" + IP);
                    handler.sendEmptyMessageDelayed(3, 0);
                    break;
                //endregion
                //region UDP广播获取设备IP地址失败
                case 2:
                    IP = mSharedPreferences.getString("LAN_IP", null);
                    log.setText(log.getText() + "\n自动获取IP地址失败,使用保存的地址:" + IP);
                    handler.sendEmptyMessageDelayed(3, 0);
                    break;
                //endregion
                //region UDP获取ip mac成功 获取设置信息
                case 3:
                    if (UdpSocketClient != null) {
                        UdpSocketClient.Disconnect();
                        UdpSocketClient = null;
                    }
                    UdpSocketClient = new UDPSocketClient(getContext(), UDPhandler, IP, 12345);
                    UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x01, (byte) 0xff});
                    handler.sendEmptyMessageDelayed(4, 20);
                    break;
                case 4:
                    UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x02, (byte) 0xff});
                    handler.sendEmptyMessageDelayed(5, 20);
                    break;
                case 5:
                    UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x5, (byte) 0xff, 0x03});
                    handler.sendEmptyMessageDelayed(6, 20);
                    break;
                case 6:
                    UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x04, (byte) 0xff});
                    pd.dismiss();// 关闭ProgressDialog
                    break;
                //endregion
            }
        }
    };
    //endregion


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clock, container, false);


        mSharedPreferences = getActivity().getSharedPreferences("Setting" + deviceNum, 0);
        Log.d(Tag, "设置文件:" + "Setting" + deviceNum);
        mEditor = mSharedPreferences.edit();

        //region Listview 闹钟列表
        item = new HashMap<String, Object>();
        item.put("on", false);
        item.put("repeat", 0);
        item.put("time", "00:00");
        item.put("hour", 0);
        item.put("minute", 0);
        for (int i = data.size(); i < 5; i++)
            data.add(item);
        adapter = new ClockAlarmListViewAdapter(getActivity(), data);
        ListView ListView = (ListView) view.findViewById(R.id.lv);
        ListView.setAdapter(adapter);
        adapter.setOnItemSwitchCheckedChangeListener(new ClockAlarmListViewAdapter.onItemSwitchCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isChecked, int position1) {
                sendAlarm(position1);
            }
        });
        ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                windowAlarmSet(position);
            }
        });
        //endregion

        //region 亮度拖动条
        sb_brightness = (SeekBar) view.findViewById(R.id.sb_brightness);
        sb_brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ck_auto_brightness.setChecked(false);
                UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x01, (byte) sb_brightness.getProgress()});
            }
        });
        //endregion
        //region 显示方向
        sw_show_opposite = (Switch) view.findViewById(R.id.sw_show_opposite);
        sw_show_opposite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x04, (byte) (isChecked ? 1 : 0)});
            }
        });
        //endregion
        //region 自动亮度开关
        ck_auto_brightness = (CheckBox) view.findViewById(R.id.ck_auto_brightness);
        ck_auto_brightness.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (UdpSocketClient == null) {
//                    UdpSocketClient = new UDPSocketClient(getContext(), UDPhandler, IP, 12345);
//                }
                if (isChecked) {
                    UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x01, 0x08});
                } else {
                    UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x01, (byte) sb_brightness.getProgress()});
                }
            }
        });
        //endregion

        //region 闹钟开关
        AlarmOn = (Switch) view.findViewById(R.id.sw_alarm_on);
        AlarmOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (UdpSocketClient == null) {
//                    UdpSocketClient = new UDPSocketClient(getContext(), UDPhandler, IP, 12345);
//                }
                UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x6, (byte) 0xff, 0x02, isChecked ? (byte) 1 : (byte) 0});

            }
        });
        //endregion

        //region Log文本
        log = (TextView) view.findViewById(R.id.tv_log);
        final ScrollView scrollView = (ScrollView) view.findViewById(R.id.scrollView);
        log.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                scrollView.post(new Runnable() {
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
        //endregion
        DateFormat df = new SimpleDateFormat("---- yyyy/hh/dd HH:mm:ss ----");
        log.setText(df.format(new Date()));
        pd = ProgressDialog.show(getActivity(), "", "loading.....");

        return view;
    }

    @Override
    public void onResume() {
        Log.d(Tag, "ButtonFragment onResume" + deviceNum);
        super.onResume();

        if (UdpSocketClient == null) {
            DeviceConnect();
        }


    }

    @Override
    public void onPause() {
        Log.d(Tag, "ButtonFragment onPause" + deviceNum);

        super.onPause();
    }


    //region UDP广播获取设备信息
    String[] UDPgetIP = new String[2];
    int UDPgetIP_flag = 0;   //获取到IP地址,连续2次获取到相同的IP地址才确认
    int UDPnum = 0;//记录广播次数

    //region UDPhandler
    @SuppressLint("HandlerLeak")    //Warning:忽略此警告可能会导致严重的内存泄露
    //TODO 处理handler可能会导致的内存泄露问题
    private Handler UDPhandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 2:
                    if (UDPgetIP_flag > 1) {//已经获取到IP
                        UDPhandler.removeMessages(2);
                        break;
                    } else if (UDPnum > 5) {//5次尝试后失败
                        Log.e(Tag, "UDP get ip fail");
                        handler.sendEmptyMessageDelayed(2, 0);
                    } else {
                        UDPnum++;
                        UdpSocketClient.UDPsend(new byte[]{(byte) 0xa5, 0x5a, 0x05, (byte) 0xff, 0x00});
                        UDPhandler.sendEmptyMessageDelayed(2, 400);    //400ms后再次发送 共5次
                    }
                    break;
                case UDPSocketClient.HANDLE_UDP_TYPE:

                    int[] Redat = new int[msg.arg1];
                    for (int i = 0; i < msg.arg1; i++) Redat[i] = ((byte[]) msg.obj)[i] & 0xff;
                    Log.d(Tag, "getData:" + Arrays.toString(Redat));
                    if (Redat[0] == 0xa5 && Redat[1] == 0x5a) {

                        switch (Redat[4]) {

                            //region 获取IP地址
                            case 0x00:
                                if (UDPgetIP_flag > 1) break;
                                UDPhandler.removeMessages(2);
                                int[] mac = new int[6];
                                int[] ip = new int[4];
                                for (int i = 0; i < 6; i++) mac[i] = Redat[i + 5];
                                for (int i = 0; i < 4; i++) ip[i] = Redat[i + 11];
                                Log.d(Tag, String.format("MAC地址:%2x-%2x-%2x-%2x-%2x-%2x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]));
                                Log.d(Tag, String.format("IP:%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]));

                                UDPgetIP[UDPgetIP_flag] = String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
                                UDPgetIP_flag++;
                                //获取到IP地址,连续2次获取到相同的IP地址才确认
                                if (UDPgetIP_flag > 1) {
                                    if (UDPgetIP[0].equals(UDPgetIP[1])) {
                                        Log.d(Tag, "UDP get/set IP:" + UDPgetIP[0]);
                                        mEditor.putString("LAN_IP", UDPgetIP[0]);
                                        mEditor.commit();
                                        handler.sendEmptyMessageDelayed(1, 0);
                                    } else {
                                        //获取数据有误重新获取
                                        UDPnum--;
                                        UDPgetIP_flag = 0;
                                        UDPhandler.sendEmptyMessageDelayed(2, 0);
                                    }
                                } else {
                                    UDPhandler.sendEmptyMessageDelayed(2, 0);
                                }
                                break;
                            //endregion
                            //region 获取亮度
                            case 1:
                                if (Redat[5] == 8) {
                                    ck_auto_brightness.setChecked(true);
                                } else {
                                    sb_brightness.setProgress(Redat[5]);
                                }
                                break;
                            //endregion
                            //region 获取闹钟总开关
                            case 2:
                                AlarmOn.setChecked(Redat[5] != 0);
                                break;
                            //endregion
                            //region 获取闹钟设置
                            case 3:
                                item = new HashMap<String, Object>();
                                item.put("on", Redat[6] != 0);
                                item.put("repeat", Redat[7]);
                                item.put("hour", Redat[8]);
                                item.put("minute", Redat[9]);
                                item.put("time", String.format("%02d:%02d", Redat[8], Redat[9]));
                                data.set(Redat[5], item);
                                adapter.notifyDataSetChanged();

                                Message msg3 = new Message();
                                msg3.what = 0;
                                msg3.arg1 = Redat[5];
                                handler.sendMessageDelayed(msg3, 100);
                                break;
                            //endregion
                            //region 获取显示方向
                            case 4:
                                sw_show_opposite.setChecked(Redat[5] == 1);
                                break;
                            //endregion
                        }
                    }


                    break;
                case UDPSocketClient.HANDLE_UDP_ERROR:
                    break;
            }
        }
    };
    //endregion

    private void UDPgetIP() {
        UDPgetIP_flag = 0;
        UDPnum = 0;
        UdpSocketClient = new UDPSocketClient(getContext(), UDPhandler, "255.255.255.255", 12345);
        UDPgetIP_flag = 0;
        UDPnum = 1;
        UDPhandler.sendEmptyMessageDelayed(2, 0);    //200ms后再次发送 共5次
    }

    //endregion
    void DeviceConnect() {
        int Netstate = MyFunction.GetNetype(getContext());


        if (Netstate > 1 || mSharedPreferences.getBoolean("always_WAN", false)) {//当前为广域网或使能了总是使用广域网
            IP = mSharedPreferences.getString("WAN_IP", null);
            handler.sendEmptyMessageDelayed(3, 0);//直接连接
            Log.d(Tag, "广域网连接");
        } else if (Netstate == 1 && mSharedPreferences.getBoolean("auto_ip", true)) {//局域网且广播获取设备IP
            UDPgetIP();
            Log.d(Tag, "局域网,自动获取IP");
        } else if (Netstate == 1) {//局域网且失能广播获取设备IP
            IP = mSharedPreferences.getString("LAN_IP", null);
            handler.sendEmptyMessageDelayed(3, 0);//直接连接
            Log.d(Tag, "局域网");
        } else {
            Log.d(Tag, "其他网络:" + Netstate);
        }
    }


    //region 弹窗
    CheckBox chk_repeat_once;
    CheckBox chk_repeat_everyday;
    CheckBox chk_repeat_week;
    int weekVal = 0;
    final String[] mWeekItems = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    void windowAlarmSet(final int id) {
        final View popupView = getActivity().getLayoutInflater().inflate(R.layout.window_alarm_set, null);
        final PopupWindow window = new PopupWindow(popupView, -2, -2, true);
        window.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha = 0.3f; //0.0-1.0
        getActivity().getWindow().setAttributes(lp);


        //region 控件初始化
        final TimePicker timePicker = (TimePicker) popupView.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        chk_repeat_once = (CheckBox) popupView.findViewById(R.id.chk_repeat_once);
        chk_repeat_everyday = (CheckBox) popupView.findViewById(R.id.chk_repeat_everyday);
        chk_repeat_week = (CheckBox) popupView.findViewById(R.id.chk_repeat_week);
        chk_repeat_once.setOnClickListener(windowOnClickListener);
        chk_repeat_everyday.setOnClickListener(windowOnClickListener);
        chk_repeat_week.setOnClickListener(windowOnClickListener);

        popupView.findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.put(id, "repeat", weekVal);
                adapter.put(id, "hour", timePicker.getHour());
                adapter.put(id, "minute", timePicker.getMinute());
                adapter.put(id, "on", ((Map<String, Object>) adapter.getItem(id)).get("on"));
                adapter.notifyDataSetChanged();
                window.dismiss();
                sendAlarm(id);
            }
        });
        popupView.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                window.dismiss();
            }
        });
        //endregion
        //region 初始化显示
        Map<String, Object> item = (Map<String, Object>) adapter.getItem(id);
        weekVal = (int) item.get("repeat");
        timePicker.setHour((int) item.get("hour"));
        timePicker.setMinute((int) item.get("minute"));

        String s = "自定义";
        if (weekVal == 0) {
            chk_repeat_once.setChecked(true);
            chk_repeat_everyday.setChecked(false);
            chk_repeat_week.setChecked(false);
        } else if (weekVal >= 0x7f) {
            chk_repeat_once.setChecked(false);
            chk_repeat_everyday.setChecked(true);
            chk_repeat_week.setChecked(false);
        } else {
            chk_repeat_once.setChecked(false);
            chk_repeat_everyday.setChecked(false);
            chk_repeat_week.setChecked(true);
            for (int i = 0; i < 7; i++)
                if ((weekVal & (1 << i)) != 0) s += "," + mWeekItems[i];
        }
        chk_repeat_week.setText(s.replaceFirst(",", ":"));
        //endregion

        window.setBackgroundDrawable(new BitmapDrawable());
        window.setOutsideTouchable(true);
        window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                lp.alpha = 1.0f; //0.0-1.0
                getActivity().getWindow().setAttributes(lp);
            }
        });
        window.update();
        window.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    //region 弹窗CheckBox监听事件
    CompoundButton.OnClickListener windowOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            chk_repeat_once.setChecked(false);
            chk_repeat_everyday.setChecked(false);
            chk_repeat_week.setChecked(false);
            ((CheckBox) v).setChecked(true);
            if (chk_repeat_week.isChecked()) {
                if (weekVal >= 0x7f) weekVal = 0;
                //region 选择星期弹窗
                boolean[] mItemsCheck = new boolean[7];
                for (int i = 0; i < 7; i++) {
                    mItemsCheck[i] = ((weekVal & (1 << i)) != 0);
                }
                final ArrayList<Integer> MultiChoiceID = new ArrayList<Integer>();
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                MultiChoiceID.clear();
                builder.setTitle("多项选择");
                builder.setMultiChoiceItems(mWeekItems, mItemsCheck,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                                if (isChecked) weekVal |= (1 << (whichButton));
                                else weekVal &= (~(1 << (whichButton)));
                            }
                        });
                builder.setPositiveButton("确定", null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        String s = "自定义";
                        if (weekVal == 0) {
                            chk_repeat_once.setChecked(true);
                            chk_repeat_everyday.setChecked(false);
                            chk_repeat_week.setChecked(false);
                        } else if (weekVal >= 0x7f) {
                            chk_repeat_once.setChecked(false);
                            chk_repeat_everyday.setChecked(true);
                            chk_repeat_week.setChecked(false);
                        } else {
                            for (int i = 0; i < 7; i++) {
                                if ((weekVal & (1 << i)) != 0) s += "," + mWeekItems[i];
                            }
                        }
                        chk_repeat_week.setText(s.replaceFirst(",", ":"));

                        Log.d(Tag, "set repeat:" + weekVal);
                    }
                });
                builder.create().show();
                //endregion
            } else if (chk_repeat_everyday.isChecked()) {
                weekVal = 0x80;
                chk_repeat_week.setText("自定义");
            } else if (chk_repeat_once.isChecked()) {
                weekVal = 0x0;
                chk_repeat_week.setText("自定义");
            }
        }
    };
    //endregion
    //endregion

    //region 功能子函数
    void sendAlarm(int x) {
        if (UdpSocketClient == null) {
            UdpSocketClient = new UDPSocketClient(getContext(), UDPhandler, IP, 12345);
        }
        byte[] data = new byte[]{(byte) 0xa5, 0x5a, 0x0a, (byte) 0xff, 0x03, (byte) x, 0, 0, 0, 0};

        Map<String, Object> item = (Map<String, Object>) adapter.getItem(x);

        data[5] = (byte) x;
        data[6] = (byte) ((boolean) item.get("on") ? 1 : 0);
        data[7] = (byte) ((int) item.get("repeat"));
        data[8] = (byte) ((int) item.get("hour"));
        data[9] = (byte) ((int) item.get("minute"));
        UdpSocketClient.UDPsend(data);
    }
    //endregion
}

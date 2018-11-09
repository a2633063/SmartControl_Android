package com.zyc.smartcontrol.button;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zyc.MyFunction;
import com.zyc.smartcontrol.R;
import com.zyc.smartcontrol.socket.TCPSocketClient;
import com.zyc.smartcontrol.socket.UDPSocketClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.zyc.MyFunction.DoubletoInt;

@SuppressLint("ValidFragment")
public class ButtonFragment extends Fragment {
    final String Tag = "ButtonFragment_Tag";


    final static String UDPdeviceReport = "Device Report!!";
    final static String UDPdeviceReport_ok = "I'm button:";
    final static String setting_pwm_max = "rudder_max=";
    final static String setting_pwm_min = "rudder_min=";
    final static String setting_pwm_middle = "rudder_middle=";
    final static String setting_pwm_middle_delay = "rudder_delay=";
    final static String setting_pwm_test = "rudder_pwm_test=";
    final static String setting_get_all = "get all setting";

    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;

    private SwipeRefreshLayout mSwipeLayout;
    private LinearLayout ll;
    private SeekBar seekBar;
    private TextView tv_seekbarVal;
    private SeekBar seekBar_delay;
    private TextView tv_seekbarDelayVal;

    private Button bt_max;
    private Button bt_min;
    private Button bt_middle;

    TextView log;

    //UDP
    UDPSocketClient UdpSocketClient;
    //TCP
    String IP;
    TCPSocketClient TcpSocketClient;

    int deviceNum;

    Boolean Sflag1 = false;
    Boolean Sflag2 = false;


    public ButtonFragment(){
    }
    public ButtonFragment(int x) {
        deviceNum = x;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //region tcp接收数据
                case TCPSocketClient.HANDLE_TCP_TYPE:
//                    Toast.makeText(getContext(), Restr, Toast.LENGTH_SHORT).show();
                    String[] strarray = ((String) msg.obj).split("\\n");
                    for (String s : strarray) {
                        String str = "";
                        int val = 0;
                        if (s.startsWith(setting_pwm_max) && s.length() >= setting_pwm_max.length() + 3) {
                            str = s.replaceFirst(setting_pwm_max, "").substring(0, 3);
                            try {
                                val = Integer.parseInt(str);
                                bt_max.setText("设为最大值(" + val + ")");
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        } else if (s.startsWith(setting_pwm_min) && s.length() >= setting_pwm_min.length() + 3) {
                            str = s.replaceFirst(setting_pwm_min, "").substring(0, 3);
                            try {
                                val = Integer.parseInt(str);
                                bt_min.setText("设为最小值(" + val + ")");
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        } else if (s.startsWith(setting_pwm_middle) && s.length() >= setting_pwm_middle.length() + 3) {
                            str = s.replaceFirst(setting_pwm_middle, "").substring(0, 3);
                            try {
                                val = Integer.parseInt(str);
                                bt_middle.setText("设为平均值(" + val + ")");
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        } else if (s.startsWith(setting_pwm_middle_delay) && s.length() >= setting_pwm_middle_delay.length() + 3) {
                            str = s.replaceFirst(setting_pwm_middle_delay, "").substring(0, 3);
                            try {
                                val = Integer.parseInt(str);
                                seekBar_delay.setProgress(val - 20);
//                                log.setText(log.getText()+"\n"+"设置延时时间:"+val+"ms");
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                //endregion
                //region tcp错误
                case TCPSocketClient.HANDLE_TCP_ERROR:
                    switch (msg.arg1) {
                        case 0:
                            log.setText(log.getText() + "\n已连接:" + IP);
                            break;
                        case -2:
                            log.setText(log.getText() + "\n连接丢失");
                            break;
                        case -1:
                            log.setText(log.getText() + "\n连接失败");
                            break;
                        case 1:
                            log.setText(log.getText() + "\n已断开");
                            break;
                        case 2:
                            log.setText(log.getText() + "\n设置改变,重新连接....");
                            break;
                    }
                    break;
                //endregion

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
                //region tcp建立连接
                case 3:
                    int delay = 0;
                    if (TcpSocketClient != null) {
                        TcpSocketClient.close();
                        delay = 300;
                        TcpSocketClient = null;
                    }
                    handler.sendEmptyMessageDelayed(4, delay);
                    break;
                case 4:
                    if (IP != null) {
                        log.setText(log.getText() + "\n正在连接:" + IP);
                        TcpSocketClient = new TCPSocketClient(handler, IP, 10191);
                        TcpSocketClient.connect();
                    }
                    break;
                //endregion

                case 10://seekBar tcp发送
                    if ((Sflag1 || Sflag2) && (TcpSocketClient == null || !TcpSocketClient.isConnect())) {
//                        ll.setVisibility(View.GONE);
                        log.setText(log.getText() + "\n未连接设备");
                        Toast.makeText(getContext(), "设备未连接,请连接设备后调试", Toast.LENGTH_SHORT).show();
                        Sflag1 = Sflag2 = false;
                        break;
                    }
                    if (Sflag1) {//seekBar tcp发送
                        TcpSocketClient.Send(setting_pwm_test + String.format("%03d", seekBar.getProgress()));
                        Sflag1 = false;
                    }
                    if (Sflag2) {//seekBar_Delay tcp发送
                        TcpSocketClient.Send(setting_pwm_middle_delay + String.format("%03d", seekBar_delay.getProgress() + 20));
                        Sflag2 = false;
                    }

                    break;
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_button, container, false);

        mSharedPreferences = getActivity().getSharedPreferences("Setting" + deviceNum, 0);
        Log.d(Tag, "设置文件:" + "Setting" + deviceNum);
        mEditor = mSharedPreferences.edit();


        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        tv_seekbarVal = (TextView) view.findViewById(R.id.tv_seekbar);

        seekBar_delay = (SeekBar) view.findViewById(R.id.seekBar_delay);
        tv_seekbarDelayVal = (TextView) view.findViewById(R.id.tv_seekbar_delay);
        ll = (LinearLayout) view.findViewById(R.id.ll);
        tv_seekbarVal.setText("角度值:" + String.format("%03d", seekBar.getProgress()));

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorAccent, R.color.colorPrimary);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (ll.getVisibility() != View.VISIBLE) {
                    ll.setVisibility(View.VISIBLE);
                    if (TcpSocketClient == null || !TcpSocketClient.isConnect()) {
                    } else {
                        TcpSocketClient.Send(setting_get_all);
                    }

                } else ll.setVisibility(View.GONE);
                mSwipeLayout.setRefreshing(false);
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_seekbarVal.setText("角度值:" + String.format("%03d", progress));

                if (TcpSocketClient != null && TcpSocketClient.isConnect()) {
                    Sflag1 = true;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (TcpSocketClient != null && TcpSocketClient.isConnect()) {
                    Sflag1 = true;
                }
            }
        });
        seekBar_delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_seekbarDelayVal.setText("按下延时时间:" + String.format("%03d", progress + 20) + "ms");
//                Sflag2 = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (TcpSocketClient != null && TcpSocketClient.isConnect()) {
                    Sflag2 = true;
                }
            }
        });

        bt_min = (Button) view.findViewById(R.id.btn_1);
        bt_middle = (Button) view.findViewById(R.id.btn_2);
        bt_max = (Button) view.findViewById(R.id.btn_3);
        bt_max.setOnClickListener(buttonListener);
        bt_min.setOnClickListener(buttonListener);
        bt_middle.setOnClickListener(buttonListener);

        ImageView imageView = (ImageView) view.findViewById(R.id.iv_main_button1);
        imageView.setOnClickListener(buttonListener);
        imageView = (ImageView) view.findViewById(R.id.iv_main_button2);
        imageView.setOnClickListener(buttonListener);
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
        DateFormat df = new SimpleDateFormat("---- yyyy/hh/dd HH:mm:ss ----");
        log.setText(df.format(new Date()));


        TimerTask mTimerTask = new TimerTask() {
            public void run() {
                handler.sendEmptyMessageDelayed(10, 0);
            }
        };

        Timer timer = new Timer(true);
        timer.schedule(mTimerTask, 200, 200); //延时200ms后执行，200ms执行一次
        //timer.cancel(); //退出计时器
        return view;
    }

    @Override
    public void onResume() {
        Log.d(Tag, "ButtonFragment onResume" + deviceNum);
        super.onResume();
//        int t = mSharedPreferences.getBoolean("theme", false) ? R.style.AppTheme_Dark : R.style.AppTheme_Light;
//
//        if (theme != t) {
//            if (TcpSocketClient.isConnect()) {
//                TcpSocketClient.close();
//            }
//            recreate();
//        }


        if (TcpSocketClient == null) {
            DeviceConnect();
        } else if (!TcpSocketClient.isConnect()) {
            TcpSocketClient.connect();
        }

    }

    @Override
    public void onPause() {
        Log.d(Tag, "ButtonFragment onPause" + deviceNum);
        if (TcpSocketClient != null) {
            Log.d(Tag, "TcpSocketClient close " + deviceNum);
            TcpSocketClient.close();
        }
        super.onPause();
    }


    //region 按钮事件
    private View.OnClickListener buttonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            if (TcpSocketClient == null || !TcpSocketClient.isConnect()) {
                DeviceConnect();
                log.setText(log.getText() + "\n未连接设备,正在连接");
                Toast.makeText(getContext(), "设备未连接,正在连接……", Toast.LENGTH_SHORT).show();
            } else {
                switch (arg0.getId()) {
                    case R.id.iv_main_button1:
                        TcpSocketClient.Send("+");
                        break;
                    case R.id.iv_main_button2:
                        TcpSocketClient.Send("-");
                        break;
                    case R.id.btn_1:
                        TcpSocketClient.Send(setting_pwm_min + String.format("%03d", seekBar.getProgress()));
                        break;
                    case R.id.btn_2:
                        TcpSocketClient.Send(setting_pwm_middle + String.format("%03d", seekBar.getProgress()));
                        break;
                    case R.id.btn_3:
                        TcpSocketClient.Send(setting_pwm_max + String.format("%03d", seekBar.getProgress()));
                        break;
                }
            }
        }

    };
//endregion

    //region UDP广播获取设备信息
    String[] UDPgetIP = new String[2];
    int UDPgetIP_flag = 0;   //获取到IP地址,连续2次获取到相同的IP地址才确认
    int UDPnum = 0;//记录广播次数
    @SuppressLint("HandlerLeak")    //Warning:忽略此警告可能会导致严重的内存泄露
    //TODO 处理handler可能会导致的内存泄露问题
    private Handler UDPhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
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
                        UdpSocketClient.UDPsend(UDPdeviceReport);
                        UDPhandler.sendEmptyMessageDelayed(2, 400);    //200ms后再次发送 共5次
                    }
                    break;
                case UDPSocketClient.HANDLE_UDP_TYPE:
                    int[] Redat = new int[msg.arg1];
                    for (int i = 0; i < msg.arg1; i++) Redat[i] = ((byte[]) msg.obj)[i] & 0xff;
//                    Log.d(Tag, "getData:" + Arrays.toString(Redat));
                    String ReStr = new String((byte[])msg.obj,0,msg.arg1);
                    if (ReStr.startsWith(UDPdeviceReport_ok) && ReStr.length() <= UDPdeviceReport_ok.length() + 33) {
                        UDPhandler.removeMessages(2);
                        if (UDPgetIP_flag > 1) break;
                        UDPgetIP[UDPgetIP_flag] = ReStr.substring(UDPdeviceReport_ok.length(), ReStr.length());
//                       Log.i(Tag, "UDP get ip " + UDPgetIP_flag + ":" + UDPgetIP[UDPgetIP_flag]);
                        UDPgetIP_flag++;

                        if (UDPgetIP_flag > 1) {
                            if (UDPgetIP[0].equals(UDPgetIP[1])) {
                                Log.d(Tag, "UDP get/set IP:" + UDPgetIP[0]);
                                mEditor.putString("LAN_IP", UDPgetIP[0].substring(18, UDPgetIP[0].length()));
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
                    }
                    break;
                case UDPSocketClient.HANDLE_UDP_ERROR:
                    break;
            }
        }
    };

    private void UDPgetIP() {
        UDPgetIP_flag = 0;
        UDPnum = 0;
        UdpSocketClient = new UDPSocketClient(getContext(),UDPhandler, "255.255.255.255", 10191);
        UDPgetIP_flag = 0;
        //UdpSocketClient.UDPsend(UDPdeviceReport);
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

}

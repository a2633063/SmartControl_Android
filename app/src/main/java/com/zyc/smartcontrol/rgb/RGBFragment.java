package com.zyc.smartcontrol.rgb;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.zyc.MyFunction.DoubletoInt;


@SuppressLint("ValidFragment")
public class RGBFragment extends Fragment {
    final String Tag = "RGBFragment_Tag";

    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;

    //region 控件初始化
    private ArrayList<SeekBar> seekBar = new ArrayList<SeekBar>();
    private ArrayList<TextView> textView = new ArrayList<TextView>();
    ImageView mImageView;
    Bitmap mBitmap;
    TextView log;
    //endregion

    int deviceNum;
    Boolean Sflag = false;

    //UDP
    UDPSocketClient UdpSocketClient;
    //TCP
    String IP;
    TCPSocketClient TcpSocketClient;

    //颜色处理
    MyFunction.rgb RGB = new MyFunction.rgb();
    MyFunction.hsl HSL = new MyFunction.hsl();

    public RGBFragment(int x) {
        deviceNum = x;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //region tcp接收数据
                case TCPSocketClient.HANDLE_TCP_TYPE:
                    //Toast.makeText(MainActivity.this, (String)message.obj, Toast.LENGTH_SHORT).show();
                    Toast.makeText(getContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
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
                        TcpSocketClient = new TCPSocketClient(handler, IP, 10181);
                        TcpSocketClient.setAckchar(0x0a);
                        TcpSocketClient.connect();
                    }
                    break;
                //endregion

                case 10://seekBar tcp发送
                    if (Sflag) {
                        sendRGB();
                        Sflag = false;
                    }
                    break;
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rgb, container, false);

        mSharedPreferences = getActivity().getSharedPreferences("Setting" + deviceNum, 0);
        Log.d(Tag,"设置文件:"+"Setting" + deviceNum);
        mEditor = mSharedPreferences.edit();

        //region 控件初始化
        seekBar.add((SeekBar) view.findViewById(R.id.seekBarR));
        seekBar.add((SeekBar) view.findViewById(R.id.seekBarG));
        seekBar.add((SeekBar) view.findViewById(R.id.seekBarB));
        seekBar.get(0).setOnSeekBarChangeListener(seekListener);
        seekBar.get(1).setOnSeekBarChangeListener(seekListener);
        seekBar.get(2).setOnSeekBarChangeListener(seekListener);

        textView.add((TextView) view.findViewById(R.id.textViewR));
        textView.add((TextView) view.findViewById(R.id.textViewG));
        textView.add((TextView) view.findViewById(R.id.textViewB));

        mImageView = ((ImageView) view.findViewById(R.id.hsl));
        mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();

        mImageView.setOnTouchListener(InamgeViewListener);

        view.findViewById(R.id.up).setOnClickListener(buttonListener);
        view.findViewById(R.id.down).setOnClickListener(buttonListener);
        view.findViewById(R.id.left).setOnClickListener(buttonListener);
        view.findViewById(R.id.right).setOnClickListener(buttonListener);
        view.findViewById(R.id.ok).setOnClickListener(buttonListener);
        view.findViewById(R.id.open).setOnClickListener(buttonListener);
        view.findViewById(R.id.close).setOnClickListener(buttonListener);
        view.findViewById(R.id.WOL).setOnClickListener(buttonListener);
        view.findViewById(R.id.button1).setOnClickListener(buttonListener);
        view.findViewById(R.id.button2).setOnClickListener(buttonListener);
        log = (TextView) view.findViewById(R.id.tv_log);
        final ScrollView scrollView = (ScrollView)view. findViewById(R.id.scrollView);
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

        //endregion


        //启动定时器
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
        super.onPause();
        if (TcpSocketClient != null){
            Log.d(Tag, "TcpSocketClient close " + deviceNum);
            TcpSocketClient.close();}
    }


    //滚动条监视事件
    private SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {


            textView.get(0).setText("R:" + String.format("%03d", seekBar.get(0).getProgress()));
            textView.get(1).setText("G:" + String.format("%03d", seekBar.get(1).getProgress()));
            textView.get(2).setText("B:" + String.format("%03d", seekBar.get(2).getProgress()));
            switch (arg0.getId()) {
                case R.id.seekBarR:
                    RGB.r = arg1;
                    break;
                case R.id.seekBarG:
                    RGB.g = arg1;
                    break;
                case R.id.seekBarB:
                    RGB.b = arg1;
                    break;
            }

            Sflag = true;//sendRGB();


			/*
            rgb xx=HSLtoRGB(r,g,b);
	        mButton = (Button) findViewById(R.id.button1);
	        mButton.setText(Color.red(xx.r)+","+Color.green(xx.g)+","+Color.blue(xx.b));*/
        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            HSL = MyFunction.RGBtoHSL(seekBar.get(0).getProgress(),
                    seekBar.get(1).getProgress(),
                    seekBar.get(2).getProgress());
            Sflag = true;//sendRGB();

        }

    };

    //按钮事件
    private View.OnClickListener buttonListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {

            switch (arg0.getId()) {
                case R.id.down:
                    //Toast.makeText(MainActivity.this, "RGB：按下按键down",Toast.LENGTH_SHORT).show();
                    //message("按下按键down");
                    HSL.l -= 10;
                    if (HSL.l < 0) HSL.l = 0;
                    RGB = MyFunction.HSLtoRGB(HSL.h, HSL.s, HSL.l);
                    seekBar.get(0).setProgress(DoubletoInt(RGB.r));
                    seekBar.get(1).setProgress(DoubletoInt(RGB.g));
                    seekBar.get(2).setProgress(DoubletoInt(RGB.b));
                    Sflag = true;//sendRGB();
                    break;
                case R.id.up:
                    //Toast.makeText(MainActivity.this, "RGB：按下按键up",Toast.LENGTH_SHORT).show();
                    //message("按下按键up");
                    HSL.l += 10;
                    if (HSL.l > 100) HSL.l = 100;
                    RGB = MyFunction.HSLtoRGB(HSL.h, HSL.s, HSL.l + 10);
                    seekBar.get(0).setProgress(DoubletoInt(RGB.r));
                    seekBar.get(1).setProgress(DoubletoInt(RGB.g));
                    seekBar.get(2).setProgress(DoubletoInt(RGB.b));
                    Sflag = true;//sendRGB();
                    break;
                case R.id.left:
                    //Toast.makeText(MainActivity.this, "RGB：按下按键left",Toast.LENGTH_SHORT).show();
                    //message("按下按键left");

                    HSL.h -= 10;
                    if (HSL.h < 0) HSL.h = 350;
                    RGB = MyFunction.HSLtoRGB(HSL.h, HSL.s, HSL.l);
                    seekBar.get(0).setProgress(DoubletoInt(RGB.r));
                    seekBar.get(1).setProgress(DoubletoInt(RGB.g));
                    seekBar.get(2).setProgress(DoubletoInt(RGB.b));
                    Sflag = true;//sendRGB();
                    break;
                case R.id.right:
                    //Toast.makeText(MainActivity.this, "RGB：按下按键right",Toast.LENGTH_SHORT).show();
                    //message("按下按键right");
                    HSL.h += 10;
                    if (HSL.h > 360) HSL.h = 10;
                    RGB = MyFunction.HSLtoRGB(HSL.h, HSL.s, HSL.l);
                    seekBar.get(0).setProgress(DoubletoInt(RGB.r));
                    seekBar.get(1).setProgress(DoubletoInt(RGB.g));
                    seekBar.get(2).setProgress(DoubletoInt(RGB.b));

                    Sflag = true;//sendRGB();
                    break;
                case R.id.ok:
                    //Toast.makeText(MainActivity.this, "RGB：按下按键ok",Toast.LENGTH_SHORT).show();
                    //message("按下按键ok");
                    String str, strTemp;

                    str = "A8 8A 08 03 ";
                    if (seekBar.get(0).getProgress() == 0 && seekBar.get(1).getProgress() == 0 && seekBar.get(2).getProgress() == 0) {
                        seekBar.get(0).setProgress(255);
                        seekBar.get(1).setProgress(255);
                        seekBar.get(2).setProgress(255);
                    }

                    strTemp = Integer.toHexString(seekBar.get(0).getProgress());
                    if (strTemp.length() < 2) strTemp = "0" + strTemp;
                    str += strTemp.toUpperCase(Locale.getDefault()) + " ";
                    strTemp = Integer.toHexString(seekBar.get(1).getProgress());
                    if (strTemp.length() < 2) strTemp = "0" + strTemp;
                    str += strTemp.toUpperCase(Locale.getDefault()) + " ";
                    strTemp = Integer.toHexString(seekBar.get(2).getProgress());
                    if (strTemp.length() < 2) strTemp = "0" + strTemp;
                    str += strTemp.toUpperCase(Locale.getDefault()) + " FF\n";
                    sendRGB(str);
                    break;
                case R.id.button1:
                    sendRGB("A8 8A 05 04 FF");
                    break;
                case R.id.button2:
                    sendRGB("A8 8A 05 05 FF");
                    break;
                case R.id.open:
//                    ((TextView) findViewById(R.id.textViewR)).setText("R:255");
//                    ((TextView) findViewById(R.id.textViewG)).setText("R:255");
//                    ((TextView) findViewById(R.id.textViewB)).setText("R:255");
                    seekBar.get(0).setProgress(255);
                    seekBar.get(1).setProgress(255);
                    seekBar.get(2).setProgress(255);
                    sendRGB("A8 8A 08 10 FF FF FF FF");
                    break;
                case R.id.close:
//                    ((TextView) findViewById(R.id.textViewR)).setText("R:000");
//                    ((TextView) findViewById(R.id.textViewG)).setText("R:000");
//                    ((TextView) findViewById(R.id.textViewB)).setText("R:000");
                    seekBar.get(0).setProgress(0);
                    seekBar.get(1).setProgress(0);
                    seekBar.get(2).setProgress(0);
                    sendRGB("A8 8A 05 00 FF");
                    break;
                case R.id.WOL:
                    sendRGB("A8 8A 05 FE FF");
                    break;

            }
        }

    };

    //ImageView触摸事件
    private View.OnTouchListener InamgeViewListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View arg0, MotionEvent arg1) {

            mImageView.getParent().requestDisallowInterceptTouchEvent(true);

            int x = (int) arg1.getX();
            int y = (int) arg1.getY();


            try {
                int pixel = mBitmap.getPixel(x, y);//获取颜色
                int redValue = Color.red(pixel);
                int greenValue = Color.green(pixel);
                int blueValue = Color.blue(pixel);

                if (redValue != 255 && greenValue != 255 && blueValue != 255) return true;
                if (pixel == 0 || (redValue == 0 && greenValue == 0 && blueValue == 0))
                    return true; //仅判断pixel会偶尔无法跳出
                seekBar.get(0).setProgress(redValue);
                seekBar.get(1).setProgress(greenValue);
                seekBar.get(2).setProgress(blueValue);

//                HSL = RGBtoHSL(redValue, greenValue, blueValue);
//                Sflag = true;//sendRGB();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;

        }

    };

    //region UDP广播获取设备信息
    String[] UDPgetIP = new String[2];
    int UDPgetIP_flag = 0;   //获取到IP地址,连续2次获取到相同的IP地址才确认
    int UDPnum = 0;//记录广播次数
    final static String UDPdeviceReport = "Device Report!!";
    final static String UDPdeviceReport_ok = "I'm button:";
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
                    String ReStr = (String) msg.obj;
                    if (ReStr.startsWith(UDPdeviceReport_ok) && ReStr.length() <= UDPdeviceReport_ok.length() + 33) {
                        UDPhandler.removeMessages(2);
                        if (UDPgetIP_flag > 1) break;
                        UDPgetIP[UDPgetIP_flag] = ReStr.substring(UDPdeviceReport_ok.length(), ReStr.length());
//                        Log.i(Tag, "UDP get ip " + UDPgetIP_flag + ":" + UDPgetIP[UDPgetIP_flag]);
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
        UdpSocketClient = new UDPSocketClient(UDPhandler, "255.255.255.255", 10191);
        UDPgetIP_flag = 0;
        UdpSocketClient.UDPsend(UDPdeviceReport);
        UDPnum = 1;
        UDPhandler.sendEmptyMessageDelayed(2, 400);    //200ms后再次发送 共5次
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

    private void sendRGB() {


        //准备发送数据
        String str, strTemp;

        str = "A8 8A 08 10 ";
        strTemp = Integer.toHexString(seekBar.get(0).getProgress());
        if (strTemp.length() < 2) strTemp = "0" + strTemp;
        str += strTemp.toUpperCase(Locale.getDefault()) + " ";
        strTemp = Integer.toHexString(seekBar.get(1).getProgress());
        if (strTemp.length() < 2) strTemp = "0" + strTemp;
        str += strTemp.toUpperCase(Locale.getDefault()) + " ";
        strTemp = Integer.toHexString(seekBar.get(2).getProgress());
        if (strTemp.length() < 2) strTemp = "0" + strTemp;
        str += strTemp.toUpperCase(Locale.getDefault()) + " FF\n";

        Log.d(Tag, "str:" + str);
        str = "A8 8A 08 10 " + String.format("%02x ", seekBar.get(0).getProgress()).toUpperCase()
                + String.format("%02x ", seekBar.get(1).getProgress()).toUpperCase()
                + String.format("%02x ", seekBar.get(2).getProgress()).toUpperCase() + "FF";
        Log.d(Tag, "str:" + str);
        sendRGB(str);

    }

    private void sendRGB(String str) {
        if (TcpSocketClient == null || !TcpSocketClient.isConnect()) {
//            DeviceConnect();
//            log.setText(log.getText() + "\n未连接设备,正在连接");
            Toast.makeText(getActivity(), "设备未连接,正在连接……", Toast.LENGTH_SHORT).show();
        } else TcpSocketClient.Send(str + "\n");
    }

}

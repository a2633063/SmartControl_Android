package com.zyc.smartcontrol.socket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class UDPSocketClient {
    private static final String Tag = "UDPSocketClient";

    public static final int HANDLE_UDP_TYPE = 1674;
    public static final int HANDLE_UDP_ERROR = 1675;
    private String host;
    private int port;
    private String sendStr = null;
    private Handler handler;
    private Context mContext;

    DatagramSocket mSocket;
    ReceiveThread mReceiveThread;
    SendThread mSendThread;
    List<byte[]> messageQueue = new ArrayList<byte[]>();

    public UDPSocketClient(Context _context, Handler _handler, String _host, int _port) {
        handler = _handler;
        host = _host;
        port = _port;
        this.mContext = _context;
//        WifiManager manager=(WifiManager)mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiManager.MulticastLock lock=manager.createMulticastLock("testwifi");
//        lock.acquire();

        if (mSocket == null || mSocket.isClosed()) {
            try {
                //获取连接 ip：192.168.1.3  port：11069
                mSocket = new DatagramSocket();
                messageQueue = new ArrayList<byte[]>();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        //开启接收线程
        mHandler.sendEmptyMessage(2);

    }


    @SuppressLint("HandlerLeak")    //Warning:忽略此警告可能会导致严重的内存泄露
    //TODO 处理handler可能会导致的内存泄露问题
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 2) {
                try {
                    mReceiveThread.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mReceiveThread = null;
                mReceiveThread = new ReceiveThread();
                mReceiveThread.start();
            }
        }
    };

    public class SendThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                if (mSocket == null || mSocket.isClosed())
                    return;
                if (messageQueue.size() < 1)
                    return;
                //发送
//                final String data = messageQueue.get(0);
                byte[] datas = messageQueue.get(0);
                final DatagramPacket packet = new DatagramPacket(datas, datas.length, InetAddress.getByName(host), port);
                mSocket.send(packet);
                Log.d("ConnectManager", "send success data is:" + datas.toString());
                messageQueue.remove(0);

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();
            if (mSocket == null || mSocket.isClosed())
                return;
            try {
                byte datas[] = new byte[512];
                DatagramPacket packet = new DatagramPacket(datas, datas.length);
                //mSocket.setSoTimeout(2000);
                mSocket.receive(packet);

                String receiveMsg = new String(packet.getData()).trim();
                Log.d("ConnectManager", "receive msg data is:" + receiveMsg);
                Message msg=new Message();
                msg.what = HANDLE_UDP_TYPE;
                msg.obj = packet.getData();
                msg.arg1=packet.getLength();
                handler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mHandler.sendEmptyMessage(2);
            }
        }
    }

    public void Disconnect(){
        mSocket=null;
    }
    public void UDPsend(final char[] message) {
        UDPsend(new String(message).getBytes());
    }
    public void UDPsend(final String message) {
        UDPsend(message.getBytes());
    }
    public void UDPsend(final byte[] message) {
        int[] Redat = new int[message.length];
        for (int i = 0; i < message.length; i++) Redat[i] = (message)[i] & 0xff;
        Log.d(Tag,"send:"+Arrays.toString(Redat));

        messageQueue.add(message);
        try {
            mSendThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSendThread = null;
        mSendThread = new SendThread();
        mSendThread.start();

    }
}

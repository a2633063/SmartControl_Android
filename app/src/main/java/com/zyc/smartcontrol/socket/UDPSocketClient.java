package com.zyc.smartcontrol.socket;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

//import java.io.BufferedReader;
//import java.io.InputStreamReader;


public class UDPSocketClient {
    private static final String Tag = "UDPSocketClient";

    public static final int HANDLE_UDP_TYPE = 1674;
    public static final int HANDLE_UDP_ERROR = 1675;
    private String host;
    private int port;
    private String sendStr = null;
    private Handler handler;

    public UDPSocketClient(Handler _handler, String _host, int _port) {
        handler = _handler;
        host = _host;
        port = _port;
    }

    public void UDPsend(final String message) {
        if (message == null || message.length() < 1) return;
        new Thread() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                InetAddress serverIP = null;
                try {
                    serverIP = InetAddress.getByName(host);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                DatagramPacket p = new DatagramPacket(message.getBytes(), message.length(), serverIP, port);
                try {
                    socket.send(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] buf = new byte[50];
                // 创建一个接收数据的DatagramPacket对象
                DatagramPacket packet = new DatagramPacket(buf, 50);
                // 接收数据报
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                String udpreceive;
                try {
                    udpreceive = new String(buf, 0, packet.getLength(), "UTF-8");
                    if (udpreceive != null || udpreceive.length() > 0) {
                        Message msg = new Message();
                        msg.what = HANDLE_UDP_TYPE;
                        msg.obj = udpreceive;
                        handler.sendMessageDelayed(msg, 0);
                        Log.d(Tag, "UDP接收:" + udpreceive);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}

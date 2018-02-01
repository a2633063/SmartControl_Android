package com.zyc.smartcontrol.socket;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

//import java.io.BufferedReader;
//import java.io.InputStreamReader;


public class TCPSocketClient {
    private static final String Tag = "TCPSocketClient";
    public static final int HANDLE_TCP_TYPE = 1672;
    public static final int HANDLE_TCP_ERROR = 1673;
    private String host;
    private int port;
    private String sendStr = null;
    private Handler handler;

    private static Socket sk = null;
    private BufferedReader in = null;
    private BufferedWriter out = null;

    private boolean readIsRunning = false;
    private boolean sendIsReady = false;
    private boolean TCPIsConnect = false;

    private int ack_char=127;

    public TCPSocketClient() {
        this(new Handler(), "localhost", 8080);
    }

    public TCPSocketClient(Handler _handler, String _host, int _port) {
        handler = _handler;
        host = _host;
        port = _port;
    }

    Handler handlerSocket = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            switch (msg.what) {
                case 1://已连接
                    TCPIsConnect = true;
                    Message m = new Message();
                    m.what = HANDLE_TCP_ERROR;
                    m.arg1 = 0;
                    handler.sendMessageDelayed(m, 0);
                    handlerSocket.sendEmptyMessageDelayed(3, 5000);
                    break;
                case 2://已断开连接
                    TCPIsConnect = false;
                    Message m2 = new Message();
                    m2.what = HANDLE_TCP_ERROR;
                    m2.arg1 = -1;
                    handler.sendMessageDelayed(m2, 0);
                    break;
                case 3:
                    handlerSocket.removeMessages(3);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (out != null) {
                                    if (sendStr == null) out.write(Character.toString((char) ack_char));
                                    else out.write(sendStr);
                                    out.flush();
                                    sendStr = null;
                                    Log.d(Tag, "当前为连接状态");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(Tag, e.getMessage());
                                if (!e.getMessage().equals("Read timed out")) {
                                    close();
                                    handlerSocket.sendEmptyMessageDelayed(2, 0);
                                }
                            }

                            handlerSocket.sendEmptyMessageDelayed(3, 5000);
                        }
                    }).start();

                    break;
//                case 99:
//                    try {
//                        sk.sendUrgentData(0xFF);
//                        handlerSocket.sendEmptyMessageDelayed(99, 5000);
//                        Log.d(Tag, "当前为连接状态");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        readIsRunning = false;
//                        sendIsReady = false;
//                        TCPIsConnect = false;
//
//                        Message m1 = new Message();
//                        m1.what = HANDLE_TCP_ERROR;
//                        m1.arg1 = -2;
//                        handler.sendMessageDelayed(m1, 0);
//                    }
//                    break;
            }
        }
    };

    private Thread socketConnect = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                System.out.println("连接:" + host + ":" + port);

                if (sk == null)//sk为空或未连接时才重新连接
                {
                    sk = new Socket(host, port);
                    System.out.println("空，重新连接");
                } else {
                    if (sk.isConnected() == false) {
                        sk = new Socket(host, port);
                        System.out.println("未连接，重新连接");
                    } else {
                        if (!sk.getInetAddress().getHostAddress().equals(host) || sk.getPort() != port) {
                            System.out.println("IP或端口变化,断开后重新连接");
                            closeTCP();
                            sk = new Socket(host, port);
                        }
                    }
                }
                sk.setKeepAlive(true);
                sk.setSoTimeout(1000);
                in = new BufferedReader(new InputStreamReader(sk.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(sk.getOutputStream()));
                readIsRunning = true;
                handlerSocket.sendEmptyMessageDelayed(1, 0);
                System.out.println("已连接");
                //region 接收信息
                while (readIsRunning) {

                    if (sk != null && !sk.isInputShutdown()) {
                        try {
                            char[] buffer = new char[99];
                            int count;
                            if ((count = in.read(buffer)) > 0) {
                                Message msg = new Message();
                                msg.what = HANDLE_TCP_TYPE;
                                msg.obj = String.valueOf(buffer, 0, count);
                                handler.sendMessageDelayed(msg, 0);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
//                            Log.e(Tag, e.getMessage());
//                            if(e!=null && !e.getMessage().equals("Read timed out")){
//                                readIsRunning=false;
//                            }
                        }
                    }
                }
                //endregion


            } catch (IOException e) {
                e.printStackTrace();
                readIsRunning = false;
                sendIsReady = false;
                TCPIsConnect = false;
            }

            closeTCP();
            sendStr = null;

            System.out.println("已断开");
            handlerSocket.sendEmptyMessageDelayed(2, 0);
        }
    });

    private void closeTCP() {
        try {
            readIsRunning = false;
            out = null;
            in = null;
            System.out.println("断开");

            if (sk != null) sk.close();
            sk = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnect() {
        return TCPIsConnect;
    }

    public void connect() {
        if (!readIsRunning && !sendIsReady) {
            socketConnect.start();
        }
    }

    public void close() {
        readIsRunning = false;
    }

    public void setAckchar(int x){
        ack_char=x;
    }
    public void Send(final String str) {
        handlerSocket.removeMessages(3);
        sendStr = str;
        handlerSocket.sendEmptyMessageDelayed(3, 0);
    }


}

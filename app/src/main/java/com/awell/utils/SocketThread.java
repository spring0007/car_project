package com.awell.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class SocketThread {
    private String TAG = "LauncherMainLog";

    private volatile boolean running = true;
    private String ip;
    private Integer port;

    private Long socketId;

    private StringBuffer strMsg = new StringBuffer();

    public static final int MESSAGE_ERROR = 0;
    public static final int MESSAGE_SUCCEED = 1;
    public static final int MESSAGE_RECEIVE_TIME = 2;
    public static final int MESSAGE_RECEIVE_WORE = 3;
    public static final int MESSAGE_RECEIVE_SONGER = 4;
    public static final int MESSAGE_RECEIVE_TIME_TOTAL = 5;
    public static final int MESSAGE_RECEIVE_TIME_STATUS = 6;

    private Handler handler;
    private Socket socket;
    private OutputStream outStream;
    private InputStream inStream;
    private String status = "";
    private int mTotalTime = 0;
    private String phoneMode = "";
    private boolean isPlaying = false;
    private boolean isAutoPalyInfo = false;
    private boolean isWholePackage = false;
    private Thread inOUtThread = null;
    private boolean disConnect;

    public SocketThread(Integer port, Handler handler, String phoneMode) {
        this.port = port;
        this.phoneMode = phoneMode;
        this.handler = handler;
        new Socket_thread().start();
    }


    /**
     * 连接服务器
     */
    class Socket_thread extends Thread {
        private int PORT = 0;//端口号

        @Override
        public void run() {
            try {
                disSocket();
                //连接服务器，此处会一直处于阻塞，直到连接成功
                socket = new Socket(InetAddress.getLocalHost(), 1555);
                //阻塞停止，表示连接成功
                Log.e(TAG, "zlink连接成功 port" + port + " phoneMode: " + phoneMode);

                setMessage("连接成功", MESSAGE_SUCCEED);
            } catch (Exception e) {
                Log.e(TAG, "zlink连接服务器时异常 ");
                setMessage("连接服务器时异常", MESSAGE_ERROR);
                e.printStackTrace();
                return;
            }
            try {
                //获取到输入输出流
                outStream = socket.getOutputStream();
                inStream = socket.getInputStream();
            } catch (Exception e) {
                Log.e(TAG, "zlink获取输入输出流异常");
                setMessage("获取输入输出流异常", MESSAGE_ERROR);
                e.printStackTrace();
                return;
            }
//            Log.e(TAG,  "MESSAGE_RECEIVE Socket_thread" + Thread.currentThread().getName());
            try {
                inOUtThread = new Inx();
                inOUtThread.start();
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }

    /**
     * 循环接收数据
     */
    class Inx extends Thread {
        @Override
        public void run() {
            while (!disConnect) {
                byte[] bu = new byte[1024];
                try {
//                    Log.e(TAG,  "MESSAGE_RECEIVE Inx" + Thread.currentThread().getName());

                    int count = inStream.read(bu);//设备重启，异常 将会一直停留在这
//                    Log.e(TAG,  "MESSAGE_RECEIVE count: "+ count);

                    if (count == -1) {
                        setMessage("服务器断开", MESSAGE_ERROR);
                        disSocket();
                        return;
                    }
//                    Log.e(TAG,  "MESSAGE_RECEIVE bu: "+ Arrays.toString(bu));
//                    String strdread = new String(bu).trim();
//                    Log.e(TAG,  "MESSAGE_RECEIVE strread: "+ strdread);
                    if (phoneMode.contains("carplay")) {
                        if (bu[0] == 10) {
                            if (bu[11] == 0) {
                                int curLengt = 12;
                                while (true) {
                                    short packgeLen = bytesToShort2(bu, curLengt);
                                    if (packgeLen == 0 || bu.length - 1 < curLengt + packgeLen) {
                                        break;
                                    }
                                    int index = bytesToShort2(bu, curLengt + 2);
//                                Log.e(TAG,  "MESSAGE_RECEIVE curLengt: "+ packgeLen + " index: "+ index);
                                    switch (index) {
                                        case 1:                     //MediaItemTitle
                                            byte[] b2 = new byte[packgeLen - 4];
                                            System.arraycopy(bu, curLengt + 4, b2, 0, packgeLen - 4);
                                            String strread = new String(b2).trim();
                                            setMessage(strread, MESSAGE_RECEIVE_WORE);
//                                        Log.e(TAG,  "MESSAGE_RECEIVE index strread 1: "+ strread );

                                            break;
                                        case 4:                      //MediaItemPlaybackDurationInMilliseconds
                                            byte[] b3 = new byte[packgeLen - 4];
                                            System.arraycopy(bu, curLengt + 4, b3, 0, packgeLen - 4);
                                            int time = bytesToInt2(b3, 0);
                                            setMessage(time, MESSAGE_RECEIVE_TIME_TOTAL);

//                                        Log.e(TAG,  "MESSAGE_RECEIVE index strread 4: "+ time );
                                            break;
                                        case 12:                      //MediaItemArtist
                                            byte[] b4 = new byte[packgeLen - 4];
                                            System.arraycopy(bu, curLengt + 4, b4, 0, packgeLen - 4);
                                            String artist = new String(b4).trim();
                                            setMessage(artist, MESSAGE_RECEIVE_SONGER);
//                                            Log.e(TAG, "MESSAGE_RECEIVE index strread 12: " + artist);
                                            break;
                                        default:
                                            break;
                                    }
                                    curLengt = curLengt + packgeLen;
                                    if (bu.length - 1 < curLengt + 4) {
                                        break;
                                    }
                                }

                            }

                            if (bu[11] == 1) {
                                int curLengt = 12;
                                while (true) {
                                    short packgeLen = bytesToShort2(bu, curLengt);
                                    if (packgeLen == 0 || bu.length - 1 < curLengt + packgeLen) {
                                        break;
                                    }
                                    int index = bytesToShort2(bu, curLengt + 2);
//                                    Log.e(TAG, "MESSAGE_RECEIVE curLengt time: " + packgeLen + " index: " + index);
                                    switch (index) {
                                        case 1:                     //MediaItemTitle
                                            byte[] b2 = new byte[packgeLen - 4];
                                            System.arraycopy(bu, curLengt + 4, b2, 0, packgeLen - 4);
                                            int time = bytesToInt2(b2, 0);
//                                            Log.e(TAG, "MESSAGE_RECEIVE time:" + time);
                                            setMessage(time, MESSAGE_RECEIVE_TIME);

                                            break;
                                        case 12:                      //MediaItemPlaybackDurationInMilliseconds
                                            byte[] b3 = new byte[packgeLen - 4];
                                            System.arraycopy(bu, curLengt + 4, b3, 0, packgeLen - 4);
                                            int status = bytesToShort2(b3, 0);
//                                            Log.e(TAG, "MESSAGE_RECEIVE status:" + status);

                                            setMessage(status, MESSAGE_RECEIVE_TIME_STATUS);

//                                        Log.e(TAG,  "MESSAGE_RECEIVE index strread 4: "+ time );
                                            break;
                                        default:
                                            break;

                                    }
                                    curLengt = curLengt + packgeLen;
                                    if (bu.length - 1 < curLengt + 4) {
                                        break;
                                    }
                                }

                            }
                        }
                    }
                    if (phoneMode.contains("auto")) {
                        if (count == 1024) {
                            isWholePackage = true;
                        } else if (count != 1024 && isWholePackage && bu[count - 5] == 48) {
                            isWholePackage = false;
                            int totalTime = 0;
                            int value1 = bu[count - 4];
                            int value2 = bu[count - 3];
//                            Log.e(TAG,  "MESSAGE_RECEIVE value1: "+ value1 +" value2: " +value2);

                            if (value2 == 32) {
                                totalTime = value1;

                            } else if (32 > value2 && value2 > 0) {
                                totalTime = (128 + value1) + value2 * 128;
                            }
//                            Log.e(TAG,  "MESSAGE_RECEIVE totalTime: "+ totalTime);
                            setMessage(totalTime * 1000, MESSAGE_RECEIVE_TIME_TOTAL);

                        }

                        if (bu[0] == 5 && bu[1] == 0) {
                            isAutoPalyInfo = true;
                            byte[] b2 = new byte[2];
                            System.arraycopy(bu, 5, b2, 0, 2);
                            int time = bytesToShort2(b2, 0);
//                            Log.e(TAG,  "MESSAGE_RECEIVE AA time:"+ time);
                            if (bu[8] == 10) {
                                byte[] b7 = new byte[bu.length - 8];
                                System.arraycopy(bu, 8, b7, 0, bu.length - 8);
                                bu = b7;
//                                Log.e(TAG,  "MESSAGE_RECEIVE bu7: "+ Arrays.toString(bu));
                            }
                        }
                        if (isAutoPalyInfo && bu[0] == 10) {
                            isAutoPalyInfo = false;
                            int curLengt = 0;
                            boolean flag = true;
                            while (flag) {
                                int packgeLen = bu[curLengt + 1];
                                if (packgeLen <= 0 || bu.length < packgeLen + curLengt) {
                                    break;
                                }
                                int index = bu[curLengt];
//                                Log.e(TAG,  "MESSAGE_RECEIVE curLengt: "+ packgeLen + " index: "+ index);
                                switch (index) {
                                    case 10:                     //MediaItemTitle
                                        byte[] b2 = new byte[packgeLen];
                                        System.arraycopy(bu, curLengt + 2, b2, 0, packgeLen);
                                        String strread = new String(b2).trim();
                                        setMessage(strread, MESSAGE_RECEIVE_WORE);
//                                        Log.e(TAG,  "MESSAGE_RECEIVE AA index 10: "+ strread );

                                        break;
                                    case 18:                      //artist
                                        byte[] b3 = new byte[packgeLen];
                                        System.arraycopy(bu, curLengt + 2, b3, 0, packgeLen);
                                        String artist = new String(b3).trim();
                                        setMessage(artist, MESSAGE_RECEIVE_SONGER);
//                                        Log.e(TAG,  "MESSAGE_RECEIVE AA index 18: "+ artist );
                                        break;
                                    case 26:                      //album:
                                        byte[] b4 = new byte[packgeLen];
                                        System.arraycopy(bu, curLengt + 2, b4, 0, packgeLen);
                                        String album = new String(b4).trim();
//                                    setMessage(artist, MESSAGE_RECEIVE_SONGER);
//                                        Log.e(TAG,  "MESSAGE_RECEIVE index strread 26: "+ album );
                                        break;
                                    case 34:
                                        byte[] b5 = new byte[4];
                                        System.arraycopy(bu, curLengt + 1, b5, 0, 4);
                                        Log.e(TAG, "MESSAGE_RECEIVE index strread 34 b50: " + b5[0]);
                                        Log.e(TAG, "MESSAGE_RECEIVE index strread 34 b51: " + b5[1]);
                                        short time1 = bytesToShort2(b5, 0);
                                        int time = bytesToInt2(b5, 0);
                                        flag = false;
//                                    setMessage(artist, MESSAGE_RECEIVE_SONGER);
//                                        Log.e(TAG,  "MESSAGE_RECEIVE index strread 34: "+ time );
//                                        Log.e(TAG,  "MESSAGE_RECEIVE index strread 34 t: "+ time1 );
                                        break;
                                    default:
                                        break;
                                }
                                curLengt = curLengt + packgeLen + 2;
                            }
                        }
                        if (!isWholePackage && bu[0] == 4) {
                            if (bu[9] == 2) {
                                if (!isPlaying) {
                                    setMessage(2, MESSAGE_RECEIVE_TIME_STATUS);
                                    isPlaying = true;
                                }
                            } else if (bu[9] == 3) {
                                isPlaying = false;
                                setMessage(0, MESSAGE_RECEIVE_TIME_STATUS);

                            }
                            if (bu[25] == 24) {
                                int curTime = 0;
                                int value1 = bu[26];
                                int value2 = bu[27];
//                                Log.e(TAG,  "MESSAGE_RECEIVE curTime value1: "+ value1 +" value2: " +value2);
                                if (value2 == 32) {
                                    curTime = value1;

                                } else if (32 > value2 && value2 > 0) {
                                    curTime = (128 + value1) + value2 * 128;
                                }
                                setMessage(curTime * 1000, MESSAGE_RECEIVE_TIME);

//                                Log.e(TAG,  "MESSAGE_RECEIVE curTime: "+ curTime );

                            }
                        }
                        if (!isWholePackage && bu[0] == 8 && bu[17] == 24) {
                            int curTime = 0;
                            int value1 = bu[18];
                            int value2 = bu[19];
//                            Log.e(TAG,  "MESSAGE_RECEIVE curTime8 value1: "+ value1 +" value2: " +value2);
                            if (value2 == 32) {
                                curTime = value1;

                            } else if (32 > value2 && value2 > 0) {
                                curTime = (128 + value1) + value2 * 128;
                            }
                            setMessage(curTime * 1000, MESSAGE_RECEIVE_TIME);

//                            Log.e(TAG,  "MESSAGE_RECEIVE curTime8: "+ curTime );

                        }

                    }

                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
    }

    public static short bytesToShort2(byte[] src, int offset) {
        short value;
        value = (short) (((src[offset] & 0xFF) << 8)
                | (src[offset + 1] & 0xFF));
        return value;
    }

    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }

    /**
     * 断开连接
     */
    public void disSocket() {
        if (socket != null) {
            try {
                disConnect = true;
                outStream.close();
                inStream.close();
                socket.close();
                socket = null;
                Log.e(TAG, "断开连接zlink");

                setMessage("断开连接时发生错误", MESSAGE_ERROR);

            } catch (Exception e) {
                Log.e(TAG, "zlink断开连接时发生错误");

                setMessage("断开连接时发生错误", MESSAGE_ERROR);
            }
        }
    }

    private void setMessage(String obj, int arg1) {
        Message message = new Message();
        message.arg1 = arg1;
        message.obj = obj;
        handler.sendMessage(message);
    }

    private void setMessage(int obj, int arg1) {
        Message message = new Message();
        message.arg1 = arg1;
        message.obj = obj;
        handler.sendMessage(message);
    }
}

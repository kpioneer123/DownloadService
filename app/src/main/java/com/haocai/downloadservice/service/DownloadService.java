package com.haocai.downloadservice.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.haocai.downloadservice.bean.FileInfo;
import com.haocai.downloadservice.utils.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author xionhgu
 * @version [版本号，2017/1/22]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */

public class DownloadService extends Service {
    public static final String FILE_INFO        = "fileInfo";
    public static final String DOWNLOAD_PATH    = Environment.getExternalStorageDirectory()+ "/downloads/";
    //开始下载命令
    public static final String ACTION_START     = "ACTION_START";
    //暂停下载命令
    public static final String ACTION_PAUSE     = "ACTION_PAUSE";
    //结束下载命令
    public static final String ACTION_FINISHED  = "ACTION_FINISHED";
    //跟新UI命令
    public static final String ACTION_UPDATE    = "ACTION_UPDATE";
    //初始化标识
    public static final int    MSG_INIT         = 0x1;
    //绑定的标识
    public static final int    MSG_BIND         = 0x2;

    public static final int    MSG_START        = 0x3;

    public static final int    MSG_FINISHED     = 0x4;

    public static final int    MSG_PAUSE        = 0x5;

    public static final int    MSG_UPDATE       = 0x6;

    private InitThread mInitThread = null;
    //下载任务的集合
    private Map<Integer,DownloadTask> mTasks = new LinkedHashMap<>();
    private  Messenger mActivityMessenger = null; //来自Activity的Messenger
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        //获得Activity传来的参数
//        if (ACTION_START.equals(intent.getAction())) {
//            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra(FILE_INFO);
//            DownloadTask task  = mTasks.get(fileInfo.getId());
//            if(task != null) {
//                task.isPause = false;
//            }
//            //启动初始化线程
//            mInitThread = new InitThread(fileInfo);
//
//          //  mInitThread.start();
//            DownloadTask.mExecutorService.execute(mInitThread); //线程池中去启动线程
//
//
//        } else if (ACTION_PAUSE.equals(intent.getAction())) {
//            //暂停下载
//            FileInfo fileInfo  = (FileInfo) intent.getSerializableExtra(FILE_INFO);
//            //从集合中取出下载任务
//            DownloadTask task  = mTasks.get(fileInfo.getId());
//            if(task != null)
//            {
//             //停止下载任务
//                task.isPause = true;
//            }
//        }
//        return super.onStartCommand(intent, flags, startId);
//    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //创建一个Messenger对象包含Handler的引用
        Messenger messenger = new Messenger(mHandler);
        //返回Messenger的Binder
        return messenger.getBinder();
    }


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg){
            FileInfo fileInfo=null;
            DownloadTask task=null;
           switch (msg.what){
               case MSG_INIT:
                    fileInfo = (FileInfo)msg.obj;
                   Log.i("test","Init:" + fileInfo );
                   //启动下载任务
                    task = new DownloadTask(DownloadService.this,mActivityMessenger, fileInfo,3);//下载线程数为3
                   task.download();
                   //把下载任务添加到集合中
                   mTasks.put(fileInfo.getId(),task);

                   //发送启动命令的广播
//                   Intent intent = new Intent(DownloadService.ACTION_START);
//                   intent.putExtra(DownloadService.FILE_INFO,fileInfo);
//                   sendBroadcast(intent);
                   Message msg2 = new Message();
                   msg2.what = MSG_START;
                   msg2.obj  = fileInfo;
                   try {
                       mActivityMessenger.send(msg2);
                   } catch (RemoteException e) {
                       e.printStackTrace();
                   }

                   break;
               case MSG_BIND:
                   //处理绑定的Messenger
                   mActivityMessenger = msg.replyTo;
                   break;
               case MSG_START:
                   fileInfo = (FileInfo) msg.obj;

            //启动初始化线程
            mInitThread = new InitThread(fileInfo);

          //  mInitThread.start();
            DownloadTask.mExecutorService.execute(mInitThread); //线程池中去启动线程
                   break;
               case MSG_PAUSE:
                   //            //暂停下载
             fileInfo  = (FileInfo) msg.obj;
            //从集合中取出下载任务
             task  = mTasks.get(fileInfo.getId());
            if(task != null)
            {
             //停止下载任务
                task.isPause = true;
            }
                   break;
           }

    }
};

    /**
     * 初始化子线程
     */
    class InitThread extends Thread {

        private FileInfo mFileInfo = null;

        public  InitThread(FileInfo mFileInfo)
        {
            this.mFileInfo = mFileInfo;
        }

        public void run(){
            HttpURLConnection conn = null;
            RandomAccessFile  raf  = null;
            try{
                //连接网络文件
                URL url = new URL(mFileInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");
                int length = -1;
                if(conn.getResponseCode() == HttpStatus.SC_OK){

                    //获得文件的长度
                    length = conn.getContentLength();
                }
                if(length <= 0){return;}

                //获得文件的长度
                File dir = new File(DOWNLOAD_PATH);
                if(!dir.exists()){
                    if (!dir.mkdir()){
                        return;
                    }
                }
                //在本地创建文件
                File file = new File(dir,mFileInfo.getFileName());
                /**
                 *RandomAccessFile 随机读取类
                 */
                raf = new RandomAccessFile(file,"rwd");

                //设置文件长度
                raf.setLength(length);
                mFileInfo.setLength(length);
                Log.e("tFileInfo.getLength==", mFileInfo.getLength() + "");
                mHandler.obtainMessage(MSG_INIT,mFileInfo).sendToTarget();

            }catch(Exception e){
                e.printStackTrace();
            }finally {
                try {
                    if (conn != null && raf != null) {
                        raf.close();
                        conn.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

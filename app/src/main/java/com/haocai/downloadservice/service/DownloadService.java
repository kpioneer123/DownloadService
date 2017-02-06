package com.haocai.downloadservice.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.haocai.downloadservice.bean.FileInfo;
import com.haocai.downloadservice.utils.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author xionhgu
 * @version [版本号，2017/1/22]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */

public class DownloadService extends Service {

    public static final String DOWNLOAD_PATH  = Environment.getExternalStorageDirectory()+ "/downloads/";
    public static final String ACTION_START   = "ACTION_START";
    public static final String ACTION_STOP    = "ACTION_STOP";
    public static final String ACTION_UPDATE  = "ACTION_UPDATE";
    public static final String FILE_INFO      = "fileInfo";
    public static final int    MSG_INIT       = 0;
    private DownloadTask mTask = null;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获得Activity传来的参数
        if (ACTION_START.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra(FILE_INFO);
            Log.i("test", "Start:" + fileInfo.toString());
            //启动初始化线程
            new InitThread(fileInfo).start();
        } else if (ACTION_STOP.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra(FILE_INFO);
            Log.i("test", "Stop:" + fileInfo.toString());
            if(mTask !=null){
                mTask.isPause = true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    Handler mHandler = new Handler() {
    public void handleMessage(android.os.Message msg){
           switch (msg.what){
               case MSG_INIT:
                   FileInfo fileInfo = (FileInfo)msg.obj;
                   Log.i("test","Init:" + fileInfo );
                   //启动下载任务
                   mTask = new DownloadTask(DownloadService.this, fileInfo);
                   mTask.download();

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
                    dir.mkdir();
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

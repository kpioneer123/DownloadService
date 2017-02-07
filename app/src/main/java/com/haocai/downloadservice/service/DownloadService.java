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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author xionhgu
 * @version [版本号，2017/1/22]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */

public class DownloadService extends Service {
    public static final String FILE_INFO      = "fileInfo";
    public static final String DOWNLOAD_PATH  = Environment.getExternalStorageDirectory()+ "/downloads/";
    //开始下载命令
    public static final String ACTION_START   = "ACTION_START";
    //暂停下载命令
    public static final String ACTION_PAUSE    = "ACTION_PAUSE";
    //结束下载命令
    public static final String ACTION_FINISHED  = "ACTION_FINISHED";
    //跟新UI命令
    public static final String ACTION_UPDATE  = "ACTION_UPDATE";
    //初始化标识
    public static final int    MSG_INIT       = 0x1;

    private InitThread mInitThread = null;
    //下载任务的集合
    private Map<Integer,DownloadTask> mTasks = new LinkedHashMap<>();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获得Activity传来的参数
        if (ACTION_START.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra(FILE_INFO);
            DownloadTask task  = mTasks.get(fileInfo.getId());
            if(task != null) {
                task.isPause = false;
            }
            //启动初始化线程
            mInitThread = new InitThread(fileInfo);

          //  mInitThread.start();
            DownloadTask.mExecutorService.execute(mInitThread); //线程池中去启动线程
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            //暂停下载
            FileInfo fileInfo  = (FileInfo) intent.getSerializableExtra(FILE_INFO);
            //从集合中取出下载任务
            DownloadTask task  = mTasks.get(fileInfo.getId());
            if(task != null)
            {
             //停止下载任务
                task.isPause = true;
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
        @Override
        public void handleMessage(android.os.Message msg){
           switch (msg.what){
               case MSG_INIT:
                   FileInfo fileInfo = (FileInfo)msg.obj;
                   Log.i("test","Init:" + fileInfo );
                   //启动下载任务
                   DownloadTask task = new DownloadTask(DownloadService.this, fileInfo,3);//下载线程数为3
                   task.download();
                   //把下载任务添加到集合中
                   mTasks.put(fileInfo.getId(),task);
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

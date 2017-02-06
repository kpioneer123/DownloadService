package com.haocai.downloadservice.service;

import android.content.Context;
import android.content.Intent;

import com.haocai.downloadservice.bean.FileInfo;
import com.haocai.downloadservice.bean.ThreadInfo;
import com.haocai.downloadservice.db.ThreadDAOImpl;
import com.haocai.downloadservice.utils.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * @author Xionghu
 *         created at 2017/1/23 11:15
 */

public class DownloadTask {
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private ThreadDAOImpl mDao = null;
    private int mFinished = 0;
    public  boolean  isPause = false;
    public DownloadTask(Context mContext, FileInfo mFileInfo) {
        this.mContext = mContext;
        this.mFileInfo = mFileInfo;
        mDao = new ThreadDAOImpl(mContext);
    }

    public void download(){
        //读取数据库的线程信息
        List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
        ThreadInfo threadInfo = null;
        if(threadInfos.size() == 0){
            //初始化线程信息对象
            threadInfo = new ThreadInfo(0,mFileInfo.getUrl(),0,mFileInfo.getLength(),0);
        }else {
            threadInfo = threadInfos.get(0);
        }
        //创建子线程进行下载
        new DownloadThread(threadInfo).start();
    }
    /**
     *  下载进程
     */
    class DownloadThread extends Thread{
        private ThreadInfo threadInfo =null;

        public DownloadThread(ThreadInfo threadInfo)
        {
            this.threadInfo = threadInfo;
        }

        public void run(){
            //向数据库中插入线程信息
            if(!mDao.isExists(threadInfo.getUrl(),threadInfo.getId())){
                mDao.insertThread(threadInfo);
            }
            //设置下载位置
            HttpURLConnection conn = null;
            RandomAccessFile raf   = null;
            InputStream input      = null;
            try {
                URL url = new URL(threadInfo.getUrl());
                conn = (HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");
                //设置下载位置
                long start = threadInfo.getStart() + threadInfo.getFinished();
                conn.setRequestProperty("Range","bytes=" + start + "-"+threadInfo.getEnd());
                //设置文件写入位置
                File file = new File(DownloadService.DOWNLOAD_PATH,mFileInfo.getFileName());
                raf = new RandomAccessFile(file,"rwd");
                raf.seek(start);
                Intent intent = new Intent(DownloadService.ACTION_UPDATE);
                mFinished += threadInfo.getFinished();
                //开始下载(conn.getResponseCode()  HttpStatus.SC_PARTIAL_CONTENT 部分内容下载 而不是200 ok)
                if(conn.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT){
                    //读取数据
                    input = conn.getInputStream();
                    byte[] buffer = new byte[1024 * 4 ];
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while((len = input.read(buffer)) != -1){
                        //写入文件
                        raf.write(buffer,0,len);
                        //把下载进度发送广播给Activity
                        mFinished += len;
                        if(System.currentTimeMillis() - time > 500)
                        {
                            time = System.currentTimeMillis();
                            intent.putExtra("finished",mFinished * 100/mFileInfo.getLength());
                            mContext.sendBroadcast(intent);
                        }
                        //在下载暂停时，保存下载进度
                        if(isPause){
                            mDao.updateThread(threadInfo.getUrl(),threadInfo.getId(),mFinished);
                            return;
                        }
                    }
                    //删除线程信息
                    mDao.deleteThread(threadInfo.getUrl(),threadInfo.getId());
                }


            }  catch (Exception e) {
                e.printStackTrace();
            }finally {
                conn.disconnect();
                try {
                    input.close(); //输入流
                    raf.close();   //输出流
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            //设置文件写入位置
            //开始下载

        }
    }
}

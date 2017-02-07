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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Xionghu
 *         created at 2017/1/23 11:15
 */

public class DownloadTask {
    public static final String FINISHED_KEY   = "finished";
    public static final String ID_KEY         = "id";
    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private ThreadDAOImpl mDao = null;
    public  boolean  isPause = false;
    private int mFinished = 0;
    private int mThreadCount = 1; //线程数量
    private List<DownloadThread> mThreadList =null;//线程集合
    public  static ExecutorService mExecutorService = Executors.newCachedThreadPool(); //线程池

    public DownloadTask(Context mContext, FileInfo mFileInfo ,int mThreadCount) {
        this.mContext     = mContext;
        this.mFileInfo    = mFileInfo;
        this.mThreadCount = mThreadCount;
        mDao = new ThreadDAOImpl(mContext);
    }

    public void download(){
        //读取数据库的线程信息
        List<ThreadInfo> threadInfos = mDao.getThreads(mFileInfo.getUrl());
        if(threadInfos.size() == 0)
        {
            //获得每个线程下载的长度
            int length = mFileInfo.getLength() / mThreadCount;
            for(int i = 0;i < mThreadCount;i++)
            {
                ThreadInfo threadInfo = new ThreadInfo(i,mFileInfo.getUrl(), length * i,( i + 1 ) * length - 1,0 );
                if(i == mThreadCount - 1){
                    threadInfo.setEnd(mFileInfo.getLength()); // 对于线程除不尽的情况，100/3 = 33   第3个线程末尾为98 显然不对 ，对于最后一个线程结束位直接设为100
                }
                //添加到线程信息集合中去
                threadInfos.add(threadInfo);

                //向数据库中插入线程信息
                mDao.insertThread(threadInfo);
            }


        }
        mThreadList = new ArrayList<DownloadThread>();

        //启动多个线程进行下载
        for(ThreadInfo info : threadInfos)
        {
            DownloadThread thread = new DownloadThread(info);
           // thread.start();
            DownloadTask.mExecutorService.execute(thread); //线程池中去启动线程
            //添加线程到集合中去
            mThreadList.add(thread); // 方便对线程进行管理
        }
    }


    /**
     *  数据下载进程
     */
    class DownloadThread extends Thread{
        private ThreadInfo threadInfo =null;
        public  boolean isFinished = false; //线程是否执行完毕

        public DownloadThread(ThreadInfo threadInfo)
        {
            this.threadInfo = threadInfo;
        }

        public void run(){

            //设置下载位置
            HttpURLConnection conn = null;
            RandomAccessFile raf   = null;
            InputStream is      = null;
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
                    is = conn.getInputStream();
                    byte[] buffer = new byte[1024 * 4 ];
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while((len = is.read(buffer)) != -1){
                        //在下载暂停时，保存下载进度
                        if(isPause){
                            mDao.updateThread(threadInfo.getUrl(),threadInfo.getId(),threadInfo.getFinished());
                            return;
                        }
                        //写入文件
                        raf.write(buffer,0,len);
                        //把下载进度发送广播给Activity
                        mFinished += len;
                        //累加每个线程完成的进度
                        threadInfo.setFinished( mFileInfo.getFinished() + len);
                        //每隔1秒刷新UI
                        if(System.currentTimeMillis() - time > 1000)//减少UI负载
                        {
                            time = System.currentTimeMillis();
                            intent.putExtra(FINISHED_KEY,mFinished * 100/mFileInfo.getLength());
                            intent.putExtra(ID_KEY,mFileInfo.getId());
                            mContext.sendBroadcast(intent);
                        }

                    }
                    //标识线程执行完毕
                    isFinished = true;
                    //检查下载任务是否执行完毕
                    checkAllThreadsFinished();
                    is.close(); //输入流
                }

            }  catch (Exception e) {
                e.printStackTrace();
            }finally {
                conn.disconnect();
                try {
                    raf.close();   //输出流
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
    /**
     * 判断是否所有线程都执行完毕
     */
    private synchronized void checkAllThreadsFinished(){
        boolean allFinished = true;
        //遍历线程集合,判断线程是否都执行完毕
        for(DownloadThread thread : mThreadList){
            if(!thread.isFinished){
                allFinished = false;
                break;
            }
        }
        if(allFinished){
            // 所有线程执行完毕了，就把线程信息删除掉 通过url，跟这个下载文件相关的线程全部删除掉
            mDao.deleteThread(mFileInfo.getUrl());

            //发送广播通知UI下载任务结束
            Intent intent = new Intent(DownloadService.ACTION_FINISHED);
            intent.putExtra(DownloadService.FILE_INFO,mFileInfo);
            mContext.sendBroadcast(intent);

        }
    }
}

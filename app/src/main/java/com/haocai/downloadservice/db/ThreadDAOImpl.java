package com.haocai.downloadservice.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.haocai.downloadservice.bean.ThreadInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据访问接口的实现
 * @author Xionghu
 * created at 2017/1/23 10:03
 */

public class ThreadDAOImpl implements ThreadDAO {

    private DatabaseHelper mHelper = null;

    public  ThreadDAOImpl(Context context){
        mHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * 同一个时间只能保证一个线程对数据库进行修改，这时要使用synchronized
     * insert delete update 多线程都需要添加synchronized 而查询只是对数据库读取，而没有操作，不用加synchronized
     */
    @Override
    public synchronized  void insertThread(ThreadInfo threadInfo) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.execSQL("insert into thread_info(thread_id , url ,start ,end ,finished) values(?,?,?,?,?)",
        new Object[]{threadInfo.getId(),threadInfo.getUrl(),threadInfo.getStart(),threadInfo.getEnd(),threadInfo.getFinished()});
        db.close();
    }

    @Override
    public synchronized void deleteThread(String url) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.execSQL("delete from thread_info where url = ? ",
           new Object[]{url });
        db.close();
    }

    @Override
    public synchronized void updateThread(String url, int thread_id, long finished) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.execSQL("update thread_info set finished = ? where url = ? and thread_id = ?",
                new Object[]{finished,url ,thread_id});
        db.close();
    }

    @Override
    public List<ThreadInfo> getThreads(String url) {
        SQLiteDatabase db = mHelper.getReadableDatabase();//得到一个只读的数据库
        List<ThreadInfo> list = new ArrayList<ThreadInfo>();
        Cursor cursor = db.rawQuery("select * from thread_info where url = ?",new String[]{url});
        while(cursor.moveToNext())
        {
            ThreadInfo threadInfo = new ThreadInfo();
            threadInfo.setId(cursor.getInt(cursor.getColumnIndex("thread_id")));
            threadInfo.setUrl(cursor.getString(cursor.getColumnIndex("url")));
            threadInfo.setStart(cursor.getLong(cursor.getColumnIndex("start")));
            threadInfo.setEnd(cursor.getLong(cursor.getColumnIndex("end")));
            threadInfo.setFinished(cursor.getLong(cursor.getColumnIndex("finished")));
            list.add(threadInfo);
        }
        cursor.close();
        db.close();
        return list;
    }

    @Override
    public boolean isExists(String url, int thread_id) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from thread_info where url = ? and thread_id =?",new String[]{url,thread_id + ""});

        boolean exists = cursor.moveToNext();
        cursor.close();
        db.close();
        return exists;
    }
}

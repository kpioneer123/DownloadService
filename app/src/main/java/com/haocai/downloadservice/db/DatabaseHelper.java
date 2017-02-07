package com.haocai.downloadservice.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author xionhgu
 * @version [版本号，2017/1/22]
 * @see [数据库帮助类]
 * @since [产品/模块版本]
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "download.db";
    private static DatabaseHelper sHelper = null; //静态对象应用
    private static final int    VERSION = 1;
    private static final String CREATE_SQL = "create table thread_info(" +
            "_id integer primary key autoincrement," +
            "thread_id integer," +
            "url text," +
            "start long," +
            "end long," +
            "finished long)";
    private static final String SQL_DROP = "drop table if exists thread_info";

    /**
     * DBHelper私有防止多次创建
     * @param context
     */
    private DatabaseHelper(Context context) {
        super(context,DB_NAME,null,VERSION );
    }

    /**
     * 获得类的对象
     *类中的方法只能在本类中应用
     * sHelper只会被实例化一次
     * 无论什么情况下返回的sHelper只是一个
     */
    public static DatabaseHelper getInstance(Context context)
    {
        if(sHelper == null){
            sHelper = new DatabaseHelper(context);
        }
        return sHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP);
        db.execSQL(CREATE_SQL);
    }
}

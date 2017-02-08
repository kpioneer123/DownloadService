package com.haocai.downloadservice.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import com.haocai.downloadservice.R;
import com.haocai.downloadservice.activity.MainActivity;
import com.haocai.downloadservice.bean.FileInfo;
import com.haocai.downloadservice.service.DownloadService;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知工具类
 * @author Xionghu
 * created at 2017/2/7 16:59
 */

public class NotificationUtil {

    private NotificationManager mNotificationManager = null;
    private Map<Integer,Notification> mNotifications = null;
    private Context context = null;
    public  NotificationUtil(Context context){
        //获得通知系统服务
        mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        //创建通知的
        mNotifications = new HashMap<>();
        this.context = context;
    }

    /**
     * 显示通知
     * @param fileInfo
     */
    public void showNotification(FileInfo fileInfo){
        //判断通知是否已经显示
       if(mNotifications.containsKey(fileInfo.getId())){
           //创建通知对象
           Notification notification = new Notification();
           //设置滚动文字
           notification.tickerText = fileInfo.getFileName() +  "开始下载";
           //设置显示时间
           notification.when  = System.currentTimeMillis();
           //设置图标
           notification.icon  = R.mipmap.ic_launcher;
           //设置通知的特性
           notification.flags = Notification.FLAG_AUTO_CANCEL;//点击通知栏后自动消失
           //设置点击通知栏的操作
//           Notification myNotify = new Notification.Builder(context)
//                   .setSmallIcon(R.mipmap.ic_launcher) //设置状态栏中的小图片，尺寸一般建议在24×24，这个图片同样也是在下拉状态栏中所显示，如果在那里需要更换更大的图片，可以使用setLargeIcon(Bitmap icon)
//                   .setTicker(fileInfo.getFileName() +  "开始下载")//设置在status bar上显示的提示文字
//                   .setWhen(System.currentTimeMillis())
//                   .setContentIntent(pendingIntent)
//                   .build(); //需要注意build()是在API level 16增加的，可以使用 getNotificatin()来替代


           Intent intent = new Intent(context, MainActivity.class);
           PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);
           notification.contentIntent = pendingIntent;

           //创建Remoteviews对象
           RemoteViews remoteViews  = new RemoteViews(context.getPackageName(),R.layout.notification);
           //设置开始按钮操作
           Intent intentStart = new Intent(context, DownloadService.class);
           intentStart.setAction(DownloadService.ACTION_START);
           intentStart.putExtra(DownloadService.FILE_INFO,fileInfo);
           PendingIntent piStart = PendingIntent.getService(context,0,intentStart,0);
           remoteViews.setOnClickPendingIntent(R.id.start,piStart);

           //设置开始按钮操作
           Intent intentStop = new Intent(context, DownloadService.class);
           intentStart.setAction(DownloadService.ACTION_PAUSE);
           intentStart.putExtra(DownloadService.FILE_INFO,fileInfo);
           PendingIntent piStop = PendingIntent.getService(context,0,intentStop,0);
           remoteViews.setOnClickPendingIntent(R.id.pause,piStop);

           //设置TextView
           remoteViews.setTextViewText(R.id.tv_filename,fileInfo.getFileName());
           //设置Notification的视图
           notification.contentView =remoteViews;

           //发出通知
           mNotificationManager.notify(fileInfo.getId(),notification);

           //把通知加到加到集合中
           mNotifications.put(fileInfo.getId(),notification);
           Log.i("99999","77777");

       }
    }

    /**
     * 取消通知
     * @param id
     */
    public void cancelNotification(int id){
        //取消通知
        mNotificationManager.cancel(id);
        mNotifications.remove(id);
    }


    /**
     * 更新进度条
     * @param id
     * @param progress
     */
    public void updateNotification(int id,int progress){
        Notification notification = mNotifications.get(id);
        if(notification !=null){
            //修改进度条
            notification.contentView.setProgressBar(R.id.progressBar,100,progress,false); //false 就是滚动条的当前值自动在最小到最大值之间来回移动,true 明确一个值
            mNotificationManager.notify(id,notification);
        }
    }
}

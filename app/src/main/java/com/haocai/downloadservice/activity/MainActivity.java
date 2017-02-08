package com.haocai.downloadservice.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.ListView;
import android.widget.Toast;

import com.haocai.downloadservice.R;
import com.haocai.downloadservice.adapter.FileListAdapter;
import com.haocai.downloadservice.bean.FileInfo;
import com.haocai.downloadservice.service.DownloadService;
import com.haocai.downloadservice.service.DownloadTask;
import com.haocai.downloadservice.utils.NotificationUtil;
import com.yanzhenjie.permission.AndPermission;
import java.util.ArrayList;
import java.util.List;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_PERMISSION_SD = 100;
    @InjectView(R.id.lvFile)
    ListView lvFile;

    private List<FileInfo>  mFileList = null;
    private FileListAdapter mAdapter  = null;
    private NotificationUtil mNotificationUtil = null;
    private Messenger mServiceMessenger = null; //Service中的Messenger
    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        initData();
        initSetup();
        //initRegister();

        // 申请单个权限。
        AndPermission.with(this)
                .requestCode(REQUEST_CODE_PERMISSION_SD)
                .permission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                // rationale作用是：用户拒绝一次权限，再次申请时先征求用户同意，再打开授权对话框，避免用户勾选不再提示。
                .rationale((requestCode, rationale) ->
                        // 这里的对话框可以自定义，只要调用rationale.resume()就可以继续申请。
                        AndPermission.rationaleDialog(MainActivity.this, rationale).show()
                )
                .send();


        //在Activity
        mNotificationUtil = new NotificationUtil(this);

        //绑定Service
        Intent intent = new Intent(this,DownloadService.class);

        bindService(intent,mConnection, Service.BIND_AUTO_CREATE);
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DownloadService.MSG_UPDATE:

                    //更新进度条
                    int id = msg.arg1;
                    int finished = msg.arg2;
                    mAdapter.updateProgress(id,finished);
                    //更新通知里的进度
                    mNotificationUtil.updateNotification(id,finished);

                    break;
                case DownloadService.MSG_FINISHED:

                    //更新进度为0
                    FileInfo fileinfo = (FileInfo) msg.obj;
                    mAdapter.updateProgress(fileinfo.getId(),0);
                    Toast.makeText(MainActivity.this,fileinfo.getFileName()+"下载完毕",Toast.LENGTH_SHORT).show();
                    //取消通知
                    mNotificationUtil.cancelNotification(fileinfo.getId());

                    break;
                case DownloadService.MSG_START:

                    mNotificationUtil.showNotification((FileInfo) msg.obj);
                    break;
            }
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        //获得Service中的Messenger
        mServiceMessenger = new Messenger(service);
        //设置适配器中的Messenger
        mAdapter.setMessenger(mServiceMessenger);
        //创建Activity中的Messenger
        Messenger messenger =new Messenger(mHandler);
        //创建消息
        Message msg  = new Message();
        msg.what  = DownloadService.MSG_BIND;
        msg.replyTo = messenger;
        //5.使用Service的Messenger发送信息给Service的Handler，信息包括Activity的Messenger
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
};
    private void initData() {
        //创建文件集合
        mFileList = new ArrayList<>();
        //创建文件信息对象
        FileInfo fileInfo  = new FileInfo(0, "http://211.161.126.174/imtt.dd.qq.com/16891/E695392B43690F52752AD0D675E73427.apk?" +
                "mkey=58847140eab5ec89&f=6920&c=0&fsname=com.tencent.mm_6.5.4_1000.apk&csr=4d5s&p=.apk", "WeChat", 0, 0);
        FileInfo fileInfo2 = new FileInfo(1, "http://211.161.126.174/imtt.dd.qq.com/16891/32CB7178A596A9BF8A102BFECDF3A521.apk?" +
                "mkey=589805a6eab5ec89&f=9432&c=0&fsname=com.tencent.mobileqq_6.6.9_482.apk&csr=4d5s&p=.apk", "QQ", 0, 0);
        FileInfo fileInfo3 = new FileInfo(2, "http://211.161.126.174/imtt.dd.qq.com/16891/2F74148DF548DC276BBB4B0843F77005.apk?" +
                "mkey=58980569eab5ec89&f=6720&c=0&fsname=com.taobao.taobao_6.4.2_149.apk&csr=4d5s&p=.apk", "taobao", 0, 0);
        FileInfo fileInfo4 = new FileInfo(3, "http://211.161.126.174/imtt.dd.qq.com/16891/36C5694F6FE468D788FFFC65166547BE.apk?" +
                "mkey=5898052beab5ec89&f=6c20&c=0&fsname=com.qiyi.video_8.1_80830.apk&csr=4d5s&p=.apk", "iqiyi", 0, 0);
        FileInfo fileInfo5 = new FileInfo(4, "http://211.161.126.174/imtt.dd.qq.com/16891/643B69A5EEBB6BF959010FAB1BF3CDEE.apk?" +
                "mkey=589802dbeab5ec89&f=6720&c=0&fsname=com.baidu.BaiduMap_9.7.1_788.apk&csr=4d5s&p=.apk", "baiduMap", 0, 0);

        mFileList.add(fileInfo);
        mFileList.add(fileInfo2);
        mFileList.add(fileInfo3);
        mFileList.add(fileInfo4);
        mFileList.add(fileInfo5);
    }
    private void initSetup() {
        mAdapter = new FileListAdapter(this,mFileList);
        lvFile.setAdapter(mAdapter);
    }
//    private void initRegister() {
//        //注册广播接收器
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(DownloadService.ACTION_UPDATE);
//        filter.addAction(DownloadService.ACTION_FINISHED);
//        filter.addAction(DownloadService.ACTION_START);
//        registerReceiver(mReceiver, filter);
//    }

//    /**
//     * 更新UI的广播接收器
//     */
//    BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            if (DownloadService.ACTION_UPDATE.equals(intent.getAction())) {
//                //更新进度条
//                int id = intent.getIntExtra(DownloadTask.ID_KEY, 0);
//                int finished = intent.getIntExtra(DownloadTask.FINISHED_KEY, 0);
//                mAdapter.updateProgress(id,finished);
//                //更新通知里的进度
//                mNotificationUtil.updateNotification(id,finished);
//            }else if(DownloadService.ACTION_FINISHED.equals(intent.getAction())){
//                //更新进度为0
//                FileInfo fileinfo = (FileInfo) intent.getSerializableExtra(DownloadService.FILE_INFO);
//                mAdapter.updateProgress(fileinfo.getId(),0);
//                Toast.makeText(MainActivity.this,fileinfo.getFileName()+"下载完毕",Toast.LENGTH_SHORT).show();
//                //取消通知
//                mNotificationUtil.cancelNotification(fileinfo.getId());
//            }else if(DownloadService.ACTION_START.equals(intent.getAction())){
//                Toast.makeText(MainActivity.this,"显示通知",Toast.LENGTH_SHORT).show();
//                //显示通知
//                mNotificationUtil.showNotification((FileInfo) intent.getSerializableExtra(DownloadService.FILE_INFO));
//            }
//        }
//    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
      //  unregisterReceiver(mReceiver);
    }
}

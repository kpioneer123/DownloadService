package com.haocai.downloadservice.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.haocai.downloadservice.R;
import com.haocai.downloadservice.bean.FileInfo;
import com.haocai.downloadservice.service.DownloadService;
import com.yanzhenjie.permission.AndPermission;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_PERMISSION_SD = 100;

    @InjectView(R.id.tv_filename)
    TextView tv_filename;
    @InjectView(R.id.progressBar)
    ProgressBar progressBar;
    @InjectView(R.id.pro_text)
    TextView pro_text;
    @InjectView(R.id.start)
    Button start;
    @InjectView(R.id.pause)
    Button pause;
    private  FileInfo fileInfo;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        progressBar.setMax(100);
        // 申请单个权限。
        AndPermission.with(this)
                .requestCode(REQUEST_CODE_PERMISSION_SD)
                .permission(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                // rationale作用是：用户拒绝一次权限，再次申请时先征求用户同意，再打开授权对话框，避免用户勾选不再提示。
                .rationale((requestCode, rationale) ->
                        // 这里的对话框可以自定义，只要调用rationale.resume()就可以继续申请。
                        AndPermission.rationaleDialog(MainActivity.this, rationale).show()
                )
                .send();

        //创建文件信息对象
           fileInfo = new FileInfo(  0, "http://211.161.126.174/imtt.dd.qq.com/16891/E695392B43690F52752AD0D675E73427.apk?" +
                "mkey=58847140eab5ec89&f=6920&c=0&fsname=com.tencent.mm_6.5.4_1000.apk&csr=4d5s&p=.apk","WeChat",0,0);



        //注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        registerReceiver(mReceiver,filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    /**
     * 更新UI的广播接收器
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(DownloadService.ACTION_UPDATE.equals(intent.getAction())){

                int finished = intent.getIntExtra("finished",0);
                progressBar.setProgress(finished);
            }
        }
    };

    @OnClick({R.id.start, R.id.pause})
    public void onClick(View view) {

        Intent intent ;
        switch (view.getId()) {

            case R.id.start:

                //通过Intent传递参数给
                intent =new Intent(MainActivity.this, DownloadService.class);
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra(DownloadService.FILE_INFO,fileInfo);
                startService(intent);

                break;
            case R.id.pause:

                intent =new Intent(MainActivity.this, DownloadService.class);
                intent.setAction(DownloadService.ACTION_STOP);
                intent.putExtra(DownloadService.FILE_INFO,fileInfo);
                startService(intent);

                break;
        }
    }
}

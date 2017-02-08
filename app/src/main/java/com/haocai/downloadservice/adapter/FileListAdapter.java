package com.haocai.downloadservice.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.ProgressBar;
import com.haocai.downloadservice.R;
import com.haocai.downloadservice.bean.FileInfo;
import com.haocai.downloadservice.service.DownloadService;

import java.util.List;


public class FileListAdapter extends CommonAdapter<FileInfo> {

    private Context context;
    private List<FileInfo> mFileList;
    private Messenger mMessenger = null;
    public FileListAdapter(Context context, List<FileInfo> mDatas) {
        super(context, R.layout.list_item, mDatas);
        this.context = context;
        this.mFileList = mDatas;
    }

   public void setMessenger(Messenger messenger)
   {
       this.mMessenger = messenger;
   }
    @Override
    public void convert(ViewHolder holder, FileInfo fileInfo) {

        holder.setText(R.id.tv_filename, fileInfo.getFileName());
//        ((ProgressBar)holder.getView(R.id.progressBar)).setMax(100);
//        ((ProgressBar)holder.getView(R.id.progressBar)).setProgress(fileInfo.getFinished());

        ProgressBar pb =holder.getView(R.id.progressBar);
        pb.setMax(100);
        pb.setProgress(fileInfo.getFinished());

        (holder.getView(R.id.start)).setOnClickListener(v -> {

//            Intent intent =new Intent(context, DownloadService.class);
//            intent.setAction(DownloadService.ACTION_START);
//            intent.putExtra(DownloadService.FILE_INFO,fileInfo);
//            context.startService(intent);
            Message msg = new Message();
            msg.what = DownloadService.MSG_START;
            msg.obj =  fileInfo;
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        });
        (holder.getView(R.id.pause)).setOnClickListener(v -> {
//            Intent intent =new Intent(context, DownloadService.class);
//            intent.setAction(DownloadService.ACTION_PAUSE);
//            intent.putExtra(DownloadService.FILE_INFO,fileInfo);
//            context.startService(intent);

            Message msg = new Message();
            msg.what = DownloadService.MSG_PAUSE;
            msg.obj =  fileInfo;
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 更新列表中的进度条
     */
    public void updateProgress(int id,int progress){
        FileInfo fileInfo =mFileList.get(id);
        fileInfo.setFinished(progress);
        notifyDataSetChanged();
    }
}

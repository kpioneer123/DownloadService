package com.haocai.downloadservice.db;

import com.haocai.downloadservice.bean.ThreadInfo;

import java.util.List;

/**
 * 数据访问接口
 * @author xionhgu
 * @version [版本号，2017/1/23]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */

public interface ThreadDAO {

    public void insertThread(ThreadInfo threadInfo);

    /**
     * 删除线程
     * @param url
     */
    public void deleteThread(String url);

    public void updateThread(String url,int thread_id ,long finished);

    /**
     * 查询文件的线程信息
     * @param url
     * @return
     */
    public List<ThreadInfo> getThreads(String url);

    /**
     * 线程信息是否存在
     * @param url
     * @param thread_id
     * @return
     */
    public boolean isExists(String url,int thread_id);

}

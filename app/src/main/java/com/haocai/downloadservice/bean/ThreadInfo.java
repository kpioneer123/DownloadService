package com.haocai.downloadservice.bean;

/**
 * @author xionhgu
 * @version [版本号，2017/1/22]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */

public class ThreadInfo {
    private int id;
    private String url;
    private int start;
    private long end;
    private long finished;

    public ThreadInfo(){
        super();
    }
    public ThreadInfo(int id, String url, int start, long end, long finish) {
        this.id = id;
        this.url = url;
        this.start = start;
        this.end = end;
        this.finished = finished;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getFinished() {
        return finished;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    @Override
    public String toString() {
        return "ThreadInfo{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", finished=" + finished +
                '}';
    }
}

package com.ts7ming.notify.model;

import org.apache.seatunnel.api.event.Event;

/**
 * 从原始 {@link Event} 中提取出的、与通知相关的上下文信息。
 */
public class EventContext {

    private EventCategory category;
    private String eventTypeName;
    private String jobId;
    private String jobName;
    private String jobStatus;
    private String tableName;
    private long time;
    private Event raw;

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public String getEventTypeName() {
        return eventTypeName;
    }

    public void setEventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Event getRaw() {
        return raw;
    }

    public void setRaw(Event raw) {
        this.raw = raw;
    }
}

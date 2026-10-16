package com.ts7ming.notify.model;

/**
 * 事件归类。SeaTunnel 事件种类繁多, 此处按业务语义归类为四大类, 便于动态订阅。
 */
public enum EventCategory {
    /** 任务状态事件 (JobStateEvent) */
    JOB,
    /** 表结构变更事件 (SchemaChangeEvent) */
    SCHEMA,
    /** 读写生命周期事件 (READER/WRITER OPEN/CLOSE) */
    LIFECYCLE,
    /** 其他未归类事件 */
    OTHER
}

package com.ts7ming.notify.model;

/**
 * 一条需要发送的通知消息。
 */
public class NotifyMessage {

    private final String title;
    private final String content;
    private final String level;
    private final long time;

    public NotifyMessage(String title, String content, String level, long time) {
        this.title = title;
        this.content = content;
        this.level = level;
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getLevel() {
        return level;
    }

    public long getTime() {
        return time;
    }
}

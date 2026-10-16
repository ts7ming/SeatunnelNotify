package com.ts7ming.notify.channel;

import com.ts7ming.notify.model.NotifyMessage;

/**
 * 通知渠道接口。新增渠道只需实现本接口并在 {@link com.ts7ming.notify.core.ChannelManager} 中注册。
 */
public interface NotifyChannel {

    /** 渠道类型标识, 用于日志 */
    String type();

    /** 发送一条通知 */
    void send(NotifyMessage msg);
}

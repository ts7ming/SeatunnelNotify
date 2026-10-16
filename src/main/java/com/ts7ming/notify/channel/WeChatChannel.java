package com.ts7ming.notify.channel;

import com.ts7ming.notify.config.NotifyConfig.ChannelConfig;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;
import com.ts7ming.notify.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 企业微信自定义机器人 (群机器人, 无需加签)。
 */
public class WeChatChannel implements NotifyChannel {

    private static final Logger log = LoggerFactory.getLogger(WeChatChannel.class);
    private final ChannelConfig cfg;

    public WeChatChannel(ChannelConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public String type() {
        return "wechat";
    }

    @Override
    public void send(NotifyMessage msg) {
        String webhook = cfg.getString("webhook", "");
        if (Common.isBlank(webhook)) {
            log.warn("企业微信渠道 webhook 未配置, 跳过发送");
            return;
        }
        String text = msg.getTitle() + "\n" + msg.getContent();
        String json = "{\"msgtype\":\"text\",\"text\":{\"content\":"
                + HttpUtil.escapeJson(text) + "}}";
        try {
            int code = HttpUtil.postJson(webhook, json, 5000);
            if (code == 200) {
                log.info("企业微信通知发送成功: {}", msg.getTitle());
            } else {
                log.error("企业微信通知发送失败, 响应码: {}, 标题: {}", code, msg.getTitle());
            }
        } catch (Exception e) {
            log.error("企业微信通知发送异常: {}", msg.getTitle(), e);
        }
    }
}

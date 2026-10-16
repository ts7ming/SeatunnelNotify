package com.ts7ming.notify.channel;

import com.ts7ming.notify.config.NotifyConfig.ChannelConfig;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;
import com.ts7ming.notify.util.HttpUtil;
import com.ts7ming.notify.util.SignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 飞书自定义机器人 (支持签名校验)。
 * 文本消息格式: {"msg_type":"text","content":{"text":"..."}}
 */
public class FeishuChannel implements NotifyChannel {

    private static final Logger log = LoggerFactory.getLogger(FeishuChannel.class);
    private final ChannelConfig cfg;

    public FeishuChannel(ChannelConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public String type() {
        return "feishu";
    }

    @Override
    public void send(NotifyMessage msg) {
        String webhook = cfg.getString("webhook", "");
        if (Common.isBlank(webhook)) {
            log.warn("飞书渠道 webhook 未配置, 跳过发送");
            return;
        }
        String secret = cfg.getString("secret", "");
        String url = webhook;
        if (!Common.isBlank(secret)) {
            try {
                long ts = System.currentTimeMillis();
                String sign = SignUtil.hmacSha256Base64(ts + "\n" + secret, secret);
                url += "?timestamp=" + ts + "&sign="
                        + URLEncoder.encode(sign, "UTF-8");
            } catch (Exception e) {
                log.error("飞书加签失败", e);
            }
        }
        String text = msg.getTitle() + "\n" + msg.getContent();
        String json = "{\"msg_type\":\"text\",\"content\":{\"text\":"
                + HttpUtil.escapeJson(text) + "}}";
        try {
            int code = HttpUtil.postJson(url, json, 5000);
            if (code == 200) {
                log.info("飞书通知发送成功: {}", msg.getTitle());
            } else {
                log.error("飞书通知发送失败, 响应码: {}, 标题: {}", code, msg.getTitle());
            }
        } catch (Exception e) {
            log.error("飞书通知发送异常: {}", msg.getTitle(), e);
        }
    }
}

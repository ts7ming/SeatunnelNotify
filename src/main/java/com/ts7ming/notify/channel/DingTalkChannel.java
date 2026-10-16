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
 * 钉钉自定义机器人 (支持加签)。
 */
public class DingTalkChannel implements NotifyChannel {

    private static final Logger log = LoggerFactory.getLogger(DingTalkChannel.class);
    private final ChannelConfig cfg;

    public DingTalkChannel(ChannelConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public String type() {
        return "dingtalk";
    }

    @Override
    public void send(NotifyMessage msg) {
        String webhook = cfg.getString("webhook", "");
        if (Common.isBlank(webhook)) {
            log.warn("钉钉渠道 webhook 未配置, 跳过发送");
            return;
        }
        String secret = cfg.getString("secret", "");
        String url = webhook;
        if (!Common.isBlank(secret)) {
            try {
                long ts = System.currentTimeMillis();
                String sign = SignUtil.hmacSha256Base64(ts + "\n" + secret, secret);
                url += (webhook.contains("?") ? "&" : "?")
                        + "timestamp=" + ts + "&sign="
                        + URLEncoder.encode(sign, "UTF-8");
            } catch (Exception e) {
                log.error("钉钉加签失败", e);
            }
        }
        String text = msg.getTitle() + "\n" + msg.getContent();
        String json = "{\"msgtype\":\"text\",\"text\":{\"content\":"
                + HttpUtil.escapeJson(text) + "}}";
        try {
            int code = HttpUtil.postJson(url, json, 5000);
            if (code == 200) {
                log.info("钉钉通知发送成功: {}", msg.getTitle());
            } else {
                log.error("钉钉通知发送失败, 响应码: {}, 标题: {}", code, msg.getTitle());
            }
        } catch (Exception e) {
            log.error("钉钉通知发送异常: {}", msg.getTitle(), e);
        }
    }
}

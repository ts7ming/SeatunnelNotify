package com.ts7ming.notify.channel;

import com.ts7ming.notify.config.NotifyConfig.ChannelConfig;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;
import com.ts7ming.notify.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        String fullUrl = webhook;
        if (!Common.isBlank(secret)) {
            try {
                long timestamp = System.currentTimeMillis();
                String sign = generateSign(timestamp, secret);
                fullUrl = webhook + (webhook.contains("?") ? "&" : "?")
                        + "timestamp=" + timestamp + "&sign=" + sign;
            } catch (Exception e) {
                log.error("钉钉加签失败", e);
            }
        }

        String text = msg.getTitle() + "\n" + msg.getContent();
        String jsonPayload = "{\"msgtype\":\"text\",\"text\":{\"content\":\""
                + HttpUtil.escapeJson(text) + "\"}}";
        try {
            HttpUtil.Response r = HttpUtil.postJsonResp(fullUrl, jsonPayload, 5000);
            if (HttpUtil.isWebhookSuccess(r.code, r.body)) {
                log.info("钉钉通知发送成功: {}", msg.getTitle());
            } else {
                log.error("钉钉通知发送失败, 响应码: {}, 响应体: {}", r.code, r.body);
            }
        } catch (Exception e) {
            log.error("钉钉通知发送异常: {}", msg.getTitle(), e);
        }
    }

    /**
     * 加签: 对 {@code timestamp + "\n" + secret} 做 HmacSHA256, Base64 后再 URL 编码。
     */
    private String generateSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(new String(Base64.getEncoder().encode(signData)), "UTF-8");
    }
}

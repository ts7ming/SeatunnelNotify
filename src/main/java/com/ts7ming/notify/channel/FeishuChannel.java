package com.ts7ming.notify.channel;

import com.ts7ming.notify.config.NotifyConfig.ChannelConfig;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;
import com.ts7ming.notify.util.HttpUtil;
import com.ts7ming.notify.util.SignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 飞书自定义机器人 (支持签名校验)。
 * 参考: https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot
 *
 * <p>与钉钉的差异:
 * <ul>
 *   <li>时间戳为<b>秒级</b>;</li>
 *   <li>加签算法为 HmacSHA256(key = "timestamp\nsecret", msg = 空) 后 Base64;</li>
 *   <li>timestamp / sign 放在<b>请求体</b>中, 而非 URL 查询参数。</li>
 * </ul>
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

        String text = msg.getTitle() + "\n" + msg.getContent();
        String content = HttpUtil.escapeJson(text);
        String secret = cfg.getString("secret", "");

        String json;
        if (Common.isBlank(secret)) {
            // 未启用签名校验
            json = "{\"msg_type\":\"text\",\"content\":{\"text\":\"" + content + "\"}}";
        } else {
            try {
                long ts = System.currentTimeMillis() / 1000L;   // 飞书要求秒级时间戳
                String sign = SignUtil.feishuSign(ts, secret);
                json = "{\"timestamp\":\"" + ts + "\",\"sign\":\""
                        + HttpUtil.escapeJson(sign)
                        + "\",\"msg_type\":\"text\",\"content\":{\"text\":\""
                        + content + "\"}}";
            } catch (Exception e) {
                log.error("飞书加签失败", e);
                return;
            }
        }

        try {
            HttpUtil.Response r = HttpUtil.postJsonResp(webhook, json, 5000);
            if (HttpUtil.isWebhookSuccess(r.code, r.body)) {
                log.info("飞书通知发送成功: {}", msg.getTitle());
            } else {
                log.error("飞书通知发送失败, 响应码: {}, 响应体: {}", r.code, r.body);
            }
        } catch (Exception e) {
            log.error("飞书通知发送异常: {}", msg.getTitle(), e);
        }
    }
}

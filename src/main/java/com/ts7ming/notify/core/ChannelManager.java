package com.ts7ming.notify.core;

import com.ts7ming.notify.channel.EmailChannel;
import com.ts7ming.notify.channel.FeishuChannel;
import com.ts7ming.notify.channel.NotifyChannel;
import com.ts7ming.notify.channel.WeChatChannel;
import com.ts7ming.notify.channel.DingTalkChannel;
import com.ts7ming.notify.config.NotifyConfig;
import com.ts7ming.notify.config.NotifyConfig.ChannelConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 根据配置动态构建启用的通知渠道列表 (支持同时开启多个)。
 */
public final class ChannelManager {

    private ChannelManager() {
    }

    public static List<NotifyChannel> build(NotifyConfig cfg) {
        List<NotifyChannel> list = new ArrayList<>();
        if (cfg == null || cfg.getChannels() == null) {
            return list;
        }
        for (Map.Entry<String, ChannelConfig> en : cfg.getChannels().entrySet()) {
            ChannelConfig c = en.getValue();
            if (c == null || !c.isEnabled()) {
                continue;
            }
            String type = c.getString("type", en.getKey()).toLowerCase().trim();
            NotifyChannel ch = create(type, c);
            if (ch != null) {
                list.add(ch);
            }
        }
        return list;
    }

    private static NotifyChannel create(String type, ChannelConfig c) {
        if (type.contains("ding")) {
            return new DingTalkChannel(c);
        }
        if (type.contains("wechat") || type.contains("workwx") || type.contains("企业微信")) {
            return new WeChatChannel(c);
        }
        if (type.contains("feishu") || type.contains("lark") || type.contains("飞书")) {
            return new FeishuChannel(c);
        }
        if (type.contains("email") || type.contains("mail") || type.contains("邮箱")) {
            return new EmailChannel(c);
        }
        return null;
    }
}

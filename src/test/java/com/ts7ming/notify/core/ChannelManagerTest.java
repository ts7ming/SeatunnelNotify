package com.ts7ming.notify.core;

import com.ts7ming.notify.channel.DingTalkChannel;
import com.ts7ming.notify.channel.EmailChannel;
import com.ts7ming.notify.channel.FeishuChannel;
import com.ts7ming.notify.channel.NotifyChannel;
import com.ts7ming.notify.channel.WeChatChannel;
import com.ts7ming.notify.config.NotifyConfig;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 渠道管理测试: 按 type/key 动态识别 钉钉 / 企业微信 / 飞书 / 邮箱 (含别名与中文名)。
 */
public class ChannelManagerTest {

    private static NotifyConfig config(String yaml) {
        return NotifyConfig.fromYaml(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static List<NotifyChannel> channels(String channelYaml) {
        return ChannelManager.build(config("channels:\n" + channelYaml));
    }

    @Test
    public void build_nullConfig() {
        assertTrue(ChannelManager.build(null).isEmpty());
    }

    @Test
    public void build_noChannels() {
        assertTrue(ChannelManager.build(config("enabled: true\n")).isEmpty());
    }

    @Test
    public void build_disabledChannelIgnored() {
        List<NotifyChannel> list = channels(
                "  dingtalk:\n    enabled: false\n    webhook: https://example.com\n");

        assertTrue(list.isEmpty());
    }

    @Test
    public void build_dingtalk() {
        List<NotifyChannel> list = channels(
                "  dingtalk:\n    enabled: true\n    webhook: https://example.com\n");

        assertEquals(1, list.size());
        assertEquals(DingTalkChannel.class, list.get(0).getClass());
        assertEquals("dingtalk", list.get(0).type());
    }

    @Test
    public void build_wechat_andAliases() {
        for (String key : new String[] {"wechat", "workwx", "企业微信"}) {
            List<NotifyChannel> list = channels(
                    "  " + key + ":\n    enabled: true\n    webhook: https://example.com\n");
            assertEquals(key, 1, list.size());
            assertEquals(WeChatChannel.class, list.get(0).getClass());
        }
    }

    @Test
    public void build_feishu_andAliases() {
        for (String key : new String[] {"feishu", "lark", "飞书"}) {
            List<NotifyChannel> list = channels(
                    "  " + key + ":\n    enabled: true\n    webhook: https://example.com\n");
            assertEquals(key, 1, list.size());
            assertEquals(FeishuChannel.class, list.get(0).getClass());
        }
    }

    @Test
    public void build_email_andAliases() {
        for (String key : new String[] {"email", "mail", "邮箱"}) {
            List<NotifyChannel> list = channels(
                    "  " + key + ":\n    enabled: true\n    host: smtp.example.com\n    to: [a@x.com]\n");
            assertEquals(key, 1, list.size());
            assertEquals(EmailChannel.class, list.get(0).getClass());
        }
    }

    @Test
    public void build_explicitTypeOverridesKey() {
        List<NotifyChannel> list = channels(
                "  my-robot:\n    enabled: true\n    type: dingtalk\n    webhook: https://example.com\n");

        assertEquals(1, list.size());
        assertEquals(DingTalkChannel.class, list.get(0).getClass());
    }

    @Test
    public void build_unknownTypeIgnored() {
        List<NotifyChannel> list = channels(
                "  sms:\n    enabled: true\n    webhook: https://example.com\n");

        assertTrue(list.isEmpty());
    }

    @Test
    public void build_multipleChannels() {
        List<NotifyChannel> list = channels(
                "  dingtalk:\n    enabled: true\n    webhook: https://example.com\n"
                        + "  feishu:\n    enabled: true\n    webhook: https://example.com\n"
                        + "  wechat:\n    enabled: true\n    webhook: https://example.com\n"
                        + "  email:\n    enabled: true\n    host: smtp.example.com\n    to: [a@x.com]\n"
                        + "  disabled_one:\n    enabled: false\n    type: email\n");

        assertEquals(4, list.size());
        assertTrue(list.stream().anyMatch(c -> "dingtalk".equals(c.type())));
        assertTrue(list.stream().anyMatch(c -> "feishu".equals(c.type())));
        assertTrue(list.stream().anyMatch(c -> "wechat".equals(c.type())));
        assertTrue(list.stream().anyMatch(c -> "email".equals(c.type())));
    }
}

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

    private static NotifyConfig config(String json) {
        return NotifyConfig.fromJson(json.getBytes(StandardCharsets.UTF_8));
    }

    private static List<NotifyChannel> channels(String channelJson) {
        return ChannelManager.build(config("{\"channels\": {" + channelJson + "}}"));
    }

    @Test
    public void build_nullConfig() {
        assertTrue(ChannelManager.build(null).isEmpty());
    }

    @Test
    public void build_noChannels() {
        assertTrue(ChannelManager.build(config("{\"enabled\": true}")).isEmpty());
    }

    @Test
    public void build_disabledChannelIgnored() {
        List<NotifyChannel> list = channels(
                "\"dingtalk\": {\"enabled\": false, \"webhook\": \"https://example.com\"}");

        assertTrue(list.isEmpty());
    }

    @Test
    public void build_dingtalk() {
        List<NotifyChannel> list = channels(
                "\"dingtalk\": {\"enabled\": true, \"webhook\": \"https://example.com\"}");

        assertEquals(1, list.size());
        assertEquals(DingTalkChannel.class, list.get(0).getClass());
        assertEquals("dingtalk", list.get(0).type());
    }

    @Test
    public void build_wechat_andAliases() {
        for (String key : new String[] {"wechat", "workwx", "企业微信"}) {
            List<NotifyChannel> list = channels(
                    "\"" + key + "\": {\"enabled\": true, \"webhook\": \"https://example.com\"}");
            assertEquals(key, 1, list.size());
            assertEquals(WeChatChannel.class, list.get(0).getClass());
        }
    }

    @Test
    public void build_feishu_andAliases() {
        for (String key : new String[] {"feishu", "lark", "飞书"}) {
            List<NotifyChannel> list = channels(
                    "\"" + key + "\": {\"enabled\": true, \"webhook\": \"https://example.com\"}");
            assertEquals(key, 1, list.size());
            assertEquals(FeishuChannel.class, list.get(0).getClass());
        }
    }

    @Test
    public void build_email_andAliases() {
        for (String key : new String[] {"email", "mail", "邮箱"}) {
            List<NotifyChannel> list = channels(
                    "\"" + key + "\": {\"enabled\": true, \"host\": \"smtp.example.com\","
                            + " \"to\": [\"a@x.com\"]}");
            assertEquals(key, 1, list.size());
            assertEquals(EmailChannel.class, list.get(0).getClass());
        }
    }

    @Test
    public void build_explicitTypeOverridesKey() {
        List<NotifyChannel> list = channels(
                "\"my-robot\": {\"enabled\": true, \"type\": \"dingtalk\","
                        + " \"webhook\": \"https://example.com\"}");

        assertEquals(1, list.size());
        assertEquals(DingTalkChannel.class, list.get(0).getClass());
    }

    @Test
    public void build_unknownTypeIgnored() {
        List<NotifyChannel> list = channels(
                "\"sms\": {\"enabled\": true, \"webhook\": \"https://example.com\"}");

        assertTrue(list.isEmpty());
    }

    @Test
    public void build_multipleChannels() {
        List<NotifyChannel> list = channels(
                "\"dingtalk\": {\"enabled\": true, \"webhook\": \"https://example.com\"},"
                        + "\"feishu\": {\"enabled\": true, \"webhook\": \"https://example.com\"},"
                        + "\"wechat\": {\"enabled\": true, \"webhook\": \"https://example.com\"},"
                        + "\"email\": {\"enabled\": true, \"host\": \"smtp.example.com\","
                        + " \"to\": [\"a@x.com\"]},"
                        + "\"disabled_one\": {\"enabled\": false, \"type\": \"email\"}");

        assertEquals(4, list.size());
        assertTrue(list.stream().anyMatch(c -> "dingtalk".equals(c.type())));
        assertTrue(list.stream().anyMatch(c -> "feishu".equals(c.type())));
        assertTrue(list.stream().anyMatch(c -> "wechat".equals(c.type())));
        assertTrue(list.stream().anyMatch(c -> "email".equals(c.type())));
    }
}

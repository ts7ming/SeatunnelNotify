package com.ts7ming.notify.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * HTTP 工具测试: JSON 转义与 webhook 业务错误码判定 (HTTP 200 但 errcode/code/StatusCode != 0 仍算失败)。
 */
public class HttpUtilTest {

    @Test
    public void escapeJson_specialCharacters() {
        assertEquals("", HttpUtil.escapeJson(null));
        assertEquals("a\\\"b\\\\c", HttpUtil.escapeJson("a\"b\\c"));
        assertEquals("line1\\nline2\\r\\t\\b\\f", HttpUtil.escapeJson("line1\nline2\r\t\b\f"));
        assertEquals("\\u0001", HttpUtil.escapeJson("\u0001"));
        assertEquals("中文 emoji 不转义", HttpUtil.escapeJson("中文 emoji 不转义"));
    }

    @Test
    public void isWebhookSuccess_dingtalkStyle() {
        assertTrue(HttpUtil.isWebhookSuccess(200, "{\"errcode\":0,\"errmsg\":\"ok\"}"));
        assertFalse(HttpUtil.isWebhookSuccess(200, "{\"errcode\":310000,\"errmsg\":\"token not exist\"}"));
    }

    @Test
    public void isWebhookSuccess_feishuStyle() {
        assertTrue(HttpUtil.isWebhookSuccess(200, "{\"code\":0,\"msg\":\"success\"}"));
        assertFalse(HttpUtil.isWebhookSuccess(200, "{\"code\":19021,\"msg\":\"sign match fail\"}"));
        assertTrue(HttpUtil.isWebhookSuccess(200, "{\"StatusCode\":0}"));
        assertFalse(HttpUtil.isWebhookSuccess(200, "{\"StatusCode\":9499}"));
    }

    @Test
    public void isWebhookSuccess_edgeCases() {
        // HTTP 非 200 一律失败
        assertFalse(HttpUtil.isWebhookSuccess(500, "{\"errcode\":0}"));
        assertFalse(HttpUtil.isWebhookSuccess(403, ""));
        // 空响应体/无错误码字段视为成功
        assertTrue(HttpUtil.isWebhookSuccess(200, ""));
        assertTrue(HttpUtil.isWebhookSuccess(200, null));
        assertTrue(HttpUtil.isWebhookSuccess(200, "   "));
        assertTrue(HttpUtil.isWebhookSuccess(200, "{\"ok\":true}"));
    }
}

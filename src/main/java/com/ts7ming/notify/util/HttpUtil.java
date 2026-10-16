package com.ts7ming.notify.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 简单的 HTTP POST(JSON) 工具, 以及 JSON 字符串转义。
 */
public final class HttpUtil {

    private HttpUtil() {
    }

    /**
     * 发送 JSON POST 请求, 返回 HTTP 响应码。
     */
    public static int postJson(String url, String json, int timeoutMs) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        int code = conn.getResponseCode();
        // 消费响应体, 释放连接
        try (InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            if (in != null) {
                byte[] buf = new byte[1024];
                while (in.read(buf) > 0) {
                    // 丢弃
                }
            }
        } catch (Exception ignore) {
            // 忽略
        }
        conn.disconnect();
        return code;
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     */
    public static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

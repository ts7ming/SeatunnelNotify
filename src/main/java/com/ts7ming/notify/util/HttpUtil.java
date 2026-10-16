package com.ts7ming.notify.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单的 HTTP POST(JSON) 工具, 以及 JSON 字符串转义。
 */
public final class HttpUtil {

    private HttpUtil() {
    }

    /** JSON POST 的响应结果 (状态码 + 响应体)。 */
    public static class Response {
        public final int code;
        public final String body;

        public Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    /**
     * 发送 JSON POST 请求, 返回 HTTP 响应码与响应体。
     */
    public static Response postJsonResp(String url, String json, int timeoutMs) throws IOException {
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
        String body = readBody(conn, code);
        conn.disconnect();
        return new Response(code, body);
    }

    /** 兼容旧调用: 仅返回状态码。 */
    public static int postJson(String url, String json, int timeoutMs) throws IOException {
        return postJsonResp(url, json, timeoutMs).code;
    }

    /** 读取响应体 (统一按 UTF-8); 失败流或异常时返回空串。 */
    private static String readBody(HttpURLConnection conn, int code) {
        try (InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            if (in == null) {
                return "";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception ignore) {
            return "";
        }
    }

    /**
     * 判断机器人 webhook 是否真正发送成功。
     * 钉钉/企业微信/飞书即便业务失败也返回 HTTP 200, 真正成败在响应体的错误码:
     * 钉钉/企微用 errcode, 飞书用 code/StatusCode。
     * 仅当 HTTP 200 且错误码 == 0 (或无错误码字段) 才算成功。
     */
    public static boolean isWebhookSuccess(int code, String body) {
        if (code != 200) {
            return false;
        }
        if (body == null || body.trim().isEmpty()) {
            return true;
        }
        for (Pattern p : ERROR_CODE_PATTERNS) {
            Matcher m = p.matcher(body);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1)) == 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final Pattern[] ERROR_CODE_PATTERNS = new Pattern[] {
            Pattern.compile("\"errcode\"\\s*:\\s*(-?\\d+)"),
            Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)"),
            Pattern.compile("\"StatusCode\"\\s*:\\s*(-?\\d+)")
    };

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

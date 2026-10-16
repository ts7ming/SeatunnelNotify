package com.ts7ming.notify.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 签名工具: 钉钉 / 飞书 机器人加签均为 HmacSHA256 后 Base64。
 */
public final class SignUtil {

    private SignUtil() {
    }

    /**
     * 计算 HmacSHA256 并做 Base64 编码。
     *
     * @param data 待签名字符串 (如 "timestamp\nsecret")
     * @param key  密钥
     */
    public static String hmacSha256Base64(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    /**
     * 飞书自定义机器人加签。
     *
     * <p>与钉钉不同: {@code string_to_sign = timestamp + "\n" + secret}, 以 string_to_sign 作为
     * HmacSHA256 的 <b>key</b>, 对<b>空消息体</b>计算后 Base64。
     *
     * @param timestamp 秒级时间戳
     * @param secret    签名校验密钥
     */
    public static String feishuSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(new byte[0]);
        return Base64.getEncoder().encodeToString(raw);
    }
}

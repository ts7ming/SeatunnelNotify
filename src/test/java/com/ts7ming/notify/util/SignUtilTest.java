package com.ts7ming.notify.util;

import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * 签名工具测试: HmacSHA256 + Base64 (钉钉), 飞书加签 (key=timestamp\nsecret, msg=空)。
 */
public class SignUtilTest {

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    @Test
    public void hmacSha256Base64_knownVector() throws Exception {
        // RFC/Wikipedia 公布的测试向量: HMAC-SHA256(key="key", msg="The quick brown fox jumps over the lazy dog")
        String expected = Base64.getEncoder().encodeToString(
                hexToBytes("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8"));

        assertEquals(expected, SignUtil.hmacSha256Base64(
                "The quick brown fox jumps over the lazy dog", "key"));
    }

    @Test
    public void hmacSha256Base64_utf8Chinese() throws Exception {
        String data = "1700000000\n密钥";
        String key = "签约密钥";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));

        assertEquals(expected, SignUtil.hmacSha256Base64(data, key));
    }

    @Test
    public void feishuSign_matchesSpec() throws Exception {
        long timestamp = 1700000000L;
        String secret = "secret-key";

        // 飞书规范: string_to_sign = timestamp + "\n" + secret 作为 HmacSHA256 的 key, 对空消息体签名
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));

        assertEquals(expected, SignUtil.feishuSign(timestamp, secret));
    }

    @Test
    public void feishuSign_deterministicAndSensitive() throws Exception {
        String sign1 = SignUtil.feishuSign(1700000000L, "secret");
        String sign2 = SignUtil.feishuSign(1700000000L, "secret");
        String sign3 = SignUtil.feishuSign(1700000001L, "secret");
        String sign4 = SignUtil.feishuSign(1700000000L, "other-secret");

        assertEquals(sign1, sign2);
        assertNotEquals(sign1, sign3);
        assertNotEquals(sign1, sign4);
    }
}

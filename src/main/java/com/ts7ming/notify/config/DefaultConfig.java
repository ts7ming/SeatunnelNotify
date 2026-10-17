package com.ts7ming.notify.config;

import com.ts7ming.notify.util.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 内置默认配置。当 {@code $SEATUNNEL_HOME/config/seatunnelnotify.json} 不存在时, 写入此默认文件。
 *
 * <p>JSON 不支持注释, 故用 {@code "_comment"} 字段承载说明信息 (解析时忽略未知字段)。
 */
public final class DefaultConfig {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfig.class);

    public static final String CONTENT =
            "{\n" +
            "  \"_comment\": \"SeaTunnel 事件通知配置文件. 位置: $SEATUNNEL_HOME/config/seatunnelnotify.json. 每次事件触发时都会实时读取本文件(内容变化即重新解析). 消息模板占位符(区分大小写): {event_type} {time} {job_id} {job_name} {job_status} {table} {raw}. 每个事件分类都可选配 title/content 模板(字符串, 换行请使用 \\\\n); 不填则使用内置默认文案.\",\n" +
            "  \"enabled\": true,\n" +
            "  \"events\": {\n" +
            "    \"job_status\": {\n" +
            "      \"enabled\": true,\n" +
            "      \"statuses\": [\"FAILED\", \"FINISHED\", \"CANCELED\"]\n" +
            "    },\n" +
            "    \"schema_change\": {\n" +
            "      \"enabled\": true\n" +
            "    },\n" +
            "    \"lifecycle\": {\n" +
            "      \"enabled\": false\n" +
            "    },\n" +
            "    \"others\": {\n" +
            "      \"enabled\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"channels\": {\n" +
            "    \"dingtalk\": {\n" +
            "      \"enabled\": false,\n" +
            "      \"webhook\": \"https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN\",\n" +
            "      \"secret\": \"YOUR_SECRET\"\n" +
            "    },\n" +
            "    \"wechat\": {\n" +
            "      \"enabled\": false,\n" +
            "      \"webhook\": \"https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY\"\n" +
            "    },\n" +
            "    \"feishu\": {\n" +
            "      \"enabled\": false,\n" +
            "      \"webhook\": \"https://open.feishu.cn/open-apis/bot/v2/hook/YOUR_HOOK\",\n" +
            "      \"secret\": \"YOUR_SECRET\"\n" +
            "    },\n" +
            "    \"email\": {\n" +
            "      \"enabled\": false,\n" +
            "      \"host\": \"smtp.example.com\",\n" +
            "      \"port\": 465,\n" +
            "      \"username\": \"user@example.com\",\n" +
            "      \"password\": \"your_password\",\n" +
            "      \"from\": \"user@example.com\",\n" +
            "      \"to\": [\"admin@example.com\", \"ops@example.com\"],\n" +
            "      \"subject\": \"SeaTunnel 事件通知\",\n" +
            "      \"ssl\": true\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    /**
     * 若目标文件不存在则创建(含父目录), 并写入默认配置。
     */
    public static void writeIfMissing(File file) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
        if (file.exists()) {
            return;
        }
        try (Writer w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write(CONTENT);
            w.flush();
        }
        log.info("已生成默认配置文件: {}", file.getAbsolutePath());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

package com.ts7ming.notify.config;

import com.ts7ming.notify.util.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 内置默认配置。当 {@code $SEATUNNEL_HOME/ext/config.yaml} 不存在时, 写入此默认文件。
 */
public final class DefaultConfig {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfig.class);

    private DefaultConfig() {
    }

    public static final String CONTENT =
            "# ============================================================\n" +
            "# SeaTunnel 事件通知 配置文件\n" +
            "# 位置: $SEATUNNEL_HOME/ext/config.yaml\n" +
            "# 每次事件触发时都会实时读取本文件(内容变化即重新解析)\n" +
            "# ============================================================\n" +
            "\n" +
            "# 总开关\n" +
            "enabled: true\n" +
            "\n" +
            "# 事件订阅 (动态订阅, 按需开启)\n" +
            "events:\n" +
            "  # 任务状态事件 (成功/失败/取消等)\n" +
            "  job_status:\n" +
            "    enabled: true\n" +
            "    # 仅在这些状态下通知, 留空表示全部状态\n" +
            "    # 可选值: CREATED, RUNNING, FINISHED, FAILED, CANCELED, FAILING, CANCELLING ...\n" +
            "    statuses: [FAILED, FINISHED, CANCELED]\n" +
            "  # 表结构变更事件 (加列/删列/改列/改表等)\n" +
            "  schema_change:\n" +
            "    enabled: true\n" +
            "  # 读写生命周期事件 (READER_OPEN/CLOSE, WRITER_OPEN/CLOSE ...)\n" +
            "  lifecycle:\n" +
            "    enabled: false\n" +
            "  # 其他未归类事件\n" +
            "  others:\n" +
            "    enabled: false\n" +
            "\n" +
            "# 通知渠道 (可同时开启多个, 动态选择接收方式)\n" +
            "channels:\n" +
            "  # 钉钉机器人\n" +
            "  dingtalk:\n" +
            "    enabled: false\n" +
            "    webhook: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN\n" +
            "    secret: YOUR_SECRET   # 加签密钥, 留空表示不使用加签\n" +
            "  # 企业微信机器人\n" +
            "  wechat:\n" +
            "    enabled: false\n" +
            "    webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY\n" +
            "  # 飞书机器人\n" +
            "  feishu:\n" +
            "    enabled: false\n" +
            "    webhook: https://open.feishu.cn/open-apis/bot/v2/hook/YOUR_HOOK\n" +
            "    secret: YOUR_SECRET   # 加签密钥(安全设置-签名校验), 留空表示不使用\n" +
            "  # 邮件\n" +
            "  email:\n" +
            "    enabled: false\n" +
            "    host: smtp.example.com\n" +
            "    port: 465\n" +
            "    username: user@example.com\n" +
            "    password: your_password\n" +
            "    from: user@example.com\n" +
            "    to: [admin@example.com, ops@example.com]\n" +
            "    subject: SeaTunnel 事件通知\n" +
            "    ssl: true\n";

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
        try (Writer w = new FileWriter(file, StandardCharsets.UTF_8)) {
            w.write(CONTENT);
            w.flush();
        }
        log.info("已生成默认配置文件: {}", file.getAbsolutePath());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

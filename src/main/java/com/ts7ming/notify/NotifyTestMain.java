package com.ts7ming.notify;

import com.ts7ming.notify.channel.NotifyChannel;
import com.ts7ming.notify.config.ConfigLoader;
import com.ts7ming.notify.config.NotifyConfig;
import com.ts7ming.notify.core.ChannelManager;
import com.ts7ming.notify.core.MessageBuilder;
import com.ts7ming.notify.model.EventContext;
import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.NotifyMessage;

import java.util.List;

/**
 * 本地测试入口 (不依赖 SeaTunnel 运行环境)。
 *
 * <p>直接读取 {@code config/seatunnelnotify.json}, 构造一条模拟的 "任务失败" 事件,
 * 并把它发送到配置中所有已启用的通知渠道, 用于本地验证渠道配置是否正确。
 *
 * <p>运行方式:
 * <ul>
 *   <li>IDE 中直接 Run {@code NotifyTestMain.main}</li>
 *   <li>命令行: {@code mvn exec:java}</li>
 * </ul>
 *
 * <p>说明: 生产路径是 {@link EventNotifyHandler#handle(org.apache.seatunnel.api.event.Event)},
 * 这里绕过 {@code EventClassifier}, 直接手工构造等价的 {@link EventContext}, 以便脱离 SeaTunnel 独立测试。
 */
public final class NotifyTestMain {

    private NotifyTestMain() {
    }

    public static void main(String[] args) {
        NotifyConfig cfg = ConfigLoader.load();
        if (cfg == null || !cfg.isEnabled()) {
            System.out.println("[NotifyTest] 配置未启用(enabled=false)或加载失败, 退出。"
                    + " 请在 config/seatunnelnotify.json 中开启 enabled 并配置渠道。");
            return;
        }

        // 构造一条模拟事件上下文, 等价于真实 JOB/FAILED 事件经 EventClassifier 后的结果
        EventContext ctx = new EventContext();
        ctx.setCategory(EventCategory.JOB);
        ctx.setEventTypeName("JOB_STATUS");
        ctx.setJobId("TEST-" + System.currentTimeMillis());
        ctx.setJobName("local-test-job");
        ctx.setJobStatus("FAILED");
        ctx.setTime(System.currentTimeMillis());

        NotifyMessage msg = MessageBuilder.build(ctx, cfg);
        List<NotifyChannel> channels = ChannelManager.build(cfg);

        if (channels.isEmpty()) {
            System.out.println("[NotifyTest] 未启用任何通知渠道, 退出。"
                    + " 请在 config/seatunnelnotify.json 的 channels 下开启至少一个渠道并填写 webhook。");
            return;
        }

        System.out.println("[NotifyTest] 标题: " + msg.getTitle());
        System.out.println("[NotifyTest] 内容:\n" + msg.getContent());
        System.out.print("[NotifyTest] 已启用渠道:");
        for (NotifyChannel ch : channels) {
            System.out.print(" " + ch.type());
        }
        System.out.println();

        for (NotifyChannel ch : channels) {
            try {
                ch.send(msg);
                System.out.println("[NotifyTest] 渠道 [" + ch.type() + "] 发送成功");
            } catch (Throwable t) {
                System.out.println("[NotifyTest] 渠道 [" + ch.type() + "] 发送失败: " + t);
            }
        }
    }
}

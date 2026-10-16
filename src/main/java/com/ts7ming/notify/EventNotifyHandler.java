package com.ts7ming.notify;

import com.ts7ming.notify.channel.NotifyChannel;
import com.ts7ming.notify.config.ConfigLoader;
import com.ts7ming.notify.config.NotifyConfig;
import com.ts7ming.notify.core.ChannelManager;
import com.ts7ming.notify.core.EventClassifier;
import com.ts7ming.notify.core.MessageBuilder;
import com.ts7ming.notify.model.EventContext;
import com.ts7ming.notify.model.NotifyMessage;
import org.apache.seatunnel.api.event.Event;
import org.apache.seatunnel.api.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SeaTunnel 事件统一通知处理器。
 *
 * <p>核心特性:
 * <ul>
 *   <li>动态订阅: 通过配置文件控制要监听的事件类别 (任务状态/表结构变更/生命周期/其他)。</li>
 *   <li>动态接收方式: 钉钉 / 企业微信 / 飞书 / 邮箱, 可任意组合开启。</li>
 *   <li>实时读取: 每次事件触发都会实时读取 {@code $SEATUNNEL_HOME/ext/config.yaml} (内容变化即重新解析)。</li>
 * </ul>
 */
public class EventNotifyHandler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventNotifyHandler.class);

    @Override
    public void handle(Event event) {
        if (event == null) {
            return;
        }
        NotifyConfig cfg;
        try {
            cfg = ConfigLoader.load();
        } catch (Throwable t) {
            log.error("加载通知配置失败", t);
            return;
        }
        if (cfg == null || !cfg.isEnabled()) {
            return;
        }
        try {
            EventContext ctx = EventClassifier.classify(event);
            if (!cfg.shouldNotify(ctx)) {
                return;
            }
            NotifyMessage msg = MessageBuilder.build(ctx, cfg);
            List<NotifyChannel> channels = ChannelManager.build(cfg);
            if (channels.isEmpty()) {
                log.warn("配置已启用但未启用任何通知渠道, 事件: {}", ctx.getEventTypeName());
                return;
            }
            for (NotifyChannel ch : channels) {
                try {
                    ch.send(msg);
                } catch (Throwable t) {
                    log.error("通过渠道 {} 发送通知失败", ch.type(), t);
                }
            }
        } catch (Throwable t) {
            log.error("处理事件通知异常, eventType={}", event.getEventType(), t);
        }
    }
}

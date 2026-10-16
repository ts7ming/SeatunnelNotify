package com.ts7ming.notify.core;

import com.ts7ming.notify.model.EventContext;
import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;

/**
 * 根据事件上下文构建通知消息内容。
 */
public final class MessageBuilder {

    private MessageBuilder() {
    }

    public static NotifyMessage build(EventContext ctx) {
        String title;
        String content;
        String level;

        switch (ctx.getCategory()) {
            case JOB: {
                String st = ctx.getJobStatus() == null ? "UNKNOWN" : ctx.getJobStatus();
                boolean failed = "FAILED".equalsIgnoreCase(st);
                boolean finished = "FINISHED".equalsIgnoreCase(st);
                level = failed ? "ERROR" : "INFO";
                title = failed ? "【任务失败】" : (finished ? "【任务完成】" : "【任务状态变更】");
                content = "事件类型: JOB_STATUS\n"
                        + "任务ID: " + Common.nvl(ctx.getJobId()) + "\n"
                        + "任务名称: " + Common.nvl(ctx.getJobName()) + "\n"
                        + "状态: " + st + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime());
                break;
            }
            case SCHEMA:
                title = "【表结构变更】";
                level = "WARN";
                content = "事件类型: " + ctx.getEventTypeName() + "\n"
                        + "表: " + Common.nvl(ctx.getTableName()) + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime()) + "\n"
                        + "详情: " + ctx.getRaw();
                break;
            case LIFECYCLE:
                title = "【读写生命周期】";
                level = "INFO";
                content = "事件类型: " + ctx.getEventTypeName() + "\n"
                        + "表: " + Common.nvl(ctx.getTableName()) + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime());
                break;
            default:
                title = "【其他事件】";
                level = "INFO";
                content = "事件类型: " + ctx.getEventTypeName() + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime()) + "\n"
                        + "详情: " + ctx.getRaw();
        }
        return new NotifyMessage(title, content, level, ctx.getTime());
    }
}

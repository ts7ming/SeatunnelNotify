package com.ts7ming.notify.core;

import com.ts7ming.notify.config.NotifyConfig;
import com.ts7ming.notify.config.NotifyConfig.EventRule;
import com.ts7ming.notify.model.EventContext;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;

/**
 * 根据事件上下文构建通知消息内容。
 *
 * <p>若配置中为该事件类型提供了自定义 {@code title}/{@code content} 模板, 则按模板渲染
 * (占位符替换), 否则回退到内置默认文案。
 *
 * <p>支持的占位符:
 * <ul>
 *   <li>{@code {event_type}} 事件类型 (如 JOB_STATUS / SCHEMA_CHANGE / READER_OPEN)</li>
 *   <li>{@code {time}} 事件时间</li>
 *   <li>{@code {job_id}} 任务ID</li>
 *   <li>{@code {job_name}} 任务名称</li>
 *   <li>{@code {job_status}} 任务状态 (如 FAILED / FINISHED)</li>
 *   <li>{@code {table}} 表名</li>
 *   <li>{@code {raw}} 原始事件 toString</li>
 * </ul>
 */
public final class MessageBuilder {

    private MessageBuilder() {
    }

    public static NotifyMessage build(EventContext ctx, NotifyConfig cfg) {
        EventRule rule = cfg == null ? null : cfg.getEvents().rule(ctx.getCategory().configKey());
        String title = (rule != null && !Common.isBlank(rule.title))
                ? render(rule.title, ctx)
                : defaultTitle(ctx);
        String content = (rule != null && !Common.isBlank(rule.content))
                ? render(rule.content, ctx)
                : defaultContent(ctx);
        String level = defaultLevel(ctx);
        return new NotifyMessage(title, content, level, ctx.getTime());
    }

    private static String defaultLevel(EventContext ctx) {
        switch (ctx.getCategory()) {
            case JOB:
                return "FAILED".equalsIgnoreCase(ctx.getJobStatus()) ? "ERROR" : "INFO";
            case SCHEMA:
                return "WARN";
            default:
                return "INFO";
        }
    }

    private static String defaultTitle(EventContext ctx) {
        switch (ctx.getCategory()) {
            case JOB: {
                String st = ctx.getJobStatus() == null ? "UNKNOWN" : ctx.getJobStatus();
                boolean failed = "FAILED".equalsIgnoreCase(st);
                boolean finished = "FINISHED".equalsIgnoreCase(st);
                return failed ? "【任务失败】" : (finished ? "【任务完成】" : "【任务状态变更】");
            }
            case SCHEMA:
                return "【表结构变更】";
            case LIFECYCLE:
                return "【读写生命周期】";
            default:
                return "【其他事件】";
        }
    }

    private static String defaultContent(EventContext ctx) {
        switch (ctx.getCategory()) {
            case JOB: {
                String st = ctx.getJobStatus() == null ? "UNKNOWN" : ctx.getJobStatus();
                return "事件类型: " + ctx.getEventTypeName() + "\n"
                        + "任务ID: " + Common.nvl(ctx.getJobId()) + "\n"
                        + "任务名称: " + Common.nvl(ctx.getJobName()) + "\n"
                        + "状态: " + st + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime());
            }
            case SCHEMA:
                return "事件类型: " + ctx.getEventTypeName() + "\n"
                        + "表: " + Common.nvl(ctx.getTableName()) + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime()) + "\n"
                        + "详情: " + ctx.getRaw();
            case LIFECYCLE:
                return "事件类型: " + ctx.getEventTypeName() + "\n"
                        + "表: " + Common.nvl(ctx.getTableName()) + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime());
            default:
                return "事件类型: " + ctx.getEventTypeName() + "\n"
                        + "时间: " + Common.formatTime(ctx.getTime()) + "\n"
                        + "详情: " + ctx.getRaw();
        }
    }

    /**
     * 渲染用户自定义模板: 用事件上下文字段替换占位符。
     * 缺失字段替换为空字符串, 不影响其余内容。
     */
    static String render(String template, EventContext ctx) {
        if (template == null) {
            return "";
        }
        String s = template;
        s = s.replace("{event_type}", nz(ctx.getEventTypeName()));
        s = s.replace("{time}", nz(Common.formatTime(ctx.getTime())));
        s = s.replace("{job_id}", nz(ctx.getJobId()));
        s = s.replace("{job_name}", nz(ctx.getJobName()));
        s = s.replace("{job_status}", nz(ctx.getJobStatus()));
        s = s.replace("{table_name}", nz(ctx.getTableName()));
        s = s.replace("{table}", nz(ctx.getTableName()));
        s = s.replace("{raw}", ctx.getRaw() == null ? "" : ctx.getRaw().toString());
        return s;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}

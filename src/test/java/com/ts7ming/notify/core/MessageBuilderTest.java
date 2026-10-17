package com.ts7ming.notify.core;

import com.ts7ming.notify.config.NotifyConfig;
import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.EventContext;
import com.ts7ming.notify.model.NotifyMessage;
import com.ts7ming.notify.util.Common;
import org.apache.seatunnel.api.event.Event;
import org.apache.seatunnel.api.source.event.MessageDelayedEvent;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 消息构建测试: 覆盖四大事件分类 (JOB / SCHEMA / LIFECYCLE / OTHER) 的默认文案,
 * 以及自定义模板的全部占位符渲染。
 */
public class MessageBuilderTest {

    private static final long FIXED_TIME = 1700000000000L;
    private static final String FIXED_TIME_TEXT = Common.formatTime(FIXED_TIME);

    private static EventContext jobCtx(String status) {
        EventContext ctx = new EventContext();
        ctx.setCategory(EventCategory.JOB);
        ctx.setEventTypeName("JOB_STATUS");
        ctx.setJobId("9527");
        ctx.setJobName("sync-mysql-to-doris");
        ctx.setJobStatus(status);
        ctx.setTime(FIXED_TIME);
        return ctx;
    }

    private static EventContext schemaCtx() {
        EventContext ctx = new EventContext();
        ctx.setCategory(EventCategory.SCHEMA);
        ctx.setEventTypeName("SCHEMA_CHANGE_ADD_COLUMN");
        ctx.setTableName("user_info");
        ctx.setTime(FIXED_TIME);
        ctx.setRaw(new MessageDelayedEvent(1000L, "record-x"));
        return ctx;
    }

    private static EventContext lifecycleCtx() {
        EventContext ctx = new EventContext();
        ctx.setCategory(EventCategory.LIFECYCLE);
        ctx.setEventTypeName("LIFECYCLE_READER_OPEN");
        ctx.setTableName("user_info");
        ctx.setTime(FIXED_TIME);
        return ctx;
    }

    private static EventContext otherCtx() {
        EventContext ctx = new EventContext();
        ctx.setCategory(EventCategory.OTHER);
        ctx.setEventTypeName("READER_MESSAGE_DELAYED");
        ctx.setTime(FIXED_TIME);
        ctx.setRaw(new MessageDelayedEvent(2000L, "record-y"));
        return ctx;
    }

    private static NotifyConfig config(String json) {
        return NotifyConfig.fromJson(json.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- 默认文案

    @Test
    public void build_jobFailed_default() {
        NotifyMessage msg = MessageBuilder.build(jobCtx("FAILED"), null);

        assertEquals("【任务失败】", msg.getTitle());
        assertEquals("ERROR", msg.getLevel());
        assertTrue(msg.getContent().contains("事件类型: JOB_STATUS"));
        assertTrue(msg.getContent().contains("任务ID: 9527"));
        assertTrue(msg.getContent().contains("任务名称: sync-mysql-to-doris"));
        assertTrue(msg.getContent().contains("状态: FAILED"));
        assertTrue(msg.getContent().contains("时间: " + FIXED_TIME_TEXT));
        assertEquals(FIXED_TIME, msg.getTime());
    }

    @Test
    public void build_jobFinished_default() {
        NotifyMessage msg = MessageBuilder.build(jobCtx("FINISHED"), null);

        assertEquals("【任务完成】", msg.getTitle());
        assertEquals("INFO", msg.getLevel());
    }

    @Test
    public void build_jobRunning_default() {
        NotifyMessage msg = MessageBuilder.build(jobCtx("RUNNING"), null);

        assertEquals("【任务状态变更】", msg.getTitle());
        assertEquals("INFO", msg.getLevel());
    }

    @Test
    public void build_jobNullStatus_default() {
        NotifyMessage msg = MessageBuilder.build(jobCtx(null), null);

        assertEquals("【任务状态变更】", msg.getTitle());
        assertEquals("INFO", msg.getLevel());
        assertTrue(msg.getContent().contains("状态: UNKNOWN"));
    }

    @Test
    public void build_schemaChange_default() {
        NotifyMessage msg = MessageBuilder.build(schemaCtx(), null);

        assertEquals("【表结构变更】", msg.getTitle());
        assertEquals("WARN", msg.getLevel());
        assertTrue(msg.getContent().contains("事件类型: SCHEMA_CHANGE_ADD_COLUMN"));
        assertTrue(msg.getContent().contains("表: user_info"));
        assertTrue(msg.getContent().contains("详情: "));
    }

    @Test
    public void build_lifecycle_default() {
        NotifyMessage msg = MessageBuilder.build(lifecycleCtx(), null);

        assertEquals("【读写生命周期】", msg.getTitle());
        assertEquals("INFO", msg.getLevel());
        assertTrue(msg.getContent().contains("事件类型: LIFECYCLE_READER_OPEN"));
        assertTrue(msg.getContent().contains("表: user_info"));
        assertFalse(msg.getContent().contains("null"));
    }

    @Test
    public void build_other_default() {
        NotifyMessage msg = MessageBuilder.build(otherCtx(), null);

        assertEquals("【其他事件】", msg.getTitle());
        assertEquals("INFO", msg.getLevel());
        assertTrue(msg.getContent().contains("事件类型: READER_MESSAGE_DELAYED"));
        assertTrue(msg.getContent().contains("详情: "));
    }

    // ---------------------------------------------------------------- 自定义模板

    @Test
    public void build_customJobTemplate() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"job_status\": {"
                        + "\"enabled\": true,"
                        + "\"title\": \"【任务{job_status}】{job_name}\","
                        + "\"content\": \"type={event_type} id={job_id} name={job_name}"
                        + " status={job_status} time={time}\"}}}");

        NotifyMessage msg = MessageBuilder.build(jobCtx("FAILED"), cfg);

        assertEquals("【任务FAILED】sync-mysql-to-doris", msg.getTitle());
        assertEquals(
                "type=JOB_STATUS id=9527 name=sync-mysql-to-doris status=FAILED time=" + FIXED_TIME_TEXT,
                msg.getContent());
    }

    @Test
    public void build_templateWithNewline() {
        // JSON 字符串中的 \n 转义解析为真实换行, 渲染后保留
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"job_status\": {"
                        + "\"enabled\": true,"
                        + "\"title\": \"【任务{job_status}】\","
                        + "\"content\": \"事件类型: {event_type}\\n任务ID: {job_id}\\n状态: {job_status}\"}}}");

        NotifyMessage msg = MessageBuilder.build(jobCtx("FAILED"), cfg);

        assertEquals("事件类型: JOB_STATUS\n任务ID: 9527\n状态: FAILED", msg.getContent());
    }

    @Test
    public void build_customSchemaTemplate() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"schema_change\": {"
                        + "\"enabled\": true,"
                        + "\"title\": \"表 {table} 发生变更\","
                        + "\"content\": \"table_name={table_name} raw={raw}\"}}}");

        NotifyMessage msg = MessageBuilder.build(schemaCtx(), cfg);

        assertEquals("表 user_info 发生变更", msg.getTitle());
        assertTrue(msg.getContent().contains("table_name=user_info"));
        assertTrue(msg.getContent().contains("raw=MessageDelayedEvent"));
    }

    @Test
    public void build_blankTemplateFallsBackToDefault() {
        // title/content 为空串时回退到内置默认文案
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"job_status\": {"
                        + "\"enabled\": true, \"title\": \"\", \"content\": \"\"}}}");

        NotifyMessage msg = MessageBuilder.build(jobCtx("FAILED"), cfg);

        assertEquals("【任务失败】", msg.getTitle());
        assertTrue(msg.getContent().contains("任务ID: 9527"));
    }

    @Test
    public void build_customTemplateForOtherCategory() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"others\": {"
                        + "\"enabled\": true,"
                        + "\"title\": \"其他[{event_type}]\","
                        + "\"content\": \"time={time}\"}}}");

        NotifyMessage msg = MessageBuilder.build(otherCtx(), cfg);

        assertEquals("其他[READER_MESSAGE_DELAYED]", msg.getTitle());
        assertEquals("time=" + FIXED_TIME_TEXT, msg.getContent());
    }

    // ---------------------------------------------------------------- render

    @Test
    public void render_allPlaceholders() {
        Event raw = new MessageDelayedEvent(1000L, "r");
        EventContext ctx = jobCtx("FAILED");
        ctx.setTableName("user_info");
        ctx.setRaw(raw);

        String out = MessageBuilder.render(
                "{event_type}|{time}|{job_id}|{job_name}|{job_status}|{table_name}|{table}|{raw}", ctx);

        assertEquals("JOB_STATUS|" + FIXED_TIME_TEXT + "|9527|sync-mysql-to-doris|FAILED|user_info|user_info|"
                + raw, out);
    }

    @Test
    public void render_missingFieldsBecomeEmpty() {
        EventContext ctx = new EventContext();

        // {time} 为空时 Common.formatTime(0) 返回 "-", 其余缺失字段替换为空串
        assertEquals("|-|||||", MessageBuilder.render(
                "{event_type}|{time}|{job_id}|{job_name}|{job_status}|{table_name}|{table}", ctx));
        assertEquals("", MessageBuilder.render("{raw}", ctx));
    }

    @Test
    public void render_nullTemplate() {
        assertEquals("", MessageBuilder.render(null, jobCtx("FAILED")));
    }
}

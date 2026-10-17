package com.ts7ming.notify.config;

import com.ts7ming.notify.core.MessageBuilder;
import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.EventContext;
import com.ts7ming.notify.model.NotifyMessage;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 配置模型测试: JSON 解析 (含异常回退)、四类事件的订阅规则、状态过滤、渠道配置读取。
 */
public class NotifyConfigTest {

    private static NotifyConfig config(String json) {
        return NotifyConfig.fromJson(json.getBytes(StandardCharsets.UTF_8));
    }

    private static EventContext ctx(EventCategory category, String jobStatus) {
        EventContext c = new EventContext();
        c.setCategory(category);
        c.setJobStatus(jobStatus);
        return c;
    }

    // ---------------------------------------------------------------- 内置配置与模板资源

    @Test
    public void builtinConfigs_areValidJson() throws Exception {
        // 内置默认配置: 合法 JSON 且解析结果符合预期
        NotifyConfig def = NotifyConfig.fromJson(
                DefaultConfig.CONTENT.getBytes(StandardCharsets.UTF_8));
        assertTrue(def.isEnabled());
        assertTrue(def.getEvents().rule("job_status").enabled);
        assertEquals(Arrays.asList("FAILED", "FINISHED", "CANCELED"),
                def.getEvents().rule("job_status").statuses);
        assertTrue(def.getEvents().rule("schema_change").enabled);
        assertFalse(def.getEvents().rule("lifecycle").enabled);
        assertFalse(def.getEvents().rule("others").enabled);
        assertEquals(4, def.getChannels().size());
        assertFalse(def.getChannels().get("dingtalk").isEnabled());
        assertEquals(465, def.getChannels().get("email").getInt("port", 0));

        // 参考模板 seatunnelnotify.json: 合法 JSON 且能被正确解析
        try (InputStream in = NotifyConfigTest.class.getResourceAsStream("/seatunnelnotify.json.example")) {
            assertNotNull("seatunnelnotify.json 应存在于 classpath", in);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            NotifyConfig tpl = NotifyConfig.fromJson(bos.toByteArray());
            assertTrue(tpl.isEnabled());
            assertTrue(tpl.getEvents().rule("job_status").enabled);
            assertTrue(tpl.getEvents().rule("schema_change").enabled);
            assertEquals(4, tpl.getChannels().size());
            // 模板中预填的 title/content 应被正确解析, 且 \n 转义为真实换行
            assertEquals("【任务{job_status}】{job_name}", tpl.getEvents().rule("job_status").title);
            assertEquals("事件类型: {event_type}\n任务ID: {job_id}\n任务名称: {job_name}\n状态: {job_status}\n时间: {time}",
                    tpl.getEvents().rule("job_status").content);
            assertEquals("【表结构变更】{table}", tpl.getEvents().rule("schema_change").title);
            assertEquals("事件类型: {event_type}\n表: {table}\n时间: {time}\n详情: {raw}",
                    tpl.getEvents().rule("schema_change").content);
            assertEquals("【读写生命周期】{event_type}", tpl.getEvents().rule("lifecycle").title);
            assertEquals("【其他事件】{event_type}", tpl.getEvents().rule("others").title);
        }
    }

    @Test
    public void parse_utf8BomTolerated() {
        // Windows 记事本等保存的 UTF-8 带 BOM, 解析器应容错
        byte[] json = "{\"enabled\": true, \"events\": {\"job_status\": {\"enabled\": true}}}"
                .getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[json.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(json, 0, withBom, 3, json.length);

        NotifyConfig cfg = NotifyConfig.fromJson(withBom);

        assertTrue(cfg.isEnabled());
        assertTrue(cfg.getEvents().rule("job_status").enabled);
    }

    // ---------------------------------------------------------------- 默认值

    @Test
    public void defaults_emptyJson() {
        for (NotifyConfig cfg : Arrays.asList(
                NotifyConfig.fromJson(null),
                NotifyConfig.fromJson(new byte[0]),
                config(""))) {
            assertFalse(cfg.isEnabled());
            assertNotNull(cfg.getEvents());
            assertNotNull(cfg.getChannels());
            assertTrue(cfg.getChannels().isEmpty());
        }
    }

    @Test
    public void defaults_eventRules() {
        NotifyConfig cfg = config("{\"enabled\": true}");

        NotifyConfig.EventRule job = cfg.getEvents().rule("job_status");
        NotifyConfig.EventRule schema = cfg.getEvents().rule("schema_change");
        NotifyConfig.EventRule lifecycle = cfg.getEvents().rule("lifecycle");
        NotifyConfig.EventRule others = cfg.getEvents().rule("others");

        assertTrue(job.enabled);
        assertTrue(job.statuses.isEmpty());
        assertTrue(schema.enabled);
        assertFalse(lifecycle.enabled);
        assertFalse(others.enabled);
    }

    @Test
    public void invalidJson_fallsBackToDefaults() {
        NotifyConfig cfg = config("{\"enabled\": true, \"events\": [unclosed");

        // 解析失败时整体回退为空配置 (总开关关闭/无渠道), 事件规则保留内置默认值
        assertFalse(cfg.isEnabled());
        assertTrue(cfg.getChannels().isEmpty());
        assertTrue(cfg.getEvents().rule("job_status").enabled);
    }

    @Test
    public void nonObjectRoot_fallsBackToDefaults() {
        NotifyConfig cfg = config("\"just-a-string\"");

        assertFalse(cfg.isEnabled());
        assertTrue(cfg.getChannels().isEmpty());
        assertTrue(cfg.getEvents().rule("job_status").enabled);
    }

    // ---------------------------------------------------------------- 完整解析

    @Test
    public void parse_fullConfig() {
        NotifyConfig cfg = config(
                "{\"enabled\": true,"
                        + "\"events\": {"
                        + "  \"job_status\": {"
                        + "    \"enabled\": true,"
                        + "    \"statuses\": [\"FAILED\", \"FINISHED\"],"
                        + "    \"title\": \"【任务{job_status}】\","
                        + "    \"content\": \"id={job_id}\"},"
                        + "  \"schema_change\": {\"enabled\": true},"
                        + "  \"lifecycle\": {\"enabled\": true},"
                        + "  \"others\": {\"enabled\": false}},"
                        + "\"channels\": {"
                        + "  \"dingtalk\": {"
                        + "    \"enabled\": true,"
                        + "    \"webhook\": \"https://oapi.dingtalk.com/robot/send?access_token=TOKEN\","
                        + "    \"secret\": \"SEC\"},"
                        + "  \"email\": {"
                        + "    \"enabled\": false,"
                        + "    \"host\": \"smtp.example.com\","
                        + "    \"port\": 465,"
                        + "    \"ssl\": true,"
                        + "    \"to\": [\"a@x.com\", \"b@y.com\"]}}}");

        assertTrue(cfg.isEnabled());

        NotifyConfig.EventRule job = cfg.getEvents().rule("job_status");
        assertTrue(job.enabled);
        assertEquals(Arrays.asList("FAILED", "FINISHED"), job.statuses);
        assertEquals("【任务{job_status}】", job.title);
        assertEquals("id={job_id}", job.content);
        assertTrue(cfg.getEvents().rule("schema_change").enabled);
        assertTrue(cfg.getEvents().rule("lifecycle").enabled);
        assertFalse(cfg.getEvents().rule("others").enabled);

        assertEquals(2, cfg.getChannels().size());

        NotifyConfig.ChannelConfig dingtalk = cfg.getChannels().get("dingtalk");
        assertTrue(dingtalk.isEnabled());
        assertEquals("https://oapi.dingtalk.com/robot/send?access_token=TOKEN",
                dingtalk.getString("webhook", ""));
        assertEquals("SEC", dingtalk.getString("secret", ""));

        NotifyConfig.ChannelConfig email = cfg.getChannels().get("email");
        assertFalse(email.isEnabled());
        assertEquals("smtp.example.com", email.getString("host", ""));
        assertEquals(465, email.getInt("port", 25));
        assertTrue(email.getBool("ssl", false));
        List<String> to = email.getList("to");
        assertEquals(Arrays.asList("a@x.com", "b@y.com"), to);
    }

    @Test
    public void parse_unknownEventKeyCreatedEnabled() {
        NotifyConfig cfg = config("{\"enabled\": true, \"events\": {\"future_event\": {\"enabled\": true}}}");

        NotifyConfig.EventRule rule = cfg.getEvents().rule("future_event");
        assertNotNull(rule);
        assertTrue(rule.enabled);
        assertNull(cfg.getEvents().rule("no_such_key"));
    }

    // ---------------------------------------------------------------- 消息模板 (JSON 字符串, 支持 \n 换行)

    @Test
    public void parse_templateWithNewline() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"job_status\": {"
                        + "\"enabled\": true,"
                        + "\"content\": \"事件类型: {event_type}\\n任务ID: {job_id}\\n状态: {job_status}\"}}}");

        NotifyConfig.EventRule rule = cfg.getEvents().rule("job_status");
        // JSON 的 \n 转义解析为真实换行符
        assertEquals("事件类型: {event_type}\n任务ID: {job_id}\n状态: {job_status}", rule.content);

        EventContext ctx = new EventContext();
        ctx.setCategory(EventCategory.JOB);
        ctx.setEventTypeName("JOB_STATUS");
        ctx.setJobId("9527");
        ctx.setJobStatus("FAILED");
        NotifyMessage msg = MessageBuilder.build(ctx, cfg);
        assertEquals("事件类型: JOB_STATUS\n任务ID: 9527\n状态: FAILED", msg.getContent());
    }

    @Test
    public void parse_nullTemplateFallsBackToDefault() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"job_status\": {"
                        + "\"enabled\": true, \"title\": null, \"content\": null}}}");

        NotifyConfig.EventRule rule = cfg.getEvents().rule("job_status");
        assertNull(rule.title);
        assertNull(rule.content);

        EventContext ctx = new EventContext();
        ctx.setCategory(EventCategory.JOB);
        ctx.setJobStatus("FAILED");
        ctx.setEventTypeName("JOB_STATUS");
        NotifyMessage msg = MessageBuilder.build(ctx, cfg);
        assertEquals("【任务失败】", msg.getTitle());
    }

    // ---------------------------------------------------------------- shouldNotify

    @Test
    public void shouldNotify_nullContext() {
        assertFalse(config("{\"enabled\": true}").shouldNotify(null));
    }

    @Test
    public void shouldNotify_disabledCategory() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {"
                        + "\"lifecycle\": {\"enabled\": false},"
                        + "\"others\": {\"enabled\": false}}}");

        assertFalse(cfg.shouldNotify(ctx(EventCategory.LIFECYCLE, null)));
        assertFalse(cfg.shouldNotify(ctx(EventCategory.OTHER, null)));
    }

    @Test
    public void shouldNotify_jobWithoutStatusFilter() {
        NotifyConfig cfg = config("{\"enabled\": true, \"events\": {\"job_status\": {\"enabled\": true}}}");

        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "FAILED")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "RUNNING")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, null)));
    }

    @Test
    public void shouldNotify_jobWithStatusFilter() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {\"job_status\": {"
                        + "\"enabled\": true, \"statuses\": [\"FAILED\"]}}}");

        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "FAILED")));
        // 状态过滤忽略大小写
        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "failed")));
        assertFalse(cfg.shouldNotify(ctx(EventCategory.JOB, "FINISHED")));
        assertFalse(cfg.shouldNotify(ctx(EventCategory.JOB, null)));
    }

    @Test
    public void shouldNotify_statusFilterOnlyAppliesToJob() {
        // 状态过滤仅对 JOB 分类生效, SCHEMA 即使带 jobStatus 也不受影响
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {"
                        + "\"job_status\": {\"enabled\": true, \"statuses\": [\"FAILED\"]},"
                        + "\"schema_change\": {\"enabled\": true},"
                        + "\"lifecycle\": {\"enabled\": true}}}");

        assertTrue(cfg.shouldNotify(ctx(EventCategory.SCHEMA, "FINISHED")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.LIFECYCLE, "FINISHED")));
    }

    @Test
    public void shouldNotify_allCategoriesEnabled() {
        NotifyConfig cfg = config(
                "{\"enabled\": true, \"events\": {"
                        + "\"job_status\": {\"enabled\": true},"
                        + "\"schema_change\": {\"enabled\": true},"
                        + "\"lifecycle\": {\"enabled\": true},"
                        + "\"others\": {\"enabled\": true}}}");

        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "FAILED")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.SCHEMA, null)));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.LIFECYCLE, null)));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.OTHER, null)));
    }

    // ---------------------------------------------------------------- ChannelConfig 读取

    @Test
    public void channelConfig_typedGetters() {
        NotifyConfig cfg = config(
                "{\"channels\": {\"test\": {"
                        + "\"enabled\": true,"
                        + "\"str_val\": \"hello\","
                        + "\"int_num\": 8080,"
                        + "\"int_str\": \"9090\","
                        + "\"int_bad\": \"not-a-number\","
                        + "\"bool_true\": true,"
                        + "\"bool_str\": \"true\","
                        + "\"list_val\": [\"x\", \"y\", \"z\"],"
                        + "\"comma_val\": \"a, b ,c\"}}}");

        NotifyConfig.ChannelConfig cc = cfg.getChannels().get("test");

        assertEquals("hello", cc.getString("str_val", "def"));
        assertEquals("def", cc.getString("missing", "def"));
        assertEquals(8080, cc.getInt("int_num", 1));
        assertEquals(9090, cc.getInt("int_str", 1));
        assertEquals(1, cc.getInt("int_bad", 1));
        assertEquals(1, cc.getInt("missing", 1));
        assertTrue(cc.getBool("bool_true", false));
        assertTrue(cc.getBool("bool_str", false));
        assertFalse(cc.getBool("missing", false));
        assertEquals(Arrays.asList("x", "y", "z"), cc.getList("list_val"));
        assertEquals(Arrays.asList("a", "b", "c"), cc.getList("comma_val"));
        assertTrue(cc.getList("missing").isEmpty());
    }
}

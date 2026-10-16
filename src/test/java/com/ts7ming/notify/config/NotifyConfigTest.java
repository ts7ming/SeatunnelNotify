package com.ts7ming.notify.config;

import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.EventContext;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 配置模型测试: YAML 解析 (含异常回退)、四类事件的订阅规则、状态过滤、渠道配置读取。
 */
public class NotifyConfigTest {

    private static NotifyConfig config(String yaml) {
        return NotifyConfig.fromYaml(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static EventContext ctx(EventCategory category, String jobStatus) {
        EventContext c = new EventContext();
        c.setCategory(category);
        c.setJobStatus(jobStatus);
        return c;
    }

    // ---------------------------------------------------------------- 默认值

    @Test
    public void defaults_emptyYaml() {
        for (NotifyConfig cfg : Arrays.asList(
                NotifyConfig.fromYaml(null),
                NotifyConfig.fromYaml(new byte[0]),
                config(""))) {
            assertFalse(cfg.isEnabled());
            assertNotNull(cfg.getEvents());
            assertNotNull(cfg.getChannels());
            assertTrue(cfg.getChannels().isEmpty());
        }
    }

    @Test
    public void defaults_eventRules() {
        NotifyConfig cfg = config("enabled: true\n");

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
    public void invalidYaml_fallsBackToDefaults() {
        NotifyConfig cfg = config("enabled: true\nevents: [unclosed\n");

        // 解析失败时整体回退为空配置 (总开关关闭/无渠道), 事件规则保留内置默认值
        assertFalse(cfg.isEnabled());
        assertTrue(cfg.getChannels().isEmpty());
        assertTrue(cfg.getEvents().rule("job_status").enabled);
    }

    @Test
    public void nonMapRoot_fallsBackToDefaults() {
        NotifyConfig cfg = config("just-a-string\n");

        assertFalse(cfg.isEnabled());
        assertTrue(cfg.getChannels().isEmpty());
        assertTrue(cfg.getEvents().rule("job_status").enabled);
    }

    // ---------------------------------------------------------------- 完整解析

    @Test
    public void parse_fullConfig() {
        NotifyConfig cfg = config(
                "enabled: true\n"
                        + "events:\n"
                        + "  job_status:\n"
                        + "    enabled: true\n"
                        + "    statuses: [FAILED, FINISHED]\n"
                        + "    title: \"【任务{job_status}】\"\n"
                        + "    content: \"id={job_id}\"\n"
                        + "  schema_change:\n"
                        + "    enabled: true\n"
                        + "  lifecycle:\n"
                        + "    enabled: true\n"
                        + "  others:\n"
                        + "    enabled: false\n"
                        + "channels:\n"
                        + "  dingtalk:\n"
                        + "    enabled: true\n"
                        + "    webhook: https://oapi.dingtalk.com/robot/send?access_token=TOKEN\n"
                        + "    secret: SEC\n"
                        + "  email:\n"
                        + "    enabled: false\n"
                        + "    host: smtp.example.com\n"
                        + "    port: 465\n"
                        + "    ssl: true\n"
                        + "    to: [a@x.com, b@y.com]\n");

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
        NotifyConfig cfg = config("enabled: true\nevents:\n  future_event:\n    enabled: true\n");

        NotifyConfig.EventRule rule = cfg.getEvents().rule("future_event");
        assertNotNull(rule);
        assertTrue(rule.enabled);
        assertNull(cfg.getEvents().rule("no_such_key"));
    }

    // ---------------------------------------------------------------- shouldNotify

    @Test
    public void shouldNotify_nullContext() {
        assertFalse(config("enabled: true\n").shouldNotify(null));
    }

    @Test
    public void shouldNotify_disabledCategory() {
        NotifyConfig cfg = config(
                "enabled: true\nevents:\n  lifecycle:\n    enabled: false\n  others:\n    enabled: false\n");

        assertFalse(cfg.shouldNotify(ctx(EventCategory.LIFECYCLE, null)));
        assertFalse(cfg.shouldNotify(ctx(EventCategory.OTHER, null)));
    }

    @Test
    public void shouldNotify_jobWithoutStatusFilter() {
        NotifyConfig cfg = config("enabled: true\nevents:\n  job_status:\n    enabled: true\n");

        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "FAILED")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "RUNNING")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, null)));
    }

    @Test
    public void shouldNotify_jobWithStatusFilter() {
        NotifyConfig cfg = config(
                "enabled: true\nevents:\n  job_status:\n    enabled: true\n    statuses: [FAILED]\n");

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
                "enabled: true\nevents:\n"
                        + "  job_status:\n    enabled: true\n    statuses: [FAILED]\n"
                        + "  schema_change:\n    enabled: true\n"
                        + "  lifecycle:\n    enabled: true\n");

        assertTrue(cfg.shouldNotify(ctx(EventCategory.SCHEMA, "FINISHED")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.LIFECYCLE, "FINISHED")));
    }

    @Test
    public void shouldNotify_allCategoriesEnabled() {
        NotifyConfig cfg = config(
                "enabled: true\nevents:\n"
                        + "  job_status:\n    enabled: true\n"
                        + "  schema_change:\n    enabled: true\n"
                        + "  lifecycle:\n    enabled: true\n"
                        + "  others:\n    enabled: true\n");

        assertTrue(cfg.shouldNotify(ctx(EventCategory.JOB, "FAILED")));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.SCHEMA, null)));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.LIFECYCLE, null)));
        assertTrue(cfg.shouldNotify(ctx(EventCategory.OTHER, null)));
    }

    // ---------------------------------------------------------------- ChannelConfig 读取

    @Test
    public void channelConfig_typedGetters() {
        NotifyConfig cfg = config(
                "channels:\n"
                        + "  test:\n"
                        + "    enabled: true\n"
                        + "    str_val: hello\n"
                        + "    int_num: 8080\n"
                        + "    int_str: \"9090\"\n"
                        + "    int_bad: not-a-number\n"
                        + "    bool_true: true\n"
                        + "    bool_str: \"true\"\n"
                        + "    list_val: [x, y, z]\n"
                        + "    comma_val: \"a, b ,c\"\n");

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

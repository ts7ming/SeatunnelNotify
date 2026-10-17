package com.ts7ming.notify.config;

import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.EventContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 通知配置模型, 由 JSON 解析而来。
 * 支持动态订阅事件、动态选择通知渠道、按事件类型自定义消息模板。
 */
public class NotifyConfig {

    private boolean enabled = false;
    private EventSubscribe events = new EventSubscribe();
    private Map<String, ChannelConfig> channels = new HashMap<>();

    /** 事件订阅配置 (按 job_status/schema_change/lifecycle/others 分类) */
    public static class EventSubscribe {
        public Map<String, EventRule> rules = defaultRules();

        public EventRule rule(String key) {
            return rules.get(key);
        }

        private static Map<String, EventRule> defaultRules() {
            Map<String, EventRule> m = new HashMap<>();
            m.put(EventCategory.JOB.configKey(), new EventRule(true));
            m.put(EventCategory.SCHEMA.configKey(), new EventRule(true));
            m.put(EventCategory.LIFECYCLE.configKey(), new EventRule(false));
            m.put(EventCategory.OTHER.configKey(), new EventRule(false));
            return m;
        }
    }

    /** 单个事件分类规则: 开关 + 状态过滤 + 自定义消息模板 */
    public static class EventRule {
        public boolean enabled;
        public List<String> statuses = new ArrayList<>();
        /** 可选标题模板, 为空则用内置默认文案 */
        public String title;
        /** 可选内容模板, 为空则用内置默认文案 */
        public String content;

        public EventRule() {
        }

        public EventRule(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** 单个渠道配置 (所有字段放在 props 中, 由各渠道自行读取, 实现动态扩展) */
    public static class ChannelConfig {
        private boolean enabled = false;
        private Map<String, Object> props = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public String getString(String key, String def) {
            Object v = props.get(key);
            return v == null ? def : String.valueOf(v);
        }

        public int getInt(String key, int def) {
            Object v = props.get(key);
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            if (v != null) {
                try {
                    return Integer.parseInt(v.toString().trim());
                } catch (Exception ignore) {
                }
            }
            return def;
        }

        public boolean getBool(String key, boolean def) {
            Object v = props.get(key);
            if (v instanceof Boolean) {
                return (Boolean) v;
            }
            if (v != null) {
                return Boolean.parseBoolean(v.toString().trim());
            }
            return def;
        }

        @SuppressWarnings("unchecked")
        public List<String> getList(String key) {
            List<String> result = new ArrayList<>();
            Object v = props.get(key);
            if (v instanceof List) {
                for (Object o : (List<Object>) v) {
                    if (o != null && !o.toString().trim().isEmpty()) {
                        result.add(o.toString().trim());
                    }
                }
            } else if (v != null) {
                for (String s : v.toString().split(",")) {
                    if (!s.trim().isEmpty()) {
                        result.add(s.trim());
                    }
                }
            }
            return result;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public EventSubscribe getEvents() {
        return events;
    }

    public Map<String, ChannelConfig> getChannels() {
        return channels;
    }

    /** 根据事件上下文判断是否应该通知 */
    public boolean shouldNotify(EventContext ctx) {
        if (ctx == null) {
            return false;
        }
        EventRule r = events.rule(ctx.getCategory().configKey());
        if (r == null || !r.enabled) {
            return false;
        }
        if (ctx.getCategory() == EventCategory.JOB) {
            if (r.statuses != null && !r.statuses.isEmpty()) {
                String st = ctx.getJobStatus();
                if (st == null) {
                    return false;
                }
                boolean matched = false;
                for (String s : r.statuses) {
                    if (s != null && s.equalsIgnoreCase(st)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 解析 JSON 字节内容 */
    public static NotifyConfig fromJson(byte[] bytes) {
        NotifyConfig cfg = new NotifyConfig();
        if (bytes == null || bytes.length == 0) {
            return cfg;
        }
        try {
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            // 兼容 Windows 记事本等工具保存的 UTF-8 BOM
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            JsonNode root = MAPPER.readTree(text);
            if (root == null || !root.isObject()) {
                return cfg;
            }
            cfg.enabled = asBool(root.get("enabled"), false);

            JsonNode ev = root.get("events");
            if (ev != null && ev.isObject()) {
                parseEvents(cfg, ev);
            }
            JsonNode ch = root.get("channels");
            if (ch != null && ch.isObject()) {
                parseChannels(cfg, ch);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(NotifyConfig.class)
                    .error("解析配置文件失败, 将使用空配置(全部关闭)", e);
            return new NotifyConfig();
        }
        return cfg;
    }

    private static void parseEvents(NotifyConfig cfg, JsonNode ev) {
        EventSubscribe e = cfg.events;
        Iterator<Map.Entry<String, JsonNode>> it = ev.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> en = it.next();
            JsonNode m = en.getValue();
            if (m == null || !m.isObject()) {
                continue;
            }
            String key = en.getKey();
            EventRule rule = e.rules.get(key);
            if (rule == null) {
                // 未知键: 新建并默认开启 (仅供将来扩展, 目前 shouldNotify 只识别四个已知键)
                rule = new EventRule(true);
                e.rules.put(key, rule);
            }
            if (m.has("enabled")) {
                rule.enabled = asBool(m.get("enabled"), rule.enabled);
            }
            if (m.has("statuses")) {
                rule.statuses = asStringList(m.get("statuses"));
            }
            if (m.has("title")) {
                rule.title = asString(m.get("title"));
            }
            if (m.has("content")) {
                rule.content = asString(m.get("content"));
            }
        }
    }

    private static void parseChannels(NotifyConfig cfg, JsonNode ch) {
        Iterator<Map.Entry<String, JsonNode>> it = ch.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> en = it.next();
            JsonNode cm = en.getValue();
            if (cm == null || !cm.isObject()) {
                continue;
            }
            ChannelConfig cc = new ChannelConfig();
            cc.enabled = asBool(cm.get("enabled"), false);
            // 复制全部键值, 渠道按需读取 (enabled/type 等也是其中的普通键)
            cc.props = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fit = cm.fields();
            while (fit.hasNext()) {
                Map.Entry<String, JsonNode> fe = fit.next();
                cc.props.put(fe.getKey(), jsonToPlain(fe.getValue()));
            }
            cfg.channels.put(en.getKey(), cc);
        }
    }

    /** JsonNode 转为普通 Java 对象, 供 ChannelConfig 的 getString/getInt/getBool/getList 使用。 */
    private static Object jsonToPlain(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode x : node) {
                list.add(jsonToPlain(x));
            }
            return list;
        }
        return node.toString();
    }

    private static boolean asBool(JsonNode o, boolean def) {
        if (o != null && o.isBoolean()) {
            return o.booleanValue();
        }
        if (o != null && !o.isNull()) {
            return Boolean.parseBoolean(o.asText().trim());
        }
        return def;
    }

    private static String asString(JsonNode o) {
        if (o == null || o.isNull()) {
            return null;
        }
        return o.asText();
    }

    private static List<String> asStringList(JsonNode o) {
        List<String> r = new ArrayList<>();
        if (o == null || o.isNull()) {
            return r;
        }
        if (o.isArray()) {
            for (JsonNode x : o) {
                if (x != null && !x.isNull() && !x.asText().trim().isEmpty()) {
                    r.add(x.asText().trim());
                }
            }
        } else {
            for (String s : o.asText().split(",")) {
                if (!s.trim().isEmpty()) {
                    r.add(s.trim());
                }
            }
        }
        return r;
    }
}

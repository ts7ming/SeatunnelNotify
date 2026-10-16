package com.ts7ming.notify.config;

import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.EventContext;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知配置模型, 由 YAML 解析而来。
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

    /** 解析 YAML 字节内容 */
    @SuppressWarnings("unchecked")
    public static NotifyConfig fromYaml(byte[] bytes) {
        NotifyConfig cfg = new NotifyConfig();
        if (bytes == null || bytes.length == 0) {
            return cfg;
        }
        try {
            Yaml yaml = new Yaml();
            Object root = yaml.load(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            if (!(root instanceof Map)) {
                return cfg;
            }
            Map<String, Object> m = (Map<String, Object>) root;
            cfg.enabled = asBool(m.get("enabled"), false);

            Object ev = m.get("events");
            if (ev instanceof Map) {
                parseEvents(cfg, (Map<String, Object>) ev);
            }
            Object ch = m.get("channels");
            if (ch instanceof Map) {
                parseChannels(cfg, (Map<String, Object>) ch);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(NotifyConfig.class)
                    .error("解析配置文件失败, 将使用空配置(全部关闭)", e);
            return new NotifyConfig();
        }
        return cfg;
    }

    @SuppressWarnings("unchecked")
    private static void parseEvents(NotifyConfig cfg, Map<String, Object> ev) {
        EventSubscribe e = cfg.events;
        for (Map.Entry<String, Object> en : ev.entrySet()) {
            Object v = en.getValue();
            if (!(v instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) v;
            String key = en.getKey();
            EventRule rule = e.rules.get(key);
            if (rule == null) {
                // 未知键: 新建并默认开启 (仅供将来扩展, 目前 shouldNotify 只识别四个已知键)
                rule = new EventRule(true);
                e.rules.put(key, rule);
            }
            if (m.containsKey("enabled")) {
                rule.enabled = asBool(m.get("enabled"), rule.enabled);
            }
            if (m.containsKey("statuses")) {
                rule.statuses = asStringList(m.get("statuses"));
            }
            if (m.containsKey("title")) {
                rule.title = asString(m.get("title"));
            }
            if (m.containsKey("content")) {
                rule.content = asString(m.get("content"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseChannels(NotifyConfig cfg, Map<String, Object> ch) {
        for (Map.Entry<String, Object> en : ch.entrySet()) {
            Object v = en.getValue();
            if (!(v instanceof Map)) {
                continue;
            }
            Map<String, Object> cm = (Map<String, Object>) v;
            ChannelConfig cc = new ChannelConfig();
            cc.enabled = asBool(cm.get("enabled"), false);
            // 复制全部键值, 渠道按需读取 (enabled/type 等也是其中的普通键)
            cc.props = new HashMap<>(cm);
            cfg.channels.put(en.getKey(), cc);
        }
    }

    private static boolean asBool(Object o, boolean def) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o != null) {
            return Boolean.parseBoolean(o.toString().trim());
        }
        return def;
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static List<String> asStringList(Object o) {
        List<String> r = new ArrayList<>();
        if (o instanceof List) {
            for (Object x : (List<Object>) o) {
                if (x != null && !x.toString().trim().isEmpty()) {
                    r.add(x.toString().trim());
                }
            }
        } else if (o != null) {
            for (String s : o.toString().split(",")) {
                if (!s.trim().isEmpty()) {
                    r.add(s.trim());
                }
            }
        }
        return r;
    }
}

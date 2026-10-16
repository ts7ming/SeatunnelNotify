package com.ts7ming.notify.core;

import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.EventContext;
import org.apache.seatunnel.api.event.Event;
import org.apache.seatunnel.api.event.EventType;
import org.apache.seatunnel.api.table.schema.event.SchemaChangeEvent;

/**
 * 事件分类器: 将 SeaTunnel 的原始 {@link Event} 归类为 {@link EventCategory} 并提取关键信息。
 */
public final class EventClassifier {

    private EventClassifier() {
    }

    public static EventContext classify(Event event) {
        EventContext ctx = new EventContext();
        ctx.setRaw(event);
        ctx.setEventTypeName(event.getEventType() == null ? "UNKNOWN" : event.getEventType().name());
        ctx.setTime(asLong(ReflectUtil.invoke(event, "getCreatedTime")));

        String simpleName = event.getClass().getSimpleName();
        boolean isJob = "JobStateEvent".equals(simpleName)
                || (event.getEventType() != null && "JOB_STATUS".equals(event.getEventType().name()));

        if (isJob) {
            ctx.setCategory(EventCategory.JOB);
            ctx.setJobId(ReflectUtil.str(event, "getJobId"));
            ctx.setJobName(ReflectUtil.str(event, "getJobName"));
            Object st = ReflectUtil.invoke(event, "getJobStatus");
            ctx.setJobStatus(st == null ? null : st.toString());
        } else if (event instanceof SchemaChangeEvent) {
            ctx.setCategory(EventCategory.SCHEMA);
            ctx.setTableName(extractTableName(event));
        } else if (isLifecycle(event.getEventType())) {
            ctx.setCategory(EventCategory.LIFECYCLE);
            ctx.setTableName(extractTableName(event));
        } else {
            ctx.setCategory(EventCategory.OTHER);
        }
        return ctx;
    }

    private static boolean isLifecycle(EventType t) {
        if (t == null) {
            return false;
        }
        switch (t.name()) {
            // SeaTunnel 2.3.x / 3.x 的实际枚举命名 (LIFECYCLE_ 前缀)
            case "LIFECYCLE_ENUMERATOR_OPEN":
            case "LIFECYCLE_ENUMERATOR_CLOSE":
            case "LIFECYCLE_READER_OPEN":
            case "LIFECYCLE_READER_CLOSE":
            case "LIFECYCLE_WRITER_OPEN":
            case "LIFECYCLE_WRITER_CLOSE":
            case "LIFECYCLE_SPLIT_READER_OPEN":
            case "LIFECYCLE_SPLIT_READER_CLOSE":
            // 兼容无前缀的历史/自定义命名
            case "READER_OPEN":
            case "READER_CLOSE":
            case "WRITER_OPEN":
            case "WRITER_CLOSE":
            case "SPLIT_READER_OPEN":
            case "SPLIT_READER_CLOSE":
                return true;
            default:
                return false;
        }
    }

    private static String extractTableName(Event event) {
        Object ti = ReflectUtil.invoke(event, "getTableIdentifier");
        if (ti != null) {
            Object tn = ReflectUtil.invoke(ti, "getTableName");
            if (tn != null) {
                return String.valueOf(tn);
            }
            Object tid = ReflectUtil.invoke(ti, "getTableId");
            if (tid != null) {
                return String.valueOf(tid);
            }
            return ti.toString();
        }
        return null;
    }

    private static long asLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o != null) {
            try {
                return Long.parseLong(o.toString().trim());
            } catch (Exception ignore) {
            }
        }
        return 0L;
    }
}

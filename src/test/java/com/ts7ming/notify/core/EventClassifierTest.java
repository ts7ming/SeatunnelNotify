package com.ts7ming.notify.core;

import com.ts7ming.notify.model.EventCategory;
import com.ts7ming.notify.model.EventContext;
import org.apache.seatunnel.api.event.Event;
import org.apache.seatunnel.api.event.EventType;
import org.apache.seatunnel.api.source.event.EnumeratorCloseEvent;
import org.apache.seatunnel.api.source.event.EnumeratorOpenEvent;
import org.apache.seatunnel.api.source.event.MessageDelayedEvent;
import org.apache.seatunnel.api.source.event.ReaderCloseEvent;
import org.apache.seatunnel.api.source.event.ReaderOpenEvent;
import org.apache.seatunnel.api.sink.event.WriterCloseEvent;
import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.catalog.PhysicalColumn;
import org.apache.seatunnel.api.table.catalog.TableIdentifier;
import org.apache.seatunnel.api.table.schema.event.AlterTableAddColumnEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableChangeColumnEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableColumnsEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableDropColumnEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableModifyColumnEvent;
import org.apache.seatunnel.api.table.schema.event.AlterTableNameEvent;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.engine.common.job.JobStatus;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 事件分类器测试: 覆盖 SeaTunnel 2.3.x 的<b>全部事件类型</b>。
 *
 * <p>SeaTunnel 2.3.13 的 {@link EventType} 共 13 个枚举值, 对应的事件实现类分为四组:
 * <ul>
 *   <li>JOB_STATUS: {@link JobStateEvent} (任务状态)</li>
 *   <li>SCHEMA_CHANGE_*: 6 个表结构变更事件</li>
 *   <li>LIFECYCLE_*: 5 个读写生命周期事件</li>
 *   <li>READER_MESSAGE_DELAYED: {@link MessageDelayedEvent} (未归类, 归入 OTHER)</li>
 * </ul>
 */
public class EventClassifierTest {

    private static final String CATALOG = "mysql_catalog";
    private static final String DATABASE = "test_db";
    private static final String TABLE = "user_info";

    private static TableIdentifier tableIdentifier() {
        return TableIdentifier.of(CATALOG, DATABASE, TABLE);
    }

    private static Column column(String name) {
        return PhysicalColumn.builder().name(name).dataType(BasicType.INT_TYPE).build();
    }

    /** 构造真实的 JobStateEvent (全限定名: 避免与下方同名的测试替身冲突)。 */
    private static org.apache.seatunnel.engine.common.job.JobStateEvent jobEvent(
            long jobId, String jobName, JobStatus status) {
        return new org.apache.seatunnel.engine.common.job.JobStateEvent(jobId, jobName, status);
    }

    // ---------------------------------------------------------------- JOB_STATUS

    @Test
    public void classify_jobStateEvent_failed() {
        EventContext ctx = EventClassifier.classify(
                jobEvent(9527L, "sync-mysql-to-doris", JobStatus.FAILED));

        assertEquals(EventCategory.JOB, ctx.getCategory());
        assertEquals("JOB_STATUS", ctx.getEventTypeName());
        assertEquals("9527", ctx.getJobId());
        assertEquals("sync-mysql-to-doris", ctx.getJobName());
        assertEquals("FAILED", ctx.getJobStatus());
        assertTrue(ctx.getTime() > 0L);
        assertNull(ctx.getTableName());
    }

    @Test
    public void classify_jobStateEvent_finished() {
        EventContext ctx = EventClassifier.classify(jobEvent(1L, "job-a", JobStatus.FINISHED));

        assertEquals(EventCategory.JOB, ctx.getCategory());
        assertEquals("FINISHED", ctx.getJobStatus());
    }

    @Test
    public void classify_jobStateEvent_canceled() {
        EventContext ctx = EventClassifier.classify(jobEvent(2L, "job-b", JobStatus.CANCELED));

        assertEquals(EventCategory.JOB, ctx.getCategory());
        assertEquals("CANCELED", ctx.getJobStatus());
    }

    @Test
    public void classify_jobStateEvent_running() {
        EventContext ctx = EventClassifier.classify(jobEvent(3L, "job-c", JobStatus.RUNNING));

        assertEquals(EventCategory.JOB, ctx.getCategory());
        assertEquals("RUNNING", ctx.getJobStatus());
    }

    /**
     * 与真实 JobStateEvent 同类名但 eventType 不同, 用于覆盖 classify 中
     * "按类简单名判定任务事件" 的分支 (eventType 不是 JOB_STATUS 时)。
     */
    public static class JobStateEvent implements Event {

        private final EventType eventType;
        private final long createdTime = System.currentTimeMillis();
        private final String jobId;
        private final String jobName;
        private final String jobStatus;

        JobStateEvent(EventType eventType, String jobId, String jobName, String jobStatus) {
            this.eventType = eventType;
            this.jobId = jobId;
            this.jobName = jobName;
            this.jobStatus = jobStatus;
        }

        @Override
        public long getCreatedTime() {
            return createdTime;
        }

        @Override
        public void setJobId(String jobId) {
        }

        @Override
        public String getJobId() {
            return jobId;
        }

        @Override
        public EventType getEventType() {
            return eventType;
        }

        public String getJobName() {
            return jobName;
        }

        public String getJobStatus() {
            return jobStatus;
        }
    }

    @Test
    public void classify_jobEventBySimpleName() {
        EventContext ctx = EventClassifier.classify(
                new JobStateEvent(EventType.READER_MESSAGE_DELAYED, "7", "job-d", "FAILED"));

        assertEquals(EventCategory.JOB, ctx.getCategory());
        assertEquals("READER_MESSAGE_DELAYED", ctx.getEventTypeName());
        assertEquals("7", ctx.getJobId());
        assertEquals("job-d", ctx.getJobName());
        assertEquals("FAILED", ctx.getJobStatus());
    }

    // ---------------------------------------------------------------- SCHEMA_CHANGE_*

    @Test
    public void classify_alterTableAddColumnEvent() {
        EventContext ctx = EventClassifier.classify(
                AlterTableAddColumnEvent.add(tableIdentifier(), column("age")));

        assertEquals(EventCategory.SCHEMA, ctx.getCategory());
        assertEquals("SCHEMA_CHANGE_ADD_COLUMN", ctx.getEventTypeName());
        assertEquals(TABLE, ctx.getTableName());
    }

    @Test
    public void classify_alterTableDropColumnEvent() {
        EventContext ctx = EventClassifier.classify(
                new AlterTableDropColumnEvent(tableIdentifier(), "age"));

        assertEquals(EventCategory.SCHEMA, ctx.getCategory());
        assertEquals("SCHEMA_CHANGE_DROP_COLUMN", ctx.getEventTypeName());
        assertEquals(TABLE, ctx.getTableName());
    }

    @Test
    public void classify_alterTableModifyColumnEvent() {
        EventContext ctx = EventClassifier.classify(
                AlterTableModifyColumnEvent.modify(tableIdentifier(), column("age")));

        assertEquals(EventCategory.SCHEMA, ctx.getCategory());
        assertEquals("SCHEMA_CHANGE_MODIFY_COLUMN", ctx.getEventTypeName());
        assertEquals(TABLE, ctx.getTableName());
    }

    @Test
    public void classify_alterTableChangeColumnEvent() {
        EventContext ctx = EventClassifier.classify(
                AlterTableChangeColumnEvent.change(tableIdentifier(), "old_age", column("age")));

        assertEquals(EventCategory.SCHEMA, ctx.getCategory());
        assertEquals("SCHEMA_CHANGE_CHANGE_COLUMN", ctx.getEventTypeName());
        assertEquals(TABLE, ctx.getTableName());
    }

    @Test
    public void classify_alterTableColumnsEvent() {
        AlterTableColumnsEvent event = new AlterTableColumnsEvent(tableIdentifier())
                .addEvent(AlterTableAddColumnEvent.add(tableIdentifier(), column("age")))
                .addEvent(new AlterTableDropColumnEvent(tableIdentifier(), "name"));

        EventContext ctx = EventClassifier.classify(event);

        assertEquals(EventCategory.SCHEMA, ctx.getCategory());
        assertEquals("SCHEMA_CHANGE_UPDATE_COLUMNS", ctx.getEventTypeName());
        assertEquals(TABLE, ctx.getTableName());
    }

    @Test
    public void classify_alterTableNameEvent() {
        EventContext ctx = EventClassifier.classify(new AlterTableNameEvent(
                tableIdentifier(), TableIdentifier.of(CATALOG, DATABASE, "user_info_v2")));

        assertEquals(EventCategory.SCHEMA, ctx.getCategory());
        assertEquals("SCHEMA_CHANGE_RENAME_TABLE", ctx.getEventTypeName());
        // 提取的是变更前的表名
        assertEquals(TABLE, ctx.getTableName());
    }

    // ---------------------------------------------------------------- LIFECYCLE_*

    @Test
    public void classify_readerOpenEvent() {
        EventContext ctx = EventClassifier.classify(new ReaderOpenEvent());

        assertEquals(EventCategory.LIFECYCLE, ctx.getCategory());
        assertEquals("LIFECYCLE_READER_OPEN", ctx.getEventTypeName());
        assertTrue(ctx.getTime() > 0L);
    }

    @Test
    public void classify_readerCloseEvent() {
        EventContext ctx = EventClassifier.classify(new ReaderCloseEvent());

        assertEquals(EventCategory.LIFECYCLE, ctx.getCategory());
        assertEquals("LIFECYCLE_READER_CLOSE", ctx.getEventTypeName());
    }

    @Test
    public void classify_writerCloseEvent() {
        EventContext ctx = EventClassifier.classify(new WriterCloseEvent());

        assertEquals(EventCategory.LIFECYCLE, ctx.getCategory());
        assertEquals("LIFECYCLE_WRITER_CLOSE", ctx.getEventTypeName());
    }

    @Test
    public void classify_enumeratorOpenEvent() {
        EventContext ctx = EventClassifier.classify(new EnumeratorOpenEvent());

        assertEquals(EventCategory.LIFECYCLE, ctx.getCategory());
        assertEquals("LIFECYCLE_ENUMERATOR_OPEN", ctx.getEventTypeName());
    }

    @Test
    public void classify_enumeratorCloseEvent() {
        EventContext ctx = EventClassifier.classify(new EnumeratorCloseEvent());

        assertEquals(EventCategory.LIFECYCLE, ctx.getCategory());
        assertEquals("LIFECYCLE_ENUMERATOR_CLOSE", ctx.getEventTypeName());
    }

    // ---------------------------------------------------------------- 其他

    @Test
    public void classify_messageDelayedEvent() {
        EventContext ctx = EventClassifier.classify(new MessageDelayedEvent(3000L, "record-1"));

        assertEquals(EventCategory.OTHER, ctx.getCategory());
        assertEquals("READER_MESSAGE_DELAYED", ctx.getEventTypeName());
        assertNull(ctx.getTableName());
    }

    /** eventType 为 null 的事件: 类型名记为 UNKNOWN, 归类为 OTHER。 */
    static class UnknownEvent implements Event {

        private final long createdTime = System.currentTimeMillis();

        @Override
        public long getCreatedTime() {
            return createdTime;
        }

        @Override
        public void setJobId(String jobId) {
        }

        @Override
        public String getJobId() {
            return null;
        }

        @Override
        public EventType getEventType() {
            return null;
        }
    }

    @Test
    public void classify_eventWithoutType() {
        Event event = new UnknownEvent();
        EventContext ctx = EventClassifier.classify(event);

        assertEquals(EventCategory.OTHER, ctx.getCategory());
        assertEquals("UNKNOWN", ctx.getEventTypeName());
        assertNotNull(ctx.getRaw());
        assertSame(event, ctx.getRaw());
    }
}

# 事件类型

插件对接 SeaTunnel 2.3.x 的事件体系。SeaTunnel 2.3.13 的 `EventType` 共 **13 个枚举值**，插件按业务语义归为四大类，与配置文件中 `events` 下的四个键一一对应。

## 分类映射总表

| 事件类型（EventType） | 事件实现类 | 插件分类 | 配置键 |
| --- | --- | --- | --- |
| `JOB_STATUS` | `JobStateEvent` | JOB | `job_status` |
| `SCHEMA_CHANGE_ADD_COLUMN` | `AlterTableAddColumnEvent` | SCHEMA | `schema_change` |
| `SCHEMA_CHANGE_DROP_COLUMN` | `AlterTableDropColumnEvent` | SCHEMA | `schema_change` |
| `SCHEMA_CHANGE_MODIFY_COLUMN` | `AlterTableModifyColumnEvent` | SCHEMA | `schema_change` |
| `SCHEMA_CHANGE_CHANGE_COLUMN` | `AlterTableChangeColumnEvent` | SCHEMA | `schema_change` |
| `SCHEMA_CHANGE_UPDATE_COLUMNS` | `AlterTableColumnsEvent` | SCHEMA | `schema_change` |
| `SCHEMA_CHANGE_RENAME_TABLE` | `AlterTableNameEvent` | SCHEMA | `schema_change` |
| `LIFECYCLE_ENUMERATOR_OPEN` | `EnumeratorOpenEvent` | LIFECYCLE | `lifecycle` |
| `LIFECYCLE_ENUMERATOR_CLOSE` | `EnumeratorCloseEvent` | LIFECYCLE | `lifecycle` |
| `LIFECYCLE_READER_OPEN` | `ReaderOpenEvent` | LIFECYCLE | `lifecycle` |
| `LIFECYCLE_READER_CLOSE` | `ReaderCloseEvent` | LIFECYCLE | `lifecycle` |
| `LIFECYCLE_WRITER_CLOSE` | `WriterCloseEvent` | LIFECYCLE | `lifecycle` |
| `READER_MESSAGE_DELAYED` | `MessageDelayedEvent` | OTHER | `others` |

分类规则：

- **JOB**：事件类名为 `JobStateEvent` 或事件类型为 `JOB_STATUS`
- **SCHEMA**：事件继承 `SchemaChangeEvent`（表结构变更）
- **LIFECYCLE**：事件类型为 `LIFECYCLE_*`（读写生命周期）
- **OTHER**：以上皆非（未归类，如 `READER_MESSAGE_DELAYED`）

## 各分类说明

### job_status — 任务状态事件

任务生命周期状态变化时触发，提取任务ID、任务名称、任务状态。常用状态：`CREATED`、`RUNNING`、`FINISHED`、`FAILED`、`CANCELED`、`FAILING`、`CANCELLING`。

可通过 `statuses` 仅推送关心的状态：

```json
"job_status": {
  "enabled": true,
  "statuses": ["FAILED", "CANCELED"]
}
```

### schema_change — 表结构变更事件

Source 端发生 schema 变更（加列 / 删列 / 改列 / 换列 / 批量改列 / 改表名）时触发，提取表名。此类事件较多，默认开启。

### lifecycle — 读写生命周期事件

Reader / Writer / Enumerator 打开、关闭时触发。**默认关闭**（事件频繁，噪音大），需要时在配置中开启：

```json
"lifecycle": {
  "enabled": true
}
```

### others — 其他未归类事件

暂未归类的未来事件类型（如 `READER_MESSAGE_DELAYED`），**默认关闭**。

## 分类与模板的对应

每类事件可独立配置 `title` / `content` 模板，详见 [消息模板](message-templates.md)。不同分类可用的占位符不同，例如 `{job_status}` 仅任务状态事件有值，`{table}` 仅表相关事件有值，缺失时替换为空字符串。

# 消息模板

每类事件都可通过 `title`（标题）和 `content`（内容）两个字段自定义通知文案。**不填、空串或 `null` 时使用内置默认文案**。

## 字段位置

```json
{
  "events": {
    "job_status": {
      "enabled": true,
      "title": "【任务{job_status}】{job_name}",
      "content": "事件类型: {event_type}\n任务ID: {job_id}\n状态: {job_status}"
    }
  }
}
```

## 占位符

占位符**区分大小写**，缺失字段替换为空字符串，不影响其余内容。

| 占位符 | 含义 | 示例 |
| --- | --- | --- |
| `{event_type}` | 事件类型 | `JOB_STATUS`、`SCHEMA_CHANGE_ADD_COLUMN`、`LIFECYCLE_READER_OPEN` |
| `{time}` | 事件时间 | `2026-10-17 21:30:00` |
| `{job_id}` | 任务ID | `1234567890` |
| `{job_name}` | 任务名称 | `sync-mysql-to-doris` |
| `{job_status}` | 任务状态（仅任务事件有值） | `FAILED`、`FINISHED` |
| `{table}` / `{table_name}` | 表名（仅表相关事件有值） | `user_info` |
| `{raw}` | 原始事件的 `toString` | `AlterTableAddColumnEvent(...)` |

## 换行

`title` / `content` 是 JSON 字符串，多行内容使用 `\n` 转义，解析后会变成真实换行：

```json
"content": "事件类型: {event_type}\n任务ID: {job_id}\n任务名称: {job_name}\n状态: {job_status}\n时间: {time}"
```

推送效果：

```
事件类型: JOB_STATUS
任务ID: 9527
任务名称: sync-mysql-to-doris
状态: FAILED
时间: 2026-10-17 21:30:00
```

## 各类事件默认文案

未配置模板时使用的内置文案：

| 分类 | 默认标题 | 默认内容 |
| --- | --- | --- |
| `job_status` | FAILED → `【任务失败】`；FINISHED → `【任务完成】`；其他 → `【任务状态变更】` | 事件类型 / 任务ID / 任务名称 / 状态 / 时间 |
| `schema_change` | `【表结构变更】` | 事件类型 / 表 / 时间 / 详情（原始事件） |
| `lifecycle` | `【读写生命周期】` | 事件类型 / 表 / 时间 |
| `others` | `【其他事件】` | 事件类型 / 时间 / 详情（原始事件） |

## 完整示例

```json
{
  "enabled": true,
  "events": {
    "job_status": {
      "enabled": true,
      "statuses": ["FAILED"],
      "title": "【任务{job_status}】{job_name}",
      "content": "事件类型: {event_type}\n任务ID: {job_id}\n任务名称: {job_name}\n状态: {job_status}\n时间: {time}"
    },
    "schema_change": {
      "enabled": true,
      "title": "【表结构变更】{table}",
      "content": "事件类型: {event_type}\n表: {table}\n时间: {time}\n详情: {raw}"
    }
  }
}
```

> 现成示例也可直接参考 `src/main/resources/config-template.json`，四类事件的 `title` / `content` 均已预填，可按需修改或删除。

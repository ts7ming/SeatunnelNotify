# 配置参考

## 配置文件路径

**`$SEATUNNEL_HOME/config/seatunnelnotify.json`**（JSON 格式）

- `SEATUNNEL_HOME` 按以下优先级确定：环境变量 → 系统属性 → 当前工作目录
- 文件不存在时，插件首次触发事件会自动生成默认配置
- 每次事件触发都会实时读取，**内容变化即重新解析**，保存后立即生效
- 解析失败时回退为空配置（全部关闭），并在 SeaTunnel 日志打印错误

## 完整结构

```json
{
  "enabled": true,
  "events": {
    "job_status": {
      "enabled": true,
      "statuses": ["FAILED", "FINISHED", "CANCELED"],
      "title": "【任务{job_status}】{job_name}",
      "content": "事件类型: {event_type}\n任务ID: {job_id}\n任务名称: {job_name}\n状态: {job_status}\n时间: {time}"
    },
    "schema_change": {
      "enabled": true,
      "title": "【表结构变更】{table}",
      "content": "事件类型: {event_type}\n表: {table}\n时间: {time}\n详情: {raw}"
    },
    "lifecycle": {
      "enabled": false,
      "title": "【读写生命周期】{event_type}",
      "content": "事件类型: {event_type}\n表: {table}\n时间: {time}"
    },
    "others": {
      "enabled": false,
      "title": "【其他事件】{event_type}",
      "content": "事件类型: {event_type}\n时间: {time}\n详情: {raw}"
    }
  },
  "channels": {
    "dingtalk": {
      "enabled": false,
      "webhook": "https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN",
      "secret": "YOUR_SECRET"
    },
    "wechat": {
      "enabled": false,
      "webhook": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY"
    },
    "feishu": {
      "enabled": false,
      "webhook": "https://open.feishu.cn/open-apis/bot/v2/hook/YOUR_HOOK",
      "secret": "YOUR_SECRET"
    },
    "email": {
      "enabled": false,
      "host": "smtp.example.com",
      "port": 465,
      "username": "user@example.com",
      "password": "your_password",
      "from": "user@example.com",
      "to": ["admin@example.com", "ops@example.com"],
      "subject": "SeaTunnel 事件通知",
      "ssl": true
    }
  }
}
```

## 字段说明

### 根字段

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | bool | `false` | 总开关。为 `false` 时任何事件都不通知 |

### `events.<分类>` 事件订阅

四个分类键名固定：`job_status`、`schema_change`、`lifecycle`、`others`。

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | bool | job_status / schema_change 为 `true`，lifecycle / others 为 `false` | 该分类是否通知 |
| `statuses` | string[] | `[]` | **仅 `job_status` 生效**。仅在这些任务状态下通知，留空表示全部状态；匹配忽略大小写 |
| `title` | string | 无 | 自定义标题模板，不填或空串则用内置默认文案 |
| `content` | string | 无 | 自定义内容模板，不填或空串则用内置默认文案 |

> `title` / `content` 的占位符与 `\n` 换行用法见 [消息模板](message-templates.md)。

`statuses` 可选值（SeaTunnel `JobStatus`）：`INITIALIZING`、`CREATED`、`PENDING`、`SCHEDULED`、`RUNNING`、`FAILING`、`FAILED`、`DOING_SAVEPOINT`、`SAVEPOINT_DONE`、`CANCELING`、`CANCELED`、`FINISHED`、`UNKNOWABLE`。

### `channels.<渠道>` 通知渠道

渠道的 key 名即类型标识（也可用 `type` 字段显式指定），支持任意命名，识别规则见 [通知渠道](channels.md)。所有渠道通用字段：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | bool | `false` | 是否启用该渠道 |
| `type` | string | 取 key 名 | 显式指定渠道类型，覆盖 key 名的自动识别 |

各渠道专属参数（`webhook`、`secret`、`host`、`to` 等）见 [通知渠道](channels.md)。

## 配置示例

只推送任务失败事件到钉钉：

```json
{
  "enabled": true,
  "events": {
    "job_status": {
      "enabled": true,
      "statuses": ["FAILED"],
      "title": "【任务{job_status}】{job_name}",
      "content": "事件类型: {event_type}\n任务ID: {job_id}\n任务名称: {job_name}\n状态: {job_status}\n时间: {time}"
    }
  },
  "channels": {
    "dingtalk": {
      "enabled": true,
      "webhook": "https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN",
      "secret": "YOUR_SECRET"
    }
  }
}
```

## 注意事项

- JSON 不支持注释，示例配置文件 `src/main/resources/config-template.json` 中用解析时忽略的 `_comment` 字段承载说明，正式配置中可保留也可删除
- 用 Windows 记事本编辑时注意保存为 UTF-8；解析器已兼容带 BOM 的文件
- 状态过滤（`statuses`）只对 `job_status` 分类生效，其他分类即使填写也不会过滤

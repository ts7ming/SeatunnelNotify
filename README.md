# SeatunnelNotify - SeaTunnel 事件动态通知插件

一个 SeaTunnel (2.3.x) 事件处理器插件, 支持 **动态订阅所有 SeaTunnel 事件**, 并 **动态选择接收方式** (钉钉 / 企业微信 / 飞书 / 邮箱)。

> 完整文档见 [docs/](docs/index.md): [快速开始](docs/getting-started.md) · [配置参考](docs/configuration.md) · [消息模板](docs/message-templates.md) · [通知渠道](docs/channels.md) · [事件类型](docs/event-types.md) · [常见问题](docs/faq.md)

[![LICENSE](https://img.shields.io/badge/license-Anti%20996-blue.svg)](https://github.com/996icu/996.ICU/blob/master/LICENSE)

## 特性

- **动态订阅事件**: 通过配置文件控制要监听的事件类别
  - `job_status` 任务状态事件 (成功 / 失败 / 取消等, 可指定仅某些状态通知)
  - `schema_change` 表结构变更事件 (加列 / 删列 / 改列 / 改表)
  - `lifecycle` 读写生命周期事件 (READER / WRITER OPEN / CLOSE)
  - `others` 其他未归类事件
- **动态接收方式**: 钉钉、企业微信、飞书、邮箱, 可任意组合同时开启
- **实时读取配置**: 每次事件触发都会实时读取配置文件; 文件内容变化即重新解析, 无需重启 SeaTunnel

## 配置文件

默认路径: **`$SEATUNNEL_HOME/config/seatunnelnotify.json`** (JSON 格式)

- 若文件不存在, 插件首次触发事件时会自动创建默认配置文件 (含全部渠道模板)
- 修改配置后**立即生效**, 无需重启

参考模板见 `src/main/resources/config-template.json`。

```json
{
  "enabled": true,
  "events": {
    "job_status": {
      "enabled": true,
      "statuses": ["FAILED", "FINISHED", "CANCELED"]
    },
    "schema_change": {
      "enabled": true
    },
    "lifecycle": {
      "enabled": false
    },
    "others": {
      "enabled": false
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

## 消息模板

每种事件分类都可在 `seatunnelnotify.json` 里自定义 `title` / `content` 模板(不填则用内置默认文案)。模板支持以下占位符(区分大小写):

| 占位符 | 含义 |
| --- | --- |
| `{event_type}` | 事件类型, 如 `JOB_STATUS` / `SCHEMA_CHANGE` / `READER_OPEN` |
| `{time}` | 事件时间 |
| `{job_id}` | 任务ID |
| `{job_name}` | 任务名称 |
| `{job_status}` | 任务状态, 如 `FAILED` / `FINISHED` / `CANCELED` |
| `{table}` | 表名 |
| `{raw}` | 原始事件 `toString` |

示例(自定义「任务失败」标题与多行内容):

```json
{
  "events": {
    "job_status": {
      "enabled": true,
      "statuses": ["FAILED"],
      "title": "【任务{job_status}】{job_name}",
      "content": "事件类型: {event_type}\n任务ID: {job_id}\n任务名称: {job_name}\n状态: {job_status}\n时间: {time}"
    }
  }
}
```

> 提示: `title` / `content` 为 JSON 字符串, 多行内容使用 `\n` 转义(解析后会变成真实换行)。修改后立即生效, 无需重启。
>
> `config-template.json` 已为四类事件预填了开箱即用的 `title` / `content` 示例模板, 可直接修改使用; 删掉某个字段即回退到该分类的内置默认文案。

## 构建

```bash
mvn clean package
```

产物: `target/seatunnel-notify.jar` (已通过 shade 将 jackson / javax.mail 等第三方依赖打包并 relocation, 插件自包含, 不会与 SeaTunnel 自带依赖冲突)。

## 部署

将 `seatunnel-notify.jar` 放入 SeaTunnel 的扩展目录 (例如 `$SEATUNNEL_HOME/ext/`), 并确保 `$SEATUNNEL_HOME/config/seatunnelnotify.json` 存在 (不存在会自动生成)。

SeaTunnel 通过 SPI (`META-INF/services/org.apache.seatunnel.api.event.EventHandler`) 自动加载本插件, 无需额外配置。

## 目录结构

```
com.ts7ming.notify
 ├─ EventNotifyHandler        # 主处理器 (EventHandler 实现)
 ├─ config/                   # 配置路径 / 模型 / 加载器 / 默认配置
 ├─ model/                    # 事件上下文 / 消息 / 分类
 ├─ core/                     # 事件分类 / 消息构建 / 渠道管理 / 反射工具
 ├─ channel/                  # 钉钉 / 企业微信 / 飞书 / 邮箱 渠道实现
 └─ util/                     # HTTP / 签名 / 通用工具
```

## 扩展新渠道

实现 `com.ts7ming.notify.channel.NotifyChannel` 接口, 在 `ChannelManager.create` 中按 `type` 注册即可, 配置字段由各渠道自行从 `props` 中读取。

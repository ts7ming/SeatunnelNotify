# SeatunnelNotify 文档

SeaTunnel 事件动态通知插件：**动态订阅所有事件类型**，**动态选择接收方式**（钉钉 / 企业微信 / 飞书 / 邮箱），配置实时生效，无需重启。

## 特性

- **动态订阅事件**：通过配置文件按需开启监听
  - `job_status` 任务状态事件（成功 / 失败 / 取消，可指定仅某些状态通知）
  - `schema_change` 表结构变更事件（加列 / 删列 / 改列 / 改表）
  - `lifecycle` 读写生命周期事件（Reader / Writer / Enumerator 的 OPEN / CLOSE）
  - `others` 其他未归类事件
- **动态接收方式**：钉钉、企业微信、飞书、邮箱，可任意组合同时开启；支持钉钉 / 飞书加签
- **自定义消息模板**：每类事件均可配置 `title` / `content` 模板，支持占位符与 `\n` 换行
- **实时读取配置**：每次事件触发实时读取配置文件，内容变化即重新解析

## 环境要求

- SeaTunnel 2.3.x（Zeta 引擎）
- JDK 8+（构建）；Maven 3.6+

## 快速开始

```bash
# 1. 构建
mvn clean package
# 产物: target/seatunnel-notify.jar (依赖已打包并 relocation, 自包含)

# 2. 部署: 将 jar 放入 SeaTunnel 扩展目录, 通过 SPI 自动加载
cp target/seatunnel-notify.jar $SEATUNNEL_HOME/ext/

# 3. 配置: 编辑 $SEATUNNEL_HOME/config/seatunnelnotify.json
#    (不存在时插件首次触发事件会自动生成默认配置)
```

修改配置保存后**立即生效**，无需重启 SeaTunnel。

## 文档导航

| 文档 | 内容 |
| --- | --- |
| [快速开始](getting-started.md) | 构建、部署、本地验证、运行测试 |
| [配置参考](configuration.md) | 配置文件路径、JSON 结构、全部字段说明 |
| [消息模板](message-templates.md) | `title` / `content` 自定义、占位符、`\n` 换行 |
| [通知渠道](channels.md) | 钉钉 / 企业微信 / 飞书 / 邮箱的参数详解 |
| [事件类型](event-types.md) | SeaTunnel 全部事件类型与分类映射 |
| [常见问题](faq.md) | 配置不生效、收不到通知、解析失败等 |
| [公众号教程](wechat-tutorial.md) | 通俗技术向的项目介绍文章（适合发布于微信公众号） |

## 工作原理

```
SeaTunnel Event
   │
   ▼
EventNotifyHandler (SPI 加载)
   │  实时读取 config/seatunnelnotify.json
   ▼
EventClassifier ──► 分类: JOB / SCHEMA / LIFECYCLE / OTHER
   │
   ▼
NotifyConfig.shouldNotify ──► 订阅开关 + 状态过滤
   │
   ▼
MessageBuilder ──► 渲染 title / content (默认文案或自定义模板)
   │
   ▼
ChannelManager ──► 钉钉 / 企业微信 / 飞书 / 邮箱 (可多选)
```

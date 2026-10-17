# 常见问题

## 配置改了但不生效？

1. 确认路径是 **`$SEATUNNEL_HOME/config/seatunnelnotify.json`**（不是 `ext/config.yaml`，旧版本路径已废弃）
2. 确认运行 SeaTunnel 的环境里 `SEATUNNEL_HOME` 环境变量/系统属性指向正确
3. 配置是实时读取的，保存后下次事件触发即生效，无需重启
4. 确认 JSON 语法正确——解析失败会回退为空配置（全部关闭）并在日志报错

## 收不到任何通知？

按顺序检查：

1. 根字段 `"enabled": true`
2. 对应事件分类的 `enabled` 为 `true`（注意 `lifecycle` 和 `others` **默认关闭**）
3. `job_status` 若配了 `statuses`，确认任务状态在列表内（忽略大小写）；留空表示全部状态
4. `channels` 下至少一个渠道 `enabled: true` 且参数完整（webhook / host + to）
5. 查看 SeaTunnel 日志中的发送结果（单个渠道失败不影响其他渠道）
6. 网络：SeaTunnel 服务器能否访问机器人 webhook / SMTP 服务器

## lifecycle 事件不触发？

`lifecycle` 分类默认 `enabled: false`，需要在配置中显式开启。生命周期事件（Reader/Writer OPEN/CLOSE）发生频繁，开启后消息量可能很大。

## JSON 解析失败/配置被回退？

- 检查 JSON 语法（逗号、引号、括号）
- 字符串中的换行必须写成 `\n` 转义，不能直接换行
- 用 Windows 记事本编辑后确认编码为 UTF-8（解析器已兼容 BOM，但其他异常字符仍会导致失败）
- 失败时插件使用空配置（全部关闭），修复后保存即恢复

## 模板不生效/占位符没被替换？

- 字段名是 `title` 和 `content`，位于 `events.<分类>` 下
- 占位符**区分大小写**：`{event_type}`、`{job_status}`、`{table}` 等，花括号不能省
- 空串 / `null` / 不填 → 回退到该分类的内置默认文案
- 缺失字段替换为空字符串，例如 lifecycle 事件中使用 `{job_name}` 会得到空值

## 消息里的换行丢了？

确认 content 中使用的是 `\n` 转义（JSON 字符串内），例如：

```json
"content": "第一行\n第二行"
```

## 邮件发送失败？

- `host` 和 `to` 必填，任一缺失则跳过发送
- `ssl: true` 对应 SSL（常用 465 端口）；`ssl: false` 走 STARTTLS（常用 25/587 端口）
- `username` 为空则不进行 SMTP 认证，适用于内网匿名中继

## 如何本地验证渠道配置？

不启动 SeaTunnel，直接用内置入口发送一条模拟的「任务失败」事件：

```bash
mvn exec:java
```

或查看单元测试与端到端测试：

```bash
mvn test
```

## 旧版本的 ext/config.yaml 还能用吗？

不能。自配置迁移为 JSON 后，插件只读取 `$SEATUNNEL_HOME/config/seatunnelnotify.json`，请将旧 YAML 配置手动转换为 JSON（结构不变，仅格式与路径变化）。

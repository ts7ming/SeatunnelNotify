# 通知渠道

支持钉钉、企业微信、飞书、邮箱四种渠道，**可同时开启多个**，同一条通知会发送到所有已启用渠道。

## 渠道类型识别

渠道类型由 `channels` 下的 key 名自动识别，也可用 `type` 字段显式指定（覆盖 key 名）：

| 渠道 | 识别关键字（key 或 type，忽略大小写） |
| --- | --- |
| 钉钉 | `ding`，如 `dingtalk` |
| 企业微信 | `wechat`、`workwx`、`企业微信` |
| 飞书 | `feishu`、`lark`、`飞书` |
| 邮箱 | `email`、`mail`、`邮箱` |

无法识别的类型会被忽略（不发送也不报错）。

## 钉钉

自定义机器人，支持加签。

```json
"dingtalk": {
  "enabled": true,
  "webhook": "https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN",
  "secret": "YOUR_SECRET"
}
```

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `webhook` | string | 机器人 Webhook 地址，留空则跳过发送 |
| `secret` | string | 加签密钥（机器人安全设置中的「加签」），留空表示不使用加签 |

加签方式：对 `timestamp + "\n" + secret` 做 HmacSHA256，Base64 后 URL 编码，作为 `timestamp`、`sign` 查询参数拼接在 webhook 上。

## 企业微信

群机器人，无需加签。

```json
"wechat": {
  "enabled": true,
  "webhook": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY"
}
```

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `webhook` | string | 机器人 Webhook 地址，留空则跳过发送 |

## 飞书

自定义机器人，支持签名校验。

```json
"feishu": {
  "enabled": true,
  "webhook": "https://open.feishu.cn/open-apis/bot/v2/hook/YOUR_HOOK",
  "secret": "YOUR_SECRET"
}
```

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `webhook` | string | 机器人 Webhook 地址，留空则跳过发送 |
| `secret` | string | 签名校验密钥（安全设置 → 签名校验），留空表示不使用 |

与钉钉的差异：时间戳为**秒级**；加签算法为 HmacSHA256（key = `timestamp + "\n" + secret`，对空消息体签名）后 Base64；`timestamp` / `sign` 放在**请求体**中而非 URL 参数。

## 邮箱

基于 JavaMail 的 SMTP 发送。

```json
"email": {
  "enabled": true,
  "host": "smtp.example.com",
  "port": 465,
  "username": "user@example.com",
  "password": "your_password",
  "from": "user@example.com",
  "to": ["admin@example.com", "ops@example.com"],
  "subject": "SeaTunnel 事件通知",
  "ssl": true
}
```

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `host` | string | 无 | SMTP 服务器地址，留空则跳过发送 |
| `port` | int | `25` | SMTP 端口 |
| `username` | string | 无 | 登录用户名；为空则不认证 |
| `password` | string | 无 | 登录密码 |
| `from` | string | 取 `username` | 发件人地址 |
| `to` | string[] | 无 | 收件人列表，为空则跳过发送 |
| `subject` | string | 取消息标题 | 邮件主题 |
| `ssl` | bool | `true` | `true` 走 SSL（如 465 端口）；`false` 走 STARTTLS |

## 同时启用多个渠道

```json
"channels": {
  "dingtalk": {
    "enabled": true,
    "webhook": "https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN",
    "secret": "YOUR_SECRET"
  },
  "email": {
    "enabled": true,
    "host": "smtp.example.com",
    "to": ["admin@example.com"],
    "subject": "SeaTunnel 事件通知",
    "ssl": true
  }
}
```

## 发送结果判断

- 机器人 webhook（钉钉 / 企微 / 飞书）业务失败时也返回 HTTP 200，真正成败在响应体的错误码：仅当 HTTP 200 且 `errcode` / `code` / `StatusCode` 为 0（或无错误码字段）才算成功
- 单个渠道发送失败不影响其他渠道，异常会记录在 SeaTunnel 日志中

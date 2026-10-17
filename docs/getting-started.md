# 快速开始

## 构建

```bash
mvn clean package
```

产物为 `target/seatunnel-notify.jar`：

- 基于 maven-shade-plugin 打成 uber-jar，插件自包含
- 对 `jackson`、`javax.mail` 等第三方依赖做了 relocation（`com.ts7ming.notify.shaded.*`），不会与 SeaTunnel 自带依赖冲突

## 部署

1. 将 `seatunnel-notify.jar` 放入 SeaTunnel 的扩展目录，例如 `$SEATUNNEL_HOME/ext/`
2. SeaTunnel 通过 SPI（`META-INF/services/org.apache.seatunnel.api.event.EventHandler`）自动加载本插件，无需额外配置
3. 配置文件路径为 `$SEATUNNEL_HOME/config/seatunnelnotify.json`：
   - 不存在时，插件首次触发事件会自动生成默认配置（含全部渠道模板）
   - 已存在则直接使用，修改保存后立即生效

## 本地验证（不启动 SeaTunnel）

项目内置本地测试入口 `NotifyTestMain`：读取配置，构造一条模拟的「任务失败」事件，发送到所有已启用的渠道。

```bash
# 方式一: Maven
mvn exec:java

# 方式二: 直接用打好的 jar (需设置 SEATUNNEL_HOME 指向含 config/seatunnelnotify.json 的目录)
#   Linux/macOS:  SEATUNNEL_HOME=/path/to/seatunnel java -cp target/seatunnel-notify.jar com.ts7ming.notify.NotifyTestMain
#   Windows:      set SEATUNNEL_HOME=D:\path\to\seatunnel && java -cp target\seatunnel-notify.jar com.ts7ming.notify.NotifyTestMain
```

输出示例：

```
[NotifyTest] 标题: 【任务FAILED】local-test-job
[NotifyTest] 内容:
事件类型: JOB_STATUS
任务ID: TEST-1792202856729
状态: FAILED
[NotifyTest] 已启用渠道: dingtalk
[NotifyTest] 渠道 [dingtalk] 发送成功
```

常见退出提示：

- `配置未启用(enabled=false)或加载失败`：检查 JSON 中 `enabled: true`，以及文件路径是否为 `$SEATUNNEL_HOME/config/seatunnelnotify.json`
- `未启用任何通知渠道`：在 `channels` 下开启至少一个渠道并填写 webhook

## 运行单元测试

```bash
mvn test
```

测试覆盖 SeaTunnel 2.3.13 全部 13 种事件类型的分类、消息构建、订阅过滤、渠道识别，以及基于本地 HTTP 服务器的端到端发送链路（不依赖外网）。

## 项目结构

```
com.ts7ming.notify
 ├─ EventNotifyHandler        # 主处理器 (EventHandler 实现, SPI 入口)
 ├─ config/                   # 配置路径 / 模型 / 加载器 / 默认配置
 ├─ model/                    # 事件上下文 / 消息 / 分类
 ├─ core/                     # 事件分类 / 消息构建 / 渠道管理 / 反射工具
 ├─ channel/                  # 钉钉 / 企业微信 / 飞书 / 邮箱 渠道实现
 └─ util/                     # HTTP / 签名 / 通用工具
```

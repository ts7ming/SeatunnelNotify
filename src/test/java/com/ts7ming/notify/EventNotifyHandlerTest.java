package com.ts7ming.notify;

import com.sun.net.httpserver.HttpServer;
import com.ts7ming.notify.config.ConfigLoader;
import com.ts7ming.notify.config.ConfigPaths;
import org.apache.seatunnel.api.source.event.MessageDelayedEvent;
import org.apache.seatunnel.api.source.event.ReaderOpenEvent;
import org.apache.seatunnel.engine.common.job.JobStateEvent;
import org.apache.seatunnel.engine.common.job.JobStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 处理器端到端测试: 通过本地 HTTP 服务器捕获钉钉 webhook 请求,
 * 验证 "配置 -> 事件分类 -> 订阅过滤 -> 消息构建 -> 渠道发送" 全链路。
 *
 * <p>不依赖外网: webhook 指向 127.0.0.1 的临时端口。
 */
public class EventNotifyHandlerTest {

    private static final String WEBHOOK_PATH = "/robot/send";

    private static File homeDir;
    private static HttpServer server;
    private static final List<String> received = new CopyOnWriteArrayList<>();

    private String originalHomeProperty;

    @BeforeClass
    public static void setUpClass() throws IOException {
        homeDir = Files.createTempDirectory("seatunnel-notify-test").toFile();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = readAll(exchange.getRequestBody());
            received.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
                    + new String(body, StandardCharsets.UTF_8));
            byte[] resp = "{\"errcode\":0,\"errmsg\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
    }

    @AfterClass
    public static void tearDownClass() {
        if (server != null) {
            server.stop(0);
        }
        deleteRecursively(homeDir);
    }

    @Before
    public void setUp() throws Exception {
        originalHomeProperty = System.getProperty("SEATUNNEL_HOME");
        System.setProperty("SEATUNNEL_HOME", homeDir.getAbsolutePath());
        received.clear();
        clearConfigLoaderCache();
    }

    @After
    public void tearDown() {
        if (originalHomeProperty == null) {
            System.clearProperty("SEATUNNEL_HOME");
        } else {
            System.setProperty("SEATUNNEL_HOME", originalHomeProperty);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** ConfigLoader 有静态缓存, 每个用例前清空以保证实时读取测试配置。 */
    private static void clearConfigLoaderCache() throws Exception {
        for (String name : new String[] {"cachedBytes", "cachedConfig"}) {
            Field f = ConfigLoader.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, null);
        }
    }

    private static String webhook() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + WEBHOOK_PATH;
    }

    private static void writeConfig(String yaml) throws IOException {
        // 写到 ConfigPaths 实际解析出的路径 ($SEATUNNEL_HOME/ext/config.yaml)
        File file = ConfigPaths.resolveConfigFile();
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static void awaitRequest() {
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline && received.isEmpty()) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void assertNoRequest() {
        try {
            Thread.sleep(400L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue("不应产生任何 webhook 请求, 实际收到: " + received, received.isEmpty());
    }

    private static void deleteRecursively(File f) {
        if (f == null) {
            return;
        }
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                deleteRecursively(k);
            }
        }
        f.delete();
    }

    // ---------------------------------------------------------------- 用例

    @Test
    public void handleNullEvent_noOp() {
        new EventNotifyHandler().handle(null);
        assertNoRequest();
    }

    @Test
    public void handle_disabledConfig_noRequest() throws Exception {
        writeConfig(
                "enabled: false\n"
                        + "events:\n  job_status:\n    enabled: true\n"
                        + "channels:\n  dingtalk:\n    enabled: true\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new JobStateEvent(1L, "job-a", JobStatus.FAILED));

        assertNoRequest();
    }

    @Test
    public void handle_jobFailed_sendsWebhook() throws Exception {
        writeConfig(
                "enabled: true\n"
                        + "events:\n  job_status:\n    enabled: true\n"
                        + "channels:\n  dingtalk:\n    enabled: true\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new JobStateEvent(9527L, "sync-mysql-to-doris", JobStatus.FAILED));

        awaitRequest();
        assertEquals(1, received.size());
        String request = received.get(0);
        assertTrue(request.startsWith("POST " + WEBHOOK_PATH));
        assertTrue(request.contains("\"msgtype\":\"text\""));
        assertTrue(request.contains("【任务失败】"));
        assertTrue(request.contains("任务ID: 9527"));
        assertTrue(request.contains("任务名称: sync-mysql-to-doris"));
    }

    @Test
    public void handle_jobFinished_sendsWebhook() throws Exception {
        writeConfig(
                "enabled: true\n"
                        + "events:\n  job_status:\n    enabled: true\n"
                        + "channels:\n  dingtalk:\n    enabled: true\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new JobStateEvent(1L, "job-a", JobStatus.FINISHED));

        awaitRequest();
        assertEquals(1, received.size());
        assertTrue(received.get(0).contains("【任务完成】"));
    }

    @Test
    public void handle_statusFilterMismatch_noRequest() throws Exception {
        writeConfig(
                "enabled: true\n"
                        + "events:\n  job_status:\n    enabled: true\n    statuses: [FAILED]\n"
                        + "channels:\n  dingtalk:\n    enabled: true\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new JobStateEvent(1L, "job-a", JobStatus.RUNNING));

        assertNoRequest();
    }

    @Test
    public void handle_unsubscribedCategory_noRequest() throws Exception {
        // 默认配置 others 关闭, MessageDelayedEvent 归入 OTHER
        writeConfig(
                "enabled: true\n"
                        + "events:\n  job_status:\n    enabled: true\n"
                        + "channels:\n  dingtalk:\n    enabled: true\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new MessageDelayedEvent(1000L, "r"));

        assertNoRequest();
    }

    @Test
    public void handle_lifecycleSubscribed_sendsWebhook() throws Exception {
        // lifecycle 订阅开启时, 真实的 LIFECYCLE_READER_OPEN 事件应能触达通知
        writeConfig(
                "enabled: true\n"
                        + "events:\n  job_status:\n    enabled: false\n  lifecycle:\n    enabled: true\n"
                        + "channels:\n  dingtalk:\n    enabled: true\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new ReaderOpenEvent());

        awaitRequest();
        assertEquals(1, received.size());
        String request = received.get(0);
        assertTrue(request.contains("【读写生命周期】"));
        assertTrue(request.contains("LIFECYCLE_READER_OPEN"));
    }

    @Test
    public void handle_lifecycleNotSubscribed_noRequest() throws Exception {
        // lifecycle 订阅关闭 (默认), 生命周期事件不应发送
        writeConfig(
                "enabled: true\n"
                        + "events:\n  job_status:\n    enabled: true\n  lifecycle:\n    enabled: false\n"
                        + "channels:\n  dingtalk:\n    enabled: true\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new ReaderOpenEvent());

        assertNoRequest();
    }

    @Test
    public void handle_noChannelEnabled_noRequest() throws Exception {
        writeConfig(
                "enabled: true\n"
                        + "events:\n  job_status:\n    enabled: true\n"
                        + "channels:\n  dingtalk:\n    enabled: false\n    webhook: " + webhook() + "\n");

        new EventNotifyHandler().handle(new JobStateEvent(1L, "job-a", JobStatus.FAILED));

        assertNoRequest();
    }
}

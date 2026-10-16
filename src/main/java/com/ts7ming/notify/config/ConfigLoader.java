package com.ts7ming.notify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * 配置加载器。
 * 每次事件触发都会调用 {@link #load()}: 实时读取配置文件内容, 仅当内容发生变化时才重新解析,
 * 从而兼顾 "实时读取" 与 "解析性能"。
 */
public final class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private static volatile byte[] cachedBytes;
    private static volatile NotifyConfig cachedConfig;
    private static final Object LOCK = new Object();

    private ConfigLoader() {
    }

    public static NotifyConfig load() {
        File file = ConfigPaths.resolveConfigFile();
        if (!file.exists()) {
            try {
                DefaultConfig.writeIfMissing(file);
            } catch (Exception e) {
                log.error("创建默认配置文件失败: {}", file.getAbsolutePath(), e);
            }
        }
        if (!file.exists() || !file.canRead()) {
            return new NotifyConfig();
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (cachedConfig != null && cachedBytes != null && Arrays.equals(bytes, cachedBytes)) {
                return cachedConfig;
            }
            synchronized (LOCK) {
                if (cachedConfig != null && cachedBytes != null && Arrays.equals(bytes, cachedBytes)) {
                    return cachedConfig;
                }
                NotifyConfig cfg = NotifyConfig.fromYaml(bytes);
                cachedBytes = bytes;
                cachedConfig = cfg;
                return cfg;
            }
        } catch (Exception e) {
            log.error("读取配置文件失败: {}", file.getAbsolutePath(), e);
            return new NotifyConfig();
        }
    }
}

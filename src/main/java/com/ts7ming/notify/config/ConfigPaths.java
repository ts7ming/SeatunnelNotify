package com.ts7ming.notify.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 解析配置文件路径: {@code $SEATUNNEL_HOME/ext/config.yaml}
 * 当环境变量 / 系统属性 SEATUNNEL_HOME 不存在时, 回退到当前工作目录。
 */
public final class ConfigPaths {

    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static final String EXT_DIR = "ext";

    private ConfigPaths() {
    }

    public static File resolveConfigFile() {
        String home = System.getenv("SEATUNNEL_HOME");
        if (isBlank(home)) {
            home = System.getProperty("SEATUNNEL_HOME");
        }
        if (isBlank(home)) {
            home = ".";
        }
        Path dir = Paths.get(home, EXT_DIR);
        return dir.resolve(CONFIG_FILE_NAME).toFile();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

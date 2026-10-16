package com.ts7ming.notify.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 通用工具: 空值处理、空白判断、时间格式化。
 */
public final class Common {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Common() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String nvl(String s) {
        return s == null ? "-" : s;
    }

    public static String formatTime(long ms) {
        if (ms <= 0) {
            return "-";
        }
        try {
            return FMT.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()));
        } catch (Exception e) {
            return String.valueOf(ms);
        }
    }
}

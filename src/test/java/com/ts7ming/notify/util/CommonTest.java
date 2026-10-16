package com.ts7ming.notify.util;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 通用工具测试: 空值处理与时间格式化。
 */
public class CommonTest {

    private static final Pattern TIME_PATTERN =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

    @Test
    public void isBlank() {
        assertTrue(Common.isBlank(null));
        assertTrue(Common.isBlank(""));
        assertTrue(Common.isBlank("   "));
        assertFalse(Common.isBlank("a"));
        assertFalse(Common.isBlank(" a "));
    }

    @Test
    public void nvl() {
        assertEquals("-", Common.nvl(null));
        assertEquals("", Common.nvl(""));
        assertEquals("abc", Common.nvl("abc"));
    }

    @Test
    public void formatTime() {
        assertEquals("-", Common.formatTime(0L));
        assertEquals("-", Common.formatTime(-1L));

        String formatted = Common.formatTime(1700000000000L);
        assertTrue("unexpected format: " + formatted, TIME_PATTERN.matcher(formatted).matches());
    }
}

package com.ts7ming.notify.core;

/**
 * 轻量反射工具。
 * 用于从 SeaTunnel 事件对象中安全提取字段 (避免对具体事件实现类的硬依赖, 提升兼容性)。
 */
public final class ReflectUtil {

    private ReflectUtil() {
    }

    public static Object invoke(Object obj, String methodName) {
        if (obj == null || methodName == null) {
            return null;
        }
        try {
            return obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static String str(Object obj, String methodName) {
        Object v = invoke(obj, methodName);
        return v == null ? null : String.valueOf(v);
    }
}

package com.ucacue.bar.config;

public final class SqlQueryCounter {

    private static final ThreadLocal<Long> QUERY_COUNT = ThreadLocal.withInitial(() -> 0L);

    private SqlQueryCounter() {
    }

    public static void increment() {
        QUERY_COUNT.set(QUERY_COUNT.get() + 1);
    }

    public static long getCount() {
        return QUERY_COUNT.get();
    }

    public static void reset() {
        QUERY_COUNT.set(0L);
    }

    public static void clear() {
        QUERY_COUNT.remove();
    }
}

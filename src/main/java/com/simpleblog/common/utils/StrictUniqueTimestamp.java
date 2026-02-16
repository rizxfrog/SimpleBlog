package com.simpleblog.common.utils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class StrictUniqueTimestamp {
    private static final AtomicLong LAST_TIMESTAMP = new AtomicLong(0L);
    private static final AtomicLong SEQUENCE = new AtomicLong(0L);

    public static String next() {
        long now = System.currentTimeMillis();
        long last = LAST_TIMESTAMP.get();
        if (now != last) {
            LAST_TIMESTAMP.set(now);
            SEQUENCE.set(0L);
        }
        long seq = SEQUENCE.incrementAndGet();
        if (seq > 9999) {
//            throw new RuntimeException("Sequence overflow");
            while (now == LAST_TIMESTAMP.get()) {
                now = System.currentTimeMillis();
            }
            LAST_TIMESTAMP.set(now);
            SEQUENCE.set(0L);
            seq = SEQUENCE.incrementAndGet();
        }
        return Instant.ofEpochMilli( now).toString() + "-" + String.format("%04d", seq);
    }
}

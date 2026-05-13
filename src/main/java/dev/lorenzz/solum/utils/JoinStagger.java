package dev.lorenzz.solum.utils;

import java.util.concurrent.atomic.AtomicInteger;

public final class JoinStagger {

    private final AtomicInteger counter = new AtomicInteger();
    private final int spread;

    public JoinStagger(int spread) {
        this.spread = Math.max(1, spread);
    }

    public long nextDelay() {
        int i = counter.getAndIncrement();
        return 1L + Math.floorMod(i, spread);
    }
}


package net.hollowcube.terraform.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadUtil {

    public static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String name;

        public NamedThreadFactory(@NotNull String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(r, name + "-" + threadNumber.getAndIncrement());
        }
    }

    public static void testInterrupt() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
    }

    private ThreadUtil() {
    }
}

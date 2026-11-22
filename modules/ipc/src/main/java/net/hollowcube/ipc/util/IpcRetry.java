package net.hollowcube.ipc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Repeats an idempotent ipc write that failed without being refused: no answer at all, or a 5xx such
/// as an api-server draining for a deploy. A failed attempt may still have landed, so a 409 on a
/// repeat is taken as that attempt having done the write.
public final class IpcRetry {
    private static final Logger logger = LoggerFactory.getLogger(IpcRetry.class);
    private static final long[] DELAYS_MILLIS = {250, 1000, 3000};

    private IpcRetry() {}

    public static void run(Runnable call) {
        for (int attempt = 0;; attempt++) {
            try {
                call.run();
                return;
            } catch (IpcException e) {
                if (attempt > 0 && e.status() == 409) return;
                if (!e.isTransient() || attempt >= DELAYS_MILLIS.length) throw e;
                logger.warn("{} failed, retrying: {}", e.call(), e.getMessage());
            }
            try {
                Thread.sleep(DELAYS_MILLIS[attempt]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while retrying", e);
            }
        }
    }
}

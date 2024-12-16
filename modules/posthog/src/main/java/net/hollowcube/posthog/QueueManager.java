package net.hollowcube.posthog;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jctools.queues.MpscArrayQueue;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

class QueueManager {
    private final MpscArrayQueue<JsonObject> queue;
    private final Consumer<JsonArray> sender;
    private final long sleepNs;

    private final Thread thread;
    private volatile boolean stop = false;

    QueueManager(@NotNull Consumer<JsonArray> sender, int maxQueueSize, @NotNull Duration maxTimeInQueue) {
        this.queue = new MpscArrayQueue<>(maxQueueSize * 2);
        this.sender = sender;
        this.sleepNs = maxTimeInQueue.toNanos();

        this.thread = Thread.startVirtualThread(this::sendLoop);
    }

    public void add(@NotNull JsonObject eventJson) {
        boolean inserted = queue.offerIfBelowThreshold(eventJson, queue.capacity() / 2);
        if (inserted) return;

        // Too many events, need to sync immediately
        LockSupport.unpark(this.thread);
        queue.offer(eventJson); // Reinsert the event now
    }

    public void shutdown() {
        this.stop = true;
        LockSupport.unpark(this.thread);
        try {
            this.thread.join();
        } catch (InterruptedException ignored) {
            // Do nothing just exit
        }
    }

    private void sendLoop() {
        do {
            LockSupport.parkNanos(this.sleepNs);

            JsonArray toSend = new JsonArray();
            queue.drain(toSend::add);
            this.sender.accept(toSend);
        } while (!stop);
    }
}

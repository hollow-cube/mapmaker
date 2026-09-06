package net.hollowcube.ipc.util;

import java.util.function.BiConsumer;

/// Where a generated server sends a failure it answered 500 for, beyond its own log.
///
/// The process hosting the servers decides what that is — the api-server reports them to PostHog —
/// and until it has said, nothing is sent anywhere. A caller's mistake, which is answered 4xx, is
/// never reported: only an implementation that threw.
public final class IpcFailures {

    private static volatile BiConsumer<String, Throwable> reporter = (_, _) -> {};

    /// `reporter` is given the ipc path that failed and what the implementation threw.
    public static void reportTo(BiConsumer<String, Throwable> reporter) {
        IpcFailures.reporter = reporter;
    }

    public static void report(String ipcPath, Throwable thrown) {
        reporter.accept(ipcPath, thrown);
    }

    private IpcFailures() {}
}

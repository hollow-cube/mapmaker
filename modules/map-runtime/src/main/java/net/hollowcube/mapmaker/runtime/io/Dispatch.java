package net.hollowcube.mapmaker.runtime.io;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.List;

/// Synchronous FIFO dispatch queue for signals.
public final class Dispatch {
    private static final Logger logger = LoggerFactory.getLogger(Dispatch.class);

    static final int MAX_SIGNALS = 256; // per drain

    public record Source(String id, Point position) {}

    public record Delivery(Player player, PlayState state, Source source) {}

    private record Signal(Source source, Connection connection) {}

    private final Player activator;
    private final PlayState state;
    private final ArrayDeque<Signal> queue = new ArrayDeque<>();

    public Dispatch(Player activator, PlayState state) {
        this.activator = activator;
        this.state = state;
    }

    /// Queues the connections on `output`, in the order they appear in `connections`.
    public void fire(Source source, Key output, List<Connection> connections) {
        for (var connection : connections) {
            if (connection.output().equals(output)) queue.add(new Signal(source, connection));
        }
    }

    public void drain() {
        int delivered = 0;
        while (!queue.isEmpty()) {
            var signal = queue.poll();
            if (++delivered > MAX_SIGNALS) {
                logger.warn(
                    "signal limit hit for {}, dropped {} signals (loop through {}?)",
                    activator.getUsername(),
                    queue.size() + 1,
                    signal.source().id()
                );
                queue.clear();
                return;
            }
            try {
                signal.connection()
                    .params()
                    .deliver(new Delivery(activator, state, signal.source()));
            } catch (Exception e) {
                ExceptionReporter.reportException(e, activator);
            }
        }
    }
}

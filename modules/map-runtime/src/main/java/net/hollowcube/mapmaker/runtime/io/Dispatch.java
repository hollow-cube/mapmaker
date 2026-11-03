package net.hollowcube.mapmaker.runtime.io;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.List;

/// Synchronous FIFO dispatch queue for signals.
public final class Dispatch {
    private static final Logger logger = LoggerFactory.getLogger(Dispatch.class);

    static final int MAX_SIGNALS = 256; // per drain

    private record Signal(String caller, Connection connection) {}

    private final Player activator;
    private final PlayState state;
    private final ArrayDeque<Signal> queue = new ArrayDeque<>();

    public Dispatch(Player activator, PlayState state) {
        this.activator = activator;
        this.state = state;
    }

    public void fire(String caller, List<Connection> output) {
        for (var connection : output) queue.add(new Signal(caller, connection));
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
                    signal.caller()
                );
                queue.clear();
                return;
            }
            try {
                signal.connection().params().applyTo(activator, state);
            } catch (Exception e) {
                ExceptionReporter.reportException(e, activator);
            }
        }
    }
}

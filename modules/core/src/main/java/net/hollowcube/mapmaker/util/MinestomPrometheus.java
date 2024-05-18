package net.hollowcube.mapmaker.util;

import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleSupplier;

@SuppressWarnings("UnstableApiUsage")
public class MinestomPrometheus {

    public static void init() {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        // Player counts
        var playersOnline = Gauge.build()
                .name("minestom_players_online")
                .help("Number of players online")
                .labelNames("state")
                .register();
        playersOnline.setChild(gaugeSupplier(() -> MinecraftServer.getConnectionManager().getOnlinePlayers().size()), "play");
        playersOnline.setChild(gaugeSupplier(() -> MinecraftServer.getConnectionManager().getConfigPlayers().size()), "config");

        // Instance & chunk counts
        var instances = MinecraftServer.getInstanceManager();
        var instanceCount = Gauge.build()
                .name("minestom_instance_count")
                .help("Number of instances registered")
                .register();
        instanceCount.setChild(gaugeSupplier(() -> instances.getInstances().size()));
        var chunkCount = Gauge.build()
                .name("minestom_chunk_count")
                .help("Number of chunks loaded in instances")
                .register();
        chunkCount.setChild(gaugeSupplier(() -> instances.getInstances().stream().mapToInt(i -> i.getChunks().size()).sum()));
        var entityCount = Gauge.build()
                .name("minestom_entity_count")
                .help("Number of entities in instances")
                .register();
        entityCount.setChild(gaugeSupplier(() -> instances.getInstances().stream().mapToInt(i -> i.getEntities().size()).sum()));

        // Tick thread info
        var dispatcher = MinecraftServer.process().dispatcher();
        var tickThreadElements = Gauge.build()
                .name("minestom_tick_thread_elements")
                .help("Number of elements in each tick thread")
                .labelNames("index")
                .register();
        var tickThreadTick = Gauge.build()
                .name("minestom_tick_thread_tick")
                .help("Current tick number for each tick thread")
                .labelNames("index")
                .register();
        var threads = dispatcher.threads();
        for (int i = 0; i < threads.size(); i++) {
            final var thread = threads.get(i);
            tickThreadElements.setChild(gaugeSupplier(() -> thread.entries().size()), String.valueOf(i));
            tickThreadTick.setChild(gaugeSupplier(thread::getTick), String.valueOf(i));
        }

        // Tick timing
        var tickTimer = Histogram.build()
                .name("minestom_tick_duration_milliseconds")
                .help("Duration of each tick (mspt)")
                .register();
        var acquisitionTimer = Histogram.build()
                .name("minestom_acquisition_duration_milliseconds")
                .help("Acq duration of each tick")
                .register();
        globalEventHandler.addListener(ServerTickMonitorEvent.class, event -> {
            tickTimer.observe(event.getTickMonitor().getTickTime());
            acquisitionTimer.observe(event.getTickMonitor().getAcquisitionTime());
        });

    }

    private static @NotNull Gauge.Child gaugeSupplier(@NotNull DoubleSupplier func) {
        return new Gauge.Child() {
            @Override
            public double get() {
                return func.getAsDouble();
            }
        };
    }
}

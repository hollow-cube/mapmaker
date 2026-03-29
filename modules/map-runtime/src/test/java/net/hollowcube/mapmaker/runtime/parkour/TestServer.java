package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerServiceImpl;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestServer {
    static {
        System.setProperty("minestom.chunk-view-distance", "16");
    }

    static void main(String[] args) {
        var server = MinecraftServer.init();

        CompatProvider.load(MinecraftServer.getGlobalEventHandler());

        DataFixer.buildModel();

        MapEntities.init(EventNode.type("dummy", EventFilter.INSTANCE));

        var commandManager = new CommandManagerImpl();

        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) ->
            new MapPlayer(connection, gameProfile) {
                @Override
                public @NotNull CommandManager getCommandManager() {
                    return commandManager;
                }
            });

        ParkourMapWorld.initGlobalReferences();

        var map = AbstractHttpService.GSON.fromJson("""
            {"clearRate":0.5602294,"createdAt":"2025-03-04T01:06:57.25622Z","difficulty":"medium","id":"65d42cdd-d044-432e-981f-e320e997f63b","lastModified":"2025-03-05T01:12:49.7626Z","likes":251,"objects":null,"owner":"503450fc-72c2-4e87-8243-94e264977437","protocolVersion":769,"publishedAt":"2025-03-05T01:12:49.7626Z","publishedId":749416779,"quality":"outstanding","settings":{"extra":{"boat":false,"no_jump":false,"no_sneak":false,"no_sprint":false,"only_sprint":true,"reset_in_water":true},"icon":"minecraft:dead_bush","name":"Glide: Canyon","size":"colossal","spawnPoint":{"pitch":-4.799709,"x":-22.531082,"y":172,"yaw":0.45348036,"z":-351.61002},"subvariant":"speedrun","tags":["exploration"],"variant":"parkour"},"uniquePlays":523,"verification":"verified"}
            """, MapData.class);


        var mapServer = new MockMapServer();
        mapServer.mapService = new MapServiceImpl("http://localhost:9125") {
            @Override
            public byte @Nullable [] getMapWorld(String id, boolean write) {
                try {
                    return Files.readAllBytes(Path.of("/Users/matt/Downloads/65d42cdd-d044-432e-981f-e320e997f63b-3"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        mapServer.playerService = new PlayerServiceImpl(io.opentelemetry.api.OpenTelemetry.noop(), "http://localhost:9127");
        mapServer.bridge = new NoopServerBridge();

        var world = new ParkourMapWorld(mapServer, map);
        world.loadWorld();

        MinecraftServer.getGlobalEventHandler()
            .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                final var player = event.getPlayer();
                player.setTag(PlayerData.TAG, new PlayerData(player));

                world.configurePlayer(event);
            })
            .addListener(PlayerSpawnEvent.class, event -> {
                if (!event.isFirstSpawn()) return;
                world.spawnPlayer(event.getPlayer());
            })
            .addListener(PlayerDisconnectEvent.class, event -> {
                var exitWorld = MapWorld.forPlayer(event.getPlayer());
                if (exitWorld == null) return;

                exitWorld.removePlayer(event.getPlayer());
            });

        MinecraftServer.getSchedulerManager()
            .scheduleEndOfTick(new Runnable() {
                @Override
                public void run() {
                    world.safePointTick();
                    MinecraftServer.getSchedulerManager()
                        .scheduleEndOfTick(this);

                }
            });
//        MinecraftServer.getSchedulerManager()
//                .buildTask(world::safePointTick)
//                .repeat(TaskSchedule.tick(1))
//                .schedule();
//        MinecraftServer.getSchedulerManager()
//                .buildShutdownTask(world::close);

        server.start("0.0.0.0", 25565);
    }

}

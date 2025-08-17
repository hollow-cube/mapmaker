package net.hollowcube.mapmaker.editor;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.editor.command.TestCommand;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerServiceImpl;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.ServerStatsHud;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

public class TestServer {
    static {
        System.setProperty("minestom.chunk-view-distance", "16");
    }

    public static void main(String[] args) {
        var server = MinecraftServer.init();

        CompatProvider.load(MinecraftServer.getGlobalEventHandler());

        DataFixer.addFixVersions(MapServerRunner.extraDataVersionsForMaps());
        DataFixer.buildModel();

        MapEntities.initNoEvents();

        var commandManager = new CommandManagerImpl();
        commandManager.register(new TestCommand());

        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) ->
                new MapPlayer(connection, gameProfile) {
                    @Override
                    public CommandManager getCommandManager() {
                        return commandManager;
                    }
                });

        ParkourMapWorld.initGlobalReferences();

        var mapServer = new MockMapServer();
        mapServer.mapService = new MapServiceImpl("http://localhost:9125");
        mapServer.playerService = new PlayerServiceImpl(io.opentelemetry.api.OpenTelemetry.noop(), "http://localhost:9126");
        mapServer.bridge = new NoopServerBridge();

        var map = new MapData("fe048ae8-8d46-475f-ba6b-d2e87c83e649",
                "aceb326f-da15-45bc-bf2f-11940c21780c");

        var world = new EditorMapWorld(mapServer, map);
        world.loadWorld();

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    final var player = event.getPlayer();
                    player.setTag(PlayerData.TAG, new PlayerData(player));

                    world.configurePlayer(event);
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    if (!event.isFirstSpawn()) return;

                    MiscFunctionality.assignTeam(event.getPlayer());

                    ActionBar.forPlayer(event.getPlayer())
                            .addProvider(new ServerStatsHud());

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

        server.start("0.0.0.0", 25565);
    }

}

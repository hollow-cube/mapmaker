package net.hollowcube.mapmaker.dev;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.HubServerRunner;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.LocalMapAllocator;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.hollowcube.mapmaker.player.SessionCreateRequestV2;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.util.thesneaky.TheSneaky;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class DevServerRunner extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(DevServerRunner.class);

    // Hub stuff
    private HubMapWorld hubWorld;

    // Map stuff
    private Terraform terraform;
    private FeatureList features;

    // Common stuff
    private final CommandManager hubCommandManager = new CommandManagerImpl(super.commandManager());
    private final CommandManager mapCommandManager = new CommandManagerImpl(super.commandManager());

    public DevServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("dev-init")
                .addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin)
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
    }

    @Override
    protected @NotNull String name() {
        return "mapmaker-dev";
    }

    @Override
    protected @NotNull MapAllocator createAllocator() {
        return new LocalMapAllocator(this);
    }

    @Override
    protected @NotNull ServerBridge createBridge() {
        return new DevServerBridge(mapService(), allocator());
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager().setPlayerProvider((uuid, username, connection) -> new MapPlayerImplImpl(uuid, username, connection) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                var world = MapWorld.forPlayerOptional(this);
                return world == null || world instanceof HubMapWorld ? hubCommandManager : mapCommandManager;
            }
        });

        addBinding(Scheduler.class, MinecraftServer.getSchedulerManager());

        performMapInit(); // Map first so placements are registered
        performHubInit();
    }

    private void performHubInit() {
        this.hubWorld = allocator().allocateDirect(HubMapWorld.HUB_MAP_DATA, HubMapWorld.class);
        addBinding(HubMapWorld.class, hubWorld, "world", "hubWorld", "hubMapWorld");

        HubServerRunner.registerCommands(this, hubCommandManager);
        HubServerRunner.loadHubFeatures(this);
    }

    private void performMapInit() {
        this.terraform = MapServerRunner.initBuildLogic(mapService(), commandManager());
        addBinding(Terraform.class, terraform);

        var hdb = new HeadDatabase();
        addBinding(HeadDatabase.class, hdb, "hdb");

        MapServerRunner.registerCommands(this, mapCommandManager);

        MapServerRunner.initFeatureFlagMonitor(bridge(), allocator());

        this.features = FeatureList.load(config);
        addBinding(FeatureList.class, features);
        shutdowner().queue(features::close);
    }

    protected void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        // DevServer is not running behind a proxy, so we need to handle the proxy side of the session interaction
        // on our own here.
        // Note that we dont transfer here, its deferred to config phase (and reconfig)

        net.minestom.server.entity.PlayerSkin skin = net.minestom.server.entity.PlayerSkin.fromUuid(event.getPlayerUuid().toString());

        var playerId = event.getPlayerUuid().toString();
        sessionService().createSessionV2(playerId, new SessionCreateRequestV2(
                "devserver-integrated", event.getUsername(), "127.0.0.1",
                new PlayerSkin(Optional.ofNullable(skin).map(net.minestom.server.entity.PlayerSkin::textures),
                        Optional.ofNullable(skin).map(net.minestom.server.entity.PlayerSkin::signature))
        ));
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();

            var targetWorld = player.getTag(DevServerBridge.TARGET_WORLD);
            if (targetWorld == null) {
                // Move the session to the hub and spawn the player
                var hubPresence = new Presence(Presence.TYPE_MAPMAKER_HUB, "__hub_unused__", "devserver", "hub");
                super.transferPlayerSession(player, hubPresence);

                hubWorld.configurePlayer(event);

//                Thread.startVirtualThread(() -> {
//                    try {
//                        Thread.sleep(3000);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                    bridge().joinMap(player, "", ServerBridge.JoinMapState.EDITING, "");
//                });

                return;
            }

            // Move session and spawn the player into the targeted map
            var world = Objects.requireNonNull(FutureUtil.getUnchecked(targetWorld));
            var joinType = world instanceof EditingMapWorld ? "editing" : "playing";
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, joinType, "devserver", world.map().id());
            super.transferPlayerSession(player, presence);

            world.configurePlayer(event);


//            Thread.startVirtualThread(() -> {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                bridge().joinHub(player);
//            });
        } catch (Exception e) {
            logger.error("Error during config phase", e);
            event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
        }
    }

    protected void handleSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());

        var p = event.getPlayer();
        p.scheduleNextTick($ -> {
            TheSneaky.getTheSneaky().send(p);
        });
    }

    protected void handleDisconnect(@NotNull PlayerDisconnectEvent event) {
        var player = event.getPlayer();
        super.handlePlayerDisconnect(player);

        // Again, need to implement the proxy part of the delete session flow
        sessionService().deleteSessionV2(player.getUuid().toString());
    }
}

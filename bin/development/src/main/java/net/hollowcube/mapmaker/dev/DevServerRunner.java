package net.hollowcube.mapmaker.dev;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.MojangUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.HubServerRunner;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.MapMgmtConsumerImpl;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.LocalMapAllocator;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

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

    private ScriptEngine scriptEngine;

    public DevServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("dev-init")
                        .addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin)
                        .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                        .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                        .addListener(PlayerDisconnectEvent.class, this::handleDisconnect))
                .addListener(ServerListPingEvent.class, this::handleServerListPing);
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
    public @NotNull ScriptEngine scriptEngine() {
        return scriptEngine;
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) -> new MapPlayerImplImpl(connection, gameProfile) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                var world = MapWorld.forPlayerOptional(this);
                return world == null || world instanceof HubMapWorld ? hubCommandManager : mapCommandManager;
            }
        });

        addBinding(Scheduler.class, MinecraftServer.getSchedulerManager());

        performMapInit(); // Map first so placements are registered
        performHubInit();

        var kafkaConfig = config.get(KafkaConfig.class);
        var mapMgmtConsumer = new MapMgmtConsumerImpl((LocalMapAllocator) allocator(), kafkaConfig.bootstrapServers());
        shutdowner().queue("map-mgmt-listener", mapMgmtConsumer::close);

        // TODO: this doesnt work since we always return the same script engine instance even in a map
        scriptEngine = new ScriptEngine(hubWorld.instance());
    }

    private void performHubInit() {
        this.hubWorld = allocator().allocateDirect(HubMapWorld.HUB_MAP_DATA, HubMapWorld.CTOR);
        addBinding(HubMapWorld.class, hubWorld, "world", "hubWorld", "hubMapWorld");

        HubServerRunner.registerCommands(this, hubCommandManager, hubWorld, MinecraftServer.getSchedulerManager());
        HubServerRunner.loadHubFeatures(this, hubWorld);
    }

    private void performMapInit() {
        this.terraform = MapServerRunner.initBuildLogic(mapService(), commandManager());
        addBinding(Terraform.class, terraform);

        var hdb = new HeadDatabase(otel);
        addBinding(HeadDatabase.class, hdb, "headDatabase", "hdb");
        MapServerRunner.registerCommands(this, mapCommandManager, hdb);

        MapServerRunner.initFeatureFlagMonitor(bridge(), allocator());

        this.features = FeatureList.load(config);
        addBinding(FeatureList.class, features);
        shutdowner().queue("features", features::close);
    }

    protected void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        // DevServer is not running behind a proxy, so we need to handle the proxy side of the session interaction
        // on our own here.
        // Note that we dont transfer here, its deferred to config phase (and reconfig)

        var profile = event.getGameProfile();
        var playerId = profile.uuid().toString();
        net.minestom.server.entity.PlayerSkin skin = MojangUtil.getSkinFromUuid(playerId);

        sessionService().createSession(playerId, "devserver-integrated", profile.name(), "127.0.0.1",
                new PlayerSkin(OpUtils.map(skin, net.minestom.server.entity.PlayerSkin::textures),
                        OpUtils.map(skin, net.minestom.server.entity.PlayerSkin::signature))
        );
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();

            var targetWorld = player.getTag(DevServerBridge.TARGET_WORLD);
            if (targetWorld == null) {
                // Move the session to the hub and spawn the player
                var hubPresence = new Presence(Presence.TYPE_MAPMAKER_HUB, "__hub_unused__", "devserver", "hub");
                try {
                    super.transferPlayerSession(player, hubPresence);
                } catch (Throwable t) {
                    logger.error("Error transferring player to hub", t);
                    event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
                    return;
                }

                hubWorld.configurePlayer(event);

                return;
            }

            // Move session and spawn the player into the targeted map
            var world = Objects.requireNonNull(FutureUtil.getUnchecked(targetWorld));
            var joinType = world instanceof EditingMapWorld ? "editing" : "playing";
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, joinType, "devserver", world.map().id());
            super.transferPlayerSession(player, presence);

            world.configurePlayer(event);
        } catch (Exception e) {
            logger.error("Error during config phase", e);
            event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
        }
    }

    protected void handleSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(@NotNull PlayerDisconnectEvent event) {
        var player = event.getPlayer();
        super.handlePlayerDisconnect(player);

        // Again, need to implement the proxy part of the delete session flow
        FutureUtil.submitVirtual(() -> sessionService().deleteSession(player.getUuid().toString()));
    }

    protected void handleServerListPing(@NotNull ServerListPingEvent event) {
        var responseData = event.getResponseData();
        responseData.setFavicon("data:image/png;base64,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");
        responseData.setDescription(MiniMessage.miniMessage().deserialize("                     <color:#dbdbdb>Hollow Cube</color> <color:#696969>|</color> <color:#bfbfbf>" + MinecraftServer.VERSION_NAME + "</color>\n                  <color:#fa4141>ᴍᴀᴘ ᴍᴀᴋᴇʀ ᴅᴇᴠ ѕᴇʀᴠᴇʀ</color>"));
        event.setResponseData(responseData);
    }

    @Override
    protected @NotNull DebugCommand createDebugCommand() {
        DebugCommand dbg = super.createDebugCommand();

        dbg.createPermissionedSubcommand("enableprogressaddition", (player, context) -> {
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) {
                player.sendMessage("You are not in a map world!");
                return;
            }

            if (world instanceof EditingMapWorld && world.canEdit(player)) {
                world.map().setSetting(MapSettings.PROGRESS_INDEX_ADDITION, true);
                player.sendMessage("Enabled progress addition");
            } else {
                player.sendMessage("You are not in an editing world!");
            }
        }, "Enables progress index add mode for the current map");

        dbg.createPermissionedSubcommand("gui", (player, ignored) -> {
            player.getInstance().scheduleNextTick(ignored2 -> {
                scriptEngine.guiManager().openGui(player, URI.create("guilib:///map_browser/map-browser-view.js"), Map.of(), Map.of());
            });
        }, "");

        return dbg;
    }
}

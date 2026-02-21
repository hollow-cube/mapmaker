package net.hollowcube.mapmaker.hub;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.common.util.Uuids;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.playerinfo.PlayerInfoCommand;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.hub.command.util.HubFlyCommand;
import net.hollowcube.mapmaker.hub.command.util.HubSpawnCommand;
import net.hollowcube.mapmaker.hub.command.util.HubTrainCommand;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.util.HubTransferData;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.setting.TimeOfDay;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.misc.ResourcePackManager;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.ServerBeginShutdownEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

import static net.hollowcube.mapmaker.map.MapPlayer.simpleMapPlayer;

public class HubServer extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(HubServer.class);
    private static final Presence HUB_PRESENCE = new Presence("mapmaker:hub",
        "__hub_unused__", ServerRuntime.getRuntime().hostname(), "hub");

    public static final MapData HUB_MAP_DATA = new MapData(MapData.SPAWN_MAP_ID, Uuids.ZERO);

    static {
        HUB_MAP_DATA.settings().setSize(MapSize.UNLIMITED);
        HUB_MAP_DATA.settings().set(MapSettings.TIME_OF_DAY, TimeOfDay.NIGHT);
        HUB_MAP_DATA.settings().set(MapSettings.LIGHTING, true);
    }

    // Its only kinda unknown. it's not created in the constructor, but after prepareState
    // it is always not-null which should cover any reasonable logic.
    private @UnknownNullability HubMapWorld world;

    HubServer(ConfigLoaderV3 config) {
        super(config);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("hub-init")
            .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
            .addListener(PlayerSpawnEvent.class, this::handleSpawn)
            .addListener(PlayerDisconnectEvent.class, this::handleDisconnect)
            .addListener(ServerBeginShutdownEvent.class, this::handleServerShutdown));
    }

    @Override
    protected String name() {
        return "mapmaker-hub";
    }

    @Override
    protected ServerBridge createBridge() {
        return globalConfig.noop() ? new NoopServerBridge() : new HubServerBridge(mapService(), sessionService());
    }

    @Override
    public Scheduler scheduler() {
        return world.scheduler();
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager()
            .setPlayerProvider(simpleMapPlayer(commandManager()));

        world = new HubMapWorld(this, HUB_MAP_DATA);
        world.loadWorld();

        // We schedule on first tick end because submitTask invokes the executor immediately to determine
        // the first schedule. If we executed it here, that would be on the wrong thread.
        var scheduler = MinecraftServer.getSchedulerManager();
        scheduler.scheduleEndOfTick(() -> scheduler.submitTask(world::safePointTick, ExecutionType.TICK_END));

        addBinding(HubMapWorld.class, world, "world", "hubWorld", "hubMapWorld");
        addBinding(Scheduler.class, world.scheduler());

        registerCommands(this, commandManager(), world, world.instance().scheduler());
        loadHubFeatures(this, world);
    }

    // Static so it can be referenced from DevHubServer
    public static void registerCommands(AbstractMapServer server, CommandManager commandManager, HubMapWorld hubWorld, Scheduler scheduler) {
        commandManager.register(new HelpCommand(commandManager, CommandCategories.GLOBAL));
        commandManager.register(new PlayerInfoCommand(server.playerService(), server.mapService(), server.sessionManager()));

        commandManager.register(new HubFlyCommand());
        commandManager.register(new HubSpawnCommand());
        commandManager.register(new HubTrainCommand(scheduler));
    }

    // Static so it can be referenced from DevHubServer
    public static void loadHubFeatures(AbstractMapServer server, HubMapWorld world) {
        for (var feature : ServiceLoader.load(HubFeature.class)) {
            try {
                logger.debug("Loading feature {}", feature.getClass().getName());
                feature.load(server, world);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load feature " + feature.getClass().getName(), e);
            }
        }
    }

    protected void handleConfigPhase(AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();

        // Queue resource pack download/apply while we do other things
        var resourcePackFuture = ResourcePackManager.sendResourcePack(player);

        if (config.get(VelocityConfig.class).secret().isEmpty()) {
            ProtocolVersions.unsafeSetProtocolVersion(player, MinecraftServer.PROTOCOL_VERSION);
        } else {
            ProtocolVersions.requestProtocolVersionFromProxy(player);
            if (!player.isOnline()) return;
        }

        if (!transferPlayerSession(event.getPlayer(), HUB_PRESENCE)) {
            return;
        }

        FutureUtil.getUnchecked(resourcePackFuture);
        if (!player.isOnline()) return;

        // Setup the player in the world
        world.configurePlayer(event);
    }

    protected void handleSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(PlayerDisconnectEvent event) {
        super.handlePlayerDisconnect(event.getPlayer());
    }

    private void handleServerShutdown(ServerBeginShutdownEvent ignored) {
        // When the hub is instructed to shut down, try to move the players to a new hub.
        var players = MinecraftServer.getConnectionManager().getOnlinePlayers();
        FutureUtil.submitVirtual(() -> {
            for (var player : players) {
                try {
                    var hub = sessionService().joinHubV2(new JoinHubRequest(
                        player.getUuid().toString(), AbstractHttpService.hostname));

                    var state = new HubTransferData(player.getPosition(), player.getHeldSlot());
                    ProxySupport.transferWithData(player, hub.serverClusterIp(), state);
                } catch (SessionService.NoAvailableServerException ignored2) {
                    // No other hub is available. Leave them here for now, they can move on their own
                    // when a hub becomes available.
                } catch (Exception e) {
                    ExceptionReporter.reportException(e, player);
                }
            }
        });
    }
}

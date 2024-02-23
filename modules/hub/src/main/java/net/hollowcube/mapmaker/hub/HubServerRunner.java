package net.hollowcube.mapmaker.hub;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.spi.ClassServiceLoader;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.hub.command.util.HubFlyCommand;
import net.hollowcube.mapmaker.hub.command.util.HubSpawnCommand;
import net.hollowcube.mapmaker.hub.command.util.HubTrainCommand;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.block.handler.BlockHandlers;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.ResourcePackManager;
import net.hollowcube.mapmaker.session.Presence;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.Scheduler;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class HubServerRunner extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(HubServerRunner.class);
    private static final Presence HUB_PRESENCE = new Presence("mapmaker:hub",
            "__hub_unused__", ServerRuntime.getRuntime().hostname(), "hub");

    private HubMapWorld world;

    HubServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        BlockHandlers.init(); // No need for placement rules etc. Just these to avoid invisible blocks

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("hub-init")
                .addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin)
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));

    }

    @Override
    public @NotNull Collection<HealthCheck> readinessChecks() {
        var checks = new ArrayList<>(super.readinessChecks());
        checks.add(() -> sessionService().ready() ? HealthCheckResponse.up("session-service") : HealthCheckResponse.down("session-service"));
        return checks;
    }

    @Override
    protected @NotNull MapAllocator createAllocator() {
        return MapAllocator.direct(this);
    }

    @Override
    protected @NotNull ServerBridge createBridge() {
        return globalConfig.noop() ? new NoopServerBridge() : new HubServerBridge(sessionService());
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        // Create the hub world once, which will never go away.
        this.world = allocator().allocateDirect(HubMapWorld.HUB_MAP_DATA, HubMapWorld.class);
        addBinding(HubMapWorld.class, world, "world", "hubWorld", "hubMapWorld");
        addBinding(Scheduler.class, world.instance().scheduler());

        registerCommands(this, commandManager());
        loadHubFeatures(this);
    }

    // Static so it can be referenced from DevHubServer
    public static void registerCommands(@NotNull AbstractMapServer server, @NotNull CommandManager commandManager) {
        commandManager.register(new HelpCommand(commandManager));

        commandManager.register(server.createInstance(HubFlyCommand.class));
        commandManager.register(server.createInstance(HubSpawnCommand.class));
        commandManager.register(server.createInstance(HubTrainCommand.class));
    }

    // Static so it can be referenced from DevHubServer
    public static void loadHubFeatures(@NotNull AbstractMapServer server) {
        for (var featureClass : ClassServiceLoader.load(HubFeature.class)) {
            try {
                logger.info("Loading feature {}", featureClass.getName());
                server.createInstance(featureClass);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load feature " + featureClass.getName(), e);
            }
        }
    }

    protected void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        transferPlayerSession(event.getPlayer(), HUB_PRESENCE);
        // Result ignored because nothing happens here, but if above returns false the player was kicked.
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();

        ResourcePackManager.sendResourcePack(player).join();
        if (!player.isOnline()) return;

        // Setup the player in the world
        world.configurePlayer(event);
    }

    protected void handleSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(@NotNull PlayerDisconnectEvent event) {
        super.handlePlayerDisconnect(event.getPlayer());
    }
}

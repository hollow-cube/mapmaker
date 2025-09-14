package net.hollowcube.mapmaker.isolate;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.Uuids;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.ResourcePackManager;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;
import net.hollowcube.mapmaker.runtime.freeform.bundle.LocalFsLoader;
import net.hollowcube.mapmaker.runtime.freeform.bundle.ScriptBundle;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.session.Presence;
import net.kyori.adventure.text.Component;
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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static net.hollowcube.mapmaker.map.MapPlayer.simpleMapPlayer;

public class MapIsolateServer extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(MapIsolateServer.class);

    private final ScriptBundle.Loader scriptLoader;

    private final String mapId;

    // Its only kinda unknown. it's not created in the constructor, but after prepareState
    // it is always not-null which should cover any reasonable logic.
    // TODO: pretty sure we could do init in constructor, should investigate.
    private @UnknownNullability AbstractMapWorld<?, ?> world;

    public MapIsolateServer(ConfigLoaderV3 config) {
        super(config);

        if (IsolateMain.args.length < 1)
            throw new IllegalArgumentException("Map ID must be provided as the last argument");
        this.mapId = UUID.fromString(IsolateMain.args[IsolateMain.args.length - 1]).toString();
        System.out.println("Map ID: " + this.mapId);
        System.out.println("Args: " + Arrays.toString(IsolateMain.args));

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("map-init")
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));

        this.scriptLoader = new LocalFsLoader(Path.of("../../scripts"));
    }

    @Override
    protected String name() {
        return "mapmaker-map-isolate";
    }

    @Override
    protected ServerBridge createBridge() {
        return new MapIsolateBridge(mapService(), sessionService());
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

        ParkourMapWorld.initGlobalReferences();

        try {
            var map = mapService().getMap(Uuids.ZERO, this.mapId);

            world = new FreeformMapWorld(this, map, scriptLoader);
            world.loadWorld();

            // We schedule on first tick end because submitTask invokes the executor immediately to determine
            // the first schedule. If we executed it here, that would be on the wrong thread.
            var scheduler = MinecraftServer.getSchedulerManager();
            scheduler.scheduleEndOfTick(() -> scheduler.submitTask(world::safePointTick, ExecutionType.TICK_END));
        } catch (Exception e) {
            logger.error("Error allocating map", e);
            throw new RuntimeException(e);
        }

        addBinding(Scheduler.class, world.instance().scheduler());
    }

    protected void handleConfigPhase(AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();

            // Queue resource pack download/apply while we do other things
            var resourcePackFuture = ResourcePackManager.sendResourcePack(player);

            logger.info("configuring player {}", player.getUuid());
            var instanceId = ServerRuntime.getRuntime().hostname();
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, "playing", instanceId, this.mapId);
            transferPlayerSession(player, presence);

            // Ensure resource pack was applied before allowing the player in
            FutureUtil.getUnchecked(resourcePackFuture);
            if (!player.isOnline()) return;

            // Setup the player in the world
            world.configurePlayer(event);
        } catch (Exception e) {
            logger.error("Error during config phase", e);
            event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
        }
    }

    protected void handleSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(PlayerDisconnectEvent event) {
        super.handlePlayerDisconnect(event.getPlayer());
    }

}

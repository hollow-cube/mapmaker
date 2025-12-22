package net.hollowcube.mapmaker.local;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.local.scripting.FreeformMapWorld;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapMapServer;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.noop.NoopPermManager;
import net.hollowcube.mapmaker.misc.noop.NoopPlayerService;
import net.hollowcube.mapmaker.misc.noop.NoopSessionService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.Presence;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.UnknownNullability;

import java.nio.file.Path;

import static net.hollowcube.mapmaker.map.MapPlayer.simpleMapPlayer;

public class LocalMapServer extends AbstractMapServer {
    private static final Presence HUB_PRESENCE = new Presence("mapmaker:local",
        "__local_unused__", ServerRuntime.getRuntime().hostname(), "unknown");

    private final Path mapDirectory;

    private final MapService mapService;

    private final ServerBridge bridge = new NoopServerBridge();
    private final SessionService sessionService = new NoopSessionService();
    private final PlayerService playerService = new NoopPlayerService();
    private final PermManager permManager = new NoopPermManager();

    // Its only kinda unknown. it's not created in the constructor, but after prepareState
    // it is always not-null which should cover any reasonable logic.
    private @UnknownNullability EditorMapWorld world;

    LocalMapServer(ConfigLoaderV3 config, Path mapDirectory) {
        super(config);
        this.mapDirectory = mapDirectory;
        this.mapService = new LocalMapService(mapDirectory);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("local-init")
            .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
            .addListener(PlayerSpawnEvent.class, this::handleSpawn)
            .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
    }

    @Override
    protected String name() {
        return "mapmaker-local";
    }

    @Override
    protected ServerBridge createBridge() {
        return bridge;
    }

    @Override
    public SessionService sessionService() {
        return sessionService;
    }

    @Override
    public PlayerService playerService() {
        return playerService;
    }

    @Override
    public MapService mapService() {
        return mapService;
    }

    @Override
    public PermManager permManager() {
        return permManager;
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager()
            .setPlayerProvider(simpleMapPlayer(commandManager()));

        var map = new MapData();
        world = new EditorMapWorld(this, map, null) {
            @Override
            protected AbstractMapWorld<?, ?> createTestWorld() {
                return new FreeformMapWorld(LocalMapServer.this, map, mapDirectory);
            }
        };
        world.loadWorld();
        shutdowner().queue("save", world::close);

        // We schedule on first tick end because submitTask invokes the executor immediately to determine
        // the first schedule. If we executed it here, that would be on the wrong thread.
        var scheduler = MinecraftServer.getSchedulerManager();
        scheduler.scheduleEndOfTick(() -> scheduler.submitTask(world::safePointTick, ExecutionType.TICK_END));

        addBinding(Scheduler.class, scheduler());

        MapMapServer.registerCommands(this, commandManager(), null);
    }

    protected void handleConfigPhase(AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();

        // TODO: in the future we should support release builds of this which load a resource pack.
        // var resourcePackFuture = ResourcePackManager.sendResourcePack(player);

        ProtocolVersions.unsafeSetProtocolVersion(player, MinecraftServer.PROTOCOL_VERSION);

        if (!transferPlayerSession(event.getPlayer(), HUB_PRESENCE)) {
            return;
        }

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

}

package net.hollowcube.mapmaker.map;

import io.nats.client.Message;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.command.DebugRenderersCommand;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.misc.ResourcePackManager;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public abstract class AbstractMultiMapServer extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMultiMapServer.class);

    private record MapKey(String mapId, boolean editing) {
    }

    private final ReentrantLock worldLock = new ReentrantLock();
    private final Map<MapKey, Future<AbstractMapWorld<?, ?>>> worlds = new HashMap<>();

    private static final String MAP_JOIN_STREAM = "MAP_JOINS";
    private static final ConsumerConfiguration MAP_JOIN_CONSUMER_CONFIG = ConsumerConfiguration.builder()
        .filterSubjects("map-join.>")
        .deliverPolicy(DeliverPolicy.New)
        .ackPolicy(AckPolicy.None)
        .inactiveThreshold(Duration.ofMinutes(5))
        .build();

    // A pending join is a map of player id to a future that will be completed when the server has the join info.
    // The future returned will never complete with an exception, but may complete with null indicating that we
    // timed out while waiting for the player info to be received.
    //
    // This will block the player from joining until the server has confirmed that it should have them and will
    // all happen during login (Minestom pre login event).
    // Note that we will separately hold them in the configuration phase until the map world is ready.
    private final Map<String, CompletableFuture<@Nullable MapJoinInfo>> pendingPlayerJoins = new ConcurrentHashMap<>();
    private final Set<AbstractMapWorld<?, ?>> closingWorlds = new ConcurrentHashMap<AbstractMapWorld<?, ?>, Integer>().keySet(1);

    private Tracer tracer;

    private volatile boolean isClosed = false;

    public AbstractMultiMapServer(@NotNull ConfigLoaderV3 config) {
        super(config);

        ParkourMapWorld.initGlobalReferences();

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("map-init")
            .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
            .addListener(PlayerSpawnEvent.class, this::handleSpawn)
            .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));

        // We schedule on first tick end because submitTask invokes the executor immediately to determine
        // the first schedule. If we executed it here, that would be on the wrong thread.
        var scheduler = MinecraftServer.getSchedulerManager();
        scheduler.scheduleEndOfTick(() -> scheduler.submitTask(this::safePointTick, ExecutionType.TICK_END));
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        if (!globalConfig.noop()) {
            var mapJoinConsumer = jetStream.subscribe(MAP_JOIN_STREAM, MAP_JOIN_CONSUMER_CONFIG, MapJoinInfoMessage.class, this::onMapJoinMessage);
            shutdowner().queue("map-join-listener", mapJoinConsumer::close);

            var mapMgmtConsumer = new MapMgmtConsumerImpl(jetStream, this);
            shutdowner().queue("map-mgmt-listener", mapMgmtConsumer::close);
        }

        addBinding(Scheduler.class, MinecraftServer.getSchedulerManager());

        shutdowner().queue("close-worlds", this::close);

        tracer = otel.getTracer(getClass().getName(), ServerRuntime.getRuntime().version());
    }

    //region Player Lifecycle

    public void addPendingJoin(@NotNull String playerId, @NotNull String mapId, @NotNull String state) {
        pendingPlayerJoins.put(playerId, CompletableFuture.completedFuture(new MapJoinInfo(playerId, mapId, state)));
    }

    protected @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId, boolean deleteCompleted) {
        var pendingJoin = pendingPlayerJoins.computeIfAbsent(playerId, id -> {
            var future = new CompletableFuture<MapJoinInfo>();
            //todo the futures are never actually removed from the map.
            // invalid to do below because it will remove the future before we can handle it in login event.
//            future.whenComplete((v, e) -> pendingPlayerJoins.remove(id));
            return future;
        });
        if (deleteCompleted && pendingJoin.isDone()) {
            pendingPlayerJoins.remove(playerId);
            pendingJoin = pendingPlayerJoins.computeIfAbsent(playerId, id -> new CompletableFuture<>());
        }

        return pendingJoin;
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();
            if (config.get(VelocityConfig.class).secret().isEmpty()) {
                ProtocolVersions.unsafeSetProtocolVersion(player, MinecraftServer.PROTOCOL_VERSION);
            } else {
                ProtocolVersions.requestProtocolVersionFromProxy(player);
                if (!player.isOnline()) return;
            }

            // Queue resource pack download/apply while we do other things
            var resourcePackFuture = ResourcePackManager.sendResourcePack(player);

            logger.info("configuring player {}", player.getUuid());
            var playerId = player.getUuid().toString();
            var joinInfo = FutureUtil.getUnchecked(getPendingJoin(playerId, false));
            logger.info("got pending join {}", joinInfo);
            if (joinInfo == null) {
                logger.error("timed out waiting for join info for {}", playerId);
                player.kick(Component.text("Failed to join. Please try again later."));
                return;
            }

            var runtime = ServerRuntime.getRuntime();
            var instanceId = runtime.isDevelopment() ? "devserver" : runtime.hostname();
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, joinInfo.state(), instanceId, joinInfo.mapId());
            transferPlayerSession(player, presence);
            logger.info("transfered session {}", joinInfo);

            // Create the world, holding the player here until it is ready for them to join.
            // TODO: Why don't we start loading the world when we receive the join request? we can always close it if they dont end up joining.
            var mapWorld = FutureUtil.getUnchecked(createWorldForRequest(joinInfo));
            if (mapWorld == null) {
                logger.error("Failed to create map world for {}: {}", joinInfo.playerId(), joinInfo.mapId());
                player.kick(Component.text("Failed to create map world. Please try again later."));
                return;
            }

            // Ensure resource pack was applied before allowing the player in
            FutureUtil.getUnchecked(resourcePackFuture);
            if (!player.isOnline()) return;

            // Setup the player in the world
            mapWorld.configurePlayer(event);
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
        handlePlayerDisconnect(event.getPlayer());
    }

    protected abstract @NotNull Future<AbstractMapWorld<?, ?>> createWorldForRequest(@NotNull MapJoinInfo joinInfo);

    //endregion

    //region World Lifecycle

    @SuppressWarnings("unchecked")
    public <T extends AbstractMapWorld<?, ?>> Future<T> createWorld(
        @NotNull MapData map, boolean editing,
        @NotNull Function<MapData, T> worldFactory,
        boolean tracked
    ) {
        return FutureUtil.fork(() -> {
            Future<T> future;

            // Small note about locking here.
            // We only lock around specifically the `maps` get and put operations and NOT
            // the actual allocation of the world which is submitted in another thread, and then
            // we suspend waiting for it after the lock is released.

            worldLock.lock();
            try {
                Check.stateCondition(isClosed, "Cannot create worlds after server is closed");
                var key = new MapKey(map.id(), editing);

                // Return existing world if present.
                future = (Future<T>) worlds.get(key);
                if (future == null) {
                    // No existing world, create a new one.
                    future = FutureUtil.fork(() -> createWorldInternal(key, map, worldFactory, tracked));
                    worlds.put(key, (Future<AbstractMapWorld<?, ?>>) future);
                }
            } finally {
                worldLock.unlock();
            }

            return future.get();
        });
    }

    private <T extends AbstractMapWorld<?, ?>> @Nullable T createWorldInternal(
        @NotNull MapKey key, @NotNull MapData map, @NotNull Function<MapData, T> worldFactory, boolean tracked
    ) {
        try {
            var createdWorld = worldFactory.apply(map);
            if (tracked) {
                createdWorld.instance().eventNode().addListener(PlayerInstanceLeaveEvent.class, event -> {
                    // Get the world from the instance because 1: the player is no longer in a world, and 2: we care about the root world (editing, not testing)
                    var world = MapWorld.forInstance(event.getInstance());
                    if (world == null) return;

                    // If there is nobody left at all, always destroy the world
                    if (event.getInstance().getPlayers().size() == 1) {
                        FutureUtil.submitVirtual(() -> destroy(key, Component.translatable("map.closed")));
                        return;
                    }

                    if (world.map().isPublished()) return;

                    // Try to close the map if there are no registered builders left.
                    var leavingId = PlayerData.fromPlayer(event.getPlayer()).id();
                    FutureUtil.submitVirtual(() -> {
                        var builders = api().maps.getMapBuilders(world.map().id(), true);
                        var leavingIsBuilder = false;
                        for (var builder : builders) {
                            if (world.hasPlayer(builder.id()))
                                return; // dont need to close
                            if (builder.id().equals(leavingId))
                                leavingIsBuilder = true;
                        }

                        // If the person leaving also is not a builder (aka we never had a builder to begin with),
                        // then dont destroy the world. Mostly this is for editing org maps where the owner is fake.
                        // TODO: a better version of this is to add some global map edit permission which is given
                        //       to all staff members, allowing them to keep the world alive as if they are a builder.
                        if (!leavingIsBuilder) return;

                        // None of the builders are present, destroy it.
                        destroy(key, Component.translatable("map.kicked"));
                    });
                });
            }

            createdWorld.loadWorld();

            return createdWorld;
        } catch (Exception e) {
            logger.error("Failed to allocate map " + map.id(), e);
            ExceptionReporter.reportException(e);

            worldLock.lock();
            try {
                worlds.remove(key);
            } finally {
                worldLock.unlock();
            }

            return null;
        }
    }

    public void destroyMapWorlds(@NotNull String mapId, @NotNull Component reason) {
        List<MapKey> keys = new ArrayList<>();
        worldLock.lock();
        try {
            for (var key : worlds.keySet()) {
                if (!key.mapId().equals(mapId)) continue;
                keys.add(key);
            }
        } finally {
            worldLock.unlock();
        }

        for (var key : keys) {
            destroy(key, reason);
        }
    }

    private void destroy(@NotNull MapKey key, @NotNull Component reason) {
        FutureUtil.assertThread();
        var span = tracer.spanBuilder("destroyWorld").setSpanKind(SpanKind.CLIENT).startSpan();
        try {
            final Future<AbstractMapWorld<?, ?>> worldFuture;
            worldLock.lock();
            try {
                worldFuture = worlds.remove(key);
            } finally {
                worldLock.unlock();
            }
            if (worldFuture == null) return;

            var world = FutureUtil.getUnchecked(worldFuture);
            try {
                closingWorlds.add(world);

                // Remove all players from the world.
                var players = List.copyOf(world.players());
                var futures = new CompletableFuture[players.size()];
                for (int i = 0; i < players.size(); i++) {
                    var player = players.get(i);
                    player.sendMessage(reason);
                    futures[i] = world.scheduleRemovePlayer(player)
                        .thenRunAsync(() -> {
                            if (!player.isOnline()) return;
                            bridge().joinHub(player);
                        }, FutureUtil.VIRUTAL_EXECUTOR)
                        .exceptionally(e -> {
                            ExceptionReporter.reportException(new RuntimeException("failed to remove player", e), player);
                            player.kick(reason);
                            return null;
                        });
                }
                span.setAttribute("players", players.size());
                try {
                    CompletableFuture.allOf(futures).get(15, TimeUnit.SECONDS);
                } catch (TimeoutException ignored) {
                    logger.error("failed to drain players in 15s, exiting.");
                } catch (RuntimeException | InterruptedException | ExecutionException e) {
                    logger.error("failed to drain players", e);
                }

                // Close the world itself
                MinecraftServer.getSchedulerManager()
                    .scheduleEndOfTick(world::close);
            } finally {
                closingWorlds.remove(world);
            }
        } finally {
            span.end();
        }
    }

    public void close() {
        worldLock.lock();
        try {
            isClosed = true;
        } finally {
            worldLock.unlock();
        }

        for (var key : List.copyOf(worlds.keySet())) {
            try {
                destroy(key, Component.translatable("mapmaker.shutdown"));
            } catch (Exception e) {
                logger.error("Failed to destroy world for " + key.mapId(), e);
            }
        }
    }

    private @NotNull TaskSchedule safePointTick() {
        try {
            final List<Future<AbstractMapWorld<?, ?>>> worldsToTick;
            worldLock.lock();
            try {
                worldsToTick = List.copyOf(worlds.values());
            } finally {
                worldLock.unlock();
            }

            for (var worldToTick : worldsToTick) {
                if (worldToTick.state() != Future.State.SUCCESS) continue;

                worldToTick.resultNow().safePointTick();
            }
            for (var worldToTick : closingWorlds) {
                worldToTick.safePointTick();
            }
        } catch (Exception e) {
            ExceptionReporter.reportException(new SafePointTickException(e));
        }

        return TaskSchedule.nextTick();
    }

    //endregion

    @Override
    protected @NotNull DebugCommand createDebugCommand() {
        var cmd = super.createDebugCommand();

        cmd.createPermissionlessSubcommand("world", (player, _) -> {
            var world = MapWorld.forPlayer(player);
            if (world == null) {
                player.sendMessage("You are not in a map world!");
                return;
            }

            player.sendMessage(Component.text("Map: ").append(Component.text(world.map().id())));
            player.sendMessage("Type: " + world.getClass().getSimpleName());
        }, "Shows information about the world you are in");

        cmd.createPermissionedSubcommand("worlds",
            (player, _) -> sendWorldDebug(player),
            "Show information about the currently loaded worlds.");

        cmd.createPermissionedSubcommand("enableprogressaddition", (player, context) -> {
            var world = MapWorld.forPlayer(player);
            if (world == null) {
                player.sendMessage("You are not in a map world!");
                return;
            }

            if (world.canEdit(player)) {
                world.map().setSetting(MapSettings.PROGRESS_INDEX_ADDITION, true);
                player.sendMessage("Enabled progress addition");
            } else {
                player.sendMessage("You are not in an editing world!");
            }
        }, "Enables progress index add mode for the current map");

        cmd.createPermissionedSubcommand(
            "boundingbox",
            DebugRenderersCommand::handleDebugBoundingBox,
            "Shows the bounding box of the player"
        );

        cmd.createPermissionlessSubcommand(
            "poi",
            DebugRenderersCommand::handleDebugRegions,
            "Shows the location information about nearby pois"
        );

        cmd.createPermissionedSubcommand(
            "save",
            (player, _) -> {
                var world = EditorMapWorld.forPlayer(player);
                if (world == null) {
                    player.sendMessage("You are not in an editor world!");
                    return;
                }

                world.save(true); // pretend to be auto save to get message
            },
            "Immediately saves the current (editor) world"
        );

        return cmd;
    }

    private void sendWorldDebug(@NotNull Player player) {
        worldLock.lock();
        try {
            var builder = Component.text();
            builder.append(Component.text("Map Worlds (" + this.worlds.size() + " active maps)"));
            for (var entry : worlds.entrySet()) {
                builder.appendNewline().append(Component.text("»"));
                var key = entry.getKey();
                var mapIdShort = key.mapId().substring(0, Math.min(8, key.mapId().length()));

                builder.append(ComponentUtil.createBasicCopy(mapIdShort, key.mapId()));

                if (entry.getValue().isDone()) {
                    try {
                        var world = FutureUtil.getUnchecked(entry.getValue());
                        builder.append(Component.text(" (" + world.getClass().getSimpleName() + ")"));
                        world.appendDebugText(builder);
                    } catch (Throwable e) {
                        builder.append(Component.text(": (failed to allocate)"));
                        builder.append(Component.text("  ᴇʀʀᴏʀ: " + e.getMessage()));
                    }
                } else {
                    builder.append(Component.text(": (loading)"));
                }
            }
            player.sendMessage(builder);
        } finally {
            worldLock.unlock();
        }
    }

    private static final class SafePointTickException extends RuntimeException {
        public SafePointTickException(Throwable cause) {
            super(cause);
        }
    }

    @RuntimeGson
    private record MapJoinInfoMessage(
        @NotNull String serverId, @NotNull String playerId,
        @NotNull String mapId, @NotNull String state) {
    }

    private void onMapJoinMessage(@NotNull Message msg, @NotNull MapJoinInfoMessage message) {
        if (!AbstractHttpService.hostname.equals(message.serverId())) return; // Not for this server, ignore.

        logger.info("received join info for {}: {}", message.playerId(), message);
        var pendingJoin = getPendingJoin(message.playerId(), true);
        pendingJoin.complete(new MapJoinInfo(message.playerId(), message.mapId(), message.state()));
    }

}

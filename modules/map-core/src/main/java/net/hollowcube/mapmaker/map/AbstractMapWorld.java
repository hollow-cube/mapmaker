package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandlerRegistry;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.map.monitoring.MapCoreJFR;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.map.util.spatial.Octree;
import net.hollowcube.mapmaker.map.util.spatial.SpatialObject;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.FeatureFlag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Weather;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.thread.TickSchedulerThread;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import static net.hollowcube.mapmaker.map.util.spatial.Octree.simpleOctree;

@NotNullByDefault
public non-sealed abstract class AbstractMapWorld<S extends PlayerState<S, W>, W extends AbstractMapWorld<S, W>> implements MapWorld {
    @SuppressWarnings("rawtypes")
    protected static final Tag<PlayerState> PLAYER_INITIAL_STATE = Tag.Transient("player_initial_state");
    static final Tag<MapWorld> ROOT_MAP_WORLD_TAG = Tag.Transient("root_map_world");

    private final MapServer server;
    private final MapData map;
    private final MapInstance instance;

    private final Set<Player> players = new HashSet<>();
    private final Set<Player> playersImmutable = Collections.unmodifiableSet(players);
    private final EventNode<InstanceEvent> eventNode = EventNode.event(
        UUID.randomUUID().toString(), EventFilter.INSTANCE,
        event -> !(event instanceof PlayerEvent playerEvent) || players.contains(playerEvent.getPlayer())
    );

    private final Class<S> stateClass;
    private final List<Class<?>> stateSubclasses;
    private final Set<Player>[] playersByState;
    private final EventNode<PlayerInstanceEvent>[] eventNodesByState;
    private final Map<Player, S> playerStates = new HashMap<>();
    // pendingStateChanges is a map to deduplicate single tick changes (it should be
    // impossible to change a player's state multiple times per tick, latest wins).
    private final Map<Player, PlayerStateChange<S, W>> pendingStateChanges = new HashMap<>();
    private final Map<Player, CompletableFuture<@Nullable Void>> pendingRemovals = new ConcurrentHashMap<>();
    protected volatile boolean isClosed;

    private final BiomeContainer biomeContainer = new BiomeContainer();
    private final ItemRegistry itemRegistry = new ItemRegistry();
    private final ObjectEntityHandlerRegistry objectEntityHandlerRegistry = new ObjectEntityHandlerRegistry();

    private Octree octree = Octree.emptyOctree();
    private boolean octreeDirty = true;

    private @Nullable List<BossBar> bossBars;

    @SuppressWarnings("unchecked")
    protected AbstractMapWorld(MapServer server, MapData map, MapInstance instance, Class<S> stateClass) {
        this.server = server;
        this.map = map;
        this.instance = instance;

        this.stateClass = stateClass;
        this.stateSubclasses = collectRecursiveSealedSubclasses(stateClass);
        this.playersByState = new Set[stateSubclasses.size()];
        this.eventNodesByState = new EventNode[stateSubclasses.size()];
        for (int i = 0; i < stateSubclasses.size(); i++) {
            this.playersByState[i] = new HashSet<>();
            final var stateSubclass = stateSubclasses.get(i);
            this.eventNodesByState[i] = OpUtils.build(EventNode.event(
                UUID.randomUUID().toString(),
                EventUtil.PLAYER_INSTANCE_FILTER,
                (e) -> stateSubclass.isInstance(getPlayerState(e.getPlayer()))
            ), eventNode()::addChild);
        }

        // Only set the root tag if this is the root, otherwise we rely on the parent to
        // return this world in canonicalWorld implementations.
        instance.updateTag(ROOT_MAP_WORLD_TAG, existing ->
            Objects.requireNonNullElse(existing, this));

        instance.eventNode()
            .addChild(eventNode)
            .addChild(itemRegistry.eventNode())
            .addListener(PlayerInstanceLeaveEvent.class, this::handlePlayerLeave)
            .addListener(PlayerDeathEvent.class, this::handlePlayerDeath);

        configureInstance();
    }

    @Override
    public MapServer server() {
        return server;
    }

    @Override
    public MapData map() {
        return map;
    }

    @Override
    public MapInstance instance() {
        return instance;
    }

    @Override
    public EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public EventNode<PlayerInstanceEvent> eventNode(Class<? extends S> stateType) {
        return eventNodesByState[stateIndex(stateType)];
    }

    @Override
    public Octree collisionTree() {
        return this.octree;
    }

    @Override
    public void queueCollisionTreeRebuild() {
        this.octreeDirty = true;
    }

    @Override
    public @UnmodifiableView Collection<Player> players() {
        return playersImmutable;
    }

    //region Registries

    @Override
    public BiomeContainer biomes() {
        return biomeContainer;
    }

    @Override
    public ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    @Override
    public ObjectEntityHandlerRegistry objectEntityHandlers() {
        return objectEntityHandlerRegistry;
    }

    //endregion

    // region Player Lifecycle

    public @Nullable S getPlayerState(Player player) {
        return playerStates.get(player);
    }

    public final void changePlayerState(Player player, S nextState) {
        changePlayerState(player, nextState, (_, _) -> true);
    }

    public void changePlayerState(Player player, S nextState, BiPredicate<Player, S> predicate) {
        pendingStateChanges.merge(
            player,
            new PlayerStateChange<>(nextState, predicate),
            PlayerStateChange::handleConflict
        );
    }

    @Override
    public final void configurePlayer(AsyncPlayerConfigurationEvent event) {
        final var player = event.getPlayer();

        MapWorldHelpers.applyMapResourcePack(map, player);

        // Always add all feature flags to the world. We don't restrict what blocks/items people use.
        FeatureFlag.values().forEach(event::addFeatureFlag);

        event.setSpawningInstance(instance());

        var initialState = configurePlayer(player);
        player.setTag(PLAYER_INITIAL_STATE, initialState);

        // addPlayer is called during PlayerSpawnEvent meaning that the player is already in the instance,
        // and all of the entity `updateNewViewer` calls were already made. This makes it unsafe to call
        // MapWorld#forPlayer during viewer add which is unexpected and strange behavior.
        // To fix it, we disable auto entity viewing during config, and then reenable it after the player is added
        // to the world.
        player.setAutoViewEntities(false);
    }

    /// !! Implementations must set player respawn position.
    protected abstract S configurePlayer(Player player);

    @Override
    public void spawnPlayer(Player player) {
        Check.stateCondition(isClosed, "Cannot add player to closed world.");
        assert !this.players.contains(player);
        this.players.add(player);

        //noinspection unchecked
        final var initialState = (S) Objects.requireNonNull(player.getTag(PLAYER_INITIAL_STATE));

        var stateChangeEvent = new MapCoreJFR.StateChange(getClass(), null, initialState.getClass());
        stateChangeEvent.begin();
        try {
            playerStates.put(player, initialState);
            playersByState[stateIndex(initialState)].add(player);
            //noinspection unchecked
            initialState.configurePlayer((W) this, player, null);
        } finally {
            stateChangeEvent.end();
        }

        if (this.bossBars != null) bossBars.forEach(player::showBossBar);
    }

    @Override
    public CompletableFuture<Void> scheduleRemovePlayer(Player player) {
        return this.pendingRemovals.computeIfAbsent(player, _ -> new CompletableFuture<>());
    }

    @Override
    public void removePlayer(Player player) {
        if (!this.players.contains(player)) return;
        if (!(Thread.currentThread() instanceof TickSchedulerThread))
            throw new UnsupportedOperationException("removePlayer must be called from the scheduler thread!");

        if (this.bossBars != null) BossBars.clear(player);

        // Have to get and remove later because we need to still be in the state when resetting
        // them out of it (this is a contract of this interface). However this is OK because all
        // of this logic only happens during a safe point tick anyway.
        final var state = playerStates.get(player);

        var stateChangeEvent = new MapCoreJFR.StateChange(getClass(), state.getClass(), null);
        stateChangeEvent.begin();
        try {
            //noinspection unchecked
            state.resetPlayer((W) this, player, null);
            playersByState[stateIndex(state)].remove(player);
            pendingStateChanges.remove(player);
            playerStates.remove(player);
        } finally {
            stateChangeEvent.commit();
        }

        this.players.remove(player);
    }

    @Override
    @NonBlocking
    public TaskSchedule safePointTick() {
        if (isClosed) return TaskSchedule.stop();

        MapWorld.super.safePointTick();

        // Rebuild octree for new entities.
        if (octreeDirty) {
            List<SpatialObject> allObjects = new ArrayList<>();
            for (var entity : instance().getEntities()) {
                if (entity.isRemoved() || !(entity instanceof SpatialObject spatial)) continue;
                if (spatial.boundingBox().size().isZero()) continue;
                allObjects.add(spatial);
            }

            var size = OpUtils.mapOr(map().settings().getSize(),
                MapSize::size, MapSize.NORMAL.size());
            var powerOfTwo = (int) Math.ceil(Math.log(Math.min(size, 4096)) / Math.log(2));
            this.octree = simpleOctree(powerOfTwo, allObjects);
            this.octreeDirty = false;
        }

        var pendingRemoveIter = pendingRemovals.entrySet().iterator();
        while (pendingRemoveIter.hasNext()) {
            var pendingLeave = pendingRemoveIter.next();
            removePlayer(pendingLeave.getKey());
            pendingLeave.getValue().complete(null);
            pendingRemoveIter.remove();
        }

        // Apply pending state changes.
        var iter = pendingStateChanges.entrySet().iterator();
        while (iter.hasNext()) {
            final var entry = iter.next();
            final var player = entry.getKey();
            final var lastState = playerStates.get(player);
            final var change = entry.getValue();
            final var nextState = change.state();

            var stateChangeEvent = new MapCoreJFR.StateChange(getClass(), lastState.getClass(), nextState.getClass());
            stateChangeEvent.begin();
            try {
                if (change.canChange(player)) {
                    lastState.resetPlayer((W) this, player, nextState);
                    playersByState[stateIndex(lastState)].remove(player);

                    // todo what should we do if theres a failure here?

                    playerStates.put(player, nextState);
                    playersByState[stateIndex(nextState)].add(player);
                    nextState.configurePlayer((W) this, player, lastState);
                }
            } finally {
                stateChangeEvent.commit();
                iter.remove();
            }
        }

        return TaskSchedule.nextTick();
    }

    private void handlePlayerLeave(PlayerInstanceLeaveEvent event) {
        // Only the canonical instance should actually handle removing the player.
        if (MapWorld.forInstance(event.getInstance()) != this) return;
        if (!players.contains(event.getPlayer())) return; // Sanity

        scheduleRemovePlayer(event.getPlayer());
    }

    private void handlePlayerDeath(PlayerDeathEvent event) {
        event.setChatMessage(null);
    }

    // endregion

    // region World Lifecycle

    protected void configureInstance() {
        instance().setGenerator(MapGenerators.voidWorld());

        var diameter = map().settings().getSize().size();
        instance().setWorldBorder(new WorldBorder(diameter,
            0f, 0f, 0,
            0, ServerFlag.WORLD_BORDER_SIZE
        ));

        instance().setTime(switch (map().getSetting(MapSettings.TIME_OF_DAY)) {
            case NOON -> 6000;
            case SUNRISE -> 23000;
            case SUNSET -> 13000;
            case NIGHT -> 18000;
        });

        instance().setWeather(switch (map().getSetting(MapSettings.WEATHER_TYPE)) {
            case CLEAR -> new Weather(0, 0);
            case RAINING -> new Weather(1f, 0f);
            case THUNDERSTORM -> new Weather(1f, 1f);
        }, 1);
    }

    @Blocking
    public void loadWorld() {
        loadWorldData();

        this.bossBars = createBossBars();
    }

    public void close() {
        if (!(Thread.currentThread() instanceof TickSchedulerThread))
            throw new UnsupportedOperationException("close must be called from the scheduler thread!");
        if (!players().isEmpty())
            throw new IllegalStateException("Cannot close a map world with players!");
        this.isClosed = true;

        // At the beginning of the _next_ tick (as in, after this safe point tick), unregister the instance.
        MinecraftServer.getSchedulerManager().scheduleNextTick(() ->
            MinecraftServer.getInstanceManager().unregisterInstance(instance()));
    }

    protected @Nullable List<BossBar> createBossBars() {
        return List.of();
    }

    protected void loadWorldData() {
        var mapData = server().mapService().getMapWorldAsStream(map().id(), false);
        if (mapData == null) return;

        instance().loadStream(mapData, new ReadWorldAccess(this));
    }

    public void loadWorldTag(TagReadable tag) {
        biomes().init(tag);
    }

    public void saveWorldTag(TagWritable tag) {
        biomes().write(tag);
    }

    // endregion

    private int stateIndex(S state) {
        return stateIndex((Class<? extends S>) state.getClass());
    }

    private int stateIndex(Class<? extends S> state) {
        int index = stateSubclasses.indexOf(state);
        Check.argCondition(index == -1, "State {0} is not a permitted subclass of {1}", state, stateClass);
        return index;
    }

    protected static MapInstance makeMapInstance(MapData map, char classifier) {
        return makeMapInstance(map, classifier, null);
    }

    protected static MapInstance makeMapInstance(
        MapData map, char classifier,
        @Nullable MapInstance.LightingMode lightingOverride
    ) {
        var lightingMode = Objects.requireNonNullElseGet(lightingOverride,
            () -> map.getSetting(MapSettings.LIGHTING) ? MapInstance.LightingMode.GENERATED : MapInstance.LightingMode.FULL_BRIGHT);
        return new MapInstance(map.createDimensionName(classifier), lightingMode);
    }

    private static List<Class<?>> collectRecursiveSealedSubclasses(Class<?> stateClass) {
        Check.argCondition(!stateClass.isSealed(), "stateClass must be sealed");
        var stateSubclasses = new HashSet<>(List.of(stateClass.getPermittedSubclasses()));
        int size;
        do {
            size = stateSubclasses.size();
            for (var subclass : stateClass.getPermittedSubclasses()) {
                if (subclass.isSealed() && subclass.isInterface())
                    stateSubclasses.addAll(List.of(subclass.getPermittedSubclasses()));
            }

        } while (size != stateSubclasses.size());
        return List.copyOf(stateSubclasses);
    }

    public void appendDebugText(TextComponent.Builder builder) {
        builder.appendNewline().append(Component.text("  ᴀɢᴇ: " + NumberUtil.formatDuration(instance().getWorldAge() * 50)));
        builder.appendNewline()
            .append(Component.text("  ᴘʟᴀʏᴇʀѕ: " + players().size()))
            .append(Component.text(" ɪɴꜱᴛᴀɴᴄᴇ: " + instance().getPlayers().size()));
    }
}

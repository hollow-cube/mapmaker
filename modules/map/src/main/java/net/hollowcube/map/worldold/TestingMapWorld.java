package net.hollowcube.map.worldold;

public class TestingMapWorld {
//    private static final System.Logger logger = System.getLogger(TestingMapWorld.class.getName());
//
//    // If set, indicates that the player is an editor.
//    private static final Tag<Boolean> TAG_TESTING = Tag.Transient("testing");
//
//    private int flags;
//
//    private final EditingMapWorld parent;
//    private final Instance instance;
//
//    private final Set<Player> activePlayers = Collections.synchronizedSet(new HashSet<>());
//
//    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
//    private final ItemRegistry itemRegistry;
//    private final EventNode<InstanceEvent> eventNode = EventNode.event("world-local-test", EventFilter.INSTANCE, ev -> {
//        if (ev instanceof PlayerEvent event) {
//            return event.getPlayer().hasTag(TAG_TESTING);
//        }
//        return true;
//    });
//
//    TestingMapWorld(@NotNull EditingMapWorld parent) {
//        this.flags |= FLAG_TESTING | FLAG_PLAYING;
//
//        this.parent = parent;
//        this.instance = parent.instance();
//        // do NOT set self tag, the instance is "officially" owned by the editing world.
//
//        this.instance.eventNode().addChild(eventNode);
//
//        this.itemRegistry = new ItemRegistry();
//        eventNode.addChild(itemRegistry.eventNode());
//    }
//
//    @Override
//    public @NotNull MapServer server() {
//        return parent.server();
//    }
//
//    @Override
//    public @NotNull MapData map() {
//        return parent.map();
//    }
//
//    @Override
//    public int flags() {
//        return flags;
//    }
//
//    @Override
//    public @NotNull ItemRegistry itemRegistry() {
//        return itemRegistry;
//    }
//
//    @Override
//    public @NotNull BiomeContainer biomes() {
//        return parent.biomes(); // Always share with the parent.
//    }
//
//    @Override
//    public void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode) {
//        this.eventNode.addChild(eventNode);
//    }
//
//    @Override
//    public @NotNull Instance instance() {
//        return instance;
//    }
//
//    @Override
//    public @NotNull Pos spawnPoint() {
//        return parent.spawnPoint();
//    }
//
//    @Override
//    public void load() {
//        this.enabledFeatures.addAll(MapWorldHelpers.loadFeatures(this));
//    }
//
//    @Override
//    public void close(boolean shutdown) {
//        // Do not unregister instance, it is owned by the parent.
//
//        // todo do we need to boot players here?
//        for (var player : Set.copyOf(activePlayers)) {
//            removePlayer(player);
//            if (shutdown) {
//                EventDispatcher.call(new PlayerDisconnectEvent(player)); // todo why isnt this done by Minestom
//                player.kick(Component.translatable("mapmaker.shutdown"));
//            }
//        }
//    }
//
//    @Override
//    public @Nullable MapWorld getMapForPlayer(@NotNull Player player) {
//        return activePlayers.contains(player) ? this : null;
//    }
//
//    @Override
//    public void acceptPlayer(@NotNull Player player, boolean firstSpawn) {
//        var playerData = PlayerDataV2.fromPlayer(player);
//
//        // The save state for verifications needs to be created remotely, but for local testing we can create it here.
//        // todo in the future verification should be done in a VerificationMapWorld or PlayingMapWorld probably.
//        SaveState saveState;
//        if (map().verification() == MapVerification.PENDING) {
//            saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());
//        } else {
//            saveState = new SaveState(
//                    UUID.randomUUID().toString(), playerData.id(), map().id(),
//                    SaveStateType.PLAYING
//            );
//        }
//
//        activePlayers.add(player);
//        player.setTag(TAG_TESTING, true);
//        player.setTag(MapHooks.PLAYING, true); // For compatibility
//        player.setTag(SaveState.TAG, saveState);
//
//        MapWorldHelpers.resetPlayer(player);
//
//        player.setGameMode(GameMode.ADVENTURE);
//
//        var startingPos = player.getPosition();
//        player.teleport(startingPos);
//
//        EventDispatcher.call(new MapPlayerInitEvent(this, player, true));
//    }
//
//    @Override
//    public void removePlayer(@NotNull Player player) {
//        EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));
//
//        player.removeTag(MapHooks.PLAYING);
//        player.removeTag(TAG_TESTING);
//        activePlayers.remove(player);
//
//        // Save their save state if this is a pending verification
//        var saveState = SaveState.optionalFromPlayer(player);
//        if (saveState == null || map().verification() != MapVerification.PENDING) return;
//
//        saveState.updatePlaytime();
//
//        var update = new SaveStateUpdateRequest();
//        update.setPlaytime(saveState.getPlaytime());
//        update.setCompleted(saveState.isCompleted());
//
//        try {
//            var playerData = PlayerDataV2.fromPlayer(player);
//            parent.server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), update);
//            logger.log(System.Logger.Level.INFO, "Updated testing savestate for {0}", player.getUuid());
//        } catch (Exception e) {
//            logger.log(System.Logger.Level.ERROR, "Failed to save player state for {0}", player.getUuid(), e);
//        }
//
//        player.removeTag(SaveState.TAG);
//    }
//
//
//    public void exitTestMode(@NotNull Player player) {
//        Thread.startVirtualThread(() -> movePlayerToBuildWorld(player));
//    }
//
//    private @Blocking void movePlayerToBuildWorld(@NotNull Player player) {
//        // remove from this map (leaving them in the Minestom instance)
//        removePlayer(player);
//
//        // add to the test world
//        parent.acceptPlayer(player, false);
//    }
//
//    @Override
//    public @NotNull Set<Player> players() {
//        return Set.copyOf(activePlayers);
//    }
}

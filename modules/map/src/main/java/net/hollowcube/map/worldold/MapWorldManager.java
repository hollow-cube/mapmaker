package net.hollowcube.map.worldold;

public class MapWorldManager {
//    private static final System.Logger logger = System.getLogger(MapWorldManager.class.getName());
//    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
//
//    public record InstanceId(String id, boolean isEditing) {
//    }
//
//    private final Map<InstanceId, Future<InternalMapWorld>> activeMaps = new ConcurrentHashMap<>();
//    private final MapServer server;
//
//    public MapWorldManager(@NotNull MapServer server) {
//        this.server = server;
//
//        MinecraftServer.getGlobalEventHandler().addListener(PlayerInstanceLeaveEvent.class, event -> {
//            // Get the world from the instance because 1: the player is no longer in a world, and 2: we care about the root world (editing, not testing)
//            var world = MapWorld.unsafeFromInstance(event.getInstance());
//            if (world == null) return;
//
//            // If the owner has left, boot all invited players
//            if (event.getPlayer().getUuid().equals(UUID.fromString(world.map().owner())) && !world.map().isPublished()) {
//                for (var player : world.players()) {
//                    // I believe the owner is still considered on the players list when leaving, so check just in case
//                    if (player == event.getPlayer()) continue;
//                    player.sendMessage(Component.translatable("map.kicked"));
//                    server.bridge().sendPlayerToHub(player);
//                }
//            }
//
//            // Stop if there are still players in the instance
//            if (event.getInstance().getPlayers().size() > 1) return;
//
//            closeMap(world);
//        });
//    }
//
//    public Map<InstanceId, Future<InternalMapWorld>> getActiveMaps() {
//        return activeMaps;
//    }
//
//    public boolean hasMap(@NotNull String mapId, boolean editing) {
//        return activeMaps.containsKey(new InstanceId(mapId, editing));
//    }
//
//    @Blocking
//    public @NotNull Future<InternalMapWorld> getOrCreateMapWorld(@NotNull MapData map, ServerBridge.JoinMapState state) {
//        boolean isEditing = state == HubToMapBridge.JoinMapState.EDITING;
//        var activeWorld = activeMaps.get(new InstanceId(map.id(), isEditing));
//
//        // Create a new world if there is not one present
//        if (activeWorld == null) {
//            var world = isEditing ? new EditingMapWorld(server, map) : new PlayingMapWorld(server, map);
//            activeWorld = VIRTUAL_EXECUTOR.submit(() -> {
//                world.load();
//                return world;
//            });
//            activeMaps.put(new InstanceId(map.id(), isEditing), activeWorld);
//        }
//
//        return activeWorld;
//    }
//
//    public @Blocking void joinMap(@NotNull Player player, @NotNull MapData map, HubToMapBridge.JoinMapState joinMapState) {
//        var activeWorld = getOrCreateMapWorld(map, joinMapState);
//
//        // Spawn player in world with loading screen (todo this should be blindness + stop player from moving i guess)
//        try {
//            // If the player is already in a map, remove them from it.
//            var currentWorld = MapWorld.forPlayerOptional(player);
//            if (currentWorld instanceof InternalMapWorld imw) {
//                imw.removePlayer(player);
//            }
//
//            player.setTag(MapHooks.TARGET_WORLD, activeWorld);
//            player.startConfigurationPhase();
//
////            var world = activeWorld.get(); // wait for the world to load
////
////
////            // spawn in minestom instance & then notify world
////            player.setInstance(world.instance(), world.spawnPoint()).join();
////            // Have the spectate functionality extracted to InternalMapWorld (probably)
////            if (joinMapState == HubToMapBridge.JoinMapState.SPECTATING && world instanceof PlayingMapWorld playingMapWorld) {
////                playingMapWorld.startSpectating(player, false);
////            } else {
////                world.acceptPlayer(player, true);
////            }
//        } catch (Exception e) {
//            logger.log(System.Logger.Level.ERROR, "Failed to load world", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void forceShutdownMap(@NotNull String mapId) {
//        for (var entry : activeMaps.entrySet()) {
//            if (!entry.getKey().id.equals(mapId)) return;
//
//            try {
//                var world = entry.getValue().get();
//                logger.log(System.Logger.Level.WARNING, "Forcing shutdown of map world {} ({})", mapId, world);
//                closeMap(world);
//            } catch (ExecutionException | InterruptedException e) {
//                MinecraftServer.getExceptionManager().handleException(e);
//            }
//        }
//    }
//
//    private void closeMap(@NotNull MapWorld world) {
//        for (var player : world.players()) {
//            player.sendMessage(Component.translatable("map.closed"));
//            //TODO this gets called when someone requests to join someone's map when playing, which is weird behavior
//            server.bridge().sendPlayerToHub(player);
//        }
//
//        var removed = activeMaps.remove(new InstanceId(world.map().id(), (world.flags() & MapWorld.FLAG_EDITING) != 0));
//        if (removed == null) return;
//        world.instance().scheduleNextTick(unused -> Thread.startVirtualThread(() -> {
//            // ok to use resultNow because we cannot close a world that is not loaded
//            // and a loaded world will always have a completed future.
//            removed.resultNow().close(false);
//        }));
//    }
//
//    public void shutdown() {
//        for (var mapFuture : activeMaps.values()) {
//            try {
//                var world = mapFuture.get();
//                world.close(true);
//            } catch (Exception e) {
//                logger.log(System.Logger.Level.ERROR, "Failed to close world", e);
//            }
//        }
//    }
}

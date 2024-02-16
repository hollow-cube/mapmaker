package net.hollowcube.mapmaker.map;

@SuppressWarnings({"UnstableApiUsage", "FieldCanBeLocal"})
class MapServerImpl {
//    private static final Logger logger = LoggerFactory.getLogger(MapServerImpl.class);
//
//    private static final Tag<MapWorld> TARGET_WORLD_TAG = Tag.Transient("mapmaker:target_world");
//
//
//    private MapJoinConsumer mapJoinConsumer;
//
//
//    // A pending join is a map of player id to a future that will be completed when the server has the join info.
//    // The future returned will never complete with an exception, but may complete with null indicating that we
//    // timed out while waiting for the player info to be received.
//    //
//    // This will block the player from joining until the server has confirmed that it should have them and will
//    // all happen during login (Minestom pre login event).
//    // Note that we will separately hold them in the configuration phase until the map world is ready.
//    private final Map<String, CompletableFuture<@Nullable MapJoinInfo>> pendingPlayerJoins = new ConcurrentHashMap<>();
//
//    @Override
//    public @Blocking void start(@NotNull ConfigLoaderV3 config) {
//        if (!noopServices) mapJoinConsumer = new MapJoinConsumer(kafkaConfig.bootstrapServersStr());
//
//    }
//
//    @Override
//    public void shutdown() {
//        mapJoinConsumer.close();
//    }
//
//    private @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId, boolean deleteCompleted) {
//        var noop = mapService instanceof NoopMapService;
//        if (noop) {
//            //todo
//            return CompletableFuture.completedFuture(new MapJoinInfo(
//                    playerId,
//                    "62da0aaf-8cad-4c13-869c-02b07688988d",
//                    "editing"
//            ));
//        }
//
//        var pendingJoin = pendingPlayerJoins.computeIfAbsent(playerId, id -> {
//            var future = new CompletableFuture<MapJoinInfo>();
//            //todo the futures are never actually removed from the map.
//            // invalid to do below because it will remove the future before we can handle it in login event.
////            future.whenComplete((v, e) -> pendingPlayerJoins.remove(id));
//            return future;
//        });
//        if (deleteCompleted && pendingJoin.isDone()) {
//            pendingPlayerJoins.remove(playerId);
//            pendingJoin = pendingPlayerJoins.computeIfAbsent(playerId, id -> new CompletableFuture<>());
//        }
//
//        return pendingJoin;
//    }
//
//    private void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
//        var player = event.getPlayer();
//        var playerId = player.getUuid().toString();
//
//        // === APPLY RESOURCE PACK ===
//
//        var joinInfo = FutureUtil.getUnchecked(getPendingJoin(playerId, false));
//        if (joinInfo == null) {
//            logger.error("timed out waiting for join info for {}", playerId);
//            player.kick(Component.text("Failed to join. Please try again later."));
//            return;
//        }
//
//        // === TRANSFER SESSION AND INIT ===
//
//        // Create the world, holding the player here until it is ready for them to join.
//        var map = mapService.getMap(joinInfo.playerId(), joinInfo.mapId());
//        var pendingWorld = worldManager().getOrCreateMapWorld(map, Presence.MAP_BUILDING_STATES.contains(joinInfo.state())
//                ? ServerBridge.JoinMapState.EDITING : ServerBridge.JoinMapState.PLAYING);
//        var mapWorld = Objects.requireNonNull(FutureUtil.getUnchecked(pendingWorld));
//
//        // === MAPWORLD.CONFIGUREPLAYER ===
//    }
//
//
//    private void handlePlayerPluginMessage(@NotNull PlayerPluginMessageEvent event) {
//        if (!event.getIdentifier().equals("mapmaker:transfer")) return;
//        var player = event.getPlayer();
//
//        // This is only sent when it is a failure.
//        player.sendMessage(Component.text("failed to join map!"));
//    }
//
//    // {"server_id":"map-c89db8f95-g92xs","player_id":"aceb326f-da15-45bc-bf2f-11940c21780c","map_id":"ddd0419e-499c-4292-87af-411bbfb362d2","state":"editing"}
//    private record MapJoinInfoMessage(@NotNull String serverId, @NotNull String playerId, @NotNull String mapId,
//                                      @NotNull String state) {
//    }
//
//    private class MapJoinConsumer extends BaseConsumer<MapJoinInfoMessage> {
//
//        protected MapJoinConsumer(@NotNull String bootstrapServers) {
//            super("map-join", AbstractHttpService.hostname, s -> AbstractHttpService.GSON.fromJson(s, MapJoinInfoMessage.class), bootstrapServers);
//        }
//
//        @Override
//        protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull MapJoinInfoMessage message) {
//            if (!AbstractHttpService.hostname.equals(message.serverId())) return; // Not for this server, ignore.
//
//            logger.info("received join info for {}: {}", message.playerId(), message);
//            var pendingJoin = getPendingJoin(message.playerId(), true);
//            pendingJoin.complete(new MapJoinInfo(message.playerId(), message.mapId(), message.state()));
//        }
//    }
}

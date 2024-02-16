package net.hollowcube.map;

@SuppressWarnings("FieldCanBeLocal")
public abstract class MapServerBase {
//    private static final BlockManager BLOCK_MANAGER = MinecraftServer.getBlockManager();
//
//    private static final Logger logger = LoggerFactory.getLogger(MapServerBase.class);
//
//    private final EventNode<Event> eventNode = EventNode.all("mapmaker:map")
//            .addListener(PlayerSpawnEvent.class, this::handleSpawn);
//
//    private final MapWorldManager mwm = new MapWorldManager(this);
//
//    private MapMgmtConsumerImpl mapMgmtConsumer;
//    private List<FeatureProvider> features;
//
//    private Controller guiController;
//
//    // Terraform
//    private Terraform terraform;
//
//    private Injector injector;
//
//    static {
//        // Idk why the static initializer is not triggering from other usages
//        //noinspection DataFlowIssue
//        new PlayerSpawnInInstanceEvent(null);
//    }
//
//    public @Blocking void init(@NotNull ConfigLoaderV3 config, @NotNull CommandManager commandManager) {
//        MapServer.StaticAbuse.instance = this;
//
//        boolean noopServices = Boolean.getBoolean("mapmaker.noop");
//
//        var unleashConfig = config.get(UnleashConfig.class);
//        if (unleashConfig.enabled()) {
//            logger.info("Unleash is enabled, loading feature flag provider");
//            var provider = new UnleashFeatureFlagProvider(unleashConfig);
//            FeatureFlagProvider.replaceGlobals(provider);
//        }
//
//        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
//        globalEventHandler.addChild(eventNode);
//
//        // Terraform initialization
//        var terraformEvents = EventNode.event("mapmaker:map/terraform", EventFilter.INSTANCE,
//                eventFilter(false, true, false));
//        globalEventHandler.addChild(terraformEvents);
//        terraform = Terraform.builder()
//                .rootEventNode(terraformEvents)
//                .rootCommandManager(commandManager)
//                .globalCommandCondition(mapFilter(false, true, false))
//                .module(Terraform.BASE_MODULE)
//                .module(Terraform.AXIOM_MODULE)
//                .module(Terraform.WORLDEDIT_MODULE)
//                .module(MapServerModule::new)
//                .storage(mapService() instanceof NoopMapService ? "TerraformStorageMemory" : "TerraformStorageHttp")
//                .build();
//
//        this.guiController = Controller.make(Map.of(
//                "mapServer", this,
//                "mapService", mapService(),
//                "playerService", playerService(),
//                "bridge", bridge()
//        ));
//
//        this.injector = Guice.createInjector(new AbstractModule() {
//            @Override
//            protected void configure() {
//                bind(MapServer.class).toInstance(MapServerBase.this);
//                bind(MapServerBase.class).toInstance(MapServerBase.this);
//
//                bind(MapWorldManager.class).toInstance(worldManager());
//                bind(Controller.class).toInstance(guiController);
//                bind(PlayerInviteService.class).toInstance(inviteService());
//                bind(ConfigLoaderV3.class).toInstance(config);
//                bind(PermManager.class).toInstance(permManager());
//                bind(PlayerService.class).toInstance(playerService());
//                bind(SessionManager.class).toInstance(sessionManager());
//                bind(CommandManager.class).toInstance(commandManager);
//                bind(MapService.class).toInstance(mapService());
//
//                bind(MapToHubBridge.class).toInstance(bridge());
//                bind(ServerBridge.class).toInstance(bridge());
//            }
//        });
//
//        // Map management update listener
//        var kafkaConfig = config.get(KafkaConfig.class);
//        if (!noopServices) mapMgmtConsumer = new MapMgmtConsumerImpl(kafkaConfig.bootstrapServersStr(), this);
//
//        // Block/item rules
//        PlacementRules.init(terraform);
//        var interactionEvents = EventNode.event("mapmaker:map/interaction", EventFilter.INSTANCE,
//                eventFilter(false, true, false));
//        globalEventHandler.addChild(interactionEvents);
//        InteractionRules.register(interactionEvents);
//
//        // Entities
//        var entityEvents = EventNode.type("mapmaker:map/entity", EventFilter.INSTANCE);
//        globalEventHandler.addChild(entityEvents);
//        MapEntities.init(entityEvents);
//        eventNode.addChild(PotionHandler.EVENT_NODE);
//        eventNode.addListener(InventoryPreClickEvent.class, event -> {
//            if (event.getInventory() != null) return;
//            event.setCancelled(true);
//        });
//
//        // Common commands
//        commandManager.register(new HelpCommand(commandManager));
//        commandManager.register(injector.getInstance(EmojisCommand.class));
//        commandManager.register(injector.getInstance(MinestomCommand.class));
//        commandManager.register(createDebugCommand());
//        commandManager.register(injector.getInstance(PingCommand.class));
//
//        commandManager.register(injector.getInstance(PlayCommand.class));
//        commandManager.register(injector.getInstance(WhereCommand.class));
//        commandManager.register(injector.getInstance(TopTimesCommand.class));
//        commandManager.register(injector.getInstance(ListCommand.class));
//
//        commandManager.register(injector.getInstance(RequestCommand.class));
//        commandManager.register(injector.getInstance(RejectCommand.class));
//        commandManager.register(injector.getInstance(InviteCommand.class));
//        commandManager.register(injector.getInstance(AcceptCommand.class));
//        commandManager.register(injector.getInstance(JoinCommand.class));
//        commandManager.register(injector.getInstance(RemoveCommand.class));
//
//        var mapCommand = injector.getInstance(MapCommand.class);
////        mapCommand.info.addSyntax(CommandDsl.playerOnly(MapListCommandMixin::showMapInfoAboutCurrent));
//        commandManager.register(mapCommand);
//
//        commandManager.register(injector.getInstance(StoreCommand.class));
//
//        // Map specific commands
//        commandManager.register(injector.getInstance(HubCommand.class));
//
//        commandManager.register(injector.getInstance(TestCommand.class));
//        commandManager.register(injector.getInstance(BuildCommand.class));
//        commandManager.register(injector.getInstance(SetSpawnCommand.class));
//
//        commandManager.register(injector.getInstance(FlyCommand.class));
//        commandManager.register(injector.getInstance(FlySpeedCommand.class));
//        commandManager.register(injector.getInstance(ClearInventoryCommand.class));
//        commandManager.register(injector.getInstance(SpawnCommand.class));
//        commandManager.register(injector.getInstance(TeleportCommand.class));
//        commandManager.register(injector.getInstance(GiveCommand.class));
//
//        commandManager.register(injector.getInstance(PHeadCommand.class));
//
//        commandManager.register(injector.getInstance(BiomesCommand.class));
//        commandManager.register(injector.getInstance(SetBiomeCommand.class));
//
//        // Register features
//        var features = new ArrayList<FeatureProvider>();
//        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
//            for (var feature : ServiceLoader.load(FeatureProvider.class)) {
//                features.add(feature);
//                for (var blockHandler : feature.blockHandlers()) {
//                    BLOCK_MANAGER.registerHandler(blockHandler.getNamespaceId(), () -> blockHandler);
//                }
//                scope.fork(Executors.callable(() -> feature.init(config)));
//            }
//
//            scope.join();
//            this.features = List.copyOf(features);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        } catch (Exception e) {
//            logger.error("Failed to initialize features", e);
//            throw new RuntimeException(e);
//        }
//
//        ChatAnnouncer.setupAnnouncements(config, sessionManager());
//
//        // Sync sessions with remote
//        sessionManager().sync();
//    }
//
//    @Override
//    public @NotNull List<FeatureProvider> features() {
//        if (features == null) {
//            return List.of();
//        }
//        return features;
//    }
//
//    @Override
//    public @NotNull Terraform terraform() {
//        return this.terraform;
//    }
//
//    public @NotNull MapWorldManager worldManager() {
//        return mwm;
//    }
//
//    public @Blocking void joinMap(@NotNull Player player, @NotNull MapData map, HubToMapBridge.JoinMapState joinMapState) {
//        mwm.joinMap(player, map, joinMapState);
//    }
//
//    private void handleSpawn(@NotNull PlayerSpawnEvent event) {
//        // Spawn event is not an InstanceEvent, so we need to filter it.
//        if (MapWorld.unsafeFromInstance(event.getSpawnInstance()) == null)
//            return;
//
//        var player = event.getPlayer();
//        player.refreshCommands();
//
//        // This is invalid because the player has not actually entered the map, so forPlayer fails.
////        var map = MapWorld.forPlayer(event.getPlayer()).map();
////        if (map.isPublished()) {
////            Scoreboards.showPlayerPlayingScoreboard(player, map);
////        } else {
////            Scoreboards.showPlayerEditingScoreboard(player, map);
////        }
//    }
//
//    @Override
//    public void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
//        guiController.show(player, viewProvider);
//    }
//
//    public void shutdown() {
//        mapMgmtConsumer.close();
//        mwm.shutdown();
//    }
//
//    private @NotNull DebugCommand createDebugCommand() {
//        var cmd = new DebugCommand(playerService(), permManager(), mapService());
//
//        cmd.createPermissionlessSubcommand("world", (player, context) -> {
//            var world = MapWorld.forPlayerOptional(player);
//            if (world == null) {
//                player.sendMessage("You are not in a map world!");
//                return;
//            }
//
//            player.sendMessage(Component.text("Map: ").append(Component.text(world.map().id())));
//            player.sendMessage("Type: " + world.getClass().getSimpleName());
//        });
//
//        return cmd;
//    }

}

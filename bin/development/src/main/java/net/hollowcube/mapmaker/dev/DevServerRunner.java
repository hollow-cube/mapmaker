package net.hollowcube.mapmaker.dev;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.HubServerRunner;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.LocalMapAllocator;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DevServerRunner extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(DevServerRunner.class);

    // Hub stuff
    private HubMapWorld hubWorld;

    // Map stuff
    private Terraform terraform;
    private FeatureList features;

    // Common stuff
    private final CommandManager hubCommandManager = new CommandManagerImpl(super.commandManager());
    private final CommandManager mapCommandManager = new CommandManagerImpl(super.commandManager());

    public DevServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("dev-init")
                .addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin)
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
    }

    @Override
    protected @NotNull MapAllocator createAllocator() {
        return new LocalMapAllocator(this);
    }

    @Override
    protected @NotNull ServerBridge createBridge() {
        return new DevServerBridge(mapService(), allocator());
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager().setPlayerProvider((uuid, username, connection) -> new CommandHandlingPlayer(uuid, username, connection) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                var world = MapWorld.forPlayerOptional(this);
                return world == null || world instanceof HubMapWorld ? hubCommandManager : mapCommandManager;
            }
        });

        addBinding(Scheduler.class, MinecraftServer.getSchedulerManager());

        performMapInit(); // Map first so placements are registered
        performHubInit();
    }

    private void performHubInit() {
        this.hubWorld = allocator().allocateDirect(HubMapWorld.HUB_MAP_DATA, HubMapWorld.class);
        addBinding(HubMapWorld.class, hubWorld, "world", "hubWorld", "hubMapWorld");

        HubServerRunner.registerCommands(this, hubCommandManager);
        HubServerRunner.loadHubFeatures(this);
    }

    private void performMapInit() {
        this.terraform = MapServerRunner.initBuildLogic(mapService(), commandManager());
        addBinding(Terraform.class, terraform);

        MapServerRunner.registerCommands(this, mapCommandManager);

        this.features = FeatureList.load(config);
        addBinding(FeatureList.class, features);
        shutdowner().queue(features::close);
    }

    protected void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        // Note: The DevServer is still using sessions v1 because it is not running behind a proxy, meaning
        // transferSession in not a valid operation.
        // We explicitly do not call super.transferPlayerSession and basically reimplement it using v1 here.

        var player = event.getPlayer();
        try {
            var playerData = sessionService().createSession(
                    event.getPlayerUuid().toString(),
                    event.getUsername(),
                    "todo"
            );
            player.setTag(PlayerDataV2.TAG, playerData);

            var mapPlayerData = mapService().getMapPlayerData(playerData.id());
            player.setTag(MapPlayerData.TAG, mapPlayerData);

            var backpack = new PlayerBackpack(player);
            player.setTag(PlayerBackpack.TAG, backpack);
            backpack.update(playerService().getPlayerBackpack(playerData.id()));
        } catch (SessionService.UnauthorizedError ignored) {
            player.kick(Component.text("The server is currently in a closed beta.\nVisit ")
                    .append(Component.text("hollowcube.net").clickEvent(ClickEvent.openUrl("https://hollowcube.net/")))
                    .append(Component.text(" for more information.")));
        } catch (Exception e) {
            logger.error("failed to create session", e);
            player.kick(Component.text("Failed to login. Please try again later."));
        }
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();

            var targetWorld = player.getTag(DevServerBridge.TARGET_WORLD);
            if (targetWorld == null) {
                hubWorld.configurePlayer(event); // Spawn into hub
                return;
            }

            // Spawn the player into the targeted map
            var world = Objects.requireNonNull(FutureUtil.getUnchecked(targetWorld));
            world.configurePlayer(event);
        } catch (Exception e) {
            logger.error("Error during config phase", e);
            event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
        }
    }

    protected void handleSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());

//        var tradeString = """
//                {"trades":[{"result":"head/hard_hat","inputs":{"cubits":50}},{"result":"head/crown","inputs":{"coins":60000,"nightmare_fuel":2}}]}
//                """;
//        var result = MerchantData.CODEC.decode(JsonOps.INSTANCE, new Gson().fromJson(tradeString, JsonObject.class));
//        guiController().show(event.getPlayer(), c -> new MerchantShopView(c, DFU.unwrap(result).getFirst()));
    }

    protected void handleDisconnect(@NotNull PlayerDisconnectEvent event) {
        // Logic required to delete session when using session v1 api.

        logger.info("disconnect - {}", event.getPlayer().getUsername());
        Runnable task = () -> {
            var player = event.getPlayer();
            var playerData = PlayerDataV2.fromPlayer(player);

            // There is just no need to do any of this if we arent shutting down.
            if (!shutdowner().isShuttingDown()) {
                Audiences.all().sendMessage(Component.translatable("chat.player.leave", playerData.displayName()));
                MiscFunctionality.broadcastTabList(Audiences.all());
            }

            try {
                sessionService().deleteSession(playerData.id());
            } catch (Exception e) {
                logger.error("Failed to close session for " + playerData.id(), e);
            }
        };

        if (shutdowner().isShuttingDown()) {
            task.run();
        } else {
            FutureUtil.submitVirtual(task);
        }
    }
}

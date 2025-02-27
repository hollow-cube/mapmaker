package net.hollowcube.mapmaker.dev;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.MojangUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.HubServerRunner;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.MapMgmtConsumerImpl;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.LocalMapAllocator;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
    protected @NotNull String name() {
        return "mapmaker-dev";
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

        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) -> new MapPlayerImplImpl(connection, gameProfile) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                var world = MapWorld.forPlayerOptional(this);
                return world == null || world instanceof HubMapWorld ? hubCommandManager : mapCommandManager;
            }
        });

        addBinding(Scheduler.class, MinecraftServer.getSchedulerManager());

        performMapInit(); // Map first so placements are registered
        performHubInit();

        var kafkaConfig = config.get(KafkaConfig.class);
        var mapMgmtConsumer = new MapMgmtConsumerImpl((LocalMapAllocator) allocator(), kafkaConfig.bootstrapServers());
        shutdowner().queue("map-mgmt-listener", mapMgmtConsumer::close);
    }

    private void performHubInit() {
        this.hubWorld = allocator().allocateDirect(HubMapWorld.HUB_MAP_DATA, HubMapWorld.CTOR);
        addBinding(HubMapWorld.class, hubWorld, "world", "hubWorld", "hubMapWorld");

        HubServerRunner.registerCommands(this, hubCommandManager, hubWorld, MinecraftServer.getSchedulerManager());
        HubServerRunner.loadHubFeatures(this, hubWorld);
    }

    private void performMapInit() {
        this.terraform = MapServerRunner.initBuildLogic(mapService(), commandManager());
        addBinding(Terraform.class, terraform);

        var hdb = new HeadDatabase(otel);
        addBinding(HeadDatabase.class, hdb, "headDatabase", "hdb");
        MapServerRunner.registerCommands(this, mapCommandManager, hdb);

        MapServerRunner.initFeatureFlagMonitor(bridge(), allocator());

        this.features = FeatureList.load(config);
        addBinding(FeatureList.class, features);
        shutdowner().queue("features", features::close);
    }

    protected void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        // DevServer is not running behind a proxy, so we need to handle the proxy side of the session interaction
        // on our own here.
        // Note that we dont transfer here, its deferred to config phase (and reconfig)

        var profile = event.getGameProfile();
        var playerId = profile.uuid().toString();
        net.minestom.server.entity.PlayerSkin skin = MojangUtil.getSkinFromUuid(playerId);

        sessionService().createSession(playerId, "devserver-integrated", profile.name(), "127.0.0.1",
                new PlayerSkin(Optional.ofNullable(skin).map(net.minestom.server.entity.PlayerSkin::textures),
                        Optional.ofNullable(skin).map(net.minestom.server.entity.PlayerSkin::signature))
        );
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();

            var targetWorld = player.getTag(DevServerBridge.TARGET_WORLD);
            if (targetWorld == null) {
                // Move the session to the hub and spawn the player
                var hubPresence = new Presence(Presence.TYPE_MAPMAKER_HUB, "__hub_unused__", "devserver", "hub");
                try {
                    super.transferPlayerSession(player, hubPresence);
                } catch (Throwable t) {
                    logger.error("Error transferring player to hub", t);
                    event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
                    return;
                }

                hubWorld.configurePlayer(event);

                return;
            }

            // Move session and spawn the player into the targeted map
            var world = Objects.requireNonNull(FutureUtil.getUnchecked(targetWorld));
            var joinType = world instanceof EditingMapWorld ? "editing" : "playing";
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, joinType, "devserver", world.map().id());
            super.transferPlayerSession(player, presence);

            world.configurePlayer(event);
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
        var player = event.getPlayer();
        super.handlePlayerDisconnect(player);

        // Again, need to implement the proxy part of the delete session flow
        FutureUtil.submitVirtual(() -> sessionService().deleteSession(player.getUuid().toString()));
    }

    @Override
    protected @NotNull DebugCommand createDebugCommand() {
        DebugCommand dbg = super.createDebugCommand();

        dbg.createPermissionedSubcommand("enableprogressaddition", (player, context) -> {
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) {
                player.sendMessage("You are not in a map world!");
                return;
            }

            if (world instanceof EditingMapWorld && world.canEdit(player)) {
                world.map().setSetting(MapSettings.PROGRESS_INDEX_ADDITION, true);
                player.sendMessage("Enabled progress addition");
            } else {
                player.sendMessage("You are not in an editing world!");
            }
        }, "Enables progress index add mode for the current map");

        dbg.createPermissionlessSubcommand("text", (player, context) -> {
            var inv = new Inventory(InventoryType.CHEST_1_ROW, Component.empty());
            player.openInventory(inv);

            var c = new AtomicInteger();
            player.scheduler()
                    .buildTask(() -> {
                        var children = new ArrayList<ComponentLike>();
                        int start = c.incrementAndGet();
                        for (int i = start; i < start + 255; i++) {
                            children.add(Component.text("i", TextColor.color(0x4E5A00 | ((i) & 0xFF))));
                            children.add(Component.text(FontUtil.computeOffset(-1)));
                        }
                        var t = Component.textOfChildren(children.toArray(new ComponentLike[0]));
                        inv.setTitle(t);
                    })
                    .repeat(TaskSchedule.tick(1))
                    .schedule();
        }, "");

        dbg.createPermissionlessSubcommand("a", (player, ignored) -> {
            ActionBar.forPlayer(player).addProvider(new ActionBar.Provider() {
                @Override public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
                    builder.pushColor(FontUtil.computeVerticalOffset(-50));
                    builder.append("Hello, world");
                    builder.popColor();
                }
            });
        }, "");


        var guiManager = new ScriptEngine().guiManager();
        dbg.createPermissionlessSubcommand("gui", (player, ignored) -> {
            guiManager.openGui(player, URI.create("guilib:///StoreView.js"));

            var inv = new Inventory(InventoryType.CHEST_6_ROW, DevServer.title);
            for (int i = 0; i < inv.getInnerSize(); i++) {
                inv.setItemStack(i, DevServer.itemList[i]);
            }
            player.openInventory(inv);
            inv.addInventoryCondition((player1, slot, clickType, inventoryConditionResult) -> {
                inventoryConditionResult.setCancel(true);

                player1.sendMessage("Clicked slot " + slot + " with click type " + clickType);
                DevServer.host.root.handleClick(clickType, slot % 9, slot / 9);

            });
        }, "");

        return dbg;
    }
}

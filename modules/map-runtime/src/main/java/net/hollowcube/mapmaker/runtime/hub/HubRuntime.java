package net.hollowcube.mapmaker.runtime.hub;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.Uuids;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.ipc.map.MapPatch;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.map.MapVariant;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.playerinfo.PlayerInfoCommand;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.setting.TimeOfDay;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.misc.TitleHud;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;
import net.hollowcube.mapmaker.runtime.freeform.FreeformRuntime;
import net.hollowcube.mapmaker.runtime.hub.command.util.HubFlyCommand;
import net.hollowcube.mapmaker.runtime.hub.command.util.HubSpawnCommand;
import net.hollowcube.mapmaker.runtime.hub.command.util.HubTrainCommand;
import net.hollowcube.mapmaker.runtime.hub.feature.HubFeature;
import net.hollowcube.mapmaker.runtime.hub.feature.InteractionFeature;
import net.hollowcube.mapmaker.runtime.hub.feature.event.christmas.AdventCalendarItem;
import net.hollowcube.mapmaker.runtime.hub.feature.event.christmas.ChristmasFeature;
import net.hollowcube.mapmaker.runtime.hub.feature.event.christmas.PresentObjectHandler;
import net.hollowcube.mapmaker.runtime.hub.feature.event.newyears.NewYearsFeature;
import net.hollowcube.mapmaker.runtime.hub.feature.leaderboard.MainLeaderboardFeature;
import net.hollowcube.mapmaker.runtime.hub.feature.misc.*;
import net.hollowcube.mapmaker.runtime.hub.item.*;
import net.hollowcube.mapmaker.runtime.hub.util.HubTime;
import net.hollowcube.mapmaker.runtime.hub.util.HubTransferData;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.timer.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public final class HubRuntime implements FreeformRuntime {
    private static final Logger logger = LoggerFactory.getLogger(HubRuntime.class);

    public static final String ID = "hub";
    // How long the shutdown drain waits before asking the session service for another hub again.
    private static final Duration DRAIN_RETRY_DELAY = Duration.ofSeconds(5);

    /// The hub map for servers which don't load it from the map service.
    public static final MapData HUB_MAP_DATA;

    static {
        var editor = new MapPatch.Builder(
            MapData.draft(UUID.fromString(MapData.SPAWN_MAP_ID), UUID.fromString(Uuids.ZERO))
        );
        editor.setVariant(MapVariant.FREEFORM);
        editor.setSize(MapSize.UNLIMITED);
        MapSettings.set(editor, MapSettings.TIME_OF_DAY, TimeOfDay.NOON);
        MapSettings.set(editor, MapSettings.LIGHTING, true);
        MapSettings.set(editor, MapSettings.RUNTIME, ID);
        HUB_MAP_DATA = editor.map();
    }

    private static final Pos MIN_SPAWN_POINT = new Pos(-1, 40, -1, 90, 0);

    private static final Vec HUB_BB_MIN = new Vec(-250, -40, -150);
    private static final Vec HUB_BB_MAX = new Vec(60, 160, 150);

    private static final AttributeModifier REACH_MOD = new AttributeModifier(
        "mapmaker:hub_reach",
        40 - Attribute.ENTITY_INTERACTION_RANGE.defaultValue(),
        AttributeOperation.ADD_VALUE
    );

    private static final PlayerHud.Module TITLE = TitleHud.module(
        TitleHud.line1(
            TitleHud.text1(Component.text("Map Maker Early Access", TextColor.color(0x3895FF)))
        ),
        TitleHud.ADDRESS_LINE
    );

    public static Pos spawnPointFor(Player player) {
        var seeded = new Random(player.getUuid().getLeastSignificantBits());
        return MIN_SPAWN_POINT.add(
            (seeded.nextDouble() * 10) % 3,
            0,
            (seeded.nextDouble() * 10) % 3
        );
    }

    /// Whether {@code point} is inside the hub's playable bounds (the box players are kept within).
    public static boolean inWorldBounds(Point point) {
        return point.x() >= HUB_BB_MIN.x()
            && point.x() <= HUB_BB_MAX.x()
            && point.y() >= HUB_BB_MIN.y()
            && point.y() <= HUB_BB_MAX.y()
            && point.z() >= HUB_BB_MIN.z()
            && point.z() <= HUB_BB_MAX.z();
    }

    public static boolean isHub(MapData map) {
        return ID.equals(MapSettings.get(map.settings(), MapSettings.RUNTIME));
    }

    public static void registerCommands(
        AbstractMapServer server,
        CommandManager commandManager,
        Scheduler scheduler
    ) {
        commandManager.register(new HelpCommand(commandManager, CommandCategories.GLOBAL));
        commandManager.register(new PlayerInfoCommand(server.api(), server.sessionManager()));

        commandManager.register(new HubFlyCommand());
        commandManager.register(new HubSpawnCommand());
        commandManager.register(new HubTrainCommand(scheduler));
    }

    public static void drainToAnotherHub(ApiClient api) {
        var connectionManager = MinecraftServer.getConnectionManager();
        FutureUtil.submitVirtual(() -> {
            // Retry because this begins immediately and another hub may not be available
            while (true) {
                var players = List.copyOf(connectionManager.getOnlinePlayers());
                if (players.isEmpty()) return;

                var stranded = transferToAnotherHub(api, players);
                if (stranded != 0)
                    logger.warn(
                        "no other hub is available for {} of {} players, retrying in {}",
                        stranded,
                        players.size(),
                        DRAIN_RETRY_DELAY
                    );

                try {
                    Thread.sleep(DRAIN_RETRY_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    private static int transferToAnotherHub(ApiClient api, List<Player> players) {
        var stranded = 0;
        for (var player : players) {
            try {
                var hub = api.sessions.findHub(AbstractHttpService.hostname);
                if (hub == null) {
                    stranded++;
                    continue;
                }

                var state = new HubTransferData(player.getPosition(), player.getHeldSlot());
                ProxySupport.transferWithData(player, hub, state);
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
            }
        }
        return stranded;
    }

    public HubRuntime(FreeformMapWorld world) {
        var server = world.server();
        world.itemRegistry().register(new PlayMapsItem(server.api(), server.bridge()));
        world.itemRegistry().register(
            new CreateMapsItem(server.api(), server.accountService(), server.bridge())
        );
        world.itemRegistry().register(
            new OpenCosmeticsMenuItem(server.accountService(), server.api().players)
        );
        world.itemRegistry().register(OpenStoreItem.INSTANCE);
        world.itemRegistry().register(new AdventCalendarItem());
        world.itemRegistry().register(OpenNotificationsItem.INSTANCE);

        world.objectEntityHandlers().registerForInteractions(
            PresentObjectHandler.ID,
            PresentObjectHandler::new
        );

        world.eventNode()
            .addChild(EventUtil.READ_ONLY_NODE)
            .addListener(PlayerChangeHeldSlotEvent.class, this::handleSwitchSlot)
            .addListener(PlayerMoveEvent.class, this::handlePlayerMove);
    }

    @Override
    public void load(FreeformMapWorld world) {
        List<HubFeature> features = List.of(
            new InteractionFeature(),
            new ChristmasFeature(),
            new NewYearsFeature(),
            new MainLeaderboardFeature(),
            new CyberpunkStatDisplayFeature(),
            new CyberpunkTrainFeature(),
            new DoubleJumpFeature(),
            new LauncherFeature(),
            new MiscNpcsFeature(),
            new PlayMapStatueFeatureProvider(),
            new StoreAdFeatureProvider(),
            new SulfurCubeBallFeature()
        );
        for (var feature : features) {
            try {
                feature.load(world.server(), world);
            } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to load feature " + feature.getClass().getName(),
                    e
                );
            }
        }
    }

    @Override
    public void configurePlayer(FreeformMapWorld world, Player player) {
        player.removeResourcePacks(MapWorldHelpers.MAP_WORLD_RESOURCE_PACK_UUID);

        // Load the state from the previous hub they were on if present.
        var existingState = ProxySupport.getTransferData(player, HubTransferData.class);
        if (existingState != null) {
            player.setRespawnPoint(existingState.position());
            player.scheduleNextTick(ignored -> player.setHeldItemSlot((byte) existingState.slot()));
        } else {
            player.setRespawnPoint(spawnPointFor(player));
        }
    }

    @Override
    public void configurePlaying(FreeformMapWorld world, Player player) {
        var playerData = localPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setFlyingSpeed(player.getTag(DoubleJumpFeature.TAG) ? 0 : 0.05f);
        player.setHeldItemSlot(playerData.getSetting(PlayerSettings.HUB_SELECTED_SLOT).byteValue());
        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).addModifier(REACH_MOD);

        world.itemRegistry().setItemStack(player, PlayMapsItem.ID, 0);
        world.itemRegistry().setItemStack(player, CreateMapsItem.ID, 1);
        world.itemRegistry().setItemStack(player, OpenNotificationsItem.ID, 4);
        if (HubTime.Christmas.isActive())
            world.itemRegistry().setItemStack(player, AdventCalendarItem.ID, 5);
        world.itemRegistry().setItemStack(player, OpenStoreItem.ID, 7);
        world.itemRegistry().setItemStack(player, OpenCosmeticsMenuItem.ID, 8);

        OpenNotificationsItem.checkForUnread(world, new WeakReference<>(player));

        if (player instanceof MapPlayer mp) {
            mp.setCanSendPose(false);
        }
    }

    @Override
    public void resetPlaying(FreeformMapWorld world, Player player) {
        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).removeModifier(REACH_MOD);

        // Write their settings to the database
        var playerData = localPlayer(player);
        FutureUtil.submitVirtual(
            () -> playerData.writeUpdatesUpstream(world.server().api().players)
        );
    }

    @Override
    public PlayerHud.Module titleHud(FreeformMapWorld world) {
        return TITLE;
    }

    private void handleSwitchSlot(PlayerChangeHeldSlotEvent event) {
        var playerData = localPlayer(event.getPlayer());
        playerData.setSetting(PlayerSettings.HUB_SELECTED_SLOT, (int) event.getNewSlot());
    }

    private void handlePlayerMove(PlayerMoveEvent event) {
        Pos playerPos = event.getPlayer().getPosition();
        if (!inWorldBounds(playerPos)) {
            event.getPlayer().teleport(spawnPointFor(event.getPlayer()));
        }
    }
}

package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandManager;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.command.MapCommand;
import net.hollowcube.mapmaker.command.PlayCommand;
import net.hollowcube.mapmaker.command.invite.*;
import net.hollowcube.mapmaker.command.util.WhereCommand;
import net.hollowcube.mapmaker.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.mapmaker.hub.command.map.legacy.MapLegacyCommand;
import net.hollowcube.mapmaker.hub.command.util.HubFlyCommand;
import net.hollowcube.mapmaker.hub.command.util.HubSpawnCommand;
import net.hollowcube.mapmaker.hub.feature.misc.CyberpunkStatDisplay;
import net.hollowcube.mapmaker.hub.feature.motw.CountdownTimer;
import net.hollowcube.mapmaker.hub.find_a_new_home.hotbar.HubHotbar;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.SchedulerManager;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public abstract class HubServerBase implements HubServer {
    //todo one readiness check should be ensuring the world is loaded

    private static final SchedulerManager SCHEDULER_MANAGER = MinecraftServer.getSchedulerManager();

    static {
        // Idk why the static initializer is not triggering from other usages
        new PlayerSpawnInInstanceEvent(null);
    }

    private final HubToMapBridge bridge;
    private HubHandler mapHandler;
    private HubWorld world;

    private Controller guiController;

    private final Tag<Boolean> DOUBLE_JUMP_COOLDOWN_TAG = Tag.Boolean("mapmaker:hub-double-jump-cooldown");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:hub", EventFilter.INSTANCE)
            .addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn)
            .addListener(PlayerStartFlyingEvent.class, this::handleDoubleJump)
            .addListener(PlayerMoveEvent.class, this::handlePlayerMovement);

    public HubServerBase(@NotNull HubToMapBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public @NotNull HubToMapBridge bridge() {
        return bridge;
    }

    @Blocking
    public void init(@NotNull CommandManager commandManager, @NotNull PlayerInviteService inviteService) {
        StaticAbuse.instance = this;
        this.mapHandler = new HubHandler(this, mapService());

        this.guiController = Controller.make(Map.of(
                "hubServer", this,
                "playerService", playerService(),
                "sessionService", sessionService(),
                "mapService", mapService(),
                "handler", mapHandler,
                "bridge", bridge
        ));

        this.world = new HubWorld(this);
        this.world.loadWorld();
        this.world.instance().eventNode().addChild(eventNode);

        // Common commands
        commandManager.register(new PlayCommand(mapService(), bridge()));
        commandManager.register(new WhereCommand());

        commandManager.register(new RequestCommand(inviteService));
        commandManager.register(new RejectCommand(inviteService));
        commandManager.register(new InviteCommand(inviteService));
        commandManager.register(new AcceptCommand(inviteService));
        commandManager.register(new JoinCommand(inviteService, permManager()));

        var mapCommand = new MapCommand(guiController, playerService(), mapService(), permManager());
        mapCommand.addSubcommand(new MapLegacyCommand(mapService(), permManager()));
        commandManager.register(mapCommand);

        // Hub specific commands
        commandManager.register(new HubFlyCommand(permManager()));
        commandManager.register(new HubSpawnCommand(this));

        // Map of the week
        var motwTimer = new CountdownTimer(world.instance());
        SCHEDULER_MANAGER.submitTask(motwTimer, ExecutionType.SYNC);

        // Misc hub stuff
        var hubStatDisplay = new CyberpunkStatDisplay(this);
        SCHEDULER_MANAGER.submitTask(hubStatDisplay, ExecutionType.SYNC);
    }

    @Override
    public @NotNull HubWorld world() {
        return world;
    }

    public HubHandler handler() {
        return mapHandler;
    }

    @Override
    public void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        guiController.show(player, viewProvider);
    }

    public void shutdown() {
        for (var player : world.instance().getPlayers()) {
            player.kick(Component.translatable("mapmaker.shutdown"));
        }
    }

    private final Pos HUB_SPAWN_POINT = new Pos(0.5, 40, 0.5, 90, 0);

    private void handlePlayerSpawn(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.refreshCommands();

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setPermissionLevel(4);
        player.teleport(HUB_SPAWN_POINT);
        player.sendActionBar(Component.empty());
        player.setFlyingSpeed(player.getTag(HubServer.DOUBLE_JUMP_TAG) ? 0 : 0.05f);

        player.getInventory().clear();
        HubHotbar.applyToPlayer(player);
    }

    private void handleDoubleJump(@NotNull PlayerStartFlyingEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (!player.getTag(HubServer.DOUBLE_JUMP_TAG)) return; // Explicitly disabled to allow normal flight
        player.setFlying(false);
        if (player.hasTag(DOUBLE_JUMP_COOLDOWN_TAG)) return;

        var boostVelocity = player.getPosition().direction().mul(20.0).withY(20.0);
        player.setVelocity(boostVelocity);
        player.setTag(DOUBLE_JUMP_COOLDOWN_TAG, true);
        player.setAllowFlying(false);
    }

    private final int LOWER_X_BOUND = Integer.getInteger("mapmaker.hub.lower-x-bound", -250);
    private final int LOWER_Y_BOUND = Integer.getInteger("mapmaker.hub.lower-y-bound", -30);
    private final int LOWER_Z_BOUND = Integer.getInteger("mapmaker.hub.lower-z-bound", -100);
    private final int UPPER_X_BOUND = Integer.getInteger("mapmaker.hub.upper-x-bound", 60);
    private final int UPPER_Y_BOUND = Integer.getInteger("mapmaker.hub.upper-y-bound", 130);
    private final int UPPER_Z_BOUND = Integer.getInteger("mapmaker.hub.upper-z-bound", 100);


    private final Vec lowerHubCoord = new Vec(LOWER_X_BOUND, LOWER_Y_BOUND, LOWER_Z_BOUND);
    private final Vec upperHubCoord = new Vec(UPPER_X_BOUND, UPPER_Y_BOUND, UPPER_Z_BOUND);

    private void handlePlayerMovement(@NotNull PlayerMoveEvent event) {
        if (event.isOnGround() && event.getPlayer().hasTag(DOUBLE_JUMP_COOLDOWN_TAG)) {
            event.getPlayer().removeTag(DOUBLE_JUMP_COOLDOWN_TAG);
            event.getPlayer().setAllowFlying(true);
        }

        Pos playerPos = event.getPlayer().getPosition();
        if (playerPos.x() < lowerHubCoord.x() || playerPos.x() > upperHubCoord.x() ||
                playerPos.y() < lowerHubCoord.y() || playerPos.y() > upperHubCoord.y() ||
                playerPos.z() < lowerHubCoord.z() || playerPos.z() > upperHubCoord.z()) {
            event.getPlayer().teleport(HUB_SPAWN_POINT);
        }
    }

    public void teleportToSpawn(@NotNull Player player) {
        // Check to see if we're in the same world
        if (player.getInstance().equals(world.instance())) {
            player.teleport(HUB_SPAWN_POINT);
        }
    }
}

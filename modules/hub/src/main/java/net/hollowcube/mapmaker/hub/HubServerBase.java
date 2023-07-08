package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.mapmaker.hub.command.map.MapV2Command;
import net.hollowcube.mapmaker.hub.find_a_new_home.hotbar.HubHotbar;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class HubServerBase implements HubServer {
    //todo one readiness check should be ensuring the world is loaded

    static {
        // Idk why the static initializer is not triggering from other usages
        new PlayerSpawnInInstanceEvent(null);
    }

    private final HubToMapBridge bridge;
    private HubHandler mapHandler;
    private HubWorld world;

    private Controller guiController;

    private EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:hub", EventFilter.INSTANCE)
            .addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn)
            .addListener(PlayerStartFlyingEvent.class, this::handleDoubleJump);

    public HubServerBase(@NotNull HubToMapBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public @NotNull HubToMapBridge bridge() {
        return bridge;
    }

    @Blocking
    public void init() {
        StaticAbuse.instance = this;
        this.mapHandler = new HubHandler(this, mapService());

        this.guiController = Controller.make(Map.of(
                "hubServer", this,
                "playerService", playerService(),
                "sessionService", sessionService(),
                "mapService", mapService(),
                "handler", mapHandler
        ));

        this.world = new HubWorld(this);
        this.world.loadWorld();
        this.world.instance().eventNode().addChild(eventNode);

        System.out.println("REGISTERING MAP COMMANDS");
        var commands = MinecraftServer.getCommandManager();
        commands.register(new MapV2Command(mapService(), mapHandler));
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

    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.refreshCommands();

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setPermissionLevel(4);
        player.teleport(new Pos(0.5, 40, 0.5, 90, 0));
        player.sendActionBar(Component.empty());

        player.getInventory().clear();
        HubHotbar.applyToPlayer(player);
    }

    private void handleDoubleJump(@NotNull PlayerStartFlyingEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

        var boostVelocity = player.getPosition().direction().mul(20.0).withY(20.0);
        player.setVelocity(boostVelocity);
        player.setFlying(false);
    }

}

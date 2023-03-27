package net.hollowcube.mapmaker.hub;

import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.section.SectionLike;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public abstract class HubServerBase implements HubServer { //todo one readiness check should be ensuring the world is loaded

    private final HubToMapBridge bridge;
    private Handler mapHandler;
    private HubWorld world;

    private Controller guiController;
    private Map<Class<?>, Object> guiContext;

    public HubServerBase(@NotNull HubToMapBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public @NotNull HubToMapBridge bridge() {
        return bridge;
    }

    public @NotNull ListenableFuture<Void> init() {
        this.mapHandler = new Handler(this);

        this.guiController = Controller.make(Map.of(
                "hubServer", this,
                "playerStorage", playerStorage(),
                "mapStorage", mapStorage(),
                "handler", mapHandler
        ));
        this.guiContext = Map.of(
                HubServer.class, this,
                PlayerStorage.class, playerStorage(),
                MapStorage.class, mapStorage(),
                Handler.class, mapHandler
        );

        this.world = new HubWorld(this);
        var worldResult = this.world.loadWorld();

        var commands = MinecraftServer.getCommandManager();
        commands.register(new MapCommand(this, mapHandler));

        return JdkFutureAdapters.listenInPoolThread(worldResult);
    }

    @Override
    public @NotNull HubWorld world() {
        return world;
    }

    @Override
    public void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        guiController.show(player, viewProvider);
    }

    @Override
    public void openGUIForPlayer(@NotNull Player player, @NotNull SectionLike gui) {
        var context = new HashMap<>(guiContext);
        context.put(Player.class, player);
        new RouterSection(gui, context).showToPlayer(player);
    }
}

package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.mapmaker.hub.legacy.LegacyMapService;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public abstract class HubServerBase implements HubServer {
    //todo one readiness check should be ensuring the world is loaded

    private final HubToMapBridge bridge;
    private Handler mapHandler;
    private HubWorld world;

    private Controller guiController;

    public HubServerBase(@NotNull HubToMapBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public @NotNull HubToMapBridge bridge() {
        return bridge;
    }

    @Blocking
    public void init() {
        this.mapHandler = new Handler(this);

        this.guiController = Controller.make(Map.of(
                "hubServer", this,
                "playerStorage", playerStorage(),
                "playerService", playerService(),
                "mapStorage", mapStorage(),
                "handler", mapHandler
        ));

        this.world = new HubWorld(this);
        this.world.loadWorld();

        var commands = MinecraftServer.getCommandManager();
        commands.register(new MapCommand(this, mapHandler));
    }

    @Override
    public @NotNull HubWorld world() {
        return world;
    }

    @Override
    public void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        guiController.show(player, viewProvider);
    }

}

package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class HubServerBase implements HubServer { //todo one readiness check should be ensuring the world is loaded
    private final Logger logger = LoggerFactory.getLogger(HubServer.class);

    private final HubToMapBridge bridge;
    private Handler mapHandler;
    private HubWorld world;

    private Map<Class<?>, Object> guiContext;

    public HubServerBase(@NotNull HubToMapBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public @NotNull HubToMapBridge bridge() {
        return bridge;
    }

    public @NotNull FutureResult<Void> init() {
        this.mapHandler = new Handler(this);

        this.guiContext = Map.of(
                HubServer.class, this,
                PlayerStorage.class, playerStorage(),
                MapStorage.class, mapStorage(),
                Handler.class, mapHandler
        );

        this.world = new HubWorld(this);
        var worldResult = FutureResult.wrap(this.world.loadWorld());

        var commands = MinecraftServer.getCommandManager();
        commands.register(new MapCommand(this, mapHandler));

        return FutureResult.allOf(worldResult)
                .then(unused -> logger.info("Hub server initialized"));
    }

    @Override
    public @NotNull HubWorld world() {
        return world;
    }

    @Override
    public void openGUIForPlayer(@NotNull Player player, @NotNull Section gui) {
        new RouterSection(gui, guiContext, player).showToPlayer(player);
    }
}

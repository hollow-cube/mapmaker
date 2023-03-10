package net.hollowcube.mapmaker.hub;

import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
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

    public @NotNull ListenableFuture<Void> init() {
        StaticAbuse.instance = this;
        this.mapHandler = new Handler(this);
        StaticAbuse.handler = mapHandler;

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
    public void openGUIForPlayer(@NotNull Player player, @NotNull SectionLike gui) {
        var context = new HashMap<>(guiContext);
        context.put(Player.class, player);
        new RouterSection(gui, context).showToPlayer(player);
    }
}

package net.hollowcube.map.command;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapServerBase;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class BuildModeCommand extends BaseMapCommand {
    private static final Logger logger = LoggerFactory.getLogger(BuildModeCommand.class);

    private MapServerBase mapServer;

    public BuildModeCommand(MapServerBase mapServer) {
        super("build");
        this.mapServer = mapServer;

        setDefaultExecutor(this::enterBuildMode);
        setCondition(this::isInTestingMap);
    }

    private void enterBuildMode(@NotNull CommandSender sender, @NotNull CommandContext context) {
        Player player = (Player) sender;
        player.sendMessage("Entering build mode");

        var curr_world = MapWorld.fromInstance(player.getInstance());

        MapWorld world = new EditingMapWorld(mapServer, curr_world.map());

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        future = world.loadWorld();
        FutureResult.wrap(future.thenCompose(unused ->
                player.setInstance(world.instance(), player.getPosition())));
        player.setAllowFlying(true);
        player.setFlying(true);
        player.setGameMode(GameMode.CREATIVE);
    }

    private boolean isInTestingMap(@Nullable CommandSender sender, @Nullable String unused) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        var world = MapWorld.fromInstance(player.getInstance());
        return world instanceof TestingMapWorld;
    }
}

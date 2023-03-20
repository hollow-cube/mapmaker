package net.hollowcube.map.command;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.MapServerBase;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class TestModeCommand extends BaseMapCommand {
    private static final Logger logger = LoggerFactory.getLogger(TestModeCommand.class);

    private MapServerBase mapServer;

    public TestModeCommand(MapServerBase mapServer) {
        super("test");
        this.mapServer = mapServer;

        setDefaultExecutor(this::enterTestMode);
        setCondition(this::isInEditingMap);
    }

    private void enterTestMode(@NotNull CommandSender sender, @NotNull CommandContext context) {
        Player player = (Player) sender;
        player.sendMessage("Entering test mode");

        Pos pos = player.getPosition();

        // Create playing map world and send user to it
        var curr_world = MapWorld.fromInstance(player.getInstance());
        String world_file_id = String.valueOf(curr_world.saveWorld());

        MapWorld world = new TestingMapWorld(mapServer, curr_world.map());

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        future = world.loadWorld();
        FutureResult.wrap(future.thenCompose(unused ->
                player.setInstance(world.instance(), pos)));
        player.setAllowFlying(false);
        player.setFlying(false);
        player.setGameMode(GameMode.ADVENTURE);
    }

    private boolean isInEditingMap(@Nullable CommandSender sender, @Nullable String unused) {
        if (!(sender instanceof Player player) || player.getInstance() == null) {
            return false;
        }

        var world = MapWorld.fromInstance(player.getInstance());
        return world instanceof EditingMapWorld;
    }
}

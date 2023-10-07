package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.MapServerBase;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildModeCommand extends BaseMapCommand {
    private static final Logger logger = LoggerFactory.getLogger(BuildModeCommand.class);

    private final MapServerBase mapServer;

    public BuildModeCommand(MapServerBase mapServer) {
        super("build");
        this.mapServer = mapServer;

        setDefaultExecutor(this::enterBuildMode);
        setCondition(this::isInTestingMap);
    }

    private void enterBuildMode(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }
        enterBuildMode(player, mapServer);
    }

    private boolean isInTestingMap(@Nullable CommandSender sender, @Nullable String unused) {
        // Stupid amount of checks to verify they're actually in a world otherwise nullptr exception
        // Probably better way to structure instantiation of this command so it doesn't have this issue
        if (!(sender instanceof Player player)) {
            return false;
        }

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return false;

        return (world.flags() & MapWorld.FLAG_TESTING) != 0;
    }

    public static void enterBuildMode(@NotNull Player player, @NotNull MapServer mapServer) {
        player.sendMessage("Entering build mode");

        Pos pos = player.getPosition();
//
//        var curr_world = MapWorldNew.fromInstance(player.getInstance());
//
//        MapWorld world = new EditingMapWorld(mapServer, curr_world.map());
//
//        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
//        future = world.loadWorld();
//        FutureResult.wrap(future.thenCompose(unused ->
//                player.setInstance(world.instance(), pos)));
//        player.setAllowFlying(true);
//        player.setFlying(true);
//        player.setGameMode(GameMode.CREATIVE);
    }
}

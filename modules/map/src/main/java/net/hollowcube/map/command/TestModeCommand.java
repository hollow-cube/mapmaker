package net.hollowcube.map.command;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.MapServerBase;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestModeCommand extends BaseMapCommand {
    private static final Logger logger = LoggerFactory.getLogger(TestModeCommand.class);

    private final MapServerBase mapServer;

    public TestModeCommand(MapServerBase mapServer) {
        super("test");
        this.mapServer = mapServer;

        setDefaultExecutor(this::enterTestMode);
        setCondition(this::isInEditingMap);
    }

    private void enterTestMode(@NotNull CommandSender sender, @NotNull CommandContext context) {
        // Stupid amount of checks to verify they're actually in a world otherwise nullptr exception
        // Probably better way to structure instantiation of this command so it doesn't have this issue
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return;
        }
        enterTestMode(player, mapServer);
    }

    private boolean isInEditingMap(@Nullable CommandSender sender, @Nullable String unused) {
//        if (!(sender instanceof Player player) ||
//            player.getInstance() == null ||
//            !(player.getInstance().hasTag(MapWorld.MAP_ID))) {
//                return false;
//        }

//        var world = MapWorld.fromInstance(player.getInstance());
//        return (world.flags() & MapWorld.FLAG_EDITING) != 0;
        return true; //todo obviously
    }

    public static void enterTestMode(@NotNull Player player, @NotNull MapServer mapServer) {
        player.sendMessage("Entering test mode");

        var map = MapWorld.forPlayerOptional(player);
        if (map instanceof EditingMapWorld editingMap) {
            editingMap.enterTestMode(player);
        } else if (map instanceof TestingMapWorld testingMap) {
            testingMap.exitTestMode(player);
        }

//        Pos pos = player.getPosition();

        // Create playing map world and send user to it
//        var curr_world = MapWorld.fromInstance(player.getInstance());
//        String world_file_id = String.valueOf(curr_world.saveWorld());

//        MapWorld world = new TestingMapWorld(mapServer, curr_world.map());

//        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
//        future = world.loadWorld();
//        FutureResult.wrap(future.thenCompose(unused ->
//                player.setInstance(world.instance(), pos)));
//        player.setAllowFlying(false);
//        player.setFlying(false);
//        player.setGameMode(GameMode.ADVENTURE);
    }
}

package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.compat.moulberrytweaks.debugrender.DebugShape;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderAddPacket;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.vanilla.DripleafBlock;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.ComponentUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class DebugCommand extends CommandDsl {

    private final CommandCondition adminCondition;
    private final CommandCondition localCondition;

    public DebugCommand(
            @NotNull PlayerService playerService, @NotNull PermManager permManager, @NotNull MapService mapService
    ) {
        super("debug");

        description = "Debugging utilities for map maker";
        category = CommandCategory.HIDDEN;

        adminCondition = permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN);
        localCondition = (_, _) -> ServerRuntime.getRuntime().isDevelopment() ? CommandCondition.ALLOW
                : CommandCondition.DENY;

        // Mapmaker stuff
        createPermissionlessSubcommand("rp", this::handleDebugResourcePack,
                "Show information about the current resource pack version");
        createPermissionlessSubcommand("self", this::handleDebugSelf,
                "Show information about yourself");
        createPermissionlessSubcommand("server", this::handleDebugServer,
                "Show information about the current server");
        createPermissionedSubcommand("gc", (ignored1, ignored2) -> System.gc(),
                "Force a garbage collection");

        // Minestom stuff
        createPermissionlessSubcommand("commands", this::handleCommandsDebug,
                "Reload the currently available commands");
        createPermissionlessSubcommand("block", this::handleBlockDebug,
                "Show debug information about the block you're looking at");
        createPermissionlessSubcommand("heightmap", this::handleHeightmapDebug,
                "Show debug information about the heightmaps at your location");
        createPermissionlessSubcommand("pvn", this::handlePvnDebug,
                "Show your current protocol version");

        createPermissionedSubcommand("relight", this::relightWorld,
                "Relight the world");
        createPermissionedSubcommand("reheightmap", this::handleReHeightmapDebug,
                "Rebuild the heightmap in the map");
        createPermissionedSubcommand("yndranth", this::handleYndranthDebug,
                "dump block nbt directly");
        createPermissionedSubcommand("tree", this::handleTreeDebug,
                "show map octree");
        createPermissionedSubcommand("fixthedripleaf", this::fixTheDripleaf,
                "add dripleaf block handlers to relevant blocks");
    }

    public @NotNull CommandDsl createPermissionlessSubcommand(
            @NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @NotNull String description) {
        return createSubcommand(name, handler, null, description);
    }

    public @NotNull CommandDsl createPermissionedSubcommand(
            @NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @NotNull String description) {
        return createSubcommand(name, handler, adminCondition, description);
    }

    public @NotNull CommandDsl createLocalSubcommand(
            @NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @NotNull String description) {
        return createSubcommand(name, handler, localCondition, description);
    }

    private void handleDebugResourcePack(@NotNull Player player, @NotNull CommandContext context) {
        var packHash = ServerRuntime.getRuntime().resourcePackSha1();
        player.sendMessage(Component.text("Resource pack: ")
                .append(ComponentUtil.createBasicCopy(packHash)));
    }

    private void handleDebugSelf(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = PlayerData.fromPlayer(player);
        player.sendMessage(Component.text(playerData.username() + " (" + playerData.id().substring(0, 8) + "...)"));
        player.sendMessage(Component.text("Display: ").append(playerData.displayName2().build()));
        var rawSettings = playerData.settingsRawValues();
        player.sendMessage(Component.text("Settings: " + (rawSettings.isEmpty() ? "empty" : "")));
        for (var entry : rawSettings) {
            player.sendMessage(Component.text("  " + entry.getKey() + ": " + entry.getValue()));
        }

        var mapPlayerData = MapPlayerData.fromPlayer(player);
        player.sendMessage(Component.text("Last played: " + mapPlayerData.lastPlayedMap()));
        player.sendMessage(Component.text("Last edited: " + mapPlayerData.lastEditedMap()));
    }

    private void handleDebugServer(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage("Host: " + AbstractHttpService.hostname);
        player.sendMessage("Release: " + !ServerRuntime.getRuntime().isDevelopment());
    }

    private @NotNull CommandDsl createSubcommand(
            @NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @Nullable CommandCondition condition,
            @NotNull String description
    ) {
        var cmd = new CommandDsl(name);
        cmd.setDescription(description);
        cmd.setCondition(condition);
        cmd.addSyntax(playerOnly(handler));
        addSubcommand(cmd);
        return cmd;
    }

    private void handleCommandsDebug(@NotNull Player player, @NotNull CommandContext context) {
        player.refreshCommands();
        player.sendMessage("Commands refreshed!");
    }

    public interface BlockDebug {
        void sendDebugInfo(@NotNull Player player, @NotNull Block block);
    }

    private void handleBlockDebug(@NotNull Player player, @NotNull CommandContext context) {
        var blockPosition = player.getTargetBlockPosition(5);
        if (blockPosition == null) {
            player.sendMessage("No block in range!");
            return;
        }

        var block = player.getInstance().getBlock(blockPosition);
        if (block.handler() != null && block.handler() instanceof BlockDebug bd) {
            player.sendMessage("Block: " + block.handler().getKey());
            bd.sendDebugInfo(player, block);
        } else {
            if (block.handler() != null) {
                player.sendMessage(
                        "Block: " + block.handler().getKey() + "@" + block.handler().getClass().getSimpleName());
            } else {
                player.sendMessage("Block: " + "no handler");
            }

            player.sendMessage(block.nbt() == null ? Component.text("No block NBT") : NbtUtil.prettyPrint(block.nbt()));
        }
    }

    private void handleHeightmapDebug(@NotNull Player player, @NotNull CommandContext context) {
        var rawChunk = player.getChunk();
        if (!(rawChunk instanceof ChunkExt chunk)) {
            player.sendMessage("No heightmaps.");
            return;
        }

        int x = player.getPosition().blockX() & 0xF;
        int z = player.getPosition().blockZ() & 0xF;

        int sh = chunk.getHeight(Heightmaps.WORLD_SURFACE, x, z);
        int sm = chunk.getHeight(Heightmaps.MOTION_BLOCKING, x, z);
        int bh = chunk.getHeight(Heightmaps.WORLD_BOTTOM, x, z);
        player.sendMessage("SH: " + sh + " SM: " + sm + " BH: " + bh);
    }

    private void handleReHeightmapDebug(@NotNull Player player, @NotNull CommandContext context) {
        queueRateLimitedWorldUpdate(player, "reheightmap", batch -> {
            for (var chunk : batch) {
                if (!(chunk instanceof ChunkExt ext)) return;

                ext.heightmap(Heightmaps.WORLD_SURFACE).recalculate();
                ext.heightmap(Heightmaps.MOTION_BLOCKING).recalculate();
                ext.heightmap(Heightmaps.WORLD_BOTTOM).recalculate();
            }
        }, 100);
    }

    private void handleYndranthDebug(@NotNull Player player, @NotNull CommandContext context) {
        var blockPosition = player.getTargetBlockPosition(5);
        if (blockPosition == null) {
            player.sendMessage("No block in range!");
            return;
        }

        var block = player.getInstance().getBlock(blockPosition);
        if (block.nbt() == null) {
            player.sendMessage("No block NBT");
            return;
        }

        player.sendMessage(NbtUtil.prettyPrint(block.nbt()));
    }

    private void handlePvnDebug(@NotNull Player player, @NotNull CommandContext context) {
        int pvn = ProtocolVersions.getProtocolVersion(player);
        player.sendMessage(Component.text("Protocol: " + pvn + " (" + ProtocolVersions.getProtocolName(pvn) + ")"));
    }

    private void handleTreeDebug(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (world == null) {
            player.sendMessage("You are not in a map world!");
            return;
        }

        player.scheduleNextTick(_ -> {
            var boundingBoxes = world.collisionTree().debugBoundingBoxes();
            player.sendMessage("Found " + boundingBoxes.size() + " bounding boxes in octree.");

            int i = 0;
            for (var bb : boundingBoxes) {
                var color = bb.isObject() ? 0x00FF00 : 0x0000FF; // Green for objects, blue for tree
                var size = bb.boundingBox().size();
                if (size.isZero()) {
                    size = new Vec(0.5);
                }
                new ClientboundDebugRenderAddPacket(
                        Key.key("octree", String.valueOf(i++)),
                        new DebugShape.Box(bb.boundingBox().center(), size, Quaternion.ZERO,
                                0, color | 0xFF000000, 5),
                        0, 20 * 20).send(player); // 20s
            }
        });

    }

    private void relightWorld(@NotNull Player player, @NotNull CommandContext context) {
        queueRateLimitedWorldUpdate(player, "relight", batch -> {
            LightingChunk.relight(player.getInstance(), batch);
            batch.forEach(player::sendChunk);
        }, 50);
    }

    private void fixTheDripleaf(@NotNull Player player, @NotNull CommandContext context) {
        var instance = player.getInstance();
        var dimensionHeight = instance.getCachedDimensionType().height();
        player.sendMessage("Fixing the dripleaf!!!");
        int fixed = 0;
        for (var chunk : instance.getChunks()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -64; y < dimensionHeight; y++) {
                        var block = chunk.getBlock(x, y, z);
                        if (block.name().equals("minecraft:big_dripleaf")) {
                            chunk.setBlock(x, y, z, block.withHandler(DripleafBlock.INSTANCE));
                            fixed++;
                        }
                    }
                }
            }
        }
        player.sendMessage("Fixed " + fixed + " dripleaf blocks!");
    }

    private void queueRateLimitedWorldUpdate(
            @NotNull Player player, @NotNull String name, @NotNull Consumer<List<Chunk>> action, int rate) {
        FutureUtil.submitVirtual(() -> {
            var chunks = List.copyOf(player.getInstance().getChunks());
            int nBatches = (chunks.size() + rate - 1) / rate;
            for (int i = 0; i < nBatches; i++) {
                player.sendMessage("processing batch " + i + "/" + nBatches + ": " + name);
                var batch = chunks.subList(i * rate, Math.min(chunks.size(), (i + 1) * rate));
                action.accept(batch);
                FutureUtil.sleep(500);
            }

            player.sendMessage("task finished: " + name);
        });
    }
}

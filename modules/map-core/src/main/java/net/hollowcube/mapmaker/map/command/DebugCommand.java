package net.hollowcube.mapmaker.map.command;

import com.sun.management.HotSpotDiagnosticMXBean;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.ComponentUtil;
import net.kyori.adventure.nbt.TagStringIOExt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class DebugCommand extends CommandDsl {

    private final MapAllocator allocator;

    private final CommandCondition adminCondition;
    private final CommandCondition localCondition;

    public DebugCommand(@NotNull PlayerService playerService, @NotNull PermManager permManager, @NotNull MapService mapService, @NotNull MapAllocator allocator) {
        super("debug");
        this.allocator = allocator;

        description = "Debugging utilities for map maker";
        category = CommandCategory.HIDDEN;

        adminCondition = permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN);
        localCondition = ($, $$) -> ServerRuntime.getRuntime().isDevelopment() ? CommandCondition.ALLOW : CommandCondition.DENY;

        addSubcommand(new SysCommand(permManager, mapService));

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

        createPermissionedSubcommand("map_alloc", this::showMapAllocatorDebug,
                "Show map allocator debug info");
        createPermissionedSubcommand("relight", this::relightWorld,
                "Relight the world");
        createPermissionedSubcommand("reheightmap", this::handleReHeightmapDebug,
                "Rebuild the heightmap in the map");
    }

    public @NotNull CommandDsl createPermissionlessSubcommand(@NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @NotNull String description) {
        return createSubcommand(name, handler, null, description);
    }

    public @NotNull CommandDsl createPermissionedSubcommand(@NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @NotNull String description) {
        return createSubcommand(name, handler, adminCondition, description);
    }

    public @NotNull CommandDsl createLocalSubcommand(@NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @NotNull String description) {
        return createSubcommand(name, handler, localCondition, description);
    }

    private void handleDebugResourcePack(@NotNull Player player, @NotNull CommandContext context) {
        var packHash = ServerRuntime.getRuntime().resourcePackSha1();
        player.sendMessage(Component.text("Resource pack: ")
                .append(ComponentUtil.createBasicCopy(packHash)));
    }

    private void handleDebugSelf(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = PlayerDataV2.fromPlayer(player);
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
    }

    private @NotNull CommandDsl createSubcommand(@NotNull String name, @NotNull CommandExecutor.PlayerOnly handler, @Nullable CommandCondition condition, @NotNull String description) {
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
                player.sendMessage("Block: " + block.handler().getKey() + "@" + block.handler().getClass().getSimpleName());
            } else {
                player.sendMessage("Block: " + "no handler");
            }

            player.sendMessage(block.nbt() == null ? "No block NBT" : TagStringIOExt.writeTag(block.nbt()));
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

    private void showMapAllocatorDebug(@NotNull Player player, @NotNull CommandContext context) {
        allocator.showDebugInfo(player);
    }

    private void relightWorld(@NotNull Player player, @NotNull CommandContext context) {
        queueRateLimitedWorldUpdate(player, "relight", batch -> {
            LightingChunk.relight(player.getInstance(), batch);
            batch.forEach(player::sendChunk);
        }, 50);
    }

    private void queueRateLimitedWorldUpdate(@NotNull Player player, @NotNull String name, @NotNull Consumer<List<Chunk>> action, int rate) {
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

    public static class SysCommand extends CommandDsl {
        private static final Logger logger = LoggerFactory.getLogger(SysCommand.class);

        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMdd-HHmmss");
        private static final Path PERF_DUMP_PATH;

        private static final String ASYNC_PROFILER_BIN;

        static {
            Path path;
            try {
                path = Files.createTempDirectory("mapmaker-perfdump");
            } catch (Exception e) {
                e.printStackTrace();
                path = null;
            }
            PERF_DUMP_PATH = path;

            String apPath;
            try {
                var basePath = Path.of("./async-profiler/profiler.sh");
                if (Files.exists(basePath)) {
                    apPath = basePath.toRealPath().toString();
                } else {
                    apPath = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                apPath = null;
            }
            ASYNC_PROFILER_BIN = apPath;

            logger.info("perf dump path: {}", PERF_DUMP_PATH);
            logger.info("async profiler: {} (pid={})", ASYNC_PROFILER_BIN, ProcessHandle.current().pid());
        }

        private final Argument<Boolean> heapDumpCollectArg = Argument.Bool("gc").defaultValue(false);
        private final Argument<Integer> profileTimeArg = Argument.Int("profile-time").min(1).max(60);

        private final MapService mapService;

        public SysCommand(@NotNull PermManager permManager, @NotNull MapService mapService) {
            super("sys");
            this.mapService = mapService;

            description = "System utilities for map maker";

            setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
            var hcmd = subcommand("heapdump", this::createHeapDump, null,
                    "Create a heap dump and upload it to r2");
            hcmd.addSyntax(this::createHeapDump, heapDumpCollectArg);
            var pcmd = subcommand("cpuprof", this::createProfile, null,
                    "Create a cpu profile and upload it to r2");
            pcmd.addSyntax(this::createProfile, profileTimeArg);
        }

        private void createHeapDump(@NotNull CommandSender sender, @NotNull CommandContext context) {
            try {
                var doGarbageCollection = context.get(heapDumpCollectArg);
                var filename = "heap-" + DATE_FORMAT.format(new Date()) + ".hprof";

                // Create the heap dump
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                        server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
                mxBean.dumpHeap(PERF_DUMP_PATH.resolve(filename).toString(), doGarbageCollection);

                sender.sendMessage("heap dump created, uploading...");

                // Write it to s3
                Thread.startVirtualThread(() -> {
                    try {
                        mapService.uploadPerfdump(filename, PERF_DUMP_PATH.resolve(filename));
                        sender.sendMessage(Component.text("heap dump uploaded: " + filename).color(NamedTextColor.GREEN));
                    } catch (Exception e) {
                        ExceptionReporter.reportException(e, (Player) sender);
                        sender.sendMessage("failed to upload heap dump");
                    }
                });
            } catch (IOException e) {
                ExceptionReporter.reportException(e, (Player) sender);
                sender.sendMessage("failed to create heap dump");
            }
        }

        private void createProfile(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (ASYNC_PROFILER_BIN == null) {
                sender.sendMessage(Component.text("async profiler is not present in this build", NamedTextColor.RED));
                return;
            }

            var profileTime = context.has(profileTimeArg) ? context.get(profileTimeArg) : 10;
            var filename = "cpu-" + DATE_FORMAT.format(new Date()) + ".jfr";

            Thread.startVirtualThread(() -> {
                try {
                    sender.sendMessage("creating cpu profile for " + profileTime + " seconds...");

                    var filePath = PERF_DUMP_PATH.resolve(filename).toAbsolutePath();
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            ASYNC_PROFILER_BIN,
                            "-e", "itimer",
                            "-o", "jfr",
                            "-d", String.valueOf(profileTime),
                            "-f", filePath.toString(),
                            String.valueOf(ProcessHandle.current().pid())
                    );
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();

                    // Wait for the command to complete
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        logger.error("Failed to create cpu profile ({}, {}): {}", exitCode, process.info(), new String(process.getErrorStream().readAllBytes()));
                        sender.sendMessage(Component.text("failed to create cpu profile", NamedTextColor.RED));
                        return;
                    }

                    sender.sendMessage("cpu profile created, uploading...");
                    mapService.uploadPerfdump(filename, filePath);

                    sender.sendMessage(Component.text("cpu profile created: " + filename).color(NamedTextColor.GREEN));
                } catch (IOException | InterruptedException e) {
                    ExceptionReporter.reportException(e, (Player) sender);
                    sender.sendMessage("failed to create cpu profile");
                }
            });
        }

        private @NotNull CommandDsl subcommand(@NotNull String name, @NotNull CommandExecutor handler, @Nullable CommandCondition condition, @NotNull String description) {
            var cmd = new CommandDsl(name);
            cmd.setCondition(condition);
            cmd.setDescription(description);
            cmd.addSyntax(handler);
            addSubcommand(cmd);
            return cmd;
        }
    }
}

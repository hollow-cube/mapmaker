package net.hollowcube.mapmaker.dev.command;

import com.sun.management.HotSpotDiagnosticMXBean;
import net.hollowcube.command.Command;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
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

public class SysCommand extends Command {
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
            var basePath = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/async-profiler/profiler.sh");
//            var basePath = Path.of("./async-profiler/profiler.sh");
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

    private final Argument<Integer> profileTimeArg = Argument.Int("profile-time").min(1).max(60);

    private final MapService mapService;

    public SysCommand(@NotNull PermManager permManager, @NotNull MapService mapService) {
        super("sys");
        this.mapService = mapService;

        var cond = permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN);
        setCondition(cond);

        subcommand("heapdump", this::createHeapDump, null);
        var pcmd = subcommand("cpuprof", this::createProfile, null);
        pcmd.addSyntax(this::createProfile, profileTimeArg);
    }

    private void createHeapDump(@NotNull CommandSender sender, @NotNull CommandContext context) {
        try {
            var filename = "heap-" + DATE_FORMAT.format(new Date()) + ".hprof";

            // Create the heap dump
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                    server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
            mxBean.dumpHeap(PERF_DUMP_PATH.resolve(filename).toString(), true);

            sender.sendMessage("heap dump created, uploading...");

            // Write it to s3
            Thread.startVirtualThread(() -> {
                try {
                    mapService.uploadPerfdump(filename, PERF_DUMP_PATH.resolve(filename));
                    sender.sendMessage(Component.text("heap dump uploaded: " + filename).color(NamedTextColor.GREEN));
                } catch (Exception e) {
                    MinecraftServer.getExceptionManager().handleException(e);
                    sender.sendMessage("failed to upload heap dump");
                }
            });
        } catch (IOException e) {
            MinecraftServer.getExceptionManager().handleException(e);
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
                        "-o", "jfr",
                        "-e", "cpu",
                        "-d", String.valueOf(profileTime),
                        "-f", filePath.toString(),
                        String.valueOf(ProcessHandle.current().pid())
                );
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                // Wait for the command to complete
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    sender.sendMessage(Component.text("failed to create cpu profile", NamedTextColor.RED));
                    return;
                }

                sender.sendMessage("cpu profile created, uploading...");
                mapService.uploadPerfdump(filename, filePath);

                sender.sendMessage(Component.text("cpu profile created: " + filename).color(NamedTextColor.GREEN));
            } catch (IOException | InterruptedException e) {
                MinecraftServer.getExceptionManager().handleException(e);
                sender.sendMessage("failed to create cpu profile");
            }
        });
    }

    private @NotNull Command subcommand(@NotNull String name, @NotNull CommandExecutor handler, @Nullable CommandCondition condition) {
        var cmd = new Command(name) {
        };
        cmd.setCondition(condition);
        cmd.addSyntax(handler);
        addSubcommand(cmd);
        return cmd;
    }
}

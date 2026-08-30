package net.hollowcube.mapmaker.dev.commands;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.anticheat.capture.TrimPolicy;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.mapmaker.anticheat.AnticheatCapture;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.utils.time.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/// Drives the anticheat control channel by hand on the dev server, against a local proxy.
///
/// Also carries the automated run of the same thing: with `MAPMAKER_DEV_ANTICHEAT_AUTO=true` every
/// player who joins gets a capture started, flushed and stopped on a timer, so the whole pipeline
/// can be exercised without anybody typing anything. The delays are
/// `MAPMAKER_DEV_ANTICHEAT_AUTO_DELAYS="start,flush,stop"` in seconds.
public class AcDevCommand extends CommandDsl {
    public static final AcDevCommand INSTANCE = new AcDevCommand();

    private static final Logger logger = LoggerFactory.getLogger(AcDevCommand.class);

    private static final int[] DEFAULT_AUTO_DELAYS = {3, 20, 35};

    private final Argument<String> captureId = Argument.Word("capture_id")
        .description("The id to file the trace under, generated if absent");

    public AcDevCommand() {
        super("acdev");

        description = "Start, stop or flush an anticheat capture of your own connection";

        addSyntax(playerOnly(this::handleStart), Argument.Literal("start"));
        addSyntax(playerOnly(this::handleStart), Argument.Literal("start"), captureId);
        addSyntax(playerOnly(this::handleStop), Argument.Literal("stop"));
        addSyntax(playerOnly(this::handleFlush), Argument.Literal("flush"));
    }

    /// Schedules the automated start/flush/stop for every player who joins, if the environment
    /// asks for it. Called once at startup.
    public static void installAutoFire() {
        if (!Boolean.parseBoolean(System.getenv("MAPMAKER_DEV_ANTICHEAT_AUTO"))) return;

        var delays = autoDelays();
        logger.info("acdev: auto capture armed, start +{}s, flush +{}s, stop +{}s", delays[0], delays[1], delays[2]);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;

            var player = event.getPlayer();
            var captureId = generateCaptureId();
            schedule(delays[0], () -> start(player, captureId));
            schedule(delays[1], () -> flush(player));
            schedule(delays[2], () -> stop(player, captureId));
        });
    }

    private static int[] autoDelays() {
        var raw = System.getenv("MAPMAKER_DEV_ANTICHEAT_AUTO_DELAYS");
        if (raw == null || raw.isBlank()) return DEFAULT_AUTO_DELAYS;

        var parts = raw.split(",");
        if (parts.length != 3) {
            logger.warn("acdev: MAPMAKER_DEV_ANTICHEAT_AUTO_DELAYS must be 'start,flush,stop', got '{}'", raw);
            return DEFAULT_AUTO_DELAYS;
        }

        var delays = new int[3];
        for (int i = 0; i < 3; i++) {
            try {
                delays[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                logger.warn("acdev: MAPMAKER_DEV_ANTICHEAT_AUTO_DELAYS is not three numbers: '{}'", raw);
                return DEFAULT_AUTO_DELAYS;
            }
        }
        return delays;
    }

    private static void schedule(int seconds, Runnable action) {
        MinecraftServer.getSchedulerManager().buildTask(action)
            .delay(seconds, TimeUnit.SECOND)
            .schedule();
    }

    private void handleStart(Player player, CommandContext context) {
        start(player, context.has(captureId) ? context.get(captureId) : generateCaptureId());
    }

    private void handleStop(Player player, CommandContext context) {
        var captureId = AnticheatCapture.activeCapture(player);
        if (captureId == null) {
            player.sendMessage("acdev: no capture is open");
            return;
        }
        stop(player, captureId);
    }

    private void handleFlush(Player player, CommandContext context) {
        flush(player);
    }

    private static void start(Player player, String captureId) {
        if (player.isRemoved()) return;

        logger.info("acdev: start {} for {}", captureId, player.getUsername());
        AnticheatCapture.start(player, captureId, TraceHeader.Reason.MANUAL, null, TrimPolicy.DEFAULT);
        player.sendMessage("acdev: started capture " + captureId);
    }

    private static void stop(Player player, String captureId) {
        if (player.isRemoved()) return;

        logger.info("acdev: stop {} for {}", captureId, player.getUsername());
        AnticheatCapture.stop(player, captureId);
        player.sendMessage("acdev: stopped capture " + captureId);
    }

    private static void flush(Player player) {
        if (player.isRemoved()) return;

        logger.info("acdev: flush for {} (capture {})", player.getUsername(), AnticheatCapture.activeCapture(player));
        AnticheatCapture.flush(player, TraceHeader.Reason.MANUAL);
        player.sendMessage("acdev: flushed the ring buffer");
    }

    private static String generateCaptureId() {
        return "acdev-" + UUID.randomUUID();
    }
}

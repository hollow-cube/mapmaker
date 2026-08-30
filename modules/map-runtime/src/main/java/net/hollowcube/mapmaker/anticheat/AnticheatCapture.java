package net.hollowcube.mapmaker.anticheat;

import net.hollowcube.anticheat.capture.TrimPolicy;
import net.hollowcube.anticheat.control.CaptureControl;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.mapmaker.feature.posthog.PostHogFeatureFlagProvider;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.posthog.PostHog;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The backend half of the anticheat capture control channel: it asks the proxy to record a
/// connection. The proxy answers nothing — a shipped trace is found in the store by capture id.
///
/// Nothing here knows how a capture is made — the tap on the proxy owns all of that — so the whole
/// surface is three messages on a plugin channel plus the sampling policy that decides when to send
/// the first of them.
///
/// The one piece of state is a tag on the player naming the capture the proxy is believed to have
/// open, so a stop is only ever sent for a start that was actually sent. It is written from the
/// tick thread that owns the player and read nowhere else, so it needs no locking.
///
/// The sampling policy is the [#FLAG] posthog feature flag: the flag's enabled state is the switch
/// and its payload is the [AnticheatConfig] json (`{"rate":0.05,"trusted":"uuid,uuid",
/// "playingOnly":true,"maxSeconds":600}`), so rate and cohort change from the posthog UI with no
/// deploy at all. Evaluation is local (the servers init posthog with a personal api key), so
/// [#config] never blocks; a server without posthog reads as disabled.
public final class AnticheatCapture {
    private static final Logger logger = LoggerFactory.getLogger(AnticheatCapture.class);

    public static final String CHANNEL = "mapmaker:anticheat";

    private static final String FLAG = "anticheat_capture";

    private static final Tag<String> ACTIVE_CAPTURE = Tag.Transient("mapmaker:anticheat/active_capture");

    /// The last payload parsed, one slot: the flag answers every call but the payload only ever
    /// changes when somebody edits it.
    private record Parsed(String payload, AnticheatConfig config) {}

    private static volatile @Nullable Parsed parsed;

    /// The sampling policy as the flag has it right now. An enabled flag with no payload is
    /// disabled — there is no rate and no trusted list to capture by.
    public static AnticheatConfig config() {
        var flag = PostHog.getFeatureFlag(FLAG, PostHogFeatureFlagProvider.NO_USER);
        var payload = flag.getPayload();
        if (!flag.isEnabled() || payload == null) return AnticheatConfig.DISABLED;

        var cached = parsed;
        if (cached == null || !cached.payload.equals(payload)) {
            cached = new Parsed(payload, parse(payload));
            parsed = cached;
        }
        return cached.config;
    }

    static AnticheatConfig parse(String payload) {
        try {
            var config = AbstractHttpService.GSON.fromJson(payload, AnticheatConfig.class);
            return new AnticheatConfig(true, config.rate(), config.trusted(), config.playingOnly(),
                config.maxSeconds());
        } catch (RuntimeException e) {
            logger.warn("anticheat: the {} flag payload does not parse, capturing nothing: {}", FLAG, payload, e);
            return AnticheatConfig.DISABLED;
        }
    }

    /// The capture the proxy is believed to have open for this player, if any.
    public static @Nullable String activeCapture(Player player) {
        return player.getTag(ACTIVE_CAPTURE);
    }

    /// Starts a capture only if the sampling policy asks for one, labelling it with the player's
    /// cohort and the default trim.
    ///
    /// @return whether a start was sent
    public static boolean startSampled(Player player, String captureId, TraceHeader.Reason reason) {
        var config = config();
        if (!config.shouldCapture(player)) return false;

        start(player, captureId, reason, config.cohort(player.getUuid()), TrimPolicy.DEFAULT);
        return true;
    }

    /// Asks the proxy to record this connection until [#stop] or the player leaves.
    ///
    /// The proxy never times a capture out, so the cap on how long one runs is scheduled here. It
    /// rides the player's own scheduler, which is cancelled when they go, and stopping a capture
    /// that already ended is a no-op.
    public static void start(Player player, String captureId, TraceHeader.Reason reason,
                             @Nullable TraceHeader.Cohort cohort, TrimPolicy trim) {
        player.setTag(ACTIVE_CAPTURE, captureId);
        send(player, new CaptureControl.Start(captureId, reason, cohort, trim));

        player.scheduler().buildTask(() -> stop(player, captureId))
            .delay(config().maxSeconds(), TimeUnit.SECOND)
            .schedule();
    }

    /// Closes the capture, if this is the one that was started for the player.
    public static void stop(Player player, String captureId) {
        if (!captureId.equals(player.getTag(ACTIVE_CAPTURE))) return;

        player.removeTag(ACTIVE_CAPTURE);
        send(player, new CaptureControl.Stop(captureId));
    }

    /// Ships the proxy's ring buffer, the last minute or so of the connection, without disturbing
    /// whatever capture is open.
    public static void flush(Player player, TraceHeader.Reason reason) {
        send(player, new CaptureControl.Flush(activeCapture(player), reason));
    }

    private static void send(Player player, CaptureControl message) {
        player.sendPluginMessage(CHANNEL, message.encode());
    }

    private AnticheatCapture() {}
}

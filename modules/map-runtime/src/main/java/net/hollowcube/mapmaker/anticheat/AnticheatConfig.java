package net.hollowcube.mapmaker.anticheat;

import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/// The sampling policy: which connections the backend asks the proxy to capture, and for how long.
///
/// It lives here rather than on the proxy so rate, cohort and filters change with a backend config
/// roll instead of a proxy deploy. [#trusted] is a comma-separated list of player uuids believed
/// legitimate — staff and long-standing players — who are always captured and labelled
/// [TraceHeader.Cohort#TRUSTED] so the corpus is mostly clean; everyone else is captured at
/// [#rate].
///
/// @param rate       fraction of eligible runs to capture, 0..1
/// @param maxSeconds how long a single capture may run before the backend stops it
/// @param trustedIds [#trusted] split into uuids, derived rather than configured: whatever a
///                   config puts here is thrown away and rebuilt, so the split happens once per
///                   config instead of once per join
@RuntimeGson
public record AnticheatConfig(
    boolean enabled,
    double rate,
    String trusted,
    boolean playingOnly,
    int maxSeconds,
    Set<String> trustedIds
) {
    /// The cap from the plan: no capture runs longer than ten minutes.
    public static final int DEFAULT_MAX_SECONDS = 600;

    /// What every server starts with, and what it keeps if the config has nothing to say.
    public static final AnticheatConfig DISABLED = new AnticheatConfig(false, 0, "", true, DEFAULT_MAX_SECONDS);

    public AnticheatConfig(boolean enabled, double rate, String trusted, boolean playingOnly, int maxSeconds) {
        this(enabled, rate, trusted, playingOnly, maxSeconds, Set.of());
    }

    public AnticheatConfig {
        rate = Math.clamp(rate, 0, 1);
        maxSeconds = maxSeconds > 0 ? maxSeconds : DEFAULT_MAX_SECONDS;
        trusted = Objects.requireNonNullElse(trusted, "");
        trustedIds = split(trusted);
    }

    public boolean shouldCapture(Player player) {
        return shouldCapture(player.getUuid(), isPlaying(player));
    }

    /// The whole decision without reaching into the player, so it can be exercised on its own.
    ///
    /// @param playing whether the player is actually playing rather than spectating or building
    public boolean shouldCapture(UUID playerId, boolean playing) {
        if (!enabled) return false;
        if (playingOnly && !playing) return false;
        if (isTrusted(playerId)) return true;
        return rate > 0 && ThreadLocalRandom.current().nextDouble() < rate;
    }

    public TraceHeader.Cohort cohort(UUID playerId) {
        return isTrusted(playerId) ? TraceHeader.Cohort.TRUSTED : TraceHeader.Cohort.RANDOM;
    }

    public boolean isTrusted(UUID playerId) {
        return trustedIds.contains(playerId.toString());
    }

    private static Set<String> split(String trusted) {
        if (trusted.isBlank()) return Set.of();
        var ids = new HashSet<String>();
        for (var id : trusted.split(",")) {
            var trimmed = id.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) ids.add(trimmed);
        }
        return Set.copyOf(ids);
    }

    /// Spectating and building both leave the player in a mode they cannot really cheat in, and a
    /// capture of either is a trace of nothing much.
    private static boolean isPlaying(Player player) {
        var gameMode = player.getGameMode();
        return gameMode == GameMode.ADVENTURE || gameMode == GameMode.SURVIVAL;
    }
}

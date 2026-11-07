package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

import static net.kyori.adventure.text.Component.translatable;

/// 'legacy' conditions on checkpoint/status plates. The goal is to convert some or all of these to molang
/// conditions which use conditionally applied molang env. But for now they are listed here :-)
///
/// @param noBacktrack    blocks the trigger while its id is in the last state's history. prevents re triggering a
///                       checkpoint while you are currently at that checkpoint (or any previous for additive mode)
/// @param statusCooldown the per-player cooldown every status shares
/// @param faceOnExit     leaving the checkpoint just reached points its saved position the way the player left it,
///                       or its checkpoint's respawn view; only while the trigger has no `teleport`, which
///                       sets the view itself
public record LegacyRules(boolean noBacktrack, boolean statusCooldown, boolean faceOnExit) {
    public static final LegacyRules NONE = new LegacyRules(false, false, false);

    public static final StructCodec<LegacyRules> CODEC = StructCodec.struct(
        "no_backtrack", Codec.BOOLEAN.optional(false), LegacyRules::noBacktrack,
        "status_cooldown", Codec.BOOLEAN.optional(false), LegacyRules::statusCooldown,
        "face_on_exit", Codec.BOOLEAN.optional(false), LegacyRules::faceOnExit,
        LegacyRules::new
    );

    private static final TagCooldown PROGRESS_INDEX_WARNING = new TagCooldown(
        "mapmaker:play/progress_index_warning",
        5000
    );

    // Some maps published before this depend on the longer cooldown, so it stays for them only.
    private static final Instant LEGACY_STATUS_PLATE_DATE = Instant.ofEpochMilli(1776387977716L); // 2026-04-16 ish
    private static final TagCooldown LEGACY_STATUS_APPLY_COOLDOWN = new TagCooldown(
        "mapmaker:status_plate_cooldown",
        250
    );
    private static final TagCooldown STATUS_APPLY_COOLDOWN = new TagCooldown(
        "mapmaker:status_plate_cooldown",
        1
    );

    /// @param progressIndex the trigger's last `progress_index` value, or -1 for none
    boolean preCondition(
        Player player,
        MapData map,
        PlayState state,
        String id,
        int progressIndex
    ) {
        if (checkProgressIndex(player, map, state, progressIndex)) return true;
        var lastState = state.lastState();
        return noBacktrack && lastState != null && lastState.hasStatus(id);
    }

    boolean postCondition(Player player, MapData map) {
        return statusCooldown && !statusCooldown(map).test(player);
    }

    void faceCheckpoint(Player player, PlayState state, String id, @Nullable RelativePos respawn) {
        var pos = state.pos();
        var lastState = state.lastState();

        if (state.history().isEmpty()) return;
        if (lastState == null) return;
        if (!state.history().getLast().equals(id)) return;
        if (pos == null) return;

        var respawnView = OpUtils.mapOr(
            respawn,
            target -> target.resolve(player.getPosition()),
            player.getPosition()
        );
        float yaw = respawnView.yaw();
        float pitch = respawnView.pitch();

        state.setPos(pos.withView(yaw, pitch));
        var lastPos = lastState.pos();
        if (lastPos != null) lastState.setPos(lastPos.withView(yaw, pitch));
    }

    private static TagCooldown statusCooldown(MapData map) {
        return !map.isPublished() || map.publishedAt().isAfter(LEGACY_STATUS_PLATE_DATE)
            ? STATUS_APPLY_COOLDOWN
            : LEGACY_STATUS_APPLY_COOLDOWN;
    }

    private static boolean checkProgressIndex(
        Player player,
        MapData map,
        PlayState state,
        int progressIndex
    ) {
        if (progressIndex > 0) {
            int currentIndex = state.get(Attachments.PROGRESS_INDEX, 0);
            boolean isFail = MapSettings.get(map.settings(), MapSettings.PROGRESS_INDEX_ADDITION)
                ? // With additive index you can get anything <= current + 1
                (progressIndex > currentIndex + 1)
                // Without additive progress index you must be at the prior index or the current one
                : (progressIndex != currentIndex && progressIndex != currentIndex + 1);
            if (isFail) {
                if (PROGRESS_INDEX_WARNING.test(player)) {
                    player.sendMessage(
                        translatable(
                            "action.progress_index.not_acceptable",
                            Component.text(currentIndex),
                            Component.text(progressIndex - 1)
                        )
                    );
                }
                return true;
            }
        }
        return false;
    }
}

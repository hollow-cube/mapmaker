package net.hollowcube.mapmaker.punishments;

import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface PunishmentService {

    @NotNull List<Punishment> getPunishments(@Nullable UUID playerId, @Nullable UUID executorId,
                                             @Nullable PunishmentType type);

    default boolean isPlayerBanned(@NotNull UUID playerId) {
        return !this.getPunishments(playerId, null, PunishmentType.BAN).isEmpty();
    }

    default boolean isPlayerMuted(@NotNull UUID playerId) {
        return !this.getPunishments(playerId, null, PunishmentType.MUTE).isEmpty();
    }

    @NotNull Punishment createPunishment(@NotNull UUID playerId, @NotNull UUID executorId,
                                         @NotNull PunishmentType type, @NotNull String comment,
                                         @Nullable String ladderId);

    void revokePunishment(@NotNull UUID playerId, @NotNull PunishmentType type, @NotNull UUID revokedBy,
                          @NotNull String revokedReason);

    @NotNull List<PunishmentLadder> getAllLadders();

    @NotNull List<PunishmentLadder> getLaddersByType(@NotNull PunishmentType type);

    @NotNull List<PunishmentLadder> searchLadders(@NotNull String idQuery, @NotNull PunishmentType type);

    @NotNull PunishmentLadder getLadderById(@NotNull String id);

    class InternalError extends RuntimeException {
        public InternalError(String message) {
            super(message);
        }
    }
}

package net.hollowcube.mapmaker.punishments;

import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface PunishmentService {

    @NotNull
    List<Punishment> getPunishments(@Nullable String playerId, @Nullable UUID executorId, @Nullable PunishmentType type);

    @NotNull Punishment createPunishment(
            @NotNull UUID playerId, @NotNull UUID executorId,
            @NotNull PunishmentType type, @Nullable String comment,
            @Nullable String reason);

    @Nullable Punishment getActivePunishment(@NotNull String playerId, @NotNull PunishmentType type);

    void revokePunishment(
            @NotNull UUID playerId, @NotNull PunishmentType type,
            @NotNull UUID revokedBy, @NotNull String revokedReason);

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

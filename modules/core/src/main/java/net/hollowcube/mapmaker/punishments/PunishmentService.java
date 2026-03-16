package net.hollowcube.mapmaker.punishments;

import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface PunishmentService {

    List<Punishment> getPunishments(@Nullable String playerId, @Nullable UUID executorId, @Nullable PunishmentType type);

    Punishment createPunishment(
        UUID playerId,
        UUID executorId,
        PunishmentType type,
        @Nullable String comment,
        @Nullable String reason
    );

    @Nullable Punishment getActivePunishment(String playerId, PunishmentType type);

    void revokePunishment(
        UUID playerId, PunishmentType type,
        UUID revokedBy, String revokedReason
    );

    List<PunishmentLadder> getAllLadders();

    List<PunishmentLadder> getLaddersByType(PunishmentType type);

    List<PunishmentLadder> searchLadders(String idQuery, PunishmentType type);

    PunishmentLadder getLadderById(String id);

    class InternalError extends RuntimeException {
        public InternalError(String message) {
            super(message);
        }
    }
}

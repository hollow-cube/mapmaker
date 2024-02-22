package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class NoopPunishmentService implements PunishmentService {

    @Override
    public @NotNull List<Punishment> getPunishments(@Nullable UUID playerId, @Nullable UUID executorId, @Nullable PunishmentType type) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull Punishment createPunishment(@NotNull UUID playerId, @NotNull UUID executorId,
                                                @NotNull PunishmentType type, @NotNull String comment,
                                                @Nullable String ladderId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void revokePunishment(@NotNull UUID playerId, @NotNull PunishmentType type, @NotNull UUID revokedBy,
                                 @NotNull String revokedReason) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull List<PunishmentLadder> getAllLadders() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull List<PunishmentLadder> getLaddersByType(@NotNull PunishmentType type) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull List<PunishmentLadder> searchLadders(@NotNull String idQuery, @NotNull PunishmentType type) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull PunishmentLadder getLadderById(@NotNull String id) {
        throw new UnsupportedOperationException("not implemented");
    }
}

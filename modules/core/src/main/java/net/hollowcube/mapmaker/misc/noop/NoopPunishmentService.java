package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.types.Punishment;
import net.hollowcube.mapmaker.punishments.types.PunishmentLadder;
import net.hollowcube.mapmaker.punishments.types.PunishmentType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class NoopPunishmentService implements PunishmentService {

    @Override
    public List<Punishment> getPunishments(@Nullable String playerId, @Nullable UUID executorId, @Nullable PunishmentType type) {
        return List.of();
    }

    @Override
    public @Nullable Punishment getActivePunishment(String playerId, PunishmentType type) {
        return null;
    }

    @Override
    public Punishment createPunishment(UUID playerId, UUID executorId,
                                       PunishmentType type, @Nullable String comment,
                                       @Nullable String ladderId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void revokePunishment(UUID playerId, PunishmentType type, UUID revokedBy,
                                 String revokedReason) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<PunishmentLadder> getAllLadders() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<PunishmentLadder> getLaddersByType(PunishmentType type) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<PunishmentLadder> searchLadders(String idQuery, PunishmentType type) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public PunishmentLadder getLadderById(String id) {
        throw new UnsupportedOperationException("not implemented");
    }
}

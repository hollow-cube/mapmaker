package net.hollowcube.compat.axiom.events;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AxiomEntitiesDataRequestEvent implements AxiomEvent {

    private final Player player;
    private final List<Entity> entities;
    private final Map<UUID, CompoundBinaryTag> data = new HashMap<>();

    public AxiomEntitiesDataRequestEvent(@NotNull Player player, @NotNull Set<UUID> uuids) {
        this(player, uuids.stream()
                .map(uuid -> player.getInstance().getEntityByUuid(uuid))
                .filter(Objects::nonNull)
                .filter(entity -> entity.isViewer(player) && !(entity instanceof Player))
                .toList());
    }

    public AxiomEntitiesDataRequestEvent(@NotNull Player player, @Nullable List<Entity> entities) {
        this.player = player;
        this.entities = entities;
    }

    @Contract(pure = true)
    public @NotNull Player player() {
        return player;
    }

    @Contract(pure = true)
    public @NotNull List<Entity> entities() {
        return entities;
    }

    public @Nullable CompoundBinaryTag getData(UUID uuid) {
        return data.get(uuid);
    }

    public void setData(UUID uuid, @Nullable CompoundBinaryTag data) {
        this.data.put(uuid, data);
    }
}
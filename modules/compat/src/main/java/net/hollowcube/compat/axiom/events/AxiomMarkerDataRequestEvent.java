package net.hollowcube.compat.axiom.events;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AxiomMarkerDataRequestEvent implements AxiomEvent, CancellableEvent {

    private final Player player;
    private final Entity marker;

    private CompoundBinaryTag data;
    private boolean cancelled;

    public AxiomMarkerDataRequestEvent(@NotNull Player player, @NotNull UUID uuid) {
        this(player, player.getInstance().getEntityByUuid(uuid));
    }

    public AxiomMarkerDataRequestEvent(@NotNull Player player, @Nullable Entity marker) {
        this.player = player;
        this.marker = marker;
    }

    @Contract(pure = true)
    public @NotNull Player player() {
        return player;
    }

    @Contract(pure = true)
    public @Nullable Entity marker() {
        return marker;
    }

    public @Nullable CompoundBinaryTag getData() {
        return data;
    }

    public void setData(@Nullable CompoundBinaryTag data) {
        this.data = data;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
package net.hollowcube.compat.axiom.events;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.RecursiveEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class AxiomMarkerDataRequestEvent implements AxiomEvent, RecursiveEvent, CancellableEvent {

    private final Player player;
    private final @Nullable Entity marker;

    private @Nullable CompoundBinaryTag data;
    private boolean cancelled;

    public AxiomMarkerDataRequestEvent(Player player, UUID uuid) {
        this(player, player.getInstance().getEntityByUuid(uuid));
    }

    public AxiomMarkerDataRequestEvent(Player player, @Nullable Entity marker) {
        this.player = player;
        this.marker = marker;
    }

    @Contract(pure = true)
    public Player player() {
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

    public static class RightClick extends AxiomMarkerDataRequestEvent {
        public RightClick(Player player, UUID uuid) {
            super(player, uuid);
        }
    }

    public static class Copying extends AxiomMarkerDataRequestEvent {
        public Copying(Player player, UUID uuid) {
            super(player, uuid);
        }
    }
}
package net.hollowcube.terraform.compat.axiom.event;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when a valid axiom client requests marker data for an entity id.
 *
 * <p>By default, no response will be sent to the client. Only after setting the data it will send a response, unless
 * the event is cancelled in which case no response will be sent also.</p>
 */
public class TerraformAxiomRequestMarkerDataEvent implements PlayerInstanceEvent, CancellableEvent {
    private final Player editor;
    private final UUID entityUuid;
    private CompoundBinaryTag data = null;
    private boolean cancelled = false;

    public TerraformAxiomRequestMarkerDataEvent(@NotNull Player editor, @NotNull UUID entityUuid) {
        this.editor = editor;
        this.entityUuid = entityUuid;
    }

    public @NotNull Player getEditor() {
        return editor;
    }

    @Override
    public @NotNull Player getPlayer() {
        return editor;
    }

    public @NotNull UUID getEntityUuid() {
        return entityUuid;
    }

    public @Nullable CompoundBinaryTag getData() {
        return data;
    }

    public void setData(@Nullable CompoundBinaryTag data) {
        this.data = data;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}

package net.hollowcube.terraform.compat.axiom.event;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record TerraformAxiomUpdateCustomEntityDataEvent(
        @NotNull Player editor,
        @NotNull UUID entityUuid,
        @NotNull CompoundBinaryTag data
) implements Event {

    /**
     * A interface to mark that the entity being edited will have this event invoked instead of setting the data.
     */
    public interface Receiver {}
}

package net.hollowcube.terraform.compat.axiom.event;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TerraformAxiomUpdateMarkerDataEvent implements Event {
    private final Player editor;
    private final UUID entityUuid;
    private final CompoundBinaryTag data;

    public TerraformAxiomUpdateMarkerDataEvent(@NotNull Player editor, @NotNull UUID entityUuid, @NotNull CompoundBinaryTag data) {
        this.editor = editor;
        this.entityUuid = entityUuid;
        this.data = data;
    }

    public @NotNull Player getEditor() {
        return editor;
    }

    public @NotNull UUID getEntityUuid() {
        return entityUuid;
    }

    public @NotNull CompoundBinaryTag getData() {
        return data;
    }

}

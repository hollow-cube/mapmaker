package net.hollowcube.terraform.compat.axiom.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.UUID;

public class TerraformAxiomUpdateMarkerDataEvent implements Event {
    private final Player editor;
    private final UUID entityUuid;
    private final NBTCompound data;

    public TerraformAxiomUpdateMarkerDataEvent(@NotNull Player editor, @NotNull UUID entityUuid, @NotNull NBTCompound data) {
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

    public @NotNull NBTCompound getData() {
        return data;
    }

}

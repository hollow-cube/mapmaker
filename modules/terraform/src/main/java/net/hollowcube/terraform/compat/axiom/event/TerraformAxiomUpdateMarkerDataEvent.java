package net.hollowcube.terraform.compat.axiom.event;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record TerraformAxiomUpdateMarkerDataEvent(
        @NotNull Player editor,
        @NotNull UUID entityUuid,
        @NotNull CompoundBinaryTag data
) implements Event {

}

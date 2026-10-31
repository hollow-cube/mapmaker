package net.hollowcube.terraform.schem;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.BlockVec;

/// A loaded schematic
///
/// @param origin the point a paste lands on its anchor
public record Schematic(EditBuffer buffer, BlockVec origin, CompoundBinaryTag metadata) {

    public Schematic(EditBuffer buffer, BlockVec origin) {
        this(buffer, origin, CompoundBinaryTag.empty());
    }
}

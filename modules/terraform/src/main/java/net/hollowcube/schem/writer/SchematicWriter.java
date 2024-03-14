package net.hollowcube.schem.writer;

import net.hollowcube.schem.Schematic;
import org.jetbrains.annotations.NotNull;

public interface SchematicWriter {
    @NotNull
    String NAME = "github.com/hollow-cube/schem";

    byte @NotNull [] write(@NotNull Schematic schematic);
}

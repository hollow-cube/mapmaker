package net.hollowcube.schem.reader;

import net.hollowcube.schem.Schematic;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface SchematicReader {

    @NotNull Schematic read(byte @NotNull [] data) throws IOException;

}

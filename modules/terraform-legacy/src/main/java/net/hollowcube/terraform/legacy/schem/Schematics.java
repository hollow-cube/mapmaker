package net.hollowcube.terraform.legacy.schem;

import net.hollowcube.schem.reader.SchematicReader;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SpongeSchematicReader;
import net.hollowcube.terraform.schem.SpongeSchematicWriter;
import net.minestom.server.coordinate.BlockVec;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Schematics to and from stored bytes. Sponge schematics go through terraform's streaming
 * reader and writer, other formats (Litematica, structures, Axiom blueprints, MCEdit) are read through the
 * schem library.
 */
public final class Schematics {

    public static @NotNull Schematic read(byte @NotNull [] data) throws IOException {
        try {
            return SpongeSchematicReader.read(data, SchemUpgrader.INSTANCE);
        } catch (IOException spongeFailure) {
            try {
                return readOtherFormat(data);
            } catch (Exception e) {
                spongeFailure.addSuppressed(e);
                throw spongeFailure;
            }
        }
    }

    public static byte @NotNull [] write(@NotNull Schematic schematic) {
        return SpongeSchematicWriter.write(schematic, SchemUpgrader.INSTANCE);
    }

    private static @NotNull Schematic readOtherFormat(byte @NotNull [] data) throws IOException {
        var schematic = SchematicReader.detecting().read(data);
        var builder = EditBuffer.builder();
        schematic.forEachBlock((pos, block) -> builder.setBlockState(pos.blockX(), pos.blockY(), pos.blockZ(), block));
        return new Schematic(builder.build(), BlockVec.ZERO, schematic.metadata());
    }

    private Schematics() {
    }
}

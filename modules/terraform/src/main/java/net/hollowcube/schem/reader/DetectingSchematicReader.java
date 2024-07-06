package net.hollowcube.schem.reader;

import net.hollowcube.schem.AxiomBlueprint;
import net.hollowcube.schem.Schematic;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class DetectingSchematicReader implements SchematicReader {

    @Override
    public @NotNull Schematic read(byte @NotNull [] data) throws IOException, UnknownSchematicTypeException {
        // Axiom Blueprint is the simplest because it always has a known magic number at the start.
        var dis = new DataInputStream(new ByteArrayInputStream(data));
        if (dis.readInt() == AxiomBlueprint.MAGIC_NUMBER) {
            return new AxiomBlueprintReader().read(data);
        }

        // All other options are an NBT compound at the root.
        final Map.Entry<String, CompoundBinaryTag> rootPair = BinaryTagIO.reader().readNamed(
                new ByteArrayInputStream(data),
                BinaryTagIO.Compression.GZIP
        );

        final Set<String> keys = rootPair.getValue().keySet();
        return switch (rootPair.getKey()) {
            case "" -> {
                // An empty key at the root can either be a Structure, Litematica, or Sponge V3 schematic
                if (keys.contains("palette") || keys.contains("palettes")) {
                    // Definitely a structure. Note that both others have palette but it is not in the root object.
                    yield new StructureReader().read(rootPair);
                } else if (keys.contains("MinecraftDataVersion") || keys.contains("Regions")) {
                    // Definitely a Litematic schematic.
                    yield new LitematicaSchematicReader().read(rootPair);
                }

                // Otherwise, its probably a sponge schematic
                yield new SpongeSchematicReader().read(rootPair);
            }
            case "Schematic" -> {
                // Schematic as the root key can be Sponge V1, V2 or an MCEdit schematic.
                if (keys.contains("Materials") || keys.contains("Platform") || keys.contains("Blocks") || keys.contains("Data")) {
                    // Any of these indicate an MCEdit schematic
                    yield new MCEditSchematicReader().read(rootPair);
                }

                // Otherwise, its probably a sponge schematic
                yield new SpongeSchematicReader().read(rootPair);
            }
            default -> throw new UnknownSchematicTypeException();
        };
    }
    
}

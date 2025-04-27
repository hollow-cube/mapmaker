package net.hollowcube.terraform.session;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.builder.SchematicBuilder;
import net.hollowcube.schem.reader.SpongeSchematicReader;
import net.hollowcube.schem.writer.SpongeSchematicWriter;
import net.hollowcube.terraform.util.transformations.SchematicTransformation;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Clipboard {

    public static final NetworkBuffer.Type<Clipboard> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, Clipboard::name,
            NetworkBuffer.BYTE_ARRAY.transform(
                    bytes -> new SpongeSchematicReader().read(bytes),
                    schematic -> new SpongeSchematicWriter().write(schematic)
            ).optional(), Clipboard::getInitialSchematic,
            Clipboard::new
    );

    public static final @NotNull String DEFAULT = "default";
    @RegExp
    public static final @NotNull String NAME_REGEX = "[a-z0-9_]+";

    private final String name;
    private final List<SchematicTransformation> transformations = new ArrayList<>();

    private Schematic schematic; // The current block data stored in this clipboard.

    public Clipboard(@NotNull String name) {
        this(name, null);
    }

    private Clipboard(@NotNull String name, @Nullable Schematic schematic) {
        this.name = name;
        this.schematic = schematic;
    }

    public @NotNull String name() {
        return name;
    }

    public @Nullable Schematic getInitialSchematic() {
        return schematic;
    }

    public boolean isEmpty() {
        return schematic == null;
    }

    public void setData(@Nullable Schematic schematic) {
        clear();
        this.schematic = schematic;
    }

    public void clear() {
        this.schematic = null;
        this.transformations.clear();
    }

    public @NotNull Schematic getTransformedSchematic() {
        if (this.transformations.isEmpty()) {
            return schematic;
        } else {
            var builder = SchematicBuilder.builder();

            schematic.forEachBlock((pos, block) -> {
                for (SchematicTransformation transformation : transformations) {
                    pos = transformation.apply(pos, schematic.size(), schematic.offset());
                    block = transformation.apply(block);
                }
                // Check why this needs its offset subtracted
                builder.block(pos.sub(schematic.offset()), block);
            });

            var offset = schematic.offset();
            for (SchematicTransformation transformation : transformations) {
                offset = transformation.apply(offset, schematic.size());
            }
            builder.offset(offset);

            return builder.build();
        }
    }

    public void transform(@NotNull SchematicTransformation transformation) {
        this.transformations.add(transformation);
    }
}

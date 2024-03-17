package net.hollowcube.test.subject;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.builder.SchematicBuilder;
import net.hollowcube.test.TestEnvImpl;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TestInstance extends InstanceContainer {
    private final TestEnvImpl owner;
    private final String id;

    public TestInstance(@NotNull TestEnvImpl owner, @NotNull String id, @Nullable IChunkLoader loader) {
        super(UUID.randomUUID(), DimensionType.OVERWORLD, loader);
        this.owner = owner;
        this.id = id;
    }

    public @NotNull TestEnvImpl owner() {
        return owner;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull Schematic makeSnapshot() {
        var builder = SchematicBuilder.builder();
        for (int x = -16; x < 16; x++) {
            for (int y = -16; y < 16; y++) {
                for (int z = -16; z < 16; z++) {
                    var block = getBlock(x, y, z);
                    builder.block(x, y, z, block);
                }
            }
        }
        return builder.build();
    }

}

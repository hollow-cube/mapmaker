package net.hollowcube.test.snapshot;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SpongeSchematicReader;
import net.hollowcube.schem.writer.SpongeSchematicWriter;
import net.hollowcube.test.assertions.Assertions;
import net.hollowcube.test.subject.TestInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InstanceSnapshot implements Snapshot<TestInstance, Schematic> {

    @Override
    public @NotNull Schematic createSnapshot(@NotNull TestInstance value) {
        return value.makeSnapshot();
    }

    @Override
    public byte @NotNull [] serializeSnapshot(@Nullable Schematic snapshot) {
        if (snapshot == null) return new byte[0];
        return new SpongeSchematicWriter().write(snapshot);
    }

    @Override
    public Schematic deserializeSnapshot(byte @NotNull [] serialized) {
        if (serialized.length == 0) return null;
        return new SpongeSchematicReader().read(serialized);
    }

    @Override
    public void assertSnapshot(Schematic expected, Schematic actual) {
        Assertions.assertSchematic(expected, actual);
    }
}

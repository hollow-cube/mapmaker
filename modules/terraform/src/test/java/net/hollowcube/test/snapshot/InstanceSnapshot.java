package net.hollowcube.test.snapshot;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.hollowcube.schem.writer.SchematicWriter;
import net.hollowcube.test.assertions.Assertions;
import net.hollowcube.test.subject.TestInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class InstanceSnapshot implements Snapshot<TestInstance, Schematic> {

    @Override
    public @NotNull Schematic createSnapshot(@NotNull TestInstance value) {
        return value.makeSnapshot();
    }

    @Override
    public byte @NotNull [] serializeSnapshot(@Nullable Schematic snapshot) {
        if (snapshot == null) return new byte[0];
        return SchematicWriter.sponge().write(snapshot);
    }

    @Override
    public Schematic deserializeSnapshot(byte @NotNull [] serialized) {
        if (serialized.length == 0) return null;
        try {
            return SchematicReader.sponge().read(serialized);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void assertSnapshot(Schematic expected, Schematic actual) {
        Assertions.assertSchematic(expected, actual);
    }
}

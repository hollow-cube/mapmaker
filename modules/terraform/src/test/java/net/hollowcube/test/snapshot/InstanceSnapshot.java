package net.hollowcube.test.snapshot;

import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SchematicReader;
import net.hollowcube.terraform.schem.SchematicWriter;
import net.hollowcube.test.assertions.Assertions;
import net.hollowcube.test.subject.TestInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;

public class InstanceSnapshot implements Snapshot<TestInstance, Schematic> {

    @Override
    public @NotNull Schematic createSnapshot(@NotNull TestInstance value) {
        return value.makeSnapshot();
    }

    @Override
    public byte @NotNull [] serializeSnapshot(@Nullable Schematic snapshot) {
        if (snapshot == null) return new byte[0];
        return SchematicWriter.write(snapshot);
    }

    @Override
    public Schematic deserializeSnapshot(byte @NotNull [] serialized) {
        if (serialized.length == 0) return null;
        return SchematicReader.read(new ByteArrayInputStream(serialized));
    }

    @Override
    public void assertSnapshot(Schematic expected, Schematic actual) {
        Assertions.assertSchematic(expected, actual);
    }
}

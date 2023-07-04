package net.hollowcube.test.assertions;

import net.hollowcube.terraform.schem.Rotation;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SchematicReader;
import net.hollowcube.terraform.schem.SchematicWriter;
import net.hollowcube.test.TestEnvImpl;
import net.hollowcube.test.internal.TestInstance;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.snapshot.Snapshot;
import org.jetbrains.annotations.NotNull;
import org.opentest4j.AssertionFailedError;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public final class Assertions {
    private Assertions() {}

    public static void assertSnapshot(@NotNull Instance instance) {
        try {
            var ti = assertInstanceOf(TestInstance.class, instance, "Instances in tests must be created through the test environment.");

            var snapshot = ti.makeSnapshot();
            Schematic oldSnapshot;

            var testId = ti.owner().getUniqueTestId();
            SchematicWriter.write(snapshot, TestEnvImpl.TEMP_PATH.resolve(testId));

            if (TestEnvImpl.WRITE_SNAPSHOTS) {
                System.err.println("Writing snapshot for " + testId + " to disk.");
                var path = TestEnvImpl.RESOURCES_PATH.resolve("snapshots/" + testId);
                SchematicWriter.write(snapshot, path);

                oldSnapshot = snapshot;
            } else {
                var snapshotInputStream = Assertions.class.getResourceAsStream("/snapshots/" + testId);
                if (snapshotInputStream == null) {
                    System.err.println("Snapshot was not present for " + testId + ", creating it now.");
                    var path = TestEnvImpl.RESOURCES_PATH.resolve("snapshots/" + testId);
                    SchematicWriter.write(snapshot, path);

                    oldSnapshot = snapshot;
                } else {
                    oldSnapshot = SchematicReader.read(snapshotInputStream);
                }
            }

            try {
                assertSchematic(oldSnapshot, snapshot);
            } catch (AssertionFailedError e) {
//                System.err.println("file://" + path.toRealPath());
                throw e;
//                System.out.println("CAUGHT FAILURE");
//                System.out.println("Snapshot for " + testId + " is out of date, writing new snapshot to disk.");
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to create snapshot", e);
        }
    }

    public static void assertSchematic(@NotNull Schematic expected, @NotNull Schematic actual) {
        assertEquals(expected.offset(), actual.offset());
        assertEquals(expected.size(), actual.size());

        // If they are totally equal this will pass and be fast, otherwise we need to inspect every block
        var equal = Arrays.equals(expected.palette(), actual.palette()) && Arrays.equals(expected.blocks(), actual.blocks());
        if (equal) return;

        var expectedBlocks = new HashMap<Point, Block>();
        expected.apply(Rotation.NONE, expectedBlocks::put);
        actual.apply(Rotation.NONE, (pos, actualBlock) -> {
            var expectedBlock = expectedBlocks.get(pos);
            if (expectedBlock.equals(actualBlock)) return;

            System.err.println("Expected block " + expectedBlock + " at " + pos + " but got " + actualBlock);
        });

        fail("Schematics are not equal");
    }
}

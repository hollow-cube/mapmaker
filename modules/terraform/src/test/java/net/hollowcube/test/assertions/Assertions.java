package net.hollowcube.test.assertions;

import net.hollowcube.schem.Schematic;
import net.hollowcube.test.snapshot.InstanceSnapshot;
import net.hollowcube.test.subject.TestInstance;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public final class Assertions {
    private Assertions() {
    }

    public static void assertSnapshot(@NotNull Instance instance) {
        var ti = assertInstanceOf(TestInstance.class, instance, "Instances in tests must be created through the test environment.");
        ti.owner().assertSnapshot(InstanceSnapshot.class, ti);
    }

    public static void assertSchematic(@NotNull Schematic expected, @NotNull Schematic actual) {
        assertEquals(expected.offset(), actual.offset());
        assertEquals(expected.size(), actual.size());

        // If they are totally equal this will pass and be fast, otherwise we need to inspect every block
//        var equal = Arrays.equals(expected.palette(), actual.palette()) && Arrays.equals(expected.blocks(), actual.blocks());
//        if (equal) return;
        //todo reimplement this

        var expectedBlocks = new HashMap<Point, Block>();
        expected.forEachBlock(expectedBlocks::put);
        actual.forEachBlock((pos, actualBlock) -> {
            var expectedBlock = expectedBlocks.get(pos);
            if (expectedBlock.equals(actualBlock)) return;

            System.err.println("Expected block " + expectedBlock + " at " + pos + " but got " + actualBlock);
        });

        fail("Schematics are not equal");
    }
}

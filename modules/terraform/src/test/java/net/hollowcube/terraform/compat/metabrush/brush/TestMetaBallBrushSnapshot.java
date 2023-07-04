package net.hollowcube.terraform.compat.metabrush.brush;

import net.hollowcube.test.ServerTest;
import net.hollowcube.test.TestEnv;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static net.hollowcube.test.assertions.Assertions.assertSnapshot;

@ServerTest
class TestMetaBallBrushSnapshot {

    @Test
    void testInitial(TestEnv test) {

        var instance = test.createEmptyInstance();



        var brush = new MetaBallBrush();
        brush.loadProperties();
        brush.handleArrowAction(
                instance, new Vec(0, 0, 0),
                Block.STONE, 5
        );

        assertSnapshot(instance);

    }
}

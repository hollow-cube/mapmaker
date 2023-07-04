package net.hollowcube.terraform.snapshot;

import net.hollowcube.test.ServerTest;
import net.hollowcube.test.TestEnv;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static net.hollowcube.test.assertions.Assertions.assertSnapshot;

@ServerTest
class TestSetBlocksIntegration {

    @Test
    void abc(TestEnv test) {
        System.out.println(test);

        var instance = test.createEmptyInstance();

        instance.setBlock(0, 0, 0, Block.STONE);
        instance.setBlock(1, 0, 0, Block.STONE);
        instance.setBlock(2, 0, 0, Block.STONE);

        assertSnapshot(instance);


//        var player = ...;
//
//        player.teleport(1, 2, 3);
//        player.executeCommand("pos1");
//
//        player.teleport(4, 5, 6);
//        player.executeCommand("pos2");
//
//        player.executeCommand("set stone");
//
//        assertSnapshot(instance)
        // Want to ensure that setting a cube to some block works as expected

    }
}

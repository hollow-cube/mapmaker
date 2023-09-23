package net.hollowcube.terraform.snapshot;

import net.hollowcube.terraform.Terraform;
import net.hollowcube.test.ServerTest;
import net.hollowcube.test.TestEnv;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.Test;

import static net.hollowcube.test.assertions.Assertions.assertSnapshot;

@ServerTest
class TestSetBlocksIntegration {

    @Test
    void abc(TestEnv test) {
        Terraform.init(test.process().command(), null, null);

        var instance = test.createEmptyInstance();
        var player = test.createPlayer(instance, new Pos(0, 0, 0));


        player.executeCommand("pos1 0 0 0");
        player.executeCommand("pos2 3 3 3");
        player.executeCommand("set stone");

        assertSnapshot(instance);
    }
}

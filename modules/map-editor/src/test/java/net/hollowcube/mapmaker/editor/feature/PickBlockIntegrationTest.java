package net.hollowcube.mapmaker.editor.feature;

import net.hollowcube.mapmaker.editor.AbstractEditorMapWorldIntegrationTest;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.play.ClientPickItemFromBlockPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PickBlockIntegrationTest extends AbstractEditorMapWorldIntegrationTest {

    @Test
    void testPickStoneEmptyInventory() {
        player.addPacketToQueue(new ClientPickItemFromBlockPacket(
                new Vec(1, 39, 1), false
        ));
        player.interpretPacketQueue();

        assertEquals(Material.STONE, player.getItemInMainHand().material());
    }

    // TODO(new worlds): more tests.

}

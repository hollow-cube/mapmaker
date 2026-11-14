package net.hollowcube.terraform.legacy.session;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.util.Transform;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardSerializationTest {

    static {
        MinecraftServer.init();
    }

    @Test
    void keepsTheBufferOriginTransformAndMetadata() {
        var buffer = EditBuffer.builder()
                .setBlockState(1, 64, 1, Block.STONE)
                .setBlockState(3, 66, 2, Block.CHEST.withNbt(CompoundBinaryTag.builder().putString("CustomName", "x").build()))
                .addEntityEdit(new EntityEdit(UUID.randomUUID(), new Pos(2.5, 64, 2.5, 90, 0),
                        new TypedData("minecraft:zombie", CompoundBinaryTag.empty())))
                .build();
        var metadata = CompoundBinaryTag.builder().putString("Writer", "test").build();
        var data = new net.hollowcube.terraform.clipboard.Clipboard(
                new Schematic(buffer, new BlockVec(1, 64, 1), metadata), Transform.rotateY(90));

        var read = roundTrip(Clipboard.NETWORK_TYPE, data);

        assertEquals(Block.STONE.stateId(), read.buffer().getBlockState(1, 64, 1));
        assertEquals(buffer.getBlock(3, 66, 2), read.buffer().getBlock(3, 66, 2));
        assertEquals(EditBuffer.NONE, read.buffer().getBlockState(2, 64, 1), "unset cells stay unset, not air");
        assertEquals(buffer.entities(), read.buffer().entities());
        assertEquals(data.origin(), read.origin());
        assertEquals(data.transform(), read.transform(), "the transform stays pending, not baked");
        assertEquals(metadata, read.schematic().metadata());
    }

    @Test
    void anEmptySlotRoundTrips() {
        var slot = new Clipboard("empty");

        var bytes = NetworkBuffer.makeArray(out -> out.write(Clipboard.NETWORK_TYPE, slot));
        var read = NetworkBuffer.wrap(bytes, 0, bytes.length).read(Clipboard.NETWORK_TYPE);

        assertEquals("empty", read.name());
        assertNull(read.data());
    }

    @Test
    void versionOneStillReads() {
        var buffer = EditBuffer.builder().setBlockState(0, 0, 0, Block.STONE).build();
        var data = new net.hollowcube.terraform.clipboard.Clipboard(buffer, BlockVec.ZERO);

        var read = roundTrip(Clipboard.NETWORK_TYPE_V1, data);

        assertEquals(Block.STONE.stateId(), read.buffer().getBlockState(0, 0, 0));
    }

    private static net.hollowcube.terraform.clipboard.Clipboard roundTrip(
            NetworkBuffer.Type<Clipboard> type, net.hollowcube.terraform.clipboard.Clipboard data
    ) {
        var slot = new Clipboard("default");
        slot.setData(data);
        var bytes = NetworkBuffer.makeArray(out -> out.write(type, slot));
        var read = NetworkBuffer.wrap(bytes, 0, bytes.length).read(type).data();
        assertNotNull(read);
        return read;
    }
}

package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.ChunkBiomesPacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import net.minestom.server.property.ServerProperties;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.minestom.testing.TestConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnvTest
class ApplyPacketTest {

    private static TestConnection viewer(Env env, Instance instance) {
        instance.loadChunk(0, 0).join();
        TestConnection connection = env.createConnection();
        connection.connect(instance, new Pos(8, 64, 8)); // views chunk (0,0)
        env.tick(); // settle initial chunk sends
        return connection;
    }

    @Test
    void smallEditSendsMultiBlockChange(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = viewer(env, instance);

        Collector<MultiBlockChangePacket> mbc = connection.trackIncoming(
            MultiBlockChangePacket.class
        );
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder()
                .setBlockState(1, 64, 1, Block.STONE)
                .setBlockState(2, 64, 2, Block.DIRT)
                .build()
        );

        mbc.assertSingle(); // both cells share section 4 → one packet
    }

    @Test
    void manyPerCellChangesSendFullChunk(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = viewer(env, instance);

        Collector<ChunkDataPacket> chunkData = connection.trackIncoming(ChunkDataPacket.class);
        Collector<MultiBlockChangePacket> mbc = connection.trackIncoming(
            MultiBlockChangePacket.class
        );

        // A checkerboard over three sections: 6144 per-cell changes, none of them a full-cover section.
        EditBuffer.Builder edit = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 112; y++)
                for (int z = 0; z < 16; z++)
                    if (((x + y + z) & 1) == 0) edit.setBlockState(x, y, z, Block.STONE);
        ApplyEngine.DEFAULT.apply(instance, edit.build());

        chunkData.assertSingle();
        mbc.assertEmpty();
    }

    @Test
    void fullSectionFillSendsFullChunk(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = viewer(env, instance);

        Collector<ChunkDataPacket> chunkData = connection.trackIncoming(ChunkDataPacket.class);
        Collector<MultiBlockChangePacket> mbc = connection.trackIncoming(
            MultiBlockChangePacket.class
        );

        EditBuffer.Builder fill = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++) fill.setBlockState(x, y, z, Block.STONE);
        ApplyEngine.DEFAULT.apply(instance, fill.build());

        chunkData.assertAny(); // whole-section change → full chunk resend
        mbc.assertEmpty(); // ...not a MultiBlockChange
    }

    @Test
    void blockEntitySendsBlockEntityData(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = viewer(env, instance);

        Collector<BlockEntityDataPacket> blockEntity = connection.trackIncoming(
            BlockEntityDataPacket.class
        );
        CompoundBinaryTag nbt = CompoundBinaryTag.builder().putString("Text", "hi").build();
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 64, 1, Block.OAK_SIGN.withNbt(nbt)).build()
        );

        blockEntity.assertSingle();
    }

    @Test
    void editReachesViewerAtEdgeOfViewDistance(Env env) {
        // Player stands in chunk (0,0). The edited chunk sits at the far edge of the chunk view distance.
        int edge = ServerProperties.CHUNK_VIEW_DISTANCE.get(); // e.g. 8 → chunk (8,0)
        Instance instance = env.createEmptyInstance();
        for (int cx = 0; cx <= edge; cx++) instance.loadChunk(cx, 0).join();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, new Pos(8, 64, 8)); // chunk (0,0)
        env.tick();

        // The edge chunk is genuinely a viewer of this player (this is the crux of the reported bug).
        assertTrue(
            instance.getChunk(edge, 0).getViewers().contains(player),
            "player views the edge-of-view-distance chunk"
        );

        Collector<MultiBlockChangePacket> mbc = connection.trackIncoming(
            MultiBlockChangePacket.class
        );
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(edge * 16 + 1, 64, 1, Block.STONE).build()
        );
        mbc.assertSingle(); // the edit in the edge chunk is sent to the edge viewer
    }

    @Test
    void editWithoutViewersStillApplies(Env env) {
        Instance instance = env.createEmptyInstance();
        instance.loadChunk(0, 0).join(); // no player viewing
        ApplyEngine.ApplyResult result = ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().setBlockState(1, 64, 1, Block.STONE).build()
        );
        assertEquals(Block.STONE, instance.getBlock(1, 64, 1));
        assertEquals(1, result.changed());
    }

    @Test
    void biomeOnlyEditSendsChunkBiomesPacket(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = viewer(env, instance);

        Collector<ChunkBiomesPacket> biomes = connection.trackIncoming(ChunkBiomesPacket.class);
        Collector<ChunkDataPacket> chunkData = connection.trackIncoming(ChunkDataPacket.class);
        Collector<MultiBlockChangePacket> mbc = connection.trackIncoming(
            MultiBlockChangePacket.class
        );

        EditBuffer.Builder buffer = EditBuffer.builder();
        buffer.setBiome(0, 64, 0, 1); // one 4³ biome cell (empty instance defaults to id 0)
        ApplyEngine.DEFAULT.apply(instance, buffer.build());

        biomes.assertSingle(); // biome updates ride the dedicated packet...
        chunkData.assertEmpty(); // ...not a full-chunk resend
        mbc.assertEmpty();
    }

    @Test
    void fullChunkResendCarriesBiomesWithoutSeparatePacket(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = viewer(env, instance);

        Collector<ChunkBiomesPacket> biomes = connection.trackIncoming(ChunkBiomesPacket.class);
        Collector<ChunkDataPacket> chunkData = connection.trackIncoming(ChunkDataPacket.class);

        EditBuffer.Builder buffer = EditBuffer.builder();
        for (int x = 0; x < 16; x++)
            for (int y = 64; y < 80; y++)
                for (int z = 0; z < 16; z++)
                    buffer.setBlockState(
                        x,
                        y,
                        z,
                        Block.STONE
                    ); // whole-section fill → full-chunk resend
        buffer.setBiome(0, 64, 0, 1);
        ApplyEngine.DEFAULT.apply(instance, buffer.build());

        chunkData.assertAny();
        biomes.assertEmpty(); // the full chunk already carries biomes, no double-send
    }

    @Test
    void unchangedUniformSectionDoesNotForceFullChunk(Env env) {
        Instance instance = env.createEmptyInstance();
        TestConnection connection = viewer(env, instance);
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder().fillSection(0, 4, 0, Block.STONE.stateId()).build()
        );
        env.tick();

        Collector<ChunkDataPacket> chunkData = connection.trackIncoming(ChunkDataPacket.class);
        Collector<MultiBlockChangePacket> mbc = connection.trackIncoming(
            MultiBlockChangePacket.class
        );
        ApplyEngine.DEFAULT.apply(
            instance,
            EditBuffer.builder()
                .fillSection(0, 4, 0, Block.STONE.stateId()) // already stone: no change
                .setBlockState(1, 100, 1, Block.DIRT) // a real change in another section
                .build()
        );

        chunkData.assertEmpty();
        mbc.assertSingle();
    }
}

package net.hollowcube.test.viewer;

import com.mattworzala.debug.DebugMessage;
import com.mattworzala.debug.Layer;
import com.mattworzala.debug.shape.Shape;
import net.hollowcube.terraform.schem.Rotation;
import net.hollowcube.terraform.schem.SchematicReader;
import net.hollowcube.test.TestEnvImpl;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.chunk.ChunkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class SnapshotViewerServer {
    public static void main(String[] args) {
        var server = MinecraftServer.init();

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);

        var loadingChunks = new ArrayList<CompletableFuture<Void>>();
        ChunkUtils.forChunksInRange(0, 0, 5, (x, z) ->
                loadingChunks.add(instance.loadChunk(x, z).thenApply(v -> null)));
        CompletableFuture.allOf(loadingChunks.toArray(CompletableFuture[]::new)).join();

        var testId = "net_hollowcube_terraform_compat_metabrush_brush_TestMetaBallBrushSnapshot-testInitial-1496723150";
        var expectedSchem = SchematicReader.read(TestEnvImpl.RESOURCES_PATH.resolve("snapshots/" + testId));
        expectedSchem.apply(Rotation.NONE, instance::setBlock);

        var actualSchem = SchematicReader.read(TestEnvImpl.TEMP_PATH.resolve(testId));
        actualSchem.apply(Rotation.NONE, (pos, block) -> instance.setBlock(pos.add(0, 0, expectedSchem.size().z() + 3), block));

        for (int x = -1; x <= actualSchem.size().x(); x++) {
            for (int y = -1; y <= actualSchem.size().y(); y++) {
                for (int z = -1; z <= actualSchem.size().z(); z++) {
                    if ((x == -1 || x == actualSchem.size().x()) && (y == -1 || y == actualSchem.size().y())
                            || (x == -1 || x == actualSchem.size().x()) && (z == -1 || z == actualSchem.size().z())
                            || (y == -1 || y == actualSchem.size().y()) && (z == -1 || z == actualSchem.size().z())) {
                        instance.setBlock(new Pos(x, y, z).add(expectedSchem.offset()), Block.RED_WOOL);
                        instance.setBlock(new Pos(x, y, z + expectedSchem.size().z() + 3).add(expectedSchem.offset()), Block.RED_WOOL);
                    }
                }
            }
        }

        var actualBlocks = new HashMap<Point, Block>();
        actualSchem.apply(Rotation.NONE, actualBlocks::put);

        var builder = DebugMessage.builder().clear("viewer");
        expectedSchem.apply(Rotation.NONE, (pos, block) -> {
            var actualBlock = actualBlocks.get(pos);
            if (block.equals(actualBlock)) return;

            var relPos = pos.add(0, 0, expectedSchem.size().z() + 3);
            var nsid = String.format("viewer:%d-%d-%d", pos.blockX(), pos.blockY(), pos.blockZ());
            builder.set(nsid, Shape.box()
                            .start(relPos.sub(0.01))
                            .end(relPos.add(1.01))
                            .edgeColor(0xFFFF0000)
                            .faceColor(0x66FF0000)
                            .edgeLayer(Layer.TOP)
                            .build());
        });
        var debugPacket = builder.build();

        MinecraftServer.getGlobalEventHandler()
                .addListener(PlayerLoginEvent.class, event -> {
                    event.setSpawningInstance(instance);
                    event.getPlayer().setRespawnPoint(new Pos(-30, 0, 18, -90, 0));
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    var player = event.getPlayer();
                    player.setGameMode(GameMode.CREATIVE);
                    player.setFlying(true);

//                    debugPacket.sendTo(player);
                });


        server.start("localhost", 25565);
    }
}

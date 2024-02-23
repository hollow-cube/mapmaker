package net.hollowcube.test.snapshot.viewer;

import com.mattworzala.debug.DebugMessage;
import com.mattworzala.debug.Layer;
import com.mattworzala.debug.shape.Shape;
import io.helidon.media.common.ContentReaders;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.WebServer;
import net.hollowcube.terraform.schem.Rotation;
import net.hollowcube.terraform.schem.SchematicReader;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static net.minestom.server.network.NetworkBuffer.BYTE_ARRAY;
import static net.minestom.server.network.NetworkBuffer.STRING;

@SuppressWarnings("UnstableApiUsage")
public class SnapshotViewerServer {

    public static void main(String[] args) {

        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        new SnapshotViewerServer().start();
    }

    private static final Logger logger = LoggerFactory.getLogger(SnapshotViewerServer.class);

    private static final Tag<Boolean> SHOW_DEBUG = Tag.Boolean("show_debug");

    private final MinecraftServer server;
    private final Instance instance;

    private final WebServer webServer;

    private DebugMessage debugPacket = null;

    public SnapshotViewerServer() {
        server = MinecraftServer.init();

        instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);

        var loadingChunks = new ArrayList<CompletableFuture<Void>>();
        ChunkUtils.forChunksInRange(0, 0, 5, (x, z) ->
                loadingChunks.add(instance.loadChunk(x, z).thenApply(v -> null)));
        CompletableFuture.allOf(loadingChunks.toArray(CompletableFuture[]::new)).join();

        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, this::handlePlayerLogin);
        globalEventHandler.addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn);

        var routing = Routing.builder()
                .get("/", this::handleHelloRequest)
                .post("/report", this::handleReportRequest)
                .build();
        webServer = WebServer.builder()
                .host("localhost").port(12415)
                .addRouting(routing)
                .build();
    }

    public void start() {
        server.start("localhost", 25565);

        webServer.start().thenAccept(ws -> logger.info("Web server is running at {}:{}", "localhost", ws.port()));
        MinecraftServer.getSchedulerManager().buildShutdownTask(webServer::shutdown);
    }

    private void handlePlayerLogin(@NotNull AsyncPlayerConfigurationEvent event) {
        event.setSpawningInstance(instance);
        event.getPlayer().setRespawnPoint(new Pos(-30, 0, 18, -90, 0));
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnEvent event) {
        var player = event.getPlayer();
        player.setGameMode(GameMode.CREATIVE);
        player.setFlying(true);

        player.setTag(SHOW_DEBUG, true);
        if (debugPacket != null) {
            debugPacket.sendTo(player);
        }
    }

    private void handleReportRequest(@NotNull ServerRequest req, @NotNull ServerResponse res) {

        ContentReaders.readBytes(req.content()).thenAccept(content -> {
            try {
                // Respond immediately, we don't want to hold up the test run and never report info back.
//                var content = ;
                res.status(200).send();

                ChunkUtils.forChunksInRange(0, 0, 5,
                        (x, z) -> instance.getChunk(x, z).reset());

                record Failure(String id, byte[] expected, byte[] actual) {
                }

                var data = new NetworkBuffer(ByteBuffer.wrap(content));
                var failures = data.readCollection(buffer -> {
                    var id = buffer.read(STRING);
                    var expected = buffer.read(BYTE_ARRAY);
                    var actual = buffer.read(BYTE_ARRAY);
                    return new Failure(id, expected, actual);
                }, Integer.MAX_VALUE);
                if (failures.isEmpty()) return;

                var testId = failures.get(0).id();
                var expectedSchem = SchematicReader.read(new ByteArrayInputStream(failures.get(0).expected()));
                var actualSchem = SchematicReader.read(new ByteArrayInputStream(failures.get(0).actual()));

                expectedSchem.apply(Rotation.NONE, instance::setBlock);

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
                updateDebugPacket(builder.build());

                Audiences.players().sendMessage(Component.text("New failure"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void handleHelloRequest(@NotNull ServerRequest req, @NotNull ServerResponse res) {
        res.status(200).send();
    }

    private void updateDebugPacket(@NotNull DebugMessage message) {
        this.debugPacket = message;
        for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.getTag(SHOW_DEBUG)) continue;

            message.sendTo(player);
        }
    }

}

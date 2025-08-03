package net.hollowcube.puppeteer;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import net.fabricmc.fabric.mixin.client.gametest.ClientWorldAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minestom.server.MinecraftServer;

import java.util.Objects;

public abstract class PuppeteerServerTest implements FabricClientGameTest {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 25566;

    public abstract void runTest(Object server, ClientGameTestContext client);

    public abstract void createServer();

    @Override
    public final void runTest(ClientGameTestContext context) {
        var server = MinecraftServer.init();
        createServer();
        server.start(SERVER_HOST, SERVER_PORT);

        try {
            var serverAddr = String.format("%s:%s", SERVER_HOST, SERVER_PORT);
            var serverData = new ServerData("the server", serverAddr, ServerData.Type.OTHER);

            ServerAddress serverAddress = ServerAddress.parseString(serverAddr);
            context.runOnClient(client -> ConnectScreen.startConnecting(
                    new TitleScreen(), client, serverAddress,
                    serverData, true, null));
            ThreadingImpl.checkOnGametestThread("waitForChunksRender");
            context.waitTicks(40);
//            context.waitFor(client -> areChunksLoaded(client) && areChunksRendered(client), 1000102105);

            runTest(null, context);
        } finally {
            context.runOnClient(client -> client.disconnect(new TitleScreen(), false));

            MinecraftServer.stopCleanly();
        }
    }

    private static boolean areChunksLoaded(Minecraft client) {
        int viewDistance = 3; //client.options.getEffectiveRenderDistance() - 1; // -1 is because of a minestom bug
        if (client.level == null) return false;
        ClientLevel world = Objects.requireNonNull(client.level);

        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                // possible stink detected it assumes we are at zero,zero
                if (world.getChunk(dx, dz, ChunkStatus.FULL, false) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean areChunksRendered(Minecraft client) {
        ClientLevel world = Objects.requireNonNull(client.level);
        return ((ClientWorldAccessor) world).getChunkUpdaters().isEmpty()
                && client.levelRenderer.hasRenderedAllSections();
    }

}

package net.hollowcube.puppeteer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import net.fabricmc.fabric.mixin.client.gametest.ClientWorldAccessor;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.GlobalConfig;
import net.hollowcube.mapmaker.hub.HubServerRunner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class SampleClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        var server = MinecraftServer.init();

        var config = ConfigLoaderV3.loadDefault(new String[0]);
        var hubServer = new HubServerRunner(new ConfigLoaderV3() {
            @Override
            public <C> @NotNull C get(@NotNull Class<C> clazz) {
                if (clazz.equals(GlobalConfig.class)) {
                    return (C) new GlobalConfig(true);
                }
                return config.get(clazz);
            }

            @Override
            public void dump() {
                config.dump();
            }
        });

        hubServer.start();
        server.start("localhost", 25565);

        try {
            var serverAddr = "localhost:25565";
            var serverData = new ServerData("peak", serverAddr, ServerData.Type.OTHER);

            ServerAddress serverAddress = ServerAddress.parseString(serverAddr);
            context.runOnClient(client -> ConnectScreen.startConnecting(
                    null,
                    client, serverAddress,
                    serverData, true, null));

            ThreadingImpl.checkOnGametestThread("waitForChunksRender");
            context.waitFor(client -> areChunksLoaded(client) && areChunksRendered(client), 1000102105);
            context.runOnClient(client -> client.gui.getDebugOverlay().toggleOverlay());
            context.takeScreenshot("server");

            context.getInput().pressKey(InputConstants.KEY_1);
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitTicks(100);
            context.takeScreenshot("gui");


        } finally {
            context.runOnClient(client -> client.disconnect(new TitleScreen(), false));

            hubServer.shutdowner().performShutdown();
            MinecraftServer.stopCleanly();
        }

    }


    private static boolean areChunksLoaded(Minecraft client) {
        int viewDistance = client.options.getEffectiveRenderDistance() - 1; // -1 is because of a minestom bug
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

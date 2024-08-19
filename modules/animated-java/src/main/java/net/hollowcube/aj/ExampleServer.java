package net.hollowcube.aj;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;

import java.nio.file.Files;
import java.nio.file.Path;

public class ExampleServer {
    public static void main(String[] args) {
        var server = MinecraftServer.init();

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setChunkSupplier(LightingChunk::new);

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    event.setSpawningInstance(instance);
                    event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    var player = event.getPlayer();
                    player.setGameMode(GameMode.CREATIVE);
                })
                .addListener(PlayerStartSneakingEvent.class, event -> {
                    try {
                        var obj = new Gson().fromJson(Files.readString(Path.of("/Users/matt/Downloads/out.json")), JsonObject.class);
                        var model = ModelLoader.loadModel(obj);
                        model.setInstance(instance, event.getPlayer().getPosition().withPitch(0).withYaw(0)).thenRun(() -> {
                            System.out.println(model);
                            System.out.println(model.getPosition());

                        });

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        server.start("0.0.0.0", 25565);
    }
}
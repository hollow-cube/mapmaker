package net.hollowcube.aj;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.hollowcube.aj.entity.ModelEntity;
import net.hollowcube.multipart.bedrock.BedrockGeoModel;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryTranscoder;

import java.nio.file.Files;
import java.nio.file.Path;

public class DemoServer {
    public static void main(String[] args) throws Exception {
        var server = MinecraftServer.init();

        var _ = ModelEntity.class;
        var _ = Node.CODEC;

        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/models/entity/assembler.geo.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/buggy_tier_1.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/assembler.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/gem_golem.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/armor_stand.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/bass_ribbit.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/qbdg/ajmodel/checkpoint.json");
        var json = new Gson().fromJson(Files.readString(path), JsonObject.class).getAsJsonArray("minecraft:geometry").get(0);
        var model = ResourcePackGen.fixRotation(BedrockGeoModel.CODEC.decode(new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process()), json).orElseThrow());

        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(model));

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setTimeRate(0);

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    event.setSpawningInstance(instance);
                    event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    event.getPlayer().setGameMode(GameMode.CREATIVE);

                    var entity = new ModelEntity(model, null);
                    entity.setInstance(instance, new Pos(0, 40, 0, -180, 0));
                })
                .addListener(PlayerChatEvent.class, event -> {
                    var entity = new ModelEntity(model, null);
                    entity.setInstance(instance, event.getPlayer().getPosition().withView(0, 0));
                });
        server.start("0.0.0.0", 25565);
    }
}

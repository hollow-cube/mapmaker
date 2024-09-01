package net.hollowcube.aj;

import com.mojang.serialization.JsonOps;
import net.hollowcube.aj.model.ExportedModel;
import net.hollowcube.aj.resourcepack.ResourcePackExporter;
import net.hollowcube.aj.util.MqlCompilerOps;
import net.hollowcube.mql.MqlCompiler;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;

import java.nio.file.Path;

public class ExampleServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, world!");

//        var modelPath = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/modules/animated-java/house.json");
//        var modelPath = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/modules/animated-java/mymodel_out.json");
        var modelPath = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/modules/animated-java/armorstand-unbaked.json");
        var compiler2 = MqlCompiler.create();
        compiler2.addInitializer("v.walk_speed = 360 * 1;\n" +
                "v.run_speed = 360 * 1.5;\n" +
                "v.stickbug_speed = 360 * 1.25;", null);
        ExportedModel model2 = ExportedModel.fromFile(new MqlCompilerOps<>(JsonOps.INSTANCE, compiler2), modelPath);

        var packRoot = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/modules/animated-java/test-rp");
        ResourcePackExporter.export(packRoot, model2);


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
                        var compiler = MqlCompiler.create();
                        compiler.addInitializer("v.walk_speed = 360 * 1;\n" +
                                "v.run_speed = 360 * 1.5;\n" +
                                "v.stickbug_speed = 360 * 1.25;", null);
                        ExportedModel model = ExportedModel.fromFile(new MqlCompilerOps<>(JsonOps.INSTANCE, compiler), modelPath);
                        var module = compiler.compile();
                        var spawned = new SpawnedModelImpl(model, module.newInstance(), instance);
                        spawned.setPosition(event.getPlayer().getPosition().withPitch(0).withYaw(0));
                        event.getPlayer().scheduleNextTick(_ -> spawned.addViewer(event.getPlayer()));

//                        var obj = new Gson().fromJson(Files.readString(Path.of("/Users/matt/Downloads/out.json")), JsonObject.class);
//                        var model = ModelLoader.loadModel(obj);
//                        model.setInstance(instance, event.getPlayer().getPosition().withPitch(0).withYaw(0)).thenRun(() -> {
//                            System.out.println(model);
//                            System.out.println(model.getPosition());
//
//                        });

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        server.start("0.0.0.0", 25565);
    }
}
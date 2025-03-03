package net.hollowcube.terraform.demo;

import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.schem.reader.SpongeSchematicReader;
import net.hollowcube.terraform.Terraform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;

import java.nio.file.Files;
import java.nio.file.Path;

public class DemoServer {


    public static void main(String[] args) throws Exception {
        System.setProperty("terraform.debug.markers", "true");


        new SpongeSchematicReader().read(Files.readAllBytes(Path.of("/Users/matt/Downloads/[ygamien]_Cubyg_I.schem")));


        var server = MinecraftServer.init();

        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
        MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProviderV2.translate(component);

        var dimension = MinecraftServer.getDimensionTypeRegistry().register(
                "mapmaker:bright_dim",
                DimensionType.builder().ambientLight(2.0f).build()
        );

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        var commandManager = new CommandManagerImpl();
        commandManager.register(new HelpCommand(commandManager, CommandCategories.GLOBAL));
        MinecraftServer.getConnectionManager().setPlayerProvider(CommandHandlingPlayer.createDefaultProvider(commandManager));

        var terraform = Terraform.builder()
                .rootCommandManager(commandManager)
                .module(Terraform.BASE_MODULE)
                .module(Terraform.WORLDEDIT_MODULE)
                .build();

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerPreLoginEvent.class, event -> {
                    // Use username as session ID here because we are in offline mode
//                    terraform.initPlayerSession(event.getPlayer(), event.getUsername());
                })
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    event.setSpawningInstance(instance);
                    event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    var player = event.getPlayer();
                    player.setGameMode(GameMode.CREATIVE);

                    //todo this is not a safe call. it blocks the tick thread during instance change.
                    terraform.initLocalSession(player, player.getInstance().getUuid().toString());
                })
                .addListener(RemoveEntityFromInstanceEvent.class, event -> {
                    if (!(event.getEntity() instanceof Player player)) return;

                    //todo this is not a safe call. it blocks the tick thread during instance change.
                    terraform.saveLocalSession(player, true);
                })
                .addListener(PlayerDisconnectEvent.class, event -> {
                    //todo this is not a safe call. it blocks the tick thread during instance change.
                    terraform.savePlayerSession(event.getPlayer(), true);
                });

        server.start("0.0.0.0", 25565);
    }
}

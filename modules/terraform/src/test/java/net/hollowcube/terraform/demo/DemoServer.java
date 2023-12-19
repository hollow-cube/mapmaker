package net.hollowcube.terraform.demo;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.HelpCommand;
import net.hollowcube.common.lang.LanguageProviderV2;
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
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

public class DemoServer {

    public static final DimensionType FULL_BRIGHT = DimensionType.builder(NamespaceID.from("mapmaker:bright_dim"))
            .ultrawarm(false)
            .natural(true)
            .piglinSafe(false)
            .respawnAnchorSafe(false)
            .bedSafe(true)
            .raidCapable(true)
            .skylightEnabled(true)
            .ceilingEnabled(false)
            .fixedTime(null)
            .ambientLight(2.0f)
            .height(384)
            .minY(-64)
            .logicalHeight(384)
            .infiniburn(NamespaceID.from("minecraft:infiniburn_overworld"))
            .build();

    public static void main(String[] args) {
        System.setProperty("terraform.debug.markers", "true");

        var server = MinecraftServer.init();

        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
        MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProviderV2.translate(component);

        var dimensionManager = MinecraftServer.getDimensionTypeManager();
        dimensionManager.addDimension(FULL_BRIGHT);

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer(FULL_BRIGHT);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        var commandManager = new CommandManager();
        CommandRewriter.init(commandManager);
        commandManager.register(new HelpCommand(commandManager));

        var terraform = Terraform.builder()
                .module(Terraform.BASE_MODULE)
                .build();

        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerPreLoginEvent.class, event -> {
                    // Use username as session ID here because we are in offline mode
                    terraform.initPlayerSession(event.getPlayer(), event.getUsername());
                })
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    event.setSpawningInstance(instance);
                    event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
                })
                .addListener(PlayerSpawnEvent.class, event -> {
                    var player = event.getPlayer();
                    player.setGameMode(GameMode.CREATIVE);

                    //todo this is not a safe call. it blocks the tick thread during instance change.
                    terraform.initLocalSession(player, player.getInstance().getUniqueId().toString());
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

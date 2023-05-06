package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.common.lang.LanguageProvider;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DemoServer {
    public static final Map<String, Function<Context, View>> guis = new HashMap<>();

    static {
        guis.put("anvil", AnvilDemo::new);
        guis.put("counter", Counter::new);
        guis.put("loading", LoadingDemo::new);
        guis.put("pagination", PaginatedList::new);
        guis.put("playmaps", PlayMaps::new);
        guis.put("context", ContextObjectDemo::new);
        guis.put("switch", SwitchDemo::new);
        guis.put("import", ImportDemo::new);
        guis.put("signal", SignalDemo::new);
    }

    public static void main(String[] args) {

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        var server = MinecraftServer.init();
        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
        MinestomAdventure.COMPONENT_TRANSLATOR = (comp, locale) -> LanguageProvider.get2(comp);

        var instanceManager = MinecraftServer.getInstanceManager();
        var instance = instanceManager.createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        var eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });
        eventHandler.addListener(PlayerSpawnEvent.class, event -> {
            var player = event.getPlayer();
            player.setGameMode(GameMode.CREATIVE);

            for (int i = 0; i < PlayerInventory.INVENTORY_SIZE; i++) {
                player.getInventory().setItemStack(i, ItemStack.of(Material.STICK));
            }
        });

        var controller = Controller.make(Map.of(
                "myContext", "Hello, ContextObject!"
        ));

        var command = new Command("gui");
        command.addSyntax((sender, context) -> {
            var name = context.<String>get("name");
            var player = (Player) sender;

            controller.show(player, guis.get(name));
        }, ArgumentType.Word("name").from(guis.keySet().toArray(new String[0])));
        MinecraftServer.getCommandManager().register(command);

        server.start("localhost", 25565);
    }

}

package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.experiment.demo.Counter;
import net.hollowcube.canvas.experiment.demo.LoadingDemo;
import net.hollowcube.canvas.experiment.demo.PaginatedList;
import net.hollowcube.canvas.experiment.demo.PlayMaps;
import net.hollowcube.canvas.section.ItemSection;
import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.section.Section;
import net.hollowcube.canvas.section.std.ButtonSection;
import net.hollowcube.canvas.section.std.GroupSection;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DemoServer {
    public static final Map<String, Supplier<Section>> guis = new HashMap<>();

    static {
        guis.put("empty", () -> new ItemSection(9, 1){});
        guis.put("button", () -> {
            var gui = new GroupSection(9, 1);

            gui.add(0, 0, new ButtonSection(1, 1, ItemStack.of(Material.STICK), () -> {
                System.out.println("You clicked the button!");
            }));

            gui.add(6, 0, new ButtonSection(3, 1, ItemStack.of(Material.STICK), () -> {
                System.out.println("You clicked the other button!");
            }));

            return gui;
        });
        guis.put("counter", CounterDemo::new);
        guis.put("pagination", PaginationDemo::new);
        guis.put("history", () -> new RouterSection(new HistoryDemo()));
        guis.put("title", TitleDemo::new);
        guis.put("big", BigInventoryDemo::new);
        guis.put("search", SearchDemo::new);
        guis.put("anvil", AnvilInputDemo::new);
        guis.put("toggle", () -> new RouterSection(new ToggleButtonDemo()));

        guis.put("ncounter", () -> new RouterSection(new Counter()));
        guis.put("nloading", () -> new RouterSection(new LoadingDemo()));
        guis.put("npagination", () -> new RouterSection(new PaginatedList()));
        guis.put("nplaymaps", () -> new RouterSection(new PlayMaps()));
    }

    public static void main(String[] args) {
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

            player.sendMessage(Component.text("\uEff4\uF822", TextColor.color(78, 92, 36))
                    .append(Component.text("notmattw", TextColor.color(172, 75, 255)))
                    .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Seth is a weirdo", NamedTextColor.WHITE)));
            player.sendMessage(Component.text("\uEff3\uF822", TextColor.color(78, 92, 36))
                    .append(Component.text("Seth28", TextColor.color(255, 45, 45)))
                    .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("ILL BAN YOU", NamedTextColor.WHITE)));
        });

        var command = new Command("gui");
        command.addSyntax((sender, context) -> {
            var name = context.<String>get("name");
            var player = (Player) sender;

            guis.get(name).get().showToPlayer(player);
        }, ArgumentType.Word("name").from(guis.keySet().toArray(new String[0])));
        MinecraftServer.getCommandManager().register(command);

        server.start("localhost", 25565);
    }

}

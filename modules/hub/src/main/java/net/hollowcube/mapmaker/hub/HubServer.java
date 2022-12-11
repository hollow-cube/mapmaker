package net.hollowcube.mapmaker.hub;

import net.hollowcube.mapmaker.hub.command.MapCommand;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.mapmaker.hub.gui.inventory.InventoryUtils;
import net.hollowcube.mapmaker.hub.handler.MapHandlerImpl;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.hub.gui.item.ItemUtils;
import net.hollowcube.mapmaker.hub.gui.section.BuildMaps;
import net.hollowcube.mapmaker.util.DimensionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HubServer implements HubManager {
    public static final Tag<Boolean> HUB_MARKER = Tag.Boolean("mapmaker:hub");

    private final EventNode<Event> eventNode = EventNode.all("mapmaker:hub");
    private final Instance instance; // Hub instance

    public HubServer(@NotNull MapStorage mapStorage, @NotNull MapManager maps) {
        TemporaryIAmTerrible.INSTANCE = this;

        instance = new InstanceContainer(UUID.randomUUID(), DimensionUtil.FULL_BRIGHT);
        MinecraftServer.getInstanceManager().registerInstance(instance);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setTag(HUB_MARKER, true);

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);

        var commands = MinecraftServer.getCommandManager();
        commands.register(new MapCommand(new MapHandlerImpl(mapStorage, maps)));
        eventNode.addListener(PlayerUseItemEvent.class, this::handleUseItem);
    }

    public @NotNull Instance getInstance() {
        return instance;
    }

    public @NotNull Pos getSpawnPos() {
        return new Pos(0.5, 40, 0.5);
    }

    @Override
    public @NotNull CompletableFuture<Void> sendToHub(@NotNull Player player) {
        return player.setInstance(instance, getSpawnPos())
                .thenAccept(unused -> player.refreshCommands());
    }

    private void handleSpawn(@NotNull PlayerSpawnEvent event) {
        // Spawn event is not an InstanceEvent, so we need to filter it.
        if (!event.getSpawnInstance().hasTag(HUB_MARKER))
            return;

        var player = event.getPlayer();
        player.refreshCommands();

        InventoryUtils.setPlayerLobbyInventory(player);
        if (event.isFirstSpawn()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Component.translatable("test.welcome_message"));
        }
    }

    private void handleUseItem(@NotNull PlayerUseItemEvent event) {
        var instance = event.getInstance();
        if (!instance.hasTag(HUB_MARKER)) return;
        event.setCancelled(true);

        var item = event.getItemStack();
        var player = event.getPlayer();
        switch (item.meta().getCustomModelData()) {
            case ItemUtils.PLAY_ITEM_CMD -> System.out.println("open play item gui");
            case ItemUtils.CREATE_ITEM_CMD -> player.openInventory(new RouterSection(new BuildMaps()).getInventory());

        }

    }

}

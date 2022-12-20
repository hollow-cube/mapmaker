package net.hollowcube.mapmaker.hub.world;

import net.hollowcube.mapmaker.hub.gui.hotbar.HubHotbar;
import net.hollowcube.mapmaker.hub.world.generator.HubGenerators;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.WorldManager;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class HubWorld extends BaseWorld {
    private static final String WORLD_NAME = "hub";

    public static final Tag<Boolean> MARKER = Tag.Boolean("mapmaker:hub/marker");

    public HubWorld(@NotNull WorldManager worldManager) {
        super(worldManager, WORLD_NAME);

        instance().setTag(MARKER, true);
        instance().setGenerator(HubGenerators.stoneWorld());

        var eventNode = instance().eventNode();
        eventNode.addChild(HubHotbar.eventNode());

        //todo add some WorldConfig options passed on world create. Can add some useful/common ones
        // like setting a generator (default to void probably), preventing block placement/breaking, etc.
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak);
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace);

        eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn);
    }

    @Override
    public @NotNull CompletableFuture<Void> loadWorld() {
        //todo actually load the world
        return CompletableFuture.completedFuture(null);
    }

    private void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.refreshCommands();

        player.setGameMode(GameMode.ADVENTURE);

        player.getInventory().clear();
        HubHotbar.applyToPlayer(player);
    }
}

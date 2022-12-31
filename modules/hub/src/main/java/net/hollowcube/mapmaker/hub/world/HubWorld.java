package net.hollowcube.mapmaker.hub.world;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.gui.hotbar.HubHotbar;
import net.hollowcube.mapmaker.hub.world.generator.HubGenerators;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class HubWorld extends BaseWorld {
    private static final String WORLD_NAME = "hub";

    public static final Tag<Boolean> MARKER = Tag.Boolean("mapmaker:hub/marker"); //todo unnecessary
    private static final Tag<HubWorld> THIS = ExtraTags.Transient("mapmaker:hub/world");

    public static @NotNull HubWorld fromInstance(@NotNull Instance instance) {
        return instance.getTag(THIS);
    }

    private final HubServer server;

    public HubWorld(@NotNull HubServer server) {
        super(server.worldManager(), WORLD_NAME);
        this.server = server;

        instance().setTag(MARKER, true);
        instance().setTag(THIS, this);
        instance().setGenerator(HubGenerators.stoneWorld());

        var eventNode = instance().eventNode();
        eventNode.addChild(HubHotbar.eventNode());

        //todo add some WorldConfig options passed on world create. Can add some useful/common ones
        // like setting a generator (default to void probably), preventing block placement/breaking, etc.
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak);
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace);

        eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn);
    }

    public @NotNull HubServer server() {
        return server;
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

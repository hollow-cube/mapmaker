package net.hollowcube.mapmaker.hub.world;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.find_a_new_home.hotbar.HubHotbar;
import net.hollowcube.mapmaker.hub.world.generator.HubGenerators;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public class HubWorld {
    private static final String WORLD_NAME = "hub";

    public static final Tag<Boolean> MARKER = Tag.Boolean("mapmaker:hub/marker"); //todo unnecessary
    private static final Tag<HubWorld> THIS_TAG = ExtraTags.Transient("mapmaker:hub/world");

    public static @NotNull HubWorld fromInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(optionalFromInstance(instance));
    }

    public static @Nullable HubWorld optionalFromInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(THIS_TAG);
    }

    private final HubServer server;

    private final MapInstance instance;

    public HubWorld(@NotNull HubServer server) {
        this.server = server;

        instance = new MapInstance("mapmaker:hub");
        instance.setTag(MARKER, true);
        instance.setTag(THIS_TAG, this);
        instance.setGenerator(HubGenerators.stoneWorld());

        var eventNode = instance.eventNode();
        eventNode.addChild(HubHotbar.eventNode());

        //todo add some WorldConfig options passed on world create. Can add some useful/common ones
        // like setting a generator (default to void probably), preventing block placement/breaking, etc.
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak);
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace);

        eventNode.addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn);

        //todo load the world
    }

    public @NotNull HubServer server() {
        return server;
    }

    public @NotNull Instance instance() {
        return instance;
    }

    public void loadWorld() {
        try (var is = getClass().getResourceAsStream("/spawn/hcspawn.polar")) {
            if (is == null) throw new IOException("hcspawn.polar not found");
            instance.setChunkLoader(new PolarLoader(is));

        } catch (IOException e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
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
        player.teleport(new Pos(0.5, 40, 0.5, 90, 0));

        player.getInventory().clear();
        HubHotbar.applyToPlayer(player);
    }
}

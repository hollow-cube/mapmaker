package net.hollowcube.map.world;

import net.hollowcube.map.item.handler.ItemTags;
import net.hollowcube.map.worldold.polar.ReadWriteWorldAccess;
import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class EditingMapWorld extends AbstractMapWorld {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("editing-events", EventFilter.INSTANCE)
            .addListener(InstanceTickEvent.class, ev -> tick())
            .addListener(PlayerUseItemEvent.class, this::handleItemUse)
            .addListener(PlayerBlockBreakEvent.class, this::handleBlockBreak);

    private final ReentrantLock saveLock = new ReentrantLock();
    private Task autoSaveTask = null;

    public EditingMapWorld(@NotNull MapData map) {
        super(map, new MapInstance(map.createDimensionName('e')));
        instance.setGenerator(MapGenerators.voidWorld());

        instance.eventNode().addChild(eventNode); // Needs spectators, so register on instance.
    }

    @Override
    void load() {

    }

    @Override
    void close() {

    }

    private void tick() {
        var minHeight = instance.getDimensionType().getMinY() - 20;
        for (var player : players()) {
            if (player.getPosition().y() < minHeight) {
                player.teleport(spawnPoint());
            }
        }
    }

    @Blocking
    private void save(boolean isAutoSave) {
        saveLock.lock();
        try {
            // Save the map settings
            map().settings().withUpdateRequest(updates -> {
                try {
                    //todo map worlds should have an "owner" which is the owner if they are present, otherwise
                    // it is the first trusted player to join the world. If someone leaves it should find a new
                    // "owner" of the world, or if it cannot (only invited people left), it should close the world.
                    server().mapService().updateMap(map().owner(), map().id(), updates);
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to save map settings for {}", map().id(), e);
                    MinecraftServer.getExceptionManager().handleException(e);
                    return false;
                }
            });

            // Save the world data (if it is unverified only)
            if (map().verification() == MapVerification.UNVERIFIED) {
                var worldData = instance.save(new ReadWriteWorldAccess(this));
                server().mapService().updateMapWorld(map().id(), worldData);
            }

            // Save the players data
            activePlayers.removeIf(Predicate.not(Player::isOnline)); // Sanity
            for (var player : activePlayers) {
                var playerData = PlayerDataV2.fromPlayer(player);

                var saveState = SaveState.fromPlayer(player);
                var update = updateSaveState(player, saveState);
                server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), update);
                //todo handle errors

                // Save terraform state
                terraform.saveLocalSession(player, false);
                terraform.savePlayerSession(player, false);
                //todo handle errors here too
            }

            if (isAutoSave) {
                instance.sendMessage(Component.translatable("build.world.save.success"));
            }
        } catch (Exception e) {
            logger.error("Failed to save world {}", map().id(), e);
            instance.sendMessage(Component.translatable("build.world.save.failure"));
        } finally {
            saveLock.unlock();
        }
    }

    private void handleBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        //todo can this become an interaction rule?

        // Prevent swords from breaking blocks
        ItemStack item = event.getPlayer().getItemInMainHand();
        if (ItemTags.SWORDS.contains(item.material().namespace())) {
            event.setCancelled(true);
        }
    }

    private void handleItemUse(@NotNull PlayerUseItemEvent event) {
        //todo can this become an interaction rule?

        // Prevent player from eating suspicious stew
        ItemStack itemStack = event.getItemStack();
        if (itemStack.material() == Material.SUSPICIOUS_STEW) {
            event.setCancelled(true);
        }
    }
}

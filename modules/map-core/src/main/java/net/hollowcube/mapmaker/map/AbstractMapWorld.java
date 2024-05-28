package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.entity.potion.PotionHandler;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket;
import net.minestom.server.network.packet.server.configuration.UpdateEnabledFeaturesPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("UnstableApiUsage")
public non-sealed abstract class AbstractMapWorld implements MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMapWorld.class);
    static final Tag<MapWorld> SELF_TAG = Tag.Transient("mapworld");

    public static final Component CLOSED_MESSAGE = Component.translatable("map.closed");

    private final String worldId = UUID.randomUUID().toString();

    private final MapServer server;
    private final MapData map;
    protected final MapInstance instance;
    private final EventNode<InstanceEvent> eventNode;

    private final ItemRegistry itemRegistry = new ItemRegistry();
    private final BiomeContainer biomeContainer = new BiomeContainer();

    private final Set<Player> players = Collections.synchronizedSet(new HashSet<>());
    private final Set<Player> playersUnmodifiable = Collections.unmodifiableSet(players);
    private final Set<Player> spectators = Collections.synchronizedSet(new HashSet<>());
    private final Set<Player> spectatorsUnmodifiable = Collections.unmodifiableSet(spectators);

    protected AbstractMapWorld(@NotNull MapServer server, @NotNull MapData map, @NotNull MapInstance instance) {
        this.server = server;
        this.map = map;
        this.instance = instance;
        this.eventNode = EventNode.event("world-local", EventFilter.INSTANCE, this::testEvent);

        // Configure the events from the instance & managers
        instance.eventNode().addChild(eventNode);
        instance.eventNode().addChild(itemRegistry.eventNode());

        // Add support for adding and removing potion effects
        eventNode().addChild(PotionHandler.EVENT_NODE);

        instance().eventNode().addListener(PlayerInstanceLeaveEvent.class, this::handleInstanceLeave);

        // Set the instance self tag so that this world can be discovered via MapWorld#unsafeFromInstance
        // If there is already a tag do nothing, it means that this is a child world and the parent has already set the tag.
        if (!instance.hasTag(SELF_TAG)) instance.setTag(SELF_TAG, this);
    }

    @Override
    public @NotNull String worldId() {
        return worldId;
    }

    @Override
    public @NotNull MapServer server() {
        return server;
    }

    @Override
    public @NotNull MapData map() {
        return map;
    }

    @Override
    public @NotNull Instance instance() {
        return instance;
    }

    @Override
    public @NotNull ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    @Override
    public @NotNull BiomeContainer biomes() {
        return biomeContainer;
    }

    @Override
    public @NotNull Set<Player> players() {
        return playersUnmodifiable;
    }

    @Override
    public @NotNull Set<Player> spectators() {
        return spectatorsUnmodifiable;
    }

    @Override
    public void configurePlayer(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();

        try {
            List<SelectKnownPacksPacket.Entry> knownPacks;
            try {
                var knownPacksFuture = PlayerUtil.stealKnownPacksFuture(player);
                if (knownPacksFuture == null) {
                    // There is a race here which could result in the client responding too quickly. In that case we need to re request
                    // the known packs. todo: find a better way around this, its really cursed.
                    knownPacksFuture = player.getPlayerConnection().requestKnownPacks(List.of(SelectKnownPacksPacket.MINECRAFT_CORE));
                }
                knownPacks = knownPacksFuture.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException e) {
                throw new RuntimeException("Client failed to respond to known packs request", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Error receiving known packs", e);
            }
            boolean excludeVanilla = !knownPacks.contains(SelectKnownPacksPacket.MINECRAFT_CORE);

            // Send registry data ourself to allow custom biomes per map
            var serverProcess = MinecraftServer.process();
            player.sendPacket(serverProcess.chatType().registryDataPacket(excludeVanilla));
            player.sendPacket(serverProcess.dimensionType().registryDataPacket(excludeVanilla));
            player.sendPacket(biomes().registryDataPacket(excludeVanilla));
            player.sendPacket(serverProcess.damageType().registryDataPacket(excludeVanilla));
            player.sendPacket(serverProcess.trimMaterial().registryDataPacket(excludeVanilla));
            player.sendPacket(serverProcess.trimPattern().registryDataPacket(excludeVanilla));
            player.sendPacket(serverProcess.bannerPattern().registryDataPacket(excludeVanilla));
            player.sendPacket(serverProcess.wolfVariant().registryDataPacket(excludeVanilla));

            player.sendPacket(new TagsPacket(MinecraftServer.getTagManager().getTagMap()));
            event.setSendRegistryData(false);

            // Enable the 1.21 features
            player.sendPacket(new UpdateEnabledFeaturesPacket(Set.of(
                    NamespaceID.from("minecraft:vanilla"),
                    NamespaceID.from("minecraft:update_1_21")
            )));

            // Set the instance and spawn point of the player.
            event.setSpawningInstance(instance());
            player.setRespawnPoint(spawnPoint(player));

            // addPlayer is called during PlayerSpawnEvent meaning that the player is already in the instance,
            // and all of the entity `updateNewViewer` calls were already made. This makes it unsafe to call
            // MapWorld#forPlayer during viewer add which is unexpected and strange behavior.
            // To fix it, we disable auto entity viewing during config, and then reenable it after the player is added
            // to the world.
            // todo: this only happens during reconfiguration, not when using the spectator item or enter/exiting test mode
            //       Those two cases need to be fixed in a generic way that handles all other cases.
            player.setAutoViewEntities(false);
        } catch (Exception e) {
            logger.error("Failed to configure player", e);
            player.kick("An unexpected error occurred while configuring your player. Please try again.");
        }
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        this.players.add(player);
        MapWorldHelpers.resetPlayer(player);
    }

    @Override
    public void addSpectator(@NotNull Player player) {
        this.spectators.add(player);
        MapWorldHelpers.resetPlayer(player);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        this.players.remove(player);
        this.spectators.remove(player);
    }

    //todo not a fan of this, but idk a better solution
    @Deprecated
    public void removePlayerImmediate(@NotNull Player player) {
        this.players.remove(player);
        this.spectators.remove(player);
    }

    //todo not a fan of this, but idk a better solution
    @Deprecated
    public void addPlayerImmediate(@NotNull Player player) {
        this.players.add(player);
    }

    /**
     * Returns this map if the player is currently playing it, or a sub map if the player is in a sub map.
     *
     * @param player The player to get the map for
     * @return The map that the player is currently playing, or null if they are not in a map of this tree, or are spectating.
     */
    protected @Nullable MapWorld getMapForPlayer(@NotNull Player player) {
        return players().contains(player) || spectators().contains(player) ? this : null;
    }

    @Override
    public @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    protected boolean testEvent(@NotNull InstanceEvent event) {
        if (event instanceof PlayerEvent pe)
            return isPlaying(pe.getPlayer()) || isSpectating(pe.getPlayer());
        return true;
    }

    private void handleInstanceLeave(@NotNull PlayerInstanceLeaveEvent event) {
        if (event.getInstance() != instance()) return; // Sanity

        // Sanity: If they are still in this world, remove them from it.
        var player = event.getPlayer();
        if (isPlaying(player) || isSpectating(player)) {
            removePlayer(player);
        }
    }

    @Blocking
    public void load() {
        this.biomes().init(this);
    }

    @Blocking
    public void close(@Nullable Component reason) {
        logger.info("Closing world {}", this);
        removePlayerSet(this, players, reason);
        removePlayerSet(this, spectators, reason);
        players.clear();
        spectators.clear();
    }

    private static void removePlayerSet(@NotNull MapWorld world, @NotNull Collection<Player> players, @Nullable Component reason) {
        for (var player : Set.copyOf(players)) {
            try {
                if (reason != null) player.sendMessage(reason);
                world.removePlayer(player);
                world.server().bridge().joinHub(player);
            } catch (Exception e) {
                logger.error("failed to move player to hub ({})", player.getUuid(), e);
                MinecraftServer.getExceptionManager().handleException(e);
                player.kick(CLOSED_MESSAGE);
            }
        }
    }
}

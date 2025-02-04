package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.entity.marker.MarkerHandlerRegistry;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.terraform.instance.TerraformInstanceBiomes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.FeatureFlag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

@SuppressWarnings("UnstableApiUsage")
public non-sealed abstract class AbstractMapWorld implements MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMapWorld.class);

    protected static <T extends AbstractMapWorld> @NotNull Constructor<T> ctor(@NotNull BiFunction<MapServer, MapData, T> create, @NotNull Class<T> type) {
        return new Constructor<>() {
            @Override
            public @NotNull T create(@NotNull MapServer server, @NotNull MapData map) {
                return create.apply(server, map);
            }

            @Override
            public @NotNull Class<T> type() {
                return type;
            }
        };
    }

    static final Tag<MapWorld> SELF_TAG = Tag.Transient("mapworld");

    public static final Component CLOSED_MESSAGE = Component.translatable("map.closed");

    private final String worldId = UUID.randomUUID().toString();

    private final MapServer server;
    private final MapData map;
    protected final MapInstance instance;
    private final EventNode<InstanceEvent> eventNode;

    private final ItemRegistry itemRegistry = new ItemRegistry();
    private final BiomeContainer biomeContainer = new BiomeContainer();
    private final MarkerHandlerRegistry markerRegistry = new MarkerHandlerRegistry();

    private final Set<Player> players = new CopyOnWriteArraySet<>();
    private final Set<Player> playersUnmodifiable = Collections.unmodifiableSet(players);
    private final Set<Player> spectators = new CopyOnWriteArraySet<>();
    private final Set<Player> spectatorsUnmodifiable = Collections.unmodifiableSet(spectators);

    protected AbstractMapWorld(@NotNull MapServer server, @NotNull MapData map, @NotNull MapInstance instance) {
        this.server = server;
        this.map = map;
        this.instance = instance;
        this.eventNode = EventNode.event("world-local", EventFilter.INSTANCE, this::testEvent);

        // Configure the events from the instance & managers
        instance.eventNode().addChild(eventNode);
        instance.eventNode().addChild(itemRegistry.eventNode());

        instance().eventNode().addListener(PlayerInstanceLeaveEvent.class, this::handleInstanceLeave);

        // Set the instance self tag so that this world can be discovered via MapWorld#unsafeFromInstance
        // If there is already a tag do nothing, it means that this is a child world and the parent has already set the tag.
        if (!instance.hasTag(SELF_TAG)) instance.setTag(SELF_TAG, this);
        instance.setTag(TerraformInstanceBiomes.BIOMES, biomeContainer);
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
    public @NotNull MarkerHandlerRegistry markerRegistry() {
        return markerRegistry;
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
    public final void configurePlayer(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();

        try {
            List<SelectKnownPacksPacket.Entry> knownPacks;
            try {
                knownPacks = PlayerUtil.stealKnownPacksFuture(player).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException e) {
                logger.warn("Client failed to respond to known packs request", e);
                knownPacks = null;
            } catch (ExecutionException e) {
                throw new RuntimeException("Error receiving known packs", e);
            }
            boolean excludeVanilla = knownPacks != null && knownPacks.contains(SelectKnownPacksPacket.MINECRAFT_CORE);

            // Send registry data ourself to allow custom biomes per map
            var serverProcess = MinecraftServer.process();
            player.sendPacket(serverProcess.chatType().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.dimensionType().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(biomes().registryDataPacket(excludeVanilla));
            player.sendPacket(serverProcess.damageType().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.trimMaterial().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.trimPattern().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.bannerPattern().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.wolfVariant().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.enchantment().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.paintingVariant().registryDataPacket(serverProcess, excludeVanilla));
            player.sendPacket(serverProcess.jukeboxSong().registryDataPacket(serverProcess, excludeVanilla));

            player.sendPacket(MinecraftServer.getTagManager().packet(serverProcess));
            event.setSendRegistryData(false);

            // Send feature flag so that vanilla doesnt show disabled items tooltip
            event.addFeatureFlag(FeatureFlag.WINTER_DROP); // TODO remove in 1.21.5

            // Set the instance and spawn point of the player.
            event.setSpawningInstance(instance());
            preAddPlayer(event);

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

    public abstract void preAddPlayer(@NotNull AsyncPlayerConfigurationEvent event);

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
            FutureUtil.submitVirtual(() -> removePlayer(player));
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
                if (reason instanceof TranslatableComponent trans && trans.key().equals("mapmaker.shutdown")) {
                    player.kick(trans);
                } else {
                    world.server().bridge().joinHub(player);
                }
            } catch (Exception e) {
                logger.error("failed to move player to hub ({})", player.getUuid(), e);
                ExceptionReporter.reportException(e, player);
                player.kick(CLOSED_MESSAGE);
            }
        }
    }
}

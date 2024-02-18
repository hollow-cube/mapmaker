package net.hollowcube.map2;

import net.hollowcube.map2.biome.BiomeContainer;
import net.hollowcube.map2.item.handler.ItemRegistry;
import net.hollowcube.map2.util.MapWorldHelpers;
import net.hollowcube.mapmaker.entity.potion.PotionHandler;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.configuration.RegistryDataPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public non-sealed abstract class AbstractMapWorld implements MapWorld {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMapWorld.class);
    static final Tag<MapWorld> SELF_TAG = Tag.Transient("mapworld");

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
        instance.eventNode().addChild(itemRegistry.eventNode()); // Needs spectator events so register on instance.

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

        // Send registry data outself
        var registry = new HashMap<String, NBT>();
        registry.put("minecraft:chat_type", Messenger.chatRegistry());
        registry.put("minecraft:dimension_type", MinecraftServer.getDimensionTypeManager().toNBT());
        registry.put("minecraft:worldgen/biome", biomes().toNBT());
        registry.put("minecraft:damage_type", DamageType.getNBT());
        player.sendPacket(new RegistryDataPacket(NBT.Compound(registry)));
        player.sendPacket(new TagsPacket(MinecraftServer.getTagManager().getTagMap()));
        event.setSendRegistryData(false);

        // Set the instance and spawn point of the player.
        event.setSpawningInstance(instance());
        player.setRespawnPoint(spawnPoint(player));
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

    private boolean testEvent(@NotNull InstanceEvent event) {
        if (event instanceof PlayerEvent pe)
            // There is a subtle detail here that spectators do not trigger instance events on the map node.
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
        this.biomes().init();
    }

    @Blocking
    public void close() {
        logger.info("Closing world {}", this);
        Set.copyOf(players).forEach(this::removePlayer);
        Set.copyOf(spectators).forEach(this::removePlayer);
        players.clear();
        spectators.clear();
    }
}

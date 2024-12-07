package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandlerRegistry;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.map.util.datafix.HCVersions;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.*;

import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;

public sealed interface MapWorld extends TagReadable, TagWritable permits AbstractMapWorld {
    int DATA_VERSION = HCVersions.V1_21_4_HC1;

    interface Constructor<T extends AbstractMapWorld> {
        @NotNull T create(@NotNull MapServer server, @NotNull MapData map);

        @NotNull Class<T> type();
    }

    @NonBlocking
    static @NotNull MapWorld forPlayer(@NotNull Player player) {
        return Objects.requireNonNull(forPlayerOptional(player));
    }

    @NonBlocking
    static @Nullable MapWorld forPlayerOptional(@NotNull Player player) {
        if (player.getInstance() == null) return null;
        var world = unsafeFromInstance(player.getInstance());
        if (world instanceof AbstractMapWorld w1)
            return w1.getMapForPlayer(player);
        return null;
    }

    @NonBlocking
    static @Nullable MapWorld unsafeFromInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(AbstractMapWorld.SELF_TAG);
    }

    /**
     * A unique identifier for this world <i>locally to this physical server</i>. This is not the map id.
     *
     * <p>No guarantees are made of the format and it should not be depended on.</p>
     *
     * @return the locally unique id of this world.
     */
    @NotNull String worldId();
    default boolean isReadOnly() {
        return true;
    }

    @NotNull MapServer server();
    @NotNull MapData map();

    @NotNull Instance instance();
    default @NotNull Pos spawnPoint(@NotNull Player player) {
        return map().settings().getSpawnPoint();
    }

    @NotNull ItemRegistry itemRegistry();
    @NotNull BiomeContainer biomes();
    @NotNull ObjectEntityHandlerRegistry objectEntityHandlers();
    // AnimationManager, etc.

    @NotNull Collection<Player> players();
    @NotNull Collection<Player> spectators();

    @Deprecated
    default @Nullable MapWorld playWorld() {
        // This is a gross hack because we need to get the test world from an edit world out of that module.
        return null;
    }

    @Blocking
    void configurePlayer(@NotNull AsyncPlayerConfigurationEvent event);
    @Blocking
    void addPlayer(@NotNull Player player);
    @Blocking
    void addSpectator(@NotNull Player player);
    @Blocking
    void removePlayer(@NotNull Player player);

    default boolean isPlaying(@NotNull Player player) {
        return players().contains(player);
    }
    default boolean isSpectating(@NotNull Player player) {
        return spectators().contains(player);
    }
    default boolean canEdit(@NotNull Player player) {
        return false; // Worlds are read-only by default
    }

    /**
     * Gets the {@link EventNode} for this world.
     *
     * @return An event node for the active players and spectators in the world.
     */
    @NotNull EventNode<InstanceEvent> eventNode();
    default void callEvent(@NotNull InstanceEvent event) {
        instance().eventNode().call(event);
    }

    // TagReadable/TagWritable read-only implementation (EditingMapWorld overrides the write methods to make it writable)

    @Override
    default boolean hasTag(@NotNull Tag<?> tag) {
        return instance().hasTag(tag);
    }
    @Override
    default <T> @UnknownNullability T getTag(@NotNull Tag<T> tag) {
        return instance().getTag(tag);
    }
    @Override
    default <T> @UnknownNullability T getAndUpdateTag(@NotNull Tag<T> tag, @NotNull UnaryOperator<@UnknownNullability T> value) {
        throw new UnsupportedOperationException("World is read-only");
    }
    @Override
    default <T> @UnknownNullability T updateAndGetTag(@NotNull Tag<T> tag, @NotNull UnaryOperator<@UnknownNullability T> value) {
        throw new UnsupportedOperationException("World is read-only");
    }
    @Override
    default <T> @Nullable T getAndSetTag(@NotNull Tag<T> tag, @Nullable T value) {
        throw new UnsupportedOperationException("World is read-only");
    }
    @Override
    default <T> void setTag(@NotNull Tag<T> tag, @Nullable T value) {
        throw new UnsupportedOperationException("World is read-only");
    }
    @Override
    default <T> void updateTag(@NotNull Tag<T> tag, @NotNull UnaryOperator<@UnknownNullability T> value) {
        throw new UnsupportedOperationException("World is read-only");
    }
    @Override
    default void removeTag(@NotNull Tag<?> tag) {
        throw new UnsupportedOperationException("World is read-only");
    }

    default void appendDebugInfo(TextComponent.@NotNull Builder builder) {
        builder.appendNewline().append(Component.text("  ᴀɢᴇ: " + NumberUtil.formatDuration(instance().getWorldAge() * 50)));
        builder.appendNewline().append(Component.text("  ᴘʟᴀʏᴇʀѕ: " + players().size()))
                .append(Component.text(" ꜱᴘᴇᴄᴛᴀᴛᴏʀѕ: " + spectators().size()));
    }
}

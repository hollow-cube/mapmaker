package net.hollowcube.map.entity;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.*;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class MapEntity extends Entity {
    private static final Logger logger = LoggerFactory.getLogger(MapEntity.class);

    protected MapEntity(@NotNull EntityType entityType) {
        super(entityType, UUID.randomUUID());
    }

    protected MapEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    // Interaction

    /**
     * Called when a player interacts (right click) with this entity in build OR test/play/verify mode.
     * Note that this function is never called for spectating players.
     *
     * <p>This should ONLY be used to handle entities which need interaction in a playing state. If the entity
     * only needs to be configured in build mode then {@link #onBuildRightClick(MapWorld, Player, Player.Hand, Point)}
     * should be used.</p>
     */
    public void onRightClick(@NotNull MapWorld world, @NotNull Player player,
                             @NotNull Player.Hand hand, @NotNull Point interactPosition) {
        if (!MapHooks.isPlayerBuilding(player)) return;
        onBuildRightClick(world, player, hand, interactPosition);
    }

    /**
     * Called when a player interacts (right click) with this entity in build mode.
     *
     * <p>Note: this function is called from {@link #onRightClick(MapWorld, Player, Player.Hand, Point)},
     * so may not be called if that is overridden.</p>
     */
    public void onBuildRightClick(@NotNull MapWorld world, @NotNull Player player,
                                  @NotNull Player.Hand hand, @NotNull Point interactPosition) {
        // No interaction by default
    }

    /**
     * Called when a player left-clicks this entity in build OR test/play/verify mode.
     * Note that this function is never called for spectating players.
     *
     * <p>This should ONLY be used to handle entities which need interaction in a playing state. If the entity
     * only needs to be configured in build mode then {@link #onBuildLeftClick(MapWorld, Player)} should be used.</p>
     */
    public void onLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        if (!MapHooks.isPlayerBuilding(player)) return;
        onBuildLeftClick(world, player);
    }

    /**
     * Called when a player left-clicks this entity in build mode.
     *
     * <p>Note: this function is called from {@link #onLeftClick(MapWorld, Player)},
     * so may not be called if that is overridden.</p>
     */
    public void onBuildLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        // No interaction by default
    }

    // Serialization

    /**
     * Called when writing the entity to the chunk data.
     *
     * <p>It is valid to override this, however it must call the super function first.</p>
     *
     * @param buffer The buffer to write the entity data
     */
    public void save(@NotNull NetworkBuffer buffer) {
        var metadata = EntityMetadataStealer.steal(this);
        ProtocolUtil.writeMap(buffer,
                NetworkBuffer.VAR_INT,
                NetworkBuffer.Writer::write,
                metadata.getEntries());

        buffer.write(NetworkBuffer.NBT, tagHandler().asCompound());
    }

    /**
     * Called when reading the entity from the chunk data.
     *
     * <p>It is valid to override this, however it must call the super function first.</p>
     *
     * @param buffer  The buffer to read the entity data
     * @param version The version of the chunk data
     */
    public void load(@NotNull NetworkBuffer buffer, int version) {
        // Read the metadata
        var metadata = EntityMetadataStealer.steal(this);
        var loadedMetadata = ProtocolUtil.readMap(buffer, NetworkBuffer.VAR_INT, b1 -> {
            int type = b1.read(NetworkBuffer.VAR_INT);
            return Metadata.Entry.read(type, b1);
        });
        for (var entry : loadedMetadata.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
            metadata.setIndex(entry.getKey(), entry.getValue());
        }

        // Read the nbt
        tagHandler().updateContent((NBTCompound) buffer.read(NetworkBuffer.NBT));
    }

    // Misc utilities

    protected @NotNull Sound.Source soundSource() {
        return Sound.Source.NEUTRAL;
    }

    protected void playSound(@NotNull SoundEvent event, float volume, float pitch) {
        var position = getPosition();
        getViewersAsAudience().playSound(Sound.sound(event, soundSource(), volume, pitch),
                position.x(), position.y(), position.z());
    }
}

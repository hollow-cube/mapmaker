package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.hollowcube.mapmaker.map.util.datafix.legacy.PreDataFixFixes;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.hollowcube.terraform.entity.TerraformEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityMetadataStealer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.UniqueIdUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class MapEntity extends Entity implements TerraformEntity {
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
        if (!world.canEdit(player)) return;
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
        if (!world.canEdit(player)) return;
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

    // Misc utilities

    protected @NotNull Sound.Source soundSource() {
        return Sound.Source.NEUTRAL;
    }

    protected void playSound(@NotNull SoundEvent event, float volume, float pitch) {
        var position = getPosition();
        getViewersAsAudience().playSound(Sound.sound(event, soundSource(), volume, pitch),
                position.x(), position.y(), position.z());
    }

    // Serialization

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        //todo read metadata fields
    }

    @Override
    public void writeData(@NotNull CompoundBinaryTag.Builder tag) {
        tag.putString("id", getEntityType().name());
        tag.put("uuid", UniqueIdUtils.toNbt(getUuid()));
        tag.put("Pos", NbtUtil.into(getPosition()));
        tag.put("Rotation", NbtUtil.writeRotation(getPosition()));
    }

    @Deprecated // Should never be used, but cannot be removed for backwards compatibility.
    public void legacyLoad(@NotNull NetworkBuffer buffer, int version) {
        // Read the metadata
        var metadata = EntityMetadataStealer.steal(this);
        var loadedMetadata = ProtocolUtil.readMap(buffer, NetworkBuffer.VAR_INT, b1 -> {
            int type = PreDataFixFixes.fixEntityMetaIndex1_20_4(b1.read(NetworkBuffer.VAR_INT));
            return PreDataFixFixes.readEntityMeta1_20_4(type, b1);
        });
        for (var entry : loadedMetadata.entrySet()) {
            metadata.setIndex(entry.getKey(), entry.getValue());
        }

        // Read the nbt
        tagHandler().updateContent((CompoundBinaryTag) buffer.read(NetworkBuffer.NBT));
    }
}

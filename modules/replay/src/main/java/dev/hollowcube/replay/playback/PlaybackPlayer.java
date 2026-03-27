package dev.hollowcube.replay.playback;

import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class PlaybackPlayer extends Entity {
    private final String username;

    private final String skinTexture;
    private final String skinSignature;

    public PlaybackPlayer(@NotNull String username, @Nullable String skinTexture, @Nullable String skinSignature) {
        super(EntityType.PLAYER);
        this.username = username;

        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;

        setNoGravity(true);
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
        if (skinTexture != null && skinSignature != null) {
            properties.add(new PlayerInfoUpdatePacket.Property("textures", skinTexture, skinSignature));
        }
        var entry = new PlayerInfoUpdatePacket.Entry(getUuid(), username, properties, false,
                0, GameMode.SURVIVAL, null, null, 0, true);
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry));

        // Spawn the player entity
        super.updateNewViewer(player);

        // Enable skin layers
        player.sendPacket(new EntityMetaDataPacket(getEntityId(), Map.of(
                MetadataDef.Avatar.DISPLAYED_MODEL_PARTS_FLAGS.index(),
                Metadata.Byte((byte) 0b1111111)
        )));
        setInvisible(true);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);

        player.sendPacket(new PlayerInfoRemovePacket(getUuid()));
    }
}

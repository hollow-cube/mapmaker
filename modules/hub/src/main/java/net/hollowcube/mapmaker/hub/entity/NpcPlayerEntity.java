package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Common logic for player-like NPCs in the hub.
 */
@SuppressWarnings("UnstableApiUsage")
public class NpcPlayerEntity extends BaseNpcEntity {
    private static final Team NPC_TEAM = MinecraftServer.getTeamManager().createBuilder("npcs")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build();

    private final String username;
    private final PlayerSkin skin;

    public NpcPlayerEntity(@NotNull String username, @Nullable PlayerSkin skin) {
        this(UUID.randomUUID(), username, skin);
    }

    public NpcPlayerEntity(@NotNull UUID uuid, @NotNull String username, @Nullable PlayerSkin skin) {
        super(EntityType.PLAYER, uuid);
        this.username = username;
        this.skin = skin;
    }

    @Override
    public void update(long time) {
        super.update(time);

        // Look at nearby players (individually)
        for (var viewer : getViewers()) {
            if (this.position.distanceSquared(viewer.getPosition()) > 100) return;

            var newPosition = this.position.add(0, getEyeHeight(), 0).withLookAt(viewer.getPosition().add(0, viewer.getEyeHeight(), 0));
            var p1 = new EntityRotationPacket(getEntityId(), newPosition.yaw(), newPosition.pitch(), true);
            viewer.sendPacket(p1);
            var p2 = new EntityHeadLookPacket(getEntityId(), newPosition.yaw());
            viewer.sendPacket(p2);
        }

    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
        if (this.skin != null) {
            properties.add(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature()));
        }
        var entry = new PlayerInfoUpdatePacket.Entry(getUuid(), username, properties, false, 0, GameMode.SURVIVAL, null, null);
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry));

        // Spawn the player entity
        super.updateNewViewer(player);

        // Enable skin layers
        player.sendPackets(new EntityMetaDataPacket(getEntityId(), Map.of(17, Metadata.Byte((byte) 127))));

        // Put them on the NPC team to hide their name tag
        final TeamsPacket addPlayerPacket = new TeamsPacket(NPC_TEAM.getTeamName(),
                new TeamsPacket.AddEntitiesToTeamAction(List.of(this.username)));
        sendPacketToViewers(addPlayerPacket);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);

        player.sendPacket(new PlayerInfoRemovePacket(getUuid()));
    }

}

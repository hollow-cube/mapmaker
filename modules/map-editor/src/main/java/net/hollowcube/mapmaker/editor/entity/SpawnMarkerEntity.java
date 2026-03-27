package net.hollowcube.mapmaker.editor.entity;

import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpawnMarkerEntity extends Entity {

    public SpawnMarkerEntity() {
        super(EntityType.PLAYER, UUID.randomUUID());

        setNoGravity(true);
        hasPhysics = false;
        collidesWithEntities = false;

        // Only show the entity to players in build mode
        updateViewableRule(SpawnMarkerEntity::playerBuildingPredicate);
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
        var viewerSkin = player.getSkin();
        if (viewerSkin != null) {
            properties.add(new PlayerInfoUpdatePacket.Property("textures", viewerSkin.textures(), viewerSkin.signature()));
        }

        var entry = new PlayerInfoUpdatePacket.Entry(getUuid(), "Spawn Point", properties, false,
                0, GameMode.SURVIVAL, null, null, 0, true);
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry));

        // Spawn the player entity
        super.updateNewViewer(player);

        player.sendPacket(new TeamsPacket(CoreTeams.DEFAULT.getTeamName(), new TeamsPacket.AddEntitiesToTeamAction(List.of("Spawn Point"))));

        setInvisible(true);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);

        player.sendPacket(new PlayerInfoRemovePacket(getUuid()));
    }

    private static boolean playerBuildingPredicate(Player player) {
        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return false;

        return world.getPlayerState(player) instanceof EditorState.Building;
    }
}

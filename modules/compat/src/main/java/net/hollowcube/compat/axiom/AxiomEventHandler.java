package net.hollowcube.compat.axiom;

import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerDataPacket;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class AxiomEventHandler {

    static void onEntityRemoved(@NotNull RemoveEntityFromInstanceEvent event) {
        var entity = event.getEntity();

        if (event.getEntity() instanceof Player player) {
            AxiomPlayer.updateIgnoredEntities(player, Set::clear);
        }

        if (entity.getEntityType().equals(EntityType.MARKER)) {
            AxiomClientboundMarkerDataPacket.removeMarker(entity.getUuid()).sendToViewers(event.getInstance());
        }
    }

    static void onEntitySpawned(@NotNull EntitySpawnEvent event) {
        var entity = event.getEntity();

        if (entity.getEntityType().equals(EntityType.MARKER)) {
            AxiomClientboundMarkerDataPacket.spawnMarker(entity.getUuid(), entity.getPosition()).sendToViewers(event.getInstance());
        }
    }
}

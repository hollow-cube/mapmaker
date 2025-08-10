package net.hollowcube.compat.axiom;

import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerDataPacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundUpdateAvailableDispatchesPacket;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class AxiomEventHandler {

    static void onEntityRemoved(@NotNull RemoveEntityFromInstanceEvent event) {
        var entity = event.getEntity();

        if (event.getEntity() instanceof Player player) {
            AxiomPlayer.updateIgnoredEntities(player, Set::clear);
        }

        if (entity.getEntityType().equals(EntityType.MARKER)) {
            AxiomClientboundMarkerDataPacket.removeMarker(entity.getUuid()).sendToInstance(event.getInstance());
        }
    }

    static void onEntitySpawned(@NotNull EntitySpawnEvent event) {
        var entity = event.getEntity();

        if (entity.getEntityType().equals(EntityType.MARKER)) {
            AxiomClientboundMarkerDataPacket.spawnMarker(entity.getUuid(), entity.getPosition()).sendToInstance(event.getInstance());
        }
    }

    static void onPlayerTick(@NotNull PlayerTickEndEvent event) {
        if (AxiomPlayer.isEnabled(event.getPlayer())) {
            new AxiomClientboundUpdateAvailableDispatchesPacket(1024, 1024).send(event.getPlayer());
        }
    }
}

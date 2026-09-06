package net.hollowcube.compat.axiom;

import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundMarkerDataPacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundUpdateAvailableDispatchesPacket;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;
import org.jetbrains.annotations.NotNull;

public final class AxiomEventHandler {

    static void onPlayerSpawn(@NotNull PlayerSpawnEvent event) {
        if (event.isFirstSpawn()) AxiomPlayer.get(event.getPlayer()).beginHandshake();
    }

    static void onPlayerDisconnect(@NotNull PlayerDisconnectEvent event) {
        AxiomPlayer.get(event.getPlayer()).onDisconnect();
    }

    static void onEntityRemoved(@NotNull RemoveEntityFromInstanceEvent event) {
        var entity = event.getEntity();

        if (entity instanceof Player player) {
            AxiomPlayer.get(player).onInstanceLeave();
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
        if (AxiomPlayer.get(event.getPlayer()).isEnabled()) {
            new AxiomClientboundUpdateAvailableDispatchesPacket(1024, 1024).send(event.getPlayer());
        }
    }
}

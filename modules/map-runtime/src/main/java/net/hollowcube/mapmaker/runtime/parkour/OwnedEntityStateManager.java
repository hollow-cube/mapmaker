package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.runtime.entity.OwnedEntityRegistry;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.OwnedEntityList;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStateUpdateEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class OwnedEntityStateManager {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerStateUpdateEvent.class, OwnedEntityStateManager::handleUpdateStateFromPlayer)
            .addListener(ParkourMapPlayerUpdateStateEvent.class, OwnedEntityStateManager::handleUpdatePlayerFromState);

    private static void handleUpdateStateFromPlayer(ParkourMapPlayerStateUpdateEvent event) {
        final var player = (MapPlayer) event.player();
        event.playState().set(Attachments.OWNED_ENTITIES, OwnedEntityList.save(player.ownedEntities()));
    }

    private static void handleUpdatePlayerFromState(ParkourMapPlayerUpdateStateEvent event) {
        final var player = (MapPlayer) event.player();
        player.removeOwnedEntities();

        var owned = event.playState().get(Attachments.OWNED_ENTITIES);
        if (owned == null) return;
        for (var saved : owned.entities()) {
            var entity = OwnedEntityRegistry.restore(saved.kind(), player, saved.nbt());
            if (entity != null) player.addOwnedEntity(entity);
        }
    }
}

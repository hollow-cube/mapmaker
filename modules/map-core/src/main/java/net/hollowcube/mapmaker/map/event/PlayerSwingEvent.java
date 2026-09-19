package net.hollowcube.mapmaker.map.event;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.item.component.SwingAnimation;

/// A swing that was sent to the player's viewers, whatever caused it.
///
/// Since 26.3 the server decides every swing, so observing the outgoing packet is the only place
/// that sees attack swings (Minestom's punch handling) and use swings (ours) alike.
public record PlayerSwingEvent(MapPlayer player, PlayerHand hand, SwingAnimation animation) implements PlayerInstanceEvent {
    @Override
    public Player getPlayer() {
        return player;
    }
}

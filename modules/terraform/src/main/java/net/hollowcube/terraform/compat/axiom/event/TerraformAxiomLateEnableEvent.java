package net.hollowcube.terraform.compat.axiom.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

/**
 * This is sort of an annoying hack. Axiom sends the hello message during the play state so we don't know if they actually
 * have the mod installed until they are in the game, which may be after we needed to send data. This event is fired to
 * indicate that axiom was enabled sometime after the player joined the game.
 *
 * @param player
 * @param instance
 */
public record TerraformAxiomLateEnableEvent(@NotNull Player player,
                                            @NotNull Instance instance) implements PlayerInstanceEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }

}

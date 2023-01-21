package net.hollowcube.terraform.session;

import net.hollowcube.common.util.ExtraTags;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player's Terraform session, responsible for holding all "global" state.
 */
public class PlayerSession {
    public static final Tag<PlayerSession> TAG = ExtraTags.Transient("terraform:player_session");

    public static @NotNull PlayerSession fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private final Player player;

    public PlayerSession(@NotNull Player player) {
        this.player = player;
    }

    public @NotNull Player player() {
        return player;
    }

}

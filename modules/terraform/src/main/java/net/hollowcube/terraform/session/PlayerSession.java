package net.hollowcube.terraform.session;

import net.hollowcube.terraform.instance.Schematic;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a player's Terraform session, responsible for holding all "global" state.
 */
public class PlayerSession {
    public static final Tag<PlayerSession> TAG = Tag.Transient("terraform:player_session");

    public static @NotNull PlayerSession forPlayer(@NotNull Player player) {
        var session = player.getTag(TAG);
        if (session == null) {
            session = new PlayerSession(player);
            player.setTag(TAG, session);
        }
        return session;
    }

    private final Player player;

    private Schematic clipboard = null; // todo need to serialize this

    public PlayerSession(@NotNull Player player) {
        this.player = player;
    }

    public @NotNull Player player() {
        return player;
    }

    // Clipboard

    public void setClipboard(@NotNull Schematic schematic) {
        this.clipboard = schematic;
    }

    public @Nullable Schematic clipboard() {
        return clipboard;
    }

}

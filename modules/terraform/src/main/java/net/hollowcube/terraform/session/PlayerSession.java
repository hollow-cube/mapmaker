package net.hollowcube.terraform.session;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.terraform.instance.Schematic;
import net.hollowcube.util.schem.Rotation;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a player's Terraform session, responsible for holding all "global" state.
 */
public class PlayerSession {
    public static final Tag<PlayerSession> TAG = ExtraTags.Transient("terraform:player_session");

    private static PlayerSession instanceBadThisIsNotGood = null;

    public static @NotNull PlayerSession forPlayer(@NotNull Player player) {
        if (instanceBadThisIsNotGood == null) {
            instanceBadThisIsNotGood = new PlayerSession(player);
        }
        return instanceBadThisIsNotGood;
//        return player.getTag(TAG);
    }

    private final Player player;

    private Schematic clipboard = null; // todo need to serialize this
    // Since I don't see any obvious way to rotate and store the Schematic, store the rotation separately
    // Also, this should be better since we don't recalculate the schematic every time we rotate
    private Rotation rotation = Rotation.NONE;

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

    public void setRotation(@NotNull Rotation rotation) {
        this.rotation = rotation;
    }

    public @Nullable Schematic clipboard() {
        return clipboard;
    }

    public @NotNull Rotation rotation() { return rotation; }

    public void clearClipboard() {
        clipboard = null;
    }

}

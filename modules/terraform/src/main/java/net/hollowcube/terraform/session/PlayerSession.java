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
    private Rotation clipboard_rotation = Rotation.NONE; // Storing this with player instead of schematic because reasons?

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

    public Rotation clipboard_rotation() {
        return clipboard_rotation;
    }

    public void rotate_clipboard(Integer angle) {
        // TODO allow more angles of rotation
        clipboard_rotation = switch (((angle / 90) % 4)) {
            case 1 -> clipboard_rotation.rotate(Rotation.CLOCKWISE_90);
            case 2 -> clipboard_rotation.rotate(Rotation.CLOCKWISE_180);
            case 3 -> clipboard_rotation.rotate(Rotation.CLOCKWISE_270);
            default -> clipboard_rotation();
        };
    }
}

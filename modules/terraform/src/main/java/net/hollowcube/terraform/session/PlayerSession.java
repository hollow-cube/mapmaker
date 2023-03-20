package net.hollowcube.terraform.session;

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
        // Reset rotation
        rotation = Rotation.NONE;
    }

    public void setRotation(@NotNull Rotation rotation) {
        this.rotation = rotation;
    }

    public void advanceRotationHorizontal() {
        Rotation[] values = Rotation.values();
        int index = (rotation.ordinal() + 1) % values.length;
        rotation = values[index];
    }

    public void flipRotationHorizontal() {
        rotation = switch (rotation) {
            case NONE -> Rotation.CLOCKWISE_180;
            case CLOCKWISE_90 -> Rotation.CLOCKWISE_270;
            case CLOCKWISE_180 -> Rotation.NONE;
            case CLOCKWISE_270 -> Rotation.CLOCKWISE_90;
        };
    }

    public @Nullable Schematic clipboard() {
        return clipboard;
    }

    public @NotNull Rotation rotation() { return rotation; }

    public void clearClipboard() {
        clipboard = null;
    }

}

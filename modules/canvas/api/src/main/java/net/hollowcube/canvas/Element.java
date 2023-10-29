package net.hollowcube.canvas;

import net.hollowcube.canvas.util.ElementLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lowest form of a serverside UI element.
 */
public interface Element extends ElementLike {

    boolean CLICK_ALLOW = true;
    boolean CLICK_DENY = false;

    String SIG_MOUNT = "player_gui_mount";
    /** Called whenever the view is removed from the inventory (including on close). */
    String SIG_UNMOUNT = "player_gui_unmount";
    /** Called when the holding inventory is closed. */
    String SIG_CLOSE = "player_gui_close";

    // Sent when receiving anvil input from a player.
    String SIG_ANVIL_INPUT = "player_gui_anvil_input";

    enum State {
        ACTIVE,
        LOADING,
        HIDDEN,
        DISABLED
    }

    @Nullable String id();

    @NotNull State getState();

    void setState(@NotNull State state);

    void performSignal(@NotNull String name, @NotNull Object... args);

    @Override
    default @NotNull Element element() {
        return this;
    }
}

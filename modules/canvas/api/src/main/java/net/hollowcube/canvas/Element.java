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
    String SIG_UNMOUNT = "player_gui_unmount";

    // Sent when receiving anvil input from a player.
    String SIG_ANVIL_INPUT = "player_gui_anvil_input";

    enum State {
        ACTIVE,
        LOADING,
        HIDDEN,
        DISABLED
    }

    @Nullable String id();

    @Deprecated
    void setLoading(boolean loading);

    @NotNull State getState();

    void setState(@NotNull State state);

    void performSignal(@NotNull String name, @NotNull Object... args);

    @Override
    default @NotNull Element element() {
        return this;
    }
}

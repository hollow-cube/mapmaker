package net.hollowcube.terraform.selection;

import net.hollowcube.terraform.selection.cui.ColorScheme;
import net.hollowcube.terraform.selection.cui.DebugRendererSelectionRenderer;
import net.hollowcube.terraform.selection.cui.SelectionRenderer;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Selection {
    public static final @NotNull String DEFAULT = "default";

    private final Player player;
    private final String name;

    private SelectionRenderer renderer;
    private RegionSelector selector;
    private Region cachedRegion = null;

    public Selection(@NotNull Player player, @NotNull String name) {
        this.player = player;
        this.name = name;

        //todo should use a player configured renderer or choose the best default if they have not chosen
        // eg if they have never chosen a renderer (including on every new join)
        //    choose in the following order: DebugRenderer, WorldEditCUI, Particles
        //    if they have chosen a renderer, use that always.
        //    if they have selected a lower priority renderer and join with a higher priority, send them a message ONCE
        //    if they have selected a renderer (such as debug renderer) and join without it, leave their selection and
        //    send a message indicating that they have fallen back to <next highest priority>

        this.renderer = new DebugRendererSelectionRenderer(player, ColorScheme.DEFAULT, name);
        this.selector = Region.Type.CUBOID.newSelector(player, renderer);
    }

    @ApiStatus.Internal
    public @NotNull RegionSelector selector() {
        return selector;
    }

    public boolean selectPrimary(@NotNull Point point) {
        if (selector.selectPrimary(point)) {
            cachedRegion = null;
            return true;
        }
        return false;
    }

    public void explainPrimary(@NotNull Point point) {
        selector.explainPrimary(point);
    }

    public boolean selectSecondary(@NotNull Point point) {
        if (selector.selectSecondary(point)) {
            cachedRegion = null;
            return true;
        }
        return false;
    }

    public void explainSecondary(@NotNull Point point) {
        selector.explainSecondary(point);
    }

    public void clear() {
        selector.clear();
        cachedRegion = null;
    }

    public @Nullable Region region() {
        if (cachedRegion == null) {
            cachedRegion = selector.region();
        }
        return cachedRegion;
    }

}

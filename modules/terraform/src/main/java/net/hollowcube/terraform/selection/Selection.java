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
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

public class Selection {
    public static final @NotNull String DEFAULT = "default";

    private final Player player;
    private final String name;

    private final SelectionRenderer renderer;
    private RegionSelector selector;
    private Region.Type regionType = Region.Type.CUBOID;
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
        this.selector = regionType.newSelector(player, renderer);
    }

    public String name() {
        return name;
    }

    public @NotNull Region.Type type() {
        return this.regionType;
    }

    public void setType(@NotNull Region.Type type) {
        this.regionType = type;
        this.selector = type.newSelector(player, renderer);
        this.selector.clear(); // Updates CUI
        this.cachedRegion = null;
    }

    @ApiStatus.Internal
    public @NotNull RegionSelector selector() {
        return selector;
    }

    public boolean selectPrimary(@NotNull Point point, boolean explain) {
        if (selector.selectPrimary(point, explain)) {
            cachedRegion = null;
            return true;
        }
        return false;
    }

    public boolean selectSecondary(@NotNull Point point, boolean explain) {
        if (selector.selectSecondary(point, explain)) {
            cachedRegion = null;
            return true;
        }
        return false;
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
    
    public @NotNull NBTCompound toNBT() {
        var root = new MutableNBTCompound();
        root.setString("name", name);
        root.setString("type", regionType.name());
        root.set("selector", selector.toNBT());
        return root.toCompound();
    }

    public static @NotNull Selection fromNBT(@NotNull Player player, @NotNull NBTCompound nbt) {
        var selection = new Selection(player, nbt.getString("name"));

        selection.setType(Region.Type.valueOf(nbt.getString("type")));
        selection.selector().fromNBT(nbt.getCompound("selector"));

        return selection;
    }

    public void changeSize(int delta, boolean changeVertical, boolean changeHorizontal) {
        if (changeVertical) {
            Region region = region();
            if (region != null) {
                int yMin = region.min().blockY();
                int yMax = region.max().blockY();
                yMin -= delta; // We subtract from yMin and add to yMax. Positive numbers will expand, negative numbers will shrink
                yMax += delta;
                if (yMin >= yMax) {
                    // If we shrink beyond appropriate bounds, what do we do?
                    // Clamp to midpoint
                    yMax = (region.min().blockY() + region.max().blockY()) / 2;
                    yMin = yMax - 1;
                }
                // Clamp to world bounds
                yMax = Math.min(yMax, player.getInstance().getDimensionType().getMaxY());
                yMin = Math.max(yMin, player.getInstance().getDimensionType().getMinY());
                selectPrimary(region.min().withY(yMin), false);
                selectSecondary(region.max().withY(yMax), false);
            }
        }
        // TODO: If both are true, there's some optimization to be done with not recalculating the region, but that is for later
        if (changeHorizontal) {
            Region region = region();
            if (region != null) {
                int xMin = region.min().blockX();
                int xMax = region.max().blockX();
                int zMin = region.min().blockZ();
                int zMax = region.max().blockZ();
                xMin -= delta;
                xMax += delta;
                zMin -= delta;
                zMax += delta;
                if (xMin >= xMax) {
                    // Clamp to midpoint
                    xMax = (region.min().blockX() + region.max().blockX()) / 2;
                    xMin = xMax - 1;
                }
                if (zMin >= zMax) {
                    // Clamp to midpoint
                    zMax = (region.min().blockZ() + region.max().blockZ()) / 2;
                    zMin = zMax - 1;
                }
                // TODO: Clamp inside world border
                selectPrimary(region.min().withX(xMin).withZ(zMin), false);
                selectSecondary(region.max().withX(xMax).withZ(zMax), false);
            }
        }
    }
}

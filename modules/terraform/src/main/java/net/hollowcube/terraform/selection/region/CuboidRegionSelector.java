package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.selection.cui.SelectionRenderer;
import net.hollowcube.terraform.util.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

public class CuboidRegionSelector implements RegionSelector {
    private final Player player;
    private final SelectionRenderer renderer;
    private Point pos1 = null, pos2 = null;

    public CuboidRegionSelector(@NotNull Player player, @NotNull SelectionRenderer renderer) {
        this.player = player;
        this.renderer = renderer;
    }

    @Override
    public boolean selectPrimary(@NotNull Point point, boolean explain) {
        if (pos1 != null && point.sameBlock(pos1)) return false;
        pos1 = CoordinateUtil.floor(point);

        updateRender();
        if (explain) {
            player.sendMessage(Component.translatable("command.worldedit.cuboid.explain.primary",
                    Component.text(point.blockX()), Component.text(point.blockY()), Component.text(point.blockZ())));
        }

        return true;
    }

    @Override
    public boolean selectSecondary(@NotNull Point point, boolean explain) {
        if (pos2 != null && point.sameBlock(pos2)) return false;
        pos2 = CoordinateUtil.floor(point);

        updateRender();
        if (explain) {
            player.sendMessage(Component.translatable("command.worldedit.cuboid.explain.secondary",
                    Component.text(point.blockX()), Component.text(point.blockY()), Component.text(point.blockZ())));
        }

        return true;
    }

    @Override
    public void clear() {
        pos1 = null;
        pos2 = null;
        updateRender();
    }

    @Override
    public @Nullable Region region() {
        if (pos1 == null || pos2 == null) return null;
        return new CuboidRegion(
                CoordinateUtil.min(pos1, pos2),
                CoordinateUtil.max(pos1, pos2).add(1, 1, 1)
        );
    }

    private void updateRender() {
        renderer.begin();
        if (pos1 != null && pos2 != null) {
            renderer.cuboid(
                    CoordinateUtil.min(pos1, pos2),
                    CoordinateUtil.max(pos1, pos2).add(1, 1, 1)
            );
        }
        renderer.end();
    }

    @Override
    public @NotNull NBTCompound toNBT() {
        var root = new MutableNBTCompound();
        if (pos1 != null) root.set("pos1", CoordinateUtil.toNBT(pos1));
        if (pos2 != null) root.set("pos2", CoordinateUtil.toNBT(pos2));
        return root.toCompound();
    }

    @Override
    public void fromNBT(@NotNull NBTCompound nbt) {
        pos1 = nbt.contains("pos1") ? CoordinateUtil.fromNBT(nbt.getCompound("pos1")) : null;
        pos2 = nbt.contains("pos2") ? CoordinateUtil.fromNBT(nbt.getCompound("pos2")) : null;
        updateRender();
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
                Point primary = boundToWorldBorder(region.min().withX(xMin).withZ(zMin), player.getInstance());
                Point secondary = boundToWorldBorder(region.max().withX(xMax).withZ(zMax), player.getInstance());
                selectPrimary(primary, false);
                selectSecondary(secondary, false);
            }
        }
    }

    private Point boundToWorldBorder(Point point, Instance world) {
        var border = world.getWorldBorder();
        double maxLimitX = border.getCenterX() + border.getDiameter() / 2;
        double minLimitX = border.getCenterX() - border.getDiameter() / 2;
        double newX = Math.max(minLimitX, Math.min(maxLimitX, point.x()));

        double maxLimitZ = border.getCenterZ() + border.getDiameter() / 2;
        double minLimitZ = border.getCenterZ() - border.getDiameter() / 2;
        double newZ = Math.max(minLimitZ, Math.min(maxLimitZ, point.z()));

        return point.withX(newX).withZ(newZ);
    }
}

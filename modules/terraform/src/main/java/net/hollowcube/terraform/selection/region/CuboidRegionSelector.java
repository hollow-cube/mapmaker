package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class CuboidRegionSelector implements RegionSelector {
    public static final Factory FACTORY = new Factory("cuboid", CuboidRegionSelector::new);
    private static final byte DATA_VERSION = 1;

    private final ClientInterface cui;

    private Point pos1 = null;
    private Point pos2 = null;

    public CuboidRegionSelector(@NotNull ClientInterface cui, @NotNull String selectionId) {
        this.cui = cui;
    }

    public CuboidRegionSelector(@NotNull ClientInterface cui) {
        this.cui = cui;
    }

    @Override
    public boolean selectPrimary(@NotNull Point point, boolean explain) {
        if (pos1 != null && point.sameBlock(pos1)) return false;
        pos1 = CoordinateUtil.floor(point);

        updateRender();
        if (explain) {
            cui.sendMessage(
                    "terraform.cuboid.explain.primary",
                    point.blockX(), point.blockY(), point.blockZ()
            );
        }

        return true;
    }

    @Override
    public boolean selectSecondary(@NotNull Point point, boolean explain) {
        if (pos2 != null && point.sameBlock(pos2)) return false;
        pos2 = CoordinateUtil.floor(point);

        updateRender();
        if (explain) {
            cui.sendMessage(
                    "terraform.cuboid.explain.secondary",
                    point.blockX(), point.blockY(), point.blockZ()
            );
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
        var renderer = cui.renderer();

        renderer.begin("cuboid"); //todo the ClientInterface should hide this detail. One should be created per selector
        if (pos1 != null && pos2 != null) {
            renderer.cuboid(
                    CoordinateUtil.min(pos1, pos2),
                    CoordinateUtil.max(pos1, pos2).add(1, 1, 1)
            );
        }
        renderer.end("cuboid");
    }

    @Override
    public void reshape(@NotNull Point low, @NotNull Point high) {
        var region = region();
        if (region == null) return;

        pos1 = region.min().add(low);
        pos2 = region.max().add(high).sub(1);
        updateRender();
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.BYTE, DATA_VERSION);
        buffer.writeOptional(NetworkBuffer.VECTOR3, pos1);
        buffer.writeOptional(NetworkBuffer.VECTOR3, pos2);
    }

    @Override
    public void read(@NotNull NetworkBuffer buffer) {
        byte version = buffer.read(NetworkBuffer.BYTE);
        Check.stateCondition(version > DATA_VERSION, "Unsupported data version: " + version);

        pos1 = buffer.readOptional(NetworkBuffer.VECTOR3);
        pos2 = buffer.readOptional(NetworkBuffer.VECTOR3);
    }

    private Point boundToWorldBorder(Point point, @Nullable Instance world) {
        if (world == null) {
            return point;
        }
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

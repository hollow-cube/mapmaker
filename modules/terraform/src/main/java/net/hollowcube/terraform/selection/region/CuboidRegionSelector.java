package net.hollowcube.terraform.selection.region;

import net.hollowcube.common.util.NetworkBufferTypes;
import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        renderer.switchTo(ClientRenderer.RenderContext.NORMAL, false);
        renderer.begin("cuboid"); //todo the ClientInterface should hide this detail. One should be created per selector
        if (pos1 != null && pos2 != null) {
            renderer.cuboid(
                    CoordinateUtil.min(pos1, pos2),
                    CoordinateUtil.max(pos1, pos2).add(1, 1, 1),
                    ClientRenderer.RenderType.PRIMARY);
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

        buffer.write(NetworkBufferTypes.OPT_VECTOR3, pos1);
        buffer.write(NetworkBufferTypes.OPT_VECTOR3, pos2);
    }

    @Override
    public void read(@NotNull NetworkBuffer buffer) {
        byte version = buffer.read(NetworkBuffer.BYTE);
        Check.stateCondition(version > DATA_VERSION, "Unsupported data version: " + version);

        pos1 = buffer.read(NetworkBufferTypes.OPT_VECTOR3);
        pos2 = buffer.read(NetworkBufferTypes.OPT_VECTOR3);
    }

    private Point boundToWorldBorder(Point point, @Nullable Instance world) {
        if (world == null) {
            return point;
        }
        var border = world.getWorldBorder();
        double maxLimitX = border.centerX() + border.diameter() / 2;
        double minLimitX = border.centerX() - border.diameter() / 2;
        double newX = Math.max(minLimitX, Math.min(maxLimitX, point.x()));

        double maxLimitZ = border.centerZ() + border.diameter() / 2;
        double minLimitZ = border.centerZ() - border.diameter() / 2;
        double newZ = Math.max(minLimitZ, Math.min(maxLimitZ, point.z()));

        return point.withX(newX).withZ(newZ);
    }

    public Point getPos1() {
        return pos1;
    }

    public Point getPos2() {
        return pos2;
    }
}

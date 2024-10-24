package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class LineRegionSelector implements RegionSelector {
    private final ClientInterface cui;
    private final String selectionId;

    private Point pos1 = null;
    private Point pos2 = null;

    public LineRegionSelector(@NotNull ClientInterface cui, @NotNull String selectionId) {
        this.cui = cui;
        this.selectionId = selectionId;
    }

    @Override
    public boolean selectPrimary(@NotNull Point point, boolean explain) {
        if (pos1 != null && point.sameBlock(pos1)) return false;
        pos1 = CoordinateUtil.floor(point);

        updateRender();
        if (explain) {
            cui.sendMessage(
                    "terraform.line.explain.primary",
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
                    "terraform.line.explain.secondary",
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
        return new LineRegion(pos1, pos2);
    }

    private void updateRender() {
        var renderer = cui.renderer();

        renderer.begin(selectionId);
        if (pos1 != null) renderer.point(pos1.add(0.5), 0.55);
        if (pos2 != null) renderer.point(pos2.add(0.5), 0.55);
        if (pos1 != null && pos2 != null) {
            renderer.line(pos1.add(0.5), pos2.add(0.5));
        }
        renderer.end(selectionId);
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer) {
        // TODO(1.21.2)
//        buffer.writeOptional(VECTOR3, pos1);
//        buffer.writeOptional(VECTOR3, pos2);
    }

    @Override
    public void read(@NotNull NetworkBuffer buffer) {
        // TODO(1.21.2)
//        pos1 = buffer.readOptional(VECTOR3);
//        pos2 = buffer.readOptional(VECTOR3);
    }

}

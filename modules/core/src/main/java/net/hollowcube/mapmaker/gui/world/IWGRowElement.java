package net.hollowcube.mapmaker.gui.world;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class IWGRowElement extends IWGContainerElement {
    private Point boundingBox;

    private float spacing = 0;

    public @NotNull IWGRowElement spacing(float spacing) {
        this.spacing = spacing;
        invalidate();
        return this;
    }

    @Override
    public @NotNull IWGRowElement addChild(@NotNull IWGElement child) {
        super.addChild(child);
        invalidate();
        return this;
    }

    @Override
    public @NotNull Point boundingBox() {
        if (boundingBox == null)
            boundingBox = computeBoundingBox();
        return boundingBox;
    }

    @Override
    public void setInstance(@NotNull Instance instance, @NotNull Point position) {

    }

    private void invalidate() {
        boundingBox = null;
    }

    private @NotNull Point computeBoundingBox() {
        double width = 0, height = 0, depth = 0;
        for (int i = 0; i < children().size(); i++) {
            Point childBox = children().get(i).boundingBox();
            if (i > 0) width += spacing;
            width += childBox.x();
            height = Math.max(height, childBox.y());
            depth = Math.max(depth, childBox.z());
        }
        return new Vec(width, height, depth);
    }
}

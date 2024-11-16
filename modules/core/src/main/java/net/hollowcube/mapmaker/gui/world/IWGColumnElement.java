package net.hollowcube.mapmaker.gui.world;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class IWGColumnElement extends IWGContainerElement {
    private Point boundingBox;

    private float spacing = 0;

    public @NotNull IWGColumnElement spacing(float spacing) {
        this.spacing = spacing;
        invalidate();
        return this;
    }

    @Override
    public @NotNull IWGColumnElement addChild(@NotNull IWGElement child) {
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
            width = Math.max(width, childBox.x());
            if (i > 0) height += spacing;
            height += childBox.y();
            depth = Math.max(depth, childBox.z());
        }
        return new Vec(width, height, depth);
    }
}

package net.hollowcube.compat.noxesium.qib;

import com.noxcrew.noxesium.api.qib.QibDefinition;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class QibBlock implements BlockHandler {

    private final QibEntity qib;
    private Block block;
    private Point position;

    public QibBlock() {
        this.qib = new QibEntity();
    }

    @Override
    public void tick(@NotNull Tick tick) {
        BlockHandler.super.tick(tick);

        if (!Objects.equals(tick.getBlock(), this.block)) {
            this.block = tick.getBlock();
            this.qib.setBoundingBox(getBoundingBox(this.block));
        }

        if (!Objects.equals(tick.getBlockPosition(), this.position)) {
            this.position = Pos.fromPoint(tick.getBlockPosition()).add(0.5, 0, 0.5);
            if (this.qib.isActive()) this.qib.teleport(Pos.fromPoint(this.position));
        }

        if (!this.qib.isActive()) {
            this.qib.setInstance(tick.getInstance(), this.position);
        }
    }

    @Override
    public void onDestroy(@NotNull Destroy destroy) {
        BlockHandler.super.onDestroy(destroy);
        this.qib.remove();
    }

    protected final void setBehavior(QibDefinition behavior) {
        this.qib.setBehavior(behavior);
    }

    protected abstract BoundingBox getBoundingBox(Block block);
}

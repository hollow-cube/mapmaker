package net.hollowcube.multipart.entity;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*

I (matt) wonder if its ever worth flattening the bones to a single block of memory?

 */

public sealed class Bone permits ItemBone {
    protected static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(-50_000_000);

    protected final Transform defaultTransform;
    private final List<Bone> children;

    protected float dx, dy, dz;
    protected float rx, ry, rz;
    protected float sx, sy, sz;

    public Bone(@NotNull Transform defaultTransform, @NotNull List<Bone> children) {
        this.defaultTransform = defaultTransform;
        this.children = children;
        reset();
    }

    public void updateNewViewer(@NotNull Player player) {
        if (this.children.isEmpty()) return;

        for (var child : this.children) {
            child.updateNewViewer(player);
        }
    }

    public void updateFor(@NotNull Player player, @NotNull Transform.Mutable tr) {
        tr.dx += dx;
        tr.dy += dy;
        tr.dz += dz;
        if (!this.children.isEmpty()) {
            for (var child : this.children) {
                child.updateFor(player, tr);
            }
        }
        tr.dx -= dx;
        tr.dy -= dy;
        tr.dz -= dz;
    }

    public void reset() {
        this.dx = defaultTransform.dx();
        this.dy = defaultTransform.dy();
        this.dz = defaultTransform.dz();
        this.rx = defaultTransform.rx();
        this.ry = defaultTransform.ry();
        this.rz = defaultTransform.rz();
        this.sx = defaultTransform.sx();
        this.sy = defaultTransform.sy();
        this.sz = defaultTransform.sz();
    }

    public void offsetPosition(Vec offset) {
        this.dx += (float) offset.x();
        this.dy += (float) offset.y();
        this.dz += (float) offset.z();
    }

    public void offsetRotation(Vec offset) {
        this.rx += (float) offset.x();
        this.ry += (float) offset.y();
        this.rz += (float) offset.z();
    }

    public void offsetScale(Vec offset) {
        this.sx *= (float) offset.x();
        this.sy *= (float) offset.y();
        this.sz *= (float) offset.z();
    }

}

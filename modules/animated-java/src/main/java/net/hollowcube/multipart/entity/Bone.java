package net.hollowcube.multipart.entity;

import net.hollowcube.aj.util.Quaternion;
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
        // 1. Compose this bone's rotation
        Quaternion localRotation = Quaternion.fromEulerAngles(new Vec(rx, ry, rz));
        Quaternion defaultRotation = Quaternion.fromEulerAngles(new Vec(defaultTransform.rx(), defaultTransform.ry(), defaultTransform.rz()));
        Quaternion boneRotation = defaultRotation.multiply(localRotation);
        Quaternion accumulatedRotation = tr.rotation.multiply(boneRotation);

        // 2. Calculate this bone's final world position
        // Start with the bone's rest position offset from model origin
        Vec restOffset = defaultTransform.pivot();

        // Add animated translation (relative to rest position)
        Vec animatedTranslation = new Vec(dx, dy, dz);
        Vec rotatedAnimatedTranslation = boneRotation.rotate(animatedTranslation);
        Vec totalOffset = restOffset.add(rotatedAnimatedTranslation);

        // Apply parent transformation to the total offset
        Vec rotatedByParent = tr.rotation.rotate(totalOffset);
        Vec accumulatedTranslation = tr.translation.add(rotatedByParent);

        // Send to entity
        sendUpdates(player, accumulatedTranslation, accumulatedRotation);

        // Recurse to children - they inherit this bone's world transformation
        if (!this.children.isEmpty()) {
            Transform.Mutable childTransform = new Transform.Mutable(
                    accumulatedTranslation.sub(restOffset),
                    accumulatedRotation
            );
            for (var child : this.children) {
                child.updateFor(player, childTransform);
            }
        }
    }

    protected void sendUpdates(@NotNull Player player, @NotNull Vec translation, @NotNull Quaternion rotation) {
        // do nothing for this bone, subclasses should override this
    }

    public void reset() {
        this.dx = defaultTransform.dx();
        this.dy = defaultTransform.dy();
        this.dz = defaultTransform.dz();
        this.rx = 0;
        this.ry = 0;
        this.rz = 0;
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

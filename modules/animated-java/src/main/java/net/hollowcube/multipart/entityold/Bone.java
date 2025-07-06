package net.hollowcube.multipart.entityold;

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
        // 1. Compute the total rotation: default + animated
        var defaultRotation = Quaternion.fromEulerAngles(new Vec(defaultTransform.rx(), defaultTransform.ry(), defaultTransform.rz()));
        var animatedRotation = Quaternion.fromEulerAngles(new Vec(rx, ry, rz));
        var totalRotation = defaultRotation.multiply(animatedRotation);

        // 2. Apply parent's scale to this bone's offset, then apply parent's rotation
        var scaledOffset = new Vec(dx * tr.scale.x(), dy * tr.scale.y(), dz * tr.scale.z());
        var rotatedOffset = tr.rotation.rotate(scaledOffset);
        var finalTranslation = tr.translation.add(rotatedOffset);

        // 3. Compose rotations: parent rotation * this bone's rotation
        var finalRotation = tr.rotation.multiply(totalRotation);

        // 4. Compose scales: parent scale * this bone's scale
        var finalScale = new Vec(tr.scale.x() * sx, tr.scale.y() * sy, tr.scale.z() * sz);

        // 5. Send the final transform to client
        sendUpdates(player, finalTranslation, finalRotation, finalScale);

        // 6. Propagate to children with accumulated transforms
        if (!this.children.isEmpty()) {
            Transform.Mutable childTransform = new Transform.Mutable(
                    finalTranslation,
                    finalRotation,
                    finalScale
            );
            for (var child : this.children) {
                child.updateFor(player, childTransform);
            }
        }
    }

    protected void sendUpdates(@NotNull Player player, @NotNull Vec translation, @NotNull Quaternion rotation, @NotNull Vec scale) {
        // do nothing for this bone, subclasses should override this
    }

    public void reset() {
        this.dx = defaultTransform.dx();
        this.dy = defaultTransform.dy();
        this.dz = defaultTransform.dz();
        this.rx = 0; //defaultTransform.rx();
        this.ry = 0; //defaultTransform.ry();
        this.rz = 0; //defaultTransform.rz();
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

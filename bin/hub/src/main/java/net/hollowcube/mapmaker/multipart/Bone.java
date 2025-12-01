package net.hollowcube.mapmaker.multipart;

import net.hollowcube.common.math.Quaternion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Bone {

    private final String id;
    private final String name;

    private Vec translation, animationTranslation;
    private Vec rotation, animationRotation;

    private List<Bone> children;
    private boolean hasItem = false;

    private Entity entity;

    public Bone(String id, String name, List<Bone> children, Point translation, Point rotation) {
        this.id = id;
        this.name = name;
        this.children = children;
        this.translation = translation.asVec();
        this.rotation = rotation.asVec();
    }

    public @Nullable Bone findById(String id) {
        if (this.id.equals(id)) return this;
        for (var child : children) {
            var found = child.findById(id);
            if (found != null) return found;
        }
        return null;
    }

    public void setHasItem(boolean hasItem) {
        this.hasItem = hasItem;
    }

    public void setAnimationPosition(Vec position) {
        animationTranslation = position;
    }

    public void setAnimationRotation(Vec rotation) {
        animationRotation = rotation;
    }

    public void spawn(Instance instance, Pos position) {
        for (var child : children)
            child.spawn(instance, position);

        if (!hasItem) return;

        entity = new Entity(EntityType.ITEM_DISPLAY);
        var meta = (ItemDisplayMeta) entity.getEntityMeta();
        var scale = 4.0;

        meta.setScale(new Vec(scale));
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.HEAD);
        meta.setItemStack(ItemStack.of(Material.STICK).withItemModel("aj:" + id));
        meta.setTransformationInterpolationDuration(1);
        entity.setNoGravity(true);
        entity.setInstance(instance, position);
    }

    public void update(Quaternion parentRotation, Vec parentTranslation) {// R_base
        Quaternion localQuat = Quaternion.fromEulerAnglesYXZ(this.rotation);
        Quaternion finalRotation = new Quaternion(parentRotation).mul(localQuat);

        Vec rotatedTranslation = parentRotation.transform(this.translation.asVec());
        Vec finalTranslation = parentTranslation.add(rotatedTranslation);

        for (var child : children)
            child.update(finalRotation, finalTranslation);

        if (!hasItem) return;

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setTranslation(finalTranslation);
            meta.setLeftRotation(finalRotation.into());
            meta.setTransformationInterpolationStartDelta(0);
        });
    }

    public void dump(int indent) {
        var indentStr = " ".repeat(indent);
        System.out.println(indentStr + (hasItem ? "(i) " : "") + name + " [" + id + "]");
        System.out.println(indentStr + "  translation: " + translation + ", rotation: " + rotation);
        for (var child : children) child.dump(indent + 2);
    }

}

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

import java.util.List;

public class Bone {

    private final String id;
    private final String name;

    private Vec translation;
    private Vec rotation;

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

    public void setHasItem(boolean hasItem) {
        this.hasItem = hasItem;
    }

    public void spawn(Instance instance, Pos position, Quaternion parentRotation, Vec parentTranslation) {
        Quaternion localQuat = Quaternion.fromEulerAnglesYXZ(this.rotation);

        Quaternion finalRotation = new Quaternion(parentRotation).mul(localQuat);

        Vec rotatedTranslation = parentRotation.transform(this.translation.asVec());
        Vec finalTranslation = parentTranslation.add(rotatedTranslation);

        for (var child : children)
            child.spawn(instance, position, finalRotation, finalTranslation);

        if (!hasItem) return;

        entity = new Entity(EntityType.ITEM_DISPLAY);
        var meta = (ItemDisplayMeta) entity.getEntityMeta();

        var scale = 4.0;

        meta.setScale(new Vec(scale));
        meta.setTranslation(finalTranslation);
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.HEAD);
        meta.setItemStack(ItemStack.of(Material.STICK).withItemModel("aj:" + id));

        meta.setLeftRotation(finalRotation.into());

        entity.setNoGravity(true);
        entity.setInstance(instance, position);
    }

    public void dump(int indent) {
        var indentStr = " ".repeat(indent);
        System.out.println(indentStr + (hasItem ? "(i) " : "") + name + " [" + id + "]");
        System.out.println(indentStr + "  translation: " + translation + ", rotation: " + rotation);
        for (var child : children) child.dump(indent + 2);
    }

}

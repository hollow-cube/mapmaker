package net.hollowcube.aj.entity;

import net.hollowcube.aj.Model;
import net.hollowcube.aj.Node;
import net.hollowcube.aj.Transform;
import net.hollowcube.aj.bone.AbstractBone;
import net.hollowcube.aj.bone.EntityBone;
import net.hollowcube.aj.bone.StructBone;
import net.hollowcube.aj.util.Quaternion;
import net.hollowcube.multipart.bedrock.BedrockGeoModel;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModelEntity extends Entity {

    private final List<AbstractBone> rootBones = new ArrayList<>();
    private final List<Integer> leafEntityIds = new ArrayList<>();

    public ModelEntity(@NotNull Model model, @Nullable UUID uuid) {
        super(EntityType.INTERACTION, Objects.requireNonNullElseGet(uuid, UUID::randomUUID));

        setNoGravity(true);
        hasPhysics = false;
        collidesWithEntities = false;

        Map<UUID, AbstractBone> bones = new HashMap<>();
        for (var entry : model.nodes().entrySet()) {
            if ("hitbox".equals(entry.getValue().name()))
                continue;
            bones.put(entry.getKey(), switch (entry.getValue()) {
                case Node.TextDisplay node -> new EntityBone(node);
                case Node.Bone node -> new EntityBone(node);
                case Node.Locator node -> new StructBone(); //todo
                case Node.Struct node -> new StructBone();
            });
        }

        for (var entry : bones.entrySet()) {
            var node = Objects.requireNonNull(model.nodes().get(entry.getKey()));

            if (node.parent() != null) {
                bones.get(node.parent()).addChild(entry.getValue());
            } else {
                this.rootBones.add(entry.getValue());
            }

            if (entry.getValue() instanceof EntityBone entity) {
                this.leafEntityIds.add(entity.entityId());
            }
        }

//        var animation = model.animations().values().stream().findFirst().orElseThrow();
//        for (var entry : animation.animators().entrySet()) {
//            var bone = Objects.requireNonNull(bones.get(entry.getKey()));
//            bone.setAnimator(new Animator.AnimatorImpl(entry.getValue()));
//        }
    }

    public ModelEntity(@NotNull BedrockGeoModel model, @Nullable UUID uuid) {
        super(EntityType.INTERACTION, Objects.requireNonNullElseGet(uuid, UUID::randomUUID));

        var _ = Node.CODEC;

        setNoGravity(true);
        hasPhysics = false;
        collidesWithEntities = false;

        Map<String, AbstractBone> elementMap = new HashMap<>();
        for (var element : model.bones()) {
            if (element.cubes().isEmpty()) {
                elementMap.put(element.name(), new StructBone());
                continue;
            }

            var leftRotation = Quaternion.fromEulerAngles(Vec.ZERO).into();

//            var cu = element.cubes().stream().findFirst().orElse(null);
            var rot = element.rotation();
            if (rot != null && !rot.isZero()) {
                leftRotation = Quaternion.fromEulerAngles(new Vec(rot.x(), rot.y(), -rot.z())).into();
            }
//            if ("as_armsupport_fix".equals(element.name())) {
//                leftRotation = Quaternion.fromEulerAngles(new Vec(0, 180, 37.5)).into();
//            }
//            if ("laserarm".equals(element.name())) {
//                leftRotation = Quaternion.fromEulerAngles(new Vec(0, 180, 42.5)).into();
//            }
            var translation = element.pivot().div(16).mul(1, 1, -1);
//            if ("as_armsupport_rotated_967027".equals(element.name())) {
//                System.out.println(element.pivot());
//                translation = new Vec(6.5, 16.475, 0.5).div(16);
////                translation = translation.add(new Vec(-1.5, 1.5, 0).div(16));
//            } else {
//                translation = Vec.ZERO;
//            }
//            var translation = Vec.ZERO;

            var entity = new EntityBone(new Node.Bone(new Node.Base(UUID.randomUUID(), element.name(), null, new Transform.Default(
                    new Transform(translation, leftRotation, Vec.ONE),
                    new float[]{0, 0}
            ))));
            elementMap.put(element.name(), entity);
//            if ("as_armsupport_rotated_967027".equals(element.name()))
            this.leafEntityIds.add(entity.entityId());
        }

        for (var element : model.bones()) {
            var thisElement = elementMap.get(element.name());
            if (element.parent() == null) {
                this.rootBones.add(thisElement);
            } else {
                var parent = elementMap.get(element.parent());
                parent.addChild(thisElement);
            }
        }

        System.out.println("done");
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);

        for (var bone : this.rootBones) bone.updateNewViewer(player);
        player.sendPacket(new SetPassengersPacket(getEntityId(), this.leafEntityIds));

    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);

        player.sendPacket(new DestroyEntitiesPacket(this.leafEntityIds));
    }

    @Override
    public void tick(long time) {
        super.tick(time);

//        teleport(getPosition().withX(x -> x + 0.1f));

//        if (getAliveTicks() < 40) return;
//
//        for (var bone : this.rootBones) {
//            bone.update(this, Vec.ZERO, Vec.ONE);
//        }
    }
}

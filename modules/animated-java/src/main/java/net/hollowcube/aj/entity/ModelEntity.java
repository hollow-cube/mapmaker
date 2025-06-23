package net.hollowcube.aj.entity;

import net.hollowcube.aj.Model;
import net.hollowcube.aj.Node;
import net.hollowcube.aj.Transform;
import net.hollowcube.aj.bone.AbstractBone;
import net.hollowcube.aj.bone.EntityBone;
import net.hollowcube.aj.bone.StructBone;
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
            var entity = new EntityBone(new Node.Bone(new Node.Base(UUID.randomUUID(), element.name(), null, new Transform.Default(
                    new Transform(Vec.ZERO, new float[]{0, 1, 0, 0}, Vec.ONE),
                    new float[]{0, 0}
            ))));
            elementMap.put(element.name(), entity);
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

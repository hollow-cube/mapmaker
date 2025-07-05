package net.hollowcube.aj.entity;

import net.hollowcube.aj.Model;
import net.hollowcube.aj.Node;
import net.hollowcube.aj.bone.AbstractBone;
import net.hollowcube.aj.bone.EntityBone;
import net.hollowcube.aj.bone.StructBone;
import net.hollowcube.multipart.bedrock.BedrockAnimation;
import net.hollowcube.multipart.bedrock.BedrockGeoModel;
import net.hollowcube.multipart.entity.Bone;
import net.hollowcube.multipart.entity.ItemBone;
import net.hollowcube.multipart.entity.Transform;
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

    private final List<Bone> rootBones = new ArrayList<>();
    private final List<Integer> leafEntityIds = new ArrayList<>();

    private final Map<String, Bone> boneById = new HashMap<>();

    public BedrockAnimation.Animation animation;

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
//                this.rootBones.add(entry.getValue());
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

        for (var element : model.bones()) {
            // Only create the root bones, others will be created recursively.
            if (element.parent() != null) continue;
            this.rootBones.add(createBone(model.bones(), element));
        }

        System.out.println("done");
    }

    private Bone createBone(@NotNull List<BedrockGeoModel.Bone> bones, @NotNull BedrockGeoModel.Bone element) {
        var children = new ArrayList<Bone>();
        for (var child : bones) {
            if (!element.name().equals(child.parent()))
                continue;
            children.add(createBone(bones, child));
        }

        var transform = new net.hollowcube.multipart.entity.Transform(
                Vec.ZERO,
//                element.pivot().div(16).mul(1, 1, -1),
                element.rotation().mul(1, 1, -1),
                Vec.ONE, element.pivot().div(16).mul(1, 1, -1)
        );

        Bone b;
        if (element.cubes().isEmpty()) {
            b = new Bone(transform, children);
        } else {
            b = new ItemBone(transform, children, "mymap:" + element.name());
            this.leafEntityIds.add(((ItemBone) b).entityId());
        }
        boneById.put(element.name(), b);
        return b;
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

        if (getAliveTicks() % 2 == 0) return;

        var animation = this.animation;
        if (animation == null) return;

        float timeSeconds = (getAliveTicks() * 50f) / 1000f;
        timeSeconds %= (float) animation.animationLength();

        boneById.values().forEach(Bone::reset);

        for (var entry : animation.bones().entrySet()) {
            var bone = this.boneById.get(entry.getKey());
            if (bone == null) {
                continue;
            }

            var animator = entry.getValue();

            var position = animator.position();
            if (!position.isEmpty()) {
                int lastIndex = -1;
                for (int i = position.size() - 1; i >= 0; i--) {
                    // todo binary search here would be better. if we dont need to play in reverse we could also be stateful and a bit smarter.
                    if (timeSeconds > position.get(i).time()) {
                        lastIndex = i;
                        break;
                    }
                }
                if (lastIndex != -1) {
                    int nextIndex = Math.min(position.size() - 1, lastIndex + 1);
                    var lastFrame = position.get(lastIndex);
                    var nextFrame = position.get(nextIndex);
                    float secondsIntoKeyframe = timeSeconds - (float) lastFrame.time();
                    float t;
                    if (lastIndex != nextIndex) {
                        t = (float) Math.clamp(secondsIntoKeyframe / (nextFrame.time() - lastFrame.time()), 0.0F, 1.0F);
                    } else {
                        t = 0.0F;
                    }

                    var currentPos = lastFrame.value().lerp(nextFrame.value(), t);
                    bone.offsetPosition(currentPos.div(16).mul(1, 1, -1));
                    if ("laserprinter2".equals(entry.getKey())) {
                        System.out.println(currentPos);
                    }
                }
            }

            var rotation = animator.rotation();
            if (!rotation.isEmpty()) {
                int lastIndex = -1;
                for (int i = rotation.size() - 1; i >= 0; i--) {
                    // todo binary search here would be better. if we dont need to play in reverse we could also be stateful and a bit smarter.
                    if (timeSeconds > rotation.get(i).time()) {
                        lastIndex = i;
                        break;
                    }
                }
                if (lastIndex != -1) {
                    int nextIndex = Math.min(rotation.size() - 1, lastIndex + 1);
                    var lastFrame = rotation.get(lastIndex);
                    var nextFrame = rotation.get(nextIndex);
                    float secondsIntoKeyframe = timeSeconds - (float) lastFrame.time();
                    float t;
                    if (lastIndex != nextIndex) {
                        t = (float) Math.clamp(secondsIntoKeyframe / (nextFrame.time() - lastFrame.time()), 0.0F, 1.0F);
                    } else {
                        t = 0.0F;
                    }

                    var currentPos = lastFrame.value().lerp(nextFrame.value(), t);
                    bone.offsetRotation(currentPos.mul(1, -1, -1));
                }
            }

            var scale = animator.scale();
            if (!scale.isEmpty()) {
                int lastIndex = -1;
                for (int i = scale.size() - 1; i >= 0; i--) {
                    // todo binary search here would be better. if we dont need to play in reverse we could also be stateful and a bit smarter.
                    if (timeSeconds > scale.get(i).time()) {
                        lastIndex = i;
                        break;
                    }
                }
                if (lastIndex != -1) {
                    int nextIndex = Math.min(scale.size() - 1, lastIndex + 1);
                    var lastFrame = scale.get(lastIndex);
                    var nextFrame = scale.get(nextIndex);
                    float secondsIntoKeyframe = timeSeconds - (float) lastFrame.time();
                    float t;
                    if (lastIndex != nextIndex) {
                        t = (float) Math.clamp(secondsIntoKeyframe / (nextFrame.time() - lastFrame.time()), 0.0F, 1.0F);
                    } else {
                        t = 0.0F;
                    }

                    var currentPos = lastFrame.value().lerp(nextFrame.value(), t);
                    bone.offsetScale(currentPos);
                }
            }
        }

        for (var viewer : getViewers()) {
            for (var root : this.rootBones) {
                root.updateFor(viewer, new Transform.Mutable());
            }
        }
    }
}

package net.hollowcube.aj.entity;

import net.hollowcube.aj.Node;
import net.kyori.adventure.text.Component;
import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/*

Bones:
- parent
- entity (text/other display in the future)
  - has associated entity
- camera?
  - has associated entity
- locator?


ALL have transformation
- invalidation of positions to indicate it needs to be resent?

 */

public interface Bone {

    @NotNull Node node();

    void setAnimator(@NotNull Animator animator);

    /// Spawn the bone and any children for the given player, accumulating spawned entity IDs in the provided list.
    void spawn(@NotNull Player player);

    /// Update the bone's state for the viewers of the given Viewable.
    void update(@NotNull Viewable self, @NotNull Vec parentTranslation, @NotNull Vec parentScale);

    class ParentBone implements Bone {
        private final Node node;
        private final List<Bone> children = new ArrayList<>();

        private Animator animator = Animator.NOOP;

        public ParentBone(@NotNull Node node) {
            this.node = node;
        }

        public @NotNull Node node() {
            return node;
        }

        @Override
        public void setAnimator(@NotNull Animator animator) {
            this.animator = Objects.requireNonNull(animator);
        }

        public void addChild(@NotNull Bone child) {
            children.add(Objects.requireNonNull(child));
        }

        @Override
        public void spawn(@NotNull Player player) {
            for (Bone child : children) {
                child.spawn(player);
            }
        }

        @Override
        public void update(@NotNull Viewable self, @NotNull Vec parentTranslation, @NotNull Vec parentScale) {
            animator.tick();

            Vec newTranslation = parentTranslation.add(animator.translation());
            Vec newScale = parentScale.mul(animator.scale());

            for (Bone child : children) {
                child.update(self, newTranslation, newScale);
            }
        }
    }

    class EntityBone implements Bone {
        private static final AtomicInteger ID_COUNTER = new AtomicInteger(-50_000_000);

        private final int entityId = ID_COUNTER.getAndIncrement();
        private final Map<Integer, Metadata.Entry<?>> spawnMetadata;

        private final Node node;

        private Animator animator = Animator.NOOP;

        public EntityBone(@NotNull Node node) {
            this.spawnMetadata = Objects.requireNonNull(createSpawnMetadata(node));
            this.node = node;
        }

        public int entityId() {
            return entityId;
        }

        @Override
        public @NotNull Node node() {
            return this.node;
        }

        @Override
        public void setAnimator(@NotNull Animator animator) {
            this.animator = Objects.requireNonNull(animator);
        }

        @Override
        public void spawn(@NotNull Player player) {
            player.sendPacket(new SpawnEntityPacket(entityId, UUID.randomUUID(), EntityType.TEXT_DISPLAY.id(),
                    Pos.ZERO, (float) 0, 0, (short) 0, (short) 0, (short) 0));
            player.sendPacket(new EntityMetaDataPacket(entityId, spawnMetadata));
        }

        @Override
        public void update(@NotNull Viewable self, @NotNull Vec parentTranslation, @NotNull Vec parentScale) {
            animator.tick();

            Vec newTranslation = node.base().defaultTransform().decomposed().translation().add(parentTranslation).add(animator.translation());
            Vec newScale = node.base().defaultTransform().decomposed().scale().mul(parentScale).mul(animator.scale());

            self.sendPacketToViewers(new EntityMetaDataPacket(entityId, Map.of(
                    MetadataDef.Display.TRANSLATION.index(), Metadata.Vector3(newTranslation),
                    MetadataDef.Display.SCALE.index(), Metadata.Vector3(newScale),
                    MetadataDef.Display.INTERPOLATION_DELAY.index(), Metadata.VarInt(0)
            )));
        }


        private static @Nullable Map<Integer, Metadata.Entry<?>> createSpawnMetadata(@NotNull Node node) {
            return switch (node) {
                case Node.TextDisplay text -> Map.of(
                        MetadataDef.Display.WIDTH.index(), Metadata.Float(3.0f), // todo what should actual value be
                        // todo width and height can come from the bounding box described in the model file. This works because we spawn all the entities riding the root interaction entity.
                        MetadataDef.Display.HEIGHT.index(), Metadata.Float(3.0f), // todo what should actual value be
                        MetadataDef.Display.TRANSFORMATION_INTERPOLATION_DURATION.index(), Metadata.VarInt(1),
                        MetadataDef.Display.TRANSLATION.index(), Metadata.Vector3(node.base().defaultTransform().decomposed().translation()),
                        MetadataDef.Display.ROTATION_LEFT.index(), Metadata.Quaternion(node.base().defaultTransform().decomposed().leftRotation()),
                        MetadataDef.Display.SCALE.index(), Metadata.Vector3(node.base().defaultTransform().decomposed().scale()),
                        MetadataDef.TextDisplay.TEXT.index(), Metadata.Chat(Component.text(text.text())),
                        MetadataDef.TextDisplay.BACKGROUND_COLOR.index(), Metadata.VarInt(0) // todo what should actual value be
                );
                case Node.Bone bone -> null; //todo
                case Node.Locator bone -> null; //todo
                case Node.Struct _ -> null;
            };
        }
    }
}

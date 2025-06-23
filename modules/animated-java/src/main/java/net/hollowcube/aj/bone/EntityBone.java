package net.hollowcube.aj.bone;

import net.hollowcube.aj.Node;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityBone extends AbstractBone {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(-50_000_000);

    private final int entityId = ID_COUNTER.getAndIncrement();
    private final EntityType entityType;
    private final Map<Integer, Metadata.Entry<?>> baseMetadata;

    public EntityBone(@NotNull Node node) {
        this.entityType = node instanceof Node.TextDisplay ? EntityType.TEXT_DISPLAY : EntityType.ITEM_DISPLAY;
        this.baseMetadata = Objects.requireNonNull(createSpawnMetadata(node));
    }

    public int entityId() {
        return entityId;
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        player.sendPacket(new SpawnEntityPacket(entityId, UUID.randomUUID(), this.entityType.id(),
                new Pos(0, 0, 0, -180, 0), (float) 0, 0, (short) 0, (short) 0, (short) 0));
        player.sendPacket(new EntityMetaDataPacket(entityId, this.baseMetadata));

        super.updateNewViewer(player);
    }

    private static @Nullable Map<Integer, Metadata.Entry<?>> createSpawnMetadata(@NotNull Node node) {
        return switch (node) {
            case Node.TextDisplay text -> Map.of(
//                    MetadataDef.Display.WIDTH.index(), Metadata.Float(3.0f), // todo what should actual value be
                    // todo width and height can come from the bounding box described in the model file. This works because we spawn all the entities riding the root interaction entity.
//                    MetadataDef.Display.HEIGHT.index(), Metadata.Float(3.0f), // todo what should actual value be
//                    MetadataDef.Display.TRANSFORMATION_INTERPOLATION_DURATION.index(), Metadata.VarInt(1),
                    MetadataDef.Display.TRANSLATION.index(), Metadata.Vector3(node.base().defaultTransform().decomposed().translation()),
//                    MetadataDef.Display.ROTATION_LEFT.index(), Metadata.Quaternion(node.base().defaultTransform().decomposed().leftRotation()),
                    MetadataDef.Display.SCALE.index(), Metadata.Vector3(node.base().defaultTransform().decomposed().scale()),
                    MetadataDef.TextDisplay.TEXT.index(), Metadata.Chat(Component.text(text.text())),
                    MetadataDef.TextDisplay.BACKGROUND_COLOR.index(), Metadata.VarInt(0) // todo what should actual value be
            );
            case Node.Bone _ -> Map.of(
//                    MetadataDef.Display.WIDTH.index(), Metadata.Float(3.0f), // todo what should actual value be
                    // todo width and height can come from the bounding box described in the model file. This works because we spawn all the entities riding the root interaction entity.
//                    MetadataDef.Display.HEIGHT.index(), Metadata.Float(3.0f), // todo what should actual value be
//                    MetadataDef.Display.TRANSFORMATION_INTERPOLATION_DURATION.index(), Metadata.VarInt(1),
                    MetadataDef.Display.TRANSLATION.index(), Metadata.Vector3(node.base().defaultTransform().decomposed().translation()),
//                    MetadataDef.Display.ROTATION_LEFT.index(), Metadata.Quaternion(node.base().defaultTransform().decomposed().leftRotation()),
                    MetadataDef.Display.SCALE.index(), Metadata.Vector3(node.base().defaultTransform().decomposed().scale()),
                    MetadataDef.ItemDisplay.DISPLAY_TYPE.index(), Metadata.Byte((byte) ItemDisplayMeta.DisplayContext.HEAD.ordinal()),
                    MetadataDef.ItemDisplay.DISPLAYED_ITEM.index(), Metadata.ItemStack(ItemStack.of(Material.STICK)
                            .with(DataComponents.ITEM_MODEL, "mymap:" + node.name()))
            );
            case Node.Locator _ -> null;
            case Node.Struct _ -> null;
        };
    }
}

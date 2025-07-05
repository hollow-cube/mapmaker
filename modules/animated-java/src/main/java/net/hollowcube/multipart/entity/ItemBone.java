package net.hollowcube.multipart.entity;

import net.hollowcube.aj.util.Quaternion;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ItemBone extends Bone {
    private final int entityId = Bone.ENTITY_ID_COUNTER.getAndIncrement();
    private final String model;

    public ItemBone(@NotNull Transform defaultTransform, @NotNull List<Bone> children, @NotNull String model) {
        super(defaultTransform, children);
        this.model = model;
    }

    public int entityId() {
        return entityId;
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        // TODO: why do we invert the yaw? I dont remember
        var spawnPosition = player.getPosition().withView(-180, 0);
        player.sendPacket(new SpawnEntityPacket(entityId, UUID.randomUUID(), EntityType.ITEM_DISPLAY.id(),
                spawnPosition, (float) 0, 0, (short) 0, (short) 0, (short) 0));
        // TODO: if we want to get fancy we could probably make the packet buffer directly and avoid all the allocation here
        //       using a bufferedpacket/PacketWriting. would need to bench if its actually worth it.
        player.sendPacket(new EntityMetaDataPacket(entityId, Map.of(
                MetadataDef.Display.TRANSFORMATION_INTERPOLATION_DURATION.index(), Metadata.VarInt(2),
                MetadataDef.Display.TRANSLATION.index(), Metadata.Vector3(new Vec(dx, dy, dz)),
                MetadataDef.Display.ROTATION_LEFT.index(), Metadata.Quaternion(Quaternion.fromEulerAngles(
                        rx + defaultTransform.rx(),
                        ry + defaultTransform.ry(),
                        rz + defaultTransform.rz()
                )),
                MetadataDef.Display.SCALE.index(), Metadata.Vector3(new Vec(sx, sy, sz)),
                MetadataDef.ItemDisplay.DISPLAY_TYPE.index(), Metadata.Byte((byte) ItemDisplayMeta.DisplayContext.HEAD.ordinal()),
                MetadataDef.ItemDisplay.DISPLAYED_ITEM.index(), Metadata.ItemStack(ItemStack.of(Material.STICK)
                        .with(DataComponents.ITEM_MODEL, model))
        )));

        super.updateNewViewer(player);
    }

    @Override
    protected void sendUpdates(@NotNull Player player, @NotNull Vec translation, @NotNull Quaternion rotation) {
        player.sendPacket(new EntityMetaDataPacket(entityId, Map.of(
                MetadataDef.Display.INTERPOLATION_DELAY.index(), Metadata.VarInt(0),
                MetadataDef.Display.TRANSLATION.index(), Metadata.Vector3(translation),
                MetadataDef.Display.ROTATION_LEFT.index(), Metadata.Quaternion(rotation.into()),
                MetadataDef.Display.SCALE.index(), Metadata.Vector3(new Vec(sx, sy, sz))
        )));
    }

}

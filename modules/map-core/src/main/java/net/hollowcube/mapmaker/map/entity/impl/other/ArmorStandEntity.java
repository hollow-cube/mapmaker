package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Equippable;
import net.minestom.server.sound.SoundEvent;

import java.util.UUID;

import static net.hollowcube.mapmaker.map.util.NbtUtilV2.readFloat3;
import static net.hollowcube.mapmaker.map.util.NbtUtilV2.writeFloat3;

public class ArmorStandEntity extends AbstractLivingEntity<ArmorStandMeta> {

    public static final MapEntityInfo<ArmorStandEntity> INFO = MapEntityInfo.<ArmorStandEntity>builder(AbstractLivingEntity.INFO)
        .with("Small", MapEntityInfoType.Bool(false, ArmorStandMeta::setSmall, ArmorStandMeta::isSmall))
        .with("Has Arms", MapEntityInfoType.Bool(false, ArmorStandMeta::setHasArms, ArmorStandMeta::isHasArms))
        .with("No Baseplate", MapEntityInfoType.Bool(false, ArmorStandMeta::setHasNoBasePlate, ArmorStandMeta::isHasNoBasePlate))
        .build();

    public ArmorStandEntity(UUID uuid) {
        super(EntityType.ARMOR_STAND, uuid);
    }

    @Override
    public void onBuildLeftClick(MapWorld world, Player player) {
        playSound(SoundEvent.ENTITY_ARMOR_STAND_BREAK, 1, 1);
        remove();
    }

    @Override
    public void onBuildRightClick(MapWorld world, Player player, PlayerHand hand, Point point) {
        var item = player.getItemInHand(hand);
        var meta = getEntityMeta();

        if (item.isAir()) {
            var slot = EquipmentSlot.MAIN_HAND;
            if (point.y() >= 0.1 && point.y() < 0.55 && !getEquipment(EquipmentSlot.BOOTS).isAir()) {
                slot = EquipmentSlot.BOOTS;
            } else if (point.y() >= 0.9 && point.y() < 1.6 && !getEquipment(EquipmentSlot.CHESTPLATE).isAir()) {
                slot = EquipmentSlot.CHESTPLATE;
            } else if (point.y() >= 0.4 && point.y() < 1.2 && !getEquipment(EquipmentSlot.LEGGINGS).isAir()) {
                slot = EquipmentSlot.LEGGINGS;
            } else if (point.y() >= 1.6 && !getEquipment(EquipmentSlot.HELMET).isAir()) {
                slot = EquipmentSlot.HELMET;
            } else if (getEquipment(EquipmentSlot.MAIN_HAND).isAir() && !getEquipment(EquipmentSlot.OFF_HAND).isAir()) {
                slot = EquipmentSlot.OFF_HAND;
            }
            if (slot.isHand() && !meta.isHasArms()) return;
            setEquipment(slot, ItemStack.AIR);
        } else {
            var slot = OpUtils.mapOr(item.get(DataComponents.EQUIPPABLE), Equippable::slot, EquipmentSlot.MAIN_HAND);
            if (slot.isHand() && !meta.isHasArms()) return;
            setEquipment(slot, item.withAmount(1));
        }
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        final ArmorStandMeta meta = getEntityMeta();
        if (meta.isInvisible()) tag.putBoolean("Invisible", true);
        if (meta.isMarker()) tag.putBoolean("Marker", true);
        if (meta.isHasNoBasePlate()) tag.putBoolean("NoBasePlate", true);

        var pose = CompoundBinaryTag.builder();
        if (!meta.getBodyRotation().isZero()) pose.put("Body", writeFloat3(meta.getBodyRotation()));
        if (!meta.getHeadRotation().isZero()) pose.put("Head", writeFloat3(meta.getHeadRotation()));
        if (!meta.getLeftArmRotation().isZero()) pose.put("LeftArm", writeFloat3(meta.getLeftArmRotation()));
        if (!meta.getLeftLegRotation().isZero()) pose.put("LeftLeg", writeFloat3(meta.getLeftLegRotation()));
        if (!meta.getRightArmRotation().isZero()) pose.put("RightArm", writeFloat3(meta.getRightArmRotation()));
        if (!meta.getRightLegRotation().isZero()) pose.put("RightLeg", writeFloat3(meta.getRightLegRotation()));
        tag.put("Pose", pose.build());

        if (meta.isHasArms()) tag.putBoolean("ShowArms", true);
        if (meta.isSmall()) tag.putBoolean("Small", true);

    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        final ArmorStandMeta meta = getEntityMeta();
        if (tag.getBoolean("Invisible")) meta.setInvisible(true);
        if (tag.getBoolean("Marker")) meta.setMarker(true);
        if (tag.getBoolean("NoBasePlate")) meta.setHasNoBasePlate(true);

        // /summon armor_stand 1 41 1 {ShowArms:1b,Pose:{Body:[29f,0f,0f],Head:[360f,0f,0f],LeftLeg:[22f,33f,0f],RightLeg:[336f,0f,35f],LeftArm:[26f,63f,346f],RightArm:[11f,0f,32f]}}

        var pose = tag.getCompound("Pose");
        meta.setBodyRotation(readFloat3(pose.get("Body")));
        meta.setHeadRotation(readFloat3(pose.get("Head")));
        meta.setLeftArmRotation(readFloat3(pose.get("LeftArm")));
        meta.setLeftLegRotation(readFloat3(pose.get("LeftLeg")));
        meta.setRightArmRotation(readFloat3(pose.get("RightArm")));
        meta.setRightLegRotation(readFloat3(pose.get("RightLeg")));

        if (tag.getBoolean("ShowArms")) meta.setHasArms(true);
        if (tag.getBoolean("Small")) meta.setSmall(true);

    }
}

package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.other.AllayMeta;

import java.util.UUID;

public class AllayEntity extends AbstractMobEntity<AllayMeta> {

    public static final MapEntityInfo<AllayEntity> INFO = MapEntityInfo.<AllayEntity>builder(AbstractLivingEntity.INFO)
        .with("Dancing", MapEntityInfoType.Bool(false, AllayMeta::setDancing, AllayMeta::isDancing))
        .build();

    private static final String DANCING_KEY = "mapmaker:dancing";

    public AllayEntity(UUID uuid) {
        super(EntityType.ALLAY, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        //mapmaker
        this.getEntityMeta().setDancing(tag.getBoolean(DANCING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        //mapmaker
        tag.putBoolean(DANCING_KEY, this.getEntityMeta().isDancing());
    }

    @Override
    public void onRightClick(MapWorld world, Player player, PlayerHand hand, Point interactPosition) {
        if (!world.canEdit(player)) {
            // Required due to allays item logic being on the client...
            var item = player.getItemInHand(hand);
            player.getInventory().sendSlotRefresh(player.getHeldSlot(), item);
            player.sendPacket(this.getEquipmentsPacket());
        } else {
            this.onBuildRightClick(world, player, hand, interactPosition);
        }
    }


}

package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.villager.VillagerDataUtils;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.entity.metadata.other.EndCrystalMeta;
import net.minestom.server.entity.metadata.villager.VillagerMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EndCrystalEntity extends MapEntity<EndCrystalMeta> {

    public static final MapEntityInfo<EndCrystalEntity> INFO = MapEntityInfo.<EndCrystalEntity>builder()
        .with("Has Bedrock", MapEntityInfoType.Bool(true, EndCrystalMeta::setShowingBottom, EndCrystalMeta::isShowingBottom))
        .build();

    private static final String SHOW_BOTTOM_KEY = "ShowBottom";

    public EndCrystalEntity(UUID uuid) {
        super(EntityType.END_CRYSTAL, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setShowingBottom(tag.getBoolean(SHOW_BOTTOM_KEY, true));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(SHOW_BOTTOM_KEY, this.getEntityMeta().isShowingBottom());
    }

    @Override
    public void onBuildLeftClick(MapWorld world, Player player) {
        this.remove();
    }
}

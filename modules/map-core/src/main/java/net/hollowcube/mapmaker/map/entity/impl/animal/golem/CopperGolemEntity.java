package net.hollowcube.mapmaker.map.entity.impl.animal.golem;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.golem.CopperGolemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CopperGolemEntity extends AbstractMobEntity<CopperGolemMeta> {

    public static final MapEntityInfo<@NotNull CopperGolemEntity> INFO = MapEntityInfo.<CopperGolemEntity>builder(AbstractLivingEntity.INFO)
        .with("Weathering", MapEntityInfoType.Enum(CopperGolemMeta.WeatherState.class, CopperGolemMeta.WeatherState.UNAFFECTED, CopperGolemMeta::setWeatherState, CopperGolemMeta::getWeatherState))
        .with("State", MapEntityInfoType.Enum(CopperGolemMeta.State.class, CopperGolemMeta.State.IDLE, CopperGolemMeta::setState, CopperGolemMeta::getState))
        .build();

    private static final String WEATHER_STATE_KEY = "weather_state";
    private static final String STATE_KEY = "mapmaker:state";

    public CopperGolemEntity(@NotNull UUID uuid) {
        super(EntityType.COPPER_GOLEM, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setWeatherState(NbtUtilV2.readStringEnum(tag.get(WEATHER_STATE_KEY), CopperGolemMeta.WeatherState.class));
        this.getEntityMeta().setState(NbtUtilV2.readStringEnum(tag.get(STATE_KEY), CopperGolemMeta.State.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(WEATHER_STATE_KEY, NbtUtilV2.writeStringEnum(this.getEntityMeta().getWeatherState()));
        tag.put(STATE_KEY, NbtUtilV2.writeStringEnum(this.getEntityMeta().getState()));
    }
}

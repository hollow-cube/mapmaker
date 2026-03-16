package net.hollowcube.mapmaker.map.entity.impl.animal.equine;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractChestedHorseEntity;
import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.LlamaMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LlamaEntity extends AbstractChestedHorseEntity<LlamaMeta> {

    public static final MapEntityInfo<@NotNull LlamaEntity> INFO = MapEntityInfo.<LlamaEntity>builder(AbstractChestedHorseEntity.INFO)
        .with("Variant", MapEntityInfoType.Enum(LlamaMeta.Variant.class, LlamaMeta.Variant.CREAMY, DataComponents.LLAMA_VARIANT))
        .with("Carpet", CommonMapEntityInfoTypes.DyeBodyArmor("_carpet"))
        .build();

    private static final String VARIANT_KEY = "Variant";

    public LlamaEntity(@NotNull UUID uuid) {
        super(EntityType.LLAMA, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.LLAMA_VARIANT, NbtUtilV2.readIntEnum(tag.get(VARIANT_KEY), LlamaMeta.Variant.class));
    }

    @Override
    public void writeData(@NotNull CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(VARIANT_KEY, NbtUtilV2.writeIntEnum(this.get(DataComponents.LLAMA_VARIANT, LlamaMeta.Variant.CREAMY)));
    }
}

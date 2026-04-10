package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.PandaMeta;

import java.util.UUID;

public class PandaEntity extends AbstractAgeableEntity<PandaMeta> {

    public static final MapEntityInfo<PandaEntity> INFO = MapEntityInfo.<PandaEntity>builder(AbstractAgeableEntity.INFO)
        .with("Sitting", MapEntityInfoType.Bool(false, PandaMeta::setSitting, PandaMeta::isSitting))
        .with("Variant", MapEntityInfoType.Enum(PandaMeta.Gene.class, PandaMeta.Gene.BROWN, (meta, gene) -> {
            meta.setMainGene(gene);
            meta.setHiddenGene(gene);
        }, PandaMeta::getMainGene))
        .build();

    private static final String MAIN_GENE_KEY = "MainGene";
    private static final String HIDDEN_GENE_KEY = "HiddenGene";

    public PandaEntity(UUID uuid) {
        super(EntityType.PANDA, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setMainGene(NbtUtilV2.readStringEnum(tag.get(MAIN_GENE_KEY), PandaMeta.Gene.class));
        this.getEntityMeta().setHiddenGene(NbtUtilV2.readStringEnum(tag.get(HIDDEN_GENE_KEY), PandaMeta.Gene.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(MAIN_GENE_KEY, NbtUtilV2.writeStringEnum(this.getEntityMeta().getMainGene()));
        tag.put(HIDDEN_GENE_KEY, NbtUtilV2.writeStringEnum(this.getEntityMeta().getHiddenGene()));
    }

}

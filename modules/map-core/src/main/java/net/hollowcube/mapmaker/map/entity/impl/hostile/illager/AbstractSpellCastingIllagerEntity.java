package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractMobEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.AbstractIllagerMeta;
import net.minestom.server.entity.metadata.monster.raider.SpellcasterIllagerMeta;

import java.util.UUID;

public class AbstractSpellCastingIllagerEntity<M extends SpellcasterIllagerMeta> extends AbstractIllagerEntity<M> {

    public static final MapEntityInfo<AbstractSpellCastingIllagerEntity<? extends SpellcasterIllagerMeta>> INFO = MapEntityInfo.<AbstractSpellCastingIllagerEntity<? extends SpellcasterIllagerMeta>>builder(AbstractIllagerEntity.INFO)
        .with("Spell", MapEntityInfoType.Enum(SpellcasterIllagerMeta.Spell.class, SpellcasterIllagerMeta.Spell.NONE, SpellcasterIllagerMeta::setSpell, SpellcasterIllagerMeta::getSpell))
        .build();

    private static final String SPELL_KEY = "mapmaker:spell";

    protected AbstractSpellCastingIllagerEntity(EntityType type, UUID uuid) {
        super(type, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setSpell(NbtUtilV2.readStringEnum(tag.get(SPELL_KEY), SpellcasterIllagerMeta.Spell.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(SPELL_KEY, NbtUtilV2.writeStringEnum(this.getEntityMeta().getSpell()));
    }
}

package net.hollowcube.mapmaker.map.entity.impl.villager;

import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.dialog.DialogInput;
import net.minestom.server.entity.VillagerProfession;
import net.minestom.server.entity.VillagerType;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.villager.VillagerMeta;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class VillagerDataUtils {

    private static final Codec<VillagerMeta.Level> LEVEL_CODEC = Codec.INT.transform(
        level -> switch (level) {
            case 2 -> VillagerMeta.Level.APPRENTICE;
            case 3 -> VillagerMeta.Level.JOURNEYMAN;
            case 4 -> VillagerMeta.Level.EXPERT;
            case 5 -> VillagerMeta.Level.MASTER;
            default -> VillagerMeta.Level.NOVICE;
        },
        level -> switch (level) {
            case NOVICE -> 1;
            case APPRENTICE -> 2;
            case JOURNEYMAN -> 3;
            case EXPERT -> 4;
            case MASTER -> 5;
        }
    );
    public static final Codec<VillagerMeta.VillagerData> CODEC = StructCodec.struct(
        "type", VillagerType.CODEC.optional(VillagerType.PLAINS), VillagerMeta.VillagerData::type,
        "profession", VillagerProfession.NBT_TYPE.optional(VillagerProfession.NONE), VillagerMeta.VillagerData::profession,
        "level", LEVEL_CODEC.optional(VillagerMeta.Level.NOVICE), VillagerMeta.VillagerData::level,
        VillagerMeta.VillagerData::new
    );

    public static VillagerMeta.VillagerData read(@Nullable BinaryTag tag, VillagerMeta.VillagerData fallback) {
        if (tag == null) return fallback;
        return CODEC.decode(Transcoder.NBT, tag).orElse(fallback);
    }

    public static @Nullable BinaryTag write(VillagerMeta.VillagerData data) {
        return CODEC.encode(Transcoder.NBT, data).orElse(null);
    }

    public static <M extends EntityMeta, E extends MapEntity<M>> MapEntityInfoType<VillagerProfession, E> ProfessionInfoType(
        BiConsumer<M, VillagerProfession> setter,
        Function<M, VillagerProfession> getter
    ) {
        return new ProfessionInfoType<>(VillagerProfession.NONE, setter, getter);
    }

    private record ProfessionInfoType<M extends EntityMeta, E extends MapEntity<M>>(
        VillagerProfession fallback,
        BiConsumer<M, VillagerProfession> setter,
        Function<M, VillagerProfession> getter
    ) implements MapEntityInfoType<VillagerProfession, E> {

        @Override
        public VillagerProfession get(E entity) {
            return this.getter().apply(entity.getEntityMeta());
        }

        @Override
        public void set(E entity, VillagerProfession value) {
            this.setter().accept(entity.getEntityMeta(), value);
        }

        @Override
        public DialogInput toInput(E entity, String key, String label) {
            var current = this.get(entity);
            return new DialogInput.SingleOption(
                key,
                DIALOG_OPTION_WIDTH,
                VillagerProfession.values()
                    .stream()
                    .map(it -> new DialogInput.SingleOption.Option(
                        it.name(),
                        Component.text(it.name()),
                        it.equals(current)
                    ))
                    .toList(),
                Component.text(label),
                true
            );
        }

        @Override
        public void fromInput(E entity, BinaryTag data) {
            if (!(data instanceof StringBinaryTag tag)) return;
            try {
                var value = VillagerProfession.fromKey(Key.key(tag.value()));
                if (value == null) return;
                this.set(entity, value);
            } catch (InvalidKeyException e) {
                // ignore invalid values
            }
        }
    }
}

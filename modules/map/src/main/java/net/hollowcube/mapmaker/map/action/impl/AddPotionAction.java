package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.entity.potion.PotionInfo;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

public class AddPotionAction extends AbstractAction<AddPotionAction.EffectData> {
    public static final AddPotionAction INSTANCE = new AddPotionAction();

    record EffectData(@NotNull PotionInfo effect, int level, int duration) {
        public static final StructCodec<EffectData> CODEC = StructCodec.struct(
                "effect", PotionInfo.CODEC, EffectData::effect,
                "level", ExtraCodecs.clamppedInt(0, 128).optional(0), EffectData::level,
                "duration", Codec.INT, EffectData::duration,
                EffectData::new);
    }

    public AddPotionAction() {
        super("mapmaker:add_potion", EffectData.CODEC);
    }
}

package net.hollowcube.mapmaker.map.feature.play.effect;

import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.ActionRegistry;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record StatusEffectData(
        @NotNull ActionList actions,
        boolean repeatable
) {
    private static final int MAX_ACTIONS = 21;
    public static final StructCodec<StatusEffectData> CODEC = StructCodec.struct(
            "actions", ActionRegistry.listCodec(MAX_ACTIONS), StatusEffectData::actions,
            "repeatable", Codec.BOOLEAN.optional(false), StatusEffectData::repeatable,
            StatusEffectData::new);

    public StatusEffectData {
        actions = new ActionList(actions);
    }

    public StatusEffectData() {
        this(new ActionList(), false);
    }

}

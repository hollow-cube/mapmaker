package net.hollowcube.mapmaker.map.feature.play.effect;

import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.ActionRegistry;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

public record CheckpointEffectDataV2(
        @NotNull ActionList actions
) {
    private static final int MAX_ACTIONS = 21;
    public static final Codec<CheckpointEffectDataV2> CODEC = StructCodec.struct(
            "actions", ActionRegistry.listCodec(MAX_ACTIONS), CheckpointEffectDataV2::actions,
            CheckpointEffectDataV2::new);

    public CheckpointEffectDataV2 {
        actions = new ActionList(actions);
    }

    public CheckpointEffectDataV2() {
        this(new ActionList());
    }

}

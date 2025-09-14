package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.generic.CoordinateActionEditor;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.base.CoordinateAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public record RespawnPosAction(RelativePos target) implements CoordinateAction<RespawnPosAction> {
    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/respawn_position", 2, 2);
    private static final String RELATIVE_ZERO = "~0.0";

    public static final Key KEY = Key.key("mapmaker:respawn_position");
    public static final StructCodec<RespawnPosAction> CODEC = StructCodec.struct(
            StructCodec.INLINE, RelativePos.STRUCT_CODEC.optional(RelativePos.ORIGIN), RespawnPosAction::target,
            RespawnPosAction::new
    );
    public static final Editor<RespawnPosAction> EDITOR = new Editor<>(
            CoordinateActionEditor::new, _ -> SPRITE_DEFAULT,
            RespawnPosAction::makeThumbnail, Set.of(KEY)
    );

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public RespawnPosAction withTarget(RelativePos target) {
        return new RespawnPosAction(target);
    }

    private static TranslatableComponent makeThumbnail(@Nullable RespawnPosAction action) {
        if (action == null) return Component.translatable("gui.action.respawn_position.thumbnail.empty");
        return Component.translatable("gui.action.respawn_position.thumbnail", List.of(
                tildeOnly(action.target.x()), tildeOnly(action.target.y()), tildeOnly(action.target.z()),
                tildeOnly(action.target.yaw()), tildeOnly(action.target.pitch())
        ));
    }

    private static Component tildeOnly(String value) {
        return Component.text(RELATIVE_ZERO.equals(value) ? "~" : value);
    }
}

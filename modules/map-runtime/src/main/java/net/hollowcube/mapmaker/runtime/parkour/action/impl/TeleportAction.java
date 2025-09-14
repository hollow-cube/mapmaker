package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.generic.CoordinateActionEditor;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.base.CoordinateAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public record TeleportAction(RelativePos target) implements CoordinateAction<TeleportAction> {
    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/teleport", 3, 3);
    private static final String RELATIVE_ZERO = "~0.0";
    private static final Sound TELEPORT_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 0.5f, 1f);

    public static final Key KEY = Key.key("mapmaker:teleport");
    public static final StructCodec<TeleportAction> CODEC = StructCodec.struct(
            StructCodec.INLINE, RelativePos.STRUCT_CODEC.optional(RelativePos.ORIGIN), TeleportAction::target,
            TeleportAction::new);
    public static final Action.Editor<TeleportAction> EDITOR = new Action.Editor<>(
            CoordinateActionEditor::new, _ -> SPRITE_DEFAULT,
            TeleportAction::makeThumbnail, Set.of(KEY)
    );

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        player.teleport(target.pos(), Vec.ZERO, null, target.flags());
        player.playSound(TELEPORT_SOUND, target.resolve(player.getPosition()));
    }

    @Override
    public TeleportAction withTarget(RelativePos target) {
        return new TeleportAction(target);
    }

    private static TranslatableComponent makeThumbnail(@Nullable TeleportAction action) {
        if (action == null) return Component.translatable("gui.action.teleport.thumbnail.empty");
        return Component.translatable("gui.action.teleport.thumbnail", List.of(
                tildeOnly(action.target.x()), tildeOnly(action.target.y()), tildeOnly(action.target.z()),
                tildeOnly(action.target.yaw()), tildeOnly(action.target.pitch())
        ));
    }

    private static Component tildeOnly(String value) {
        return Component.text(RELATIVE_ZERO.equals(value) ? "~" : value);
    }
}

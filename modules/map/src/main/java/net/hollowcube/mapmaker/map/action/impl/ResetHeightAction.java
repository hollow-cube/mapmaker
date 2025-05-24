package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.gui.ActionEditorAnvil;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ResetHeightAction(
        int value
) implements Action {
    private static final int NO_RESET_HEIGHT = Integer.MIN_VALUE;

    private static final Sprite SPRITE = new Sprite("action/icon/reset_height", 2, 2);

    public static final Key KEY = Key.key("mapmaker:reset_height");
    public static final StructCodec<ResetHeightAction> CODEC = StructCodec.struct(
            "value", Codec.INT.optional(NO_RESET_HEIGHT), ResetHeightAction::value,
            ResetHeightAction::new);
    public static final Editor<ResetHeightAction> EDITOR = new Editor<>(
            ResetHeightAction::makeEditor, SPRITE, ResetHeightAction::makeThumbnail);

    public @NotNull ResetHeightAction withValue(int value) {
        return new ResetHeightAction(value);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        state.set(Attachments.RESET_HEIGHT, value == NO_RESET_HEIGHT ? null : value);
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable ResetHeightAction action) {
        return action == null
                ? Component.translatable("gui.action.reset_height.thumbnail.clear")
                : Component.translatable("gui.action.reset_height.thumbnail", List.of(
                Component.text(action.value)
        ));
    }

    private static @NotNull Panel makeEditor(@NotNull ActionList.Ref ref) {
        return new ActionEditorAnvil<>(ref, ResetHeightAction::valueToString, ResetHeightAction::stringToValue);
    }

    private static @NotNull String valueToString(@NotNull ResetHeightAction action) {
        return String.valueOf(action.value);
    }

    private static @NotNull ResetHeightAction stringToValue(@NotNull ResetHeightAction action, @NotNull String value) {
        return action.withValue(Integer.parseInt(value));
    }

}

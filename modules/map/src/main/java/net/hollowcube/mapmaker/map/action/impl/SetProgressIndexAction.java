package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
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
import java.util.Set;

public record SetProgressIndexAction(
        int value
) implements Action {
    private static final Sprite SPRITE = new Sprite("action/icon/progress_index", 2, 2);

    public static final Key KEY = Key.key("mapmaker:progress_index");
    public static final StructCodec<SetProgressIndexAction> CODEC = StructCodec.struct(
            "value", Codec.INT.optional(0), SetProgressIndexAction::value,
            SetProgressIndexAction::new);
    public static final Action.Editor<SetProgressIndexAction> EDITOR = new Action.Editor<>(
            SetProgressIndexAction::makeEditor, _ -> SPRITE,
            SetProgressIndexAction::makeThumbnail, Set.of(SetProgressIndexAction.KEY));

    public @NotNull SetProgressIndexAction withValue(int value) {
        return new SetProgressIndexAction(value);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        boolean useProgressAddition = MapWorld.forPlayer(player).map().getSetting(MapSettings.PROGRESS_INDEX_ADDITION);
        state.set(Attachments.PROGRESS_INDEX, useProgressAddition ? (state.get(Attachments.PROGRESS_INDEX, 0) + value) : value);
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable SetProgressIndexAction action) {
        return action == null || action.value == 0
                ? Component.translatable("gui.action.progress_index.thumbnail.clear")
                : Component.translatable("gui.action.progress_index.thumbnail", List.of(
                Component.text(action.value)
        ));
    }

    private static @NotNull Panel makeEditor(@NotNull ActionList.Ref ref) {
        return new ActionEditorAnvil<>(ref, SetProgressIndexAction::valueToString, SetProgressIndexAction::stringToValue);
    }

    private static @NotNull String valueToString(@NotNull SetProgressIndexAction action) {
        return action.value == 0 ? "" : String.valueOf(action.value);
    }

    private static @NotNull SetProgressIndexAction stringToValue(@NotNull SetProgressIndexAction action, @NotNull String value) {
        return action.withValue(Integer.parseInt(value));
    }

}

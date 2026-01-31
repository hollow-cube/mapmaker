package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorAnvil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
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

    public SetProgressIndexAction withValue(int value) {
        return new SetProgressIndexAction(value);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        boolean useProgressAddition = OpUtils.mapOr(MapWorld.forPlayer(player),
                world -> world.map().getSetting(MapSettings.PROGRESS_INDEX_ADDITION),
                false);
        state.set(Attachments.PROGRESS_INDEX, useProgressAddition ? (state.get(Attachments.PROGRESS_INDEX, 0) + value) : value);
    }

    private static TranslatableComponent makeThumbnail(@Nullable SetProgressIndexAction action) {
        return action == null || action.value == 0
                ? Component.translatable("gui.action.progress_index.thumbnail.clear")
                : Component.translatable("gui.action.progress_index.thumbnail", List.of(
                Component.text(action.value)
        ));
    }

    private static Panel makeEditor(ActionList.Ref ref) {
        return new ActionEditorAnvil<>(ref, SetProgressIndexAction::valueToString, SetProgressIndexAction::stringToValue) {
            @Override
            protected SetProgressIndexAction parse(SetProgressIndexAction data, String text) {
                try {
                    return super.parse(data, text);
                } catch (NumberFormatException _) {
                    host.player().sendMessage(Component.translatable("create_maps.checkpoint.progress_index.nan"));
                    host.player().closeInventory();
                    return data.withValue(0);
                }
            }
        };
    }

    private static String valueToString(SetProgressIndexAction action) {
        return action.value == 0 ? "" : String.valueOf(action.value);
    }

    private static SetProgressIndexAction stringToValue(SetProgressIndexAction action, String value) {
        return action.withValue(Integer.parseInt(value));
    }

}

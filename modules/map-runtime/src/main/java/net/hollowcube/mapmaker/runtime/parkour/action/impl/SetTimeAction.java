package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.map.setting.TimeOfDay;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.text.Component.translatable;

public record SetTimeAction(TimeOfDay time) implements Action {
    private static final Sprite SPRITE_NOON = new Sprite("icon2/1_1/time_noon", 1, 1);
    private static final Sprite SPRITE_SUNSET = new Sprite("icon2/1_1/time_sunset", 1, 1);
    private static final Sprite SPRITE_NIGHT = new Sprite("icon2/1_1/time_night", 1, 1);
    private static final Sprite SPRITE_SUNRISE = new Sprite("icon2/1_1/time_sunrise", 1, 1);

    public static final Key KEY = key("mapmaker:set_time");
    public static final StructCodec<SetTimeAction> CODEC = StructCodec.struct(
        "time", TimeOfDay.CODEC.optional(TimeOfDay.NOON), SetTimeAction::time,
        SetTimeAction::new);
    public static final Action.Editor<SetTimeAction> EDITOR = new Action.Editor<>(
        SetTimeAction.Editor::new, SetTimeAction::makeSprite,
        SetTimeAction::makeThumbnail, Set.of(KEY));
    public static final PlayState.Attachment<TimeOfDay> SAVE_DATA = PlayState.attachment(key("mapmaker:time_of_day"), TimeOfDay.CODEC);

    public SetTimeAction withTime(TimeOfDay time) {
        return new SetTimeAction(time);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        state.set(SAVE_DATA, time);
    }

    private static Sprite makeSprite(@Nullable SetTimeAction action) {
        if (action == null) return SPRITE_NOON;
        return iconSprite(action.time);
    }

    private static TranslatableComponent makeThumbnail(@Nullable SetTimeAction action) {
        if (action == null) return translatable("gui.action.set_time_of_day.thumbnail.empty");
        return translatable("gui.action.set_time_of_day.thumbnail", List.of(
            translatable("gui.action.set_time_of_day." + action.time.name().toLowerCase() + ".label")
        ));
    }

    private static Sprite iconSprite(TimeOfDay time) {
        return switch (time) {
            case NOON -> SPRITE_NOON;
            case SUNSET -> SPRITE_SUNSET;
            case NIGHT -> SPRITE_NIGHT;
            case SUNRISE -> SPRITE_SUNRISE;
        };
    }

    private static class Editor extends AbstractActionEditorPanel<SetTimeAction> {

        private final Switch timeSwitch;

        public Editor(ActionList.Ref ref) {
            super(ref);

            background("action/editor/container_sm", -10, -31);

            add(1, 1, groupText(7, "time"));

            this.timeSwitch = add(1, 2, new Switch(7, 1, List.of(
                new Row(TimeOfDay.NOON, update(SetTimeAction::withTime)),
                new Row(TimeOfDay.SUNSET, update(SetTimeAction::withTime)),
                new Row(TimeOfDay.NIGHT, update(SetTimeAction::withTime)),
                new Row(TimeOfDay.SUNRISE, update(SetTimeAction::withTime))
            )));
        }

        @Override
        protected void update(SetTimeAction data) {
            this.timeSwitch.select(data.time().ordinal());
        }

        private static class Row extends Panel {
            public Row(TimeOfDay value, Consumer<TimeOfDay> onChange) {
                super(7, 1);

                var translationKey = "gui.action.set_time_of_day." + value.name().toLowerCase();
                var name = LanguageProviderV2.translateToPlain(translationKey + ".label");
                add(0, 0, new Button(translationKey, 1, 1)
                    .background("generic2/btn/tristate/icon", -1, -1)
                    .sprite(iconSprite(value))
                    .onLeftClick(() -> onChange.accept(value.next()))
                    .lorePostfix(ExtraPanels.LORE_POSTFIX_CLICKCYCLE));
                add(1, 0, new Text(translationKey, 6, 1, name)
                    .align(4, Text.CENTER)
                    .background("generic2/btn/default/6_1")
                    .onLeftClick(() -> onChange.accept(value.next()))
                    .lorePostfix(ExtraPanels.LORE_POSTFIX_CLICKCYCLE));
            }
        }
    }
}

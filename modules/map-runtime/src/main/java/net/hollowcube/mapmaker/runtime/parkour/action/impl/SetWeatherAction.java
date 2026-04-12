package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.map.setting.WeatherType;
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

public record SetWeatherAction(WeatherType weather) implements Action {
    private static final Sprite SPRITE_CLEAR = new Sprite("icon2/1_1/weather_clear", 1, 1);
    private static final Sprite SPRITE_RAINING = new Sprite("icon2/1_1/weather_rain", 1, 1);
    private static final Sprite SPRITE_THUNDERSTORM = new Sprite("icon2/1_1/weather_thunder", 1, 1);

    public static final Key KEY = key("mapmaker:set_weather");
    public static final StructCodec<SetWeatherAction> CODEC = StructCodec.struct(
        "weather", WeatherType.CODEC.optional(WeatherType.CLEAR), SetWeatherAction::weather,
        SetWeatherAction::new);
    public static final Action.Editor<SetWeatherAction> EDITOR = new Action.Editor<>(
        SetWeatherAction.Editor::new, SetWeatherAction::makeSprite,
        SetWeatherAction::makeThumbnail, Set.of(KEY));
    public static final PlayState.Attachment<WeatherType> SAVE_DATA = PlayState.attachment(key("mapmaker:weather"), WeatherType.CODEC);

    public SetWeatherAction withWeather(WeatherType weather) {
        return new SetWeatherAction(weather);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        state.set(SAVE_DATA, weather);
    }

    private static Sprite makeSprite(@Nullable SetWeatherAction action) {
        if (action == null) return SPRITE_CLEAR;
        return iconSprite(action.weather);
    }

    private static TranslatableComponent makeThumbnail(@Nullable SetWeatherAction action) {
        if (action == null) return translatable("gui.action.set_weather.thumbnail.empty");
        return translatable("gui.action.set_weather.thumbnail", List.of(
            translatable("gui.action.set_weather." + action.weather.name().toLowerCase() + ".label")
        ));
    }

    private static Sprite iconSprite(WeatherType weather) {
        return switch (weather) {
            case CLEAR -> SPRITE_CLEAR;
            case RAINING -> SPRITE_RAINING;
            case THUNDERSTORM -> SPRITE_THUNDERSTORM;
        };
    }

    private static class Editor extends AbstractActionEditorPanel<SetWeatherAction> {

        private final Switch weatherSwitch;

        public Editor(ActionList.Ref ref) {
            super(ref);

            background("action/editor/container_sm", -10, -31);

            add(1, 1, groupText(7, "weather"));

            this.weatherSwitch = add(1, 2, new Switch(7, 1, List.of(
                new Row(WeatherType.CLEAR, update(SetWeatherAction::withWeather)),
                new Row(WeatherType.RAINING, update(SetWeatherAction::withWeather)),
                new Row(WeatherType.THUNDERSTORM, update(SetWeatherAction::withWeather))
            )));
        }

        @Override
        protected void update(SetWeatherAction data) {
            this.weatherSwitch.select(data.weather().ordinal());
        }

        private static class Row extends Panel {
            public Row(WeatherType value, Consumer<WeatherType> onChange) {
                super(7, 1);

                // TODO: would be nice to have a mechanism for reusing a tooltip/click action across a bunch of components.

                var translationKey = "gui.action.set_weather." + value.name().toLowerCase();
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

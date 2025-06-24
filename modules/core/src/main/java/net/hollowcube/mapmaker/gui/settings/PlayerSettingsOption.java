package net.hollowcube.mapmaker.gui.settings;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogInput;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public record PlayerSettingsOption(
        @NotNull String key,
        @NotNull Function<PlayerDataV2, DialogInput> input,
        @NotNull BiConsumer<PlayerDataV2, BinaryTag> applicator
) {

    private static DialogInput.SingleOption.Option option(@NotNull String value, boolean selected) {
        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        for (var i = 0; i < parts.length; i++) {
            var part = parts[i];
            if (part.length() < 2) continue;
            parts[i] = part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase();
        }
        return new DialogInput.SingleOption.Option(value, Component.text(String.join(" ", parts)), selected);
    }

    public static PlayerSettingsOption forBool(
            @NotNull PlayerSetting<Boolean> setting,
            @NotNull Component label
    ) {
        return new PlayerSettingsOption(
                setting.key(),
                (data) -> new DialogInput.SingleOption(
                        setting.key(),
                        PlayerSettingsOptions.OPTION_WIDTH,
                        List.of(
                                new DialogInput.SingleOption.Option("true", Component.text("Enabled"), data.getSetting(setting)),
                                new DialogInput.SingleOption.Option("false", Component.text("Disabled"), !data.getSetting(setting))
                        ),
                        LanguageProviderV2.translate(label),
                        true
                ),
                (data, value) -> {
                    if (!(value instanceof StringBinaryTag it)) return;
                    data.setSetting(setting, it.value().equals("true"));
                }
        );
    }

    public static PlayerSettingsOption forSelect(
            @NotNull PlayerSetting<String> setting,
            @NotNull Component label,
            @NotNull String... options
    ) {
        var values = new HashSet<>(List.of(options));
        return new PlayerSettingsOption(
                setting.key(),
                (data) -> {
                    var current = data.getSetting(setting);
                    return new DialogInput.SingleOption(
                            setting.key(),
                            PlayerSettingsOptions.OPTION_WIDTH,
                            Stream.of(options).map(it -> option(it, current.equals(it))).toList(),
                            LanguageProviderV2.translate(label),
                            true
                    );
                },
                (data, value) -> {
                    if (!(value instanceof StringBinaryTag it)) return;
                    if (!values.contains(it.value())) return;
                    data.setSetting(setting, it.value());
                }
        );
    }

    public static <T extends Enum<T>> PlayerSettingsOption forEnum(
            @NotNull PlayerSetting<T> setting,
            @NotNull Component label,
            @NotNull Class<T> type
    ) {
        var options = type.getEnumConstants();
        return new PlayerSettingsOption(
                setting.key(),
                (data) -> {
                    var current = data.getSetting(setting);
                    return new DialogInput.SingleOption(
                            setting.key(),
                            PlayerSettingsOptions.OPTION_WIDTH,
                            Stream.of(options).map(it -> option(it.name(), current == it)).toList(),
                            LanguageProviderV2.translate(label),
                            true
                    );
                },
                (data, value) -> {
                    if (!(value instanceof StringBinaryTag it)) return;
                    for (var option : options) {
                        if (option.name().equalsIgnoreCase(it.value())) {
                            data.setSetting(setting, option);
                            return;
                        }
                    }
                }
        );
    }





}

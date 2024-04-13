package net.hollowcube.mapmaker.player;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public record DisplayName(
        @NotNull List<Part> parts
) implements ComponentLike {

    public record Part(@NotNull String type, @NotNull String text, @Nullable String color) {
    }

    public enum Context {
        DEFAULT,
        PLAIN,
        // Like DEFAULT, but includes an ordering character before the badge
        TAB_LIST
    }

    public DisplayName {
        parts = List.copyOf(parts);
    }

    @Override
    public @NotNull Component asComponent() {
        return build();
    }

    public @NotNull Component build() {
        return build(Context.DEFAULT);
    }

    public @NotNull Component build(@NotNull Context context) {
        var builder = Component.text();
        for (var part : parts) {
            switch (part.type) {
                case "username" -> {
                    //todo get colors from placeholder file
                    var color = TextColor.color(0xB0B0B0);
                    if (part.color != null && !part.color.isEmpty()) color = TextColor.fromCSSHexString(part.color);
                    builder.append(Component.text(part.text, color));
                }
                case "badge" -> {
                    if (context == Context.PLAIN) continue;

                    if (context == Context.TAB_LIST) {
                        builder.append(Component.text(switch (part.text) {
                            case "dev_3", "mod_3", "ct_3" -> '\uF830';
                            case "dev_2", "mod_2", "ct_2" -> '\uF831';
                            case "dev_1", "mod_1", "ct_1" -> '\uF832';
                            case null, default -> '\uF833';
                        }));
                    }

                    var sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("icon/staff/" + part.text), "unknown badge sprite " + part.text);
                    builder.append(Component.text(sprite.fontChar() + FontUtil.computeOffset(1), FontUtil.NO_SHADOW)
                            .hoverEvent(HoverEvent.showText(LanguageProviderV2.translate(Component.translatable("badge." + part.text + ".lore")))));
                }
                default -> throw new IllegalArgumentException("Unknown part type: " + part.type);
            }
        }
        return builder.build();
    }

    public @Nullable String getBadgeName() {
        for (var part : parts) {
            if (part.type.equals("badge")) return part.text;
        }
        return null;
    }

    public @Nullable String getUsername() {
        for (var part : parts) {
            if (part.type.equals("username")) return part.text;
        }
        return null;
    }

    public @NotNull String getUsernameForTabList() {
        char sortPrefix = switch (getBadgeName()) {
            case "dev_3", "mod_3", "ct_3" -> '\uF830';
            case "dev_2", "mod_2", "ct_2" -> '\uF831';
            case "dev_1", "mod_1", "ct_1" -> '\uF832';
            case "media" -> '\uF833';
            case null, default -> '\uF834';
        };
        // Need to cut off the end of their username to not hit the max length.
        var username = Objects.requireNonNull(getUsername(), "unknown");
        if (username.length() > 14) username = username.substring(0, 14);
        return sortPrefix + username;
    }

}

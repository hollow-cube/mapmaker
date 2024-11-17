package net.hollowcube.mapmaker.player;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        // Like DEFAULT, but uses offset characters vertically
        BOSS_BAR
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

                    var text = part.text;
                    if (context == Context.BOSS_BAR) text = FontUtil.rewrite("bossbar_ascii_1", text);
                    builder.append(Component.text(text, color));
                }
                case "badge" -> {
                    if (context == Context.PLAIN) continue;

                    var icon = part.text.contains("hypercube") ? "icon/" + part.text : "icon/staff/" + part.text;
                    if (context == Context.BOSS_BAR) icon += "_bb";
                    builder.append(Component.text(BadSprite.require(icon).fontChar() + FontUtil.computeOffset(1), NamedTextColor.WHITE)
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

    public int orderIndex() {
        for (var part : parts) {
            if (!"badge".equals(part.type)) continue;

            return switch (part.text) {
                case "dev_3", "mod_3", "ct_3" -> 40;
                case "dev_2", "mod_2", "ct_2" -> 30;
                case "dev_1", "mod_1", "ct_1" -> 20;
                case "hypercube/gold" -> 10;
                case null, default -> 0;
            };
        }
        return 0;
    }

    public @NotNull String getUsernameForTabList() {
        // Need to cut off the end of their username to not hit the max length.
        var username = Objects.requireNonNull(getUsername(), "unknown");
        if (username.length() > 16) username = username.substring(0, 16);
        return username;
    }

}

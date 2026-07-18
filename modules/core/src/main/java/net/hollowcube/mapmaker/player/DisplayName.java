package net.hollowcube.mapmaker.player;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RuntimeGson
public record DisplayName(
    @NotNull List<Part> parts
) implements ComponentLike {

    @RuntimeGson
    public record Part(@NotNull String type, @NotNull String text, @Nullable String color) {
    }

    private static final TextColor DEFAULT_COLOR = TextColor.color(0xB0B0B0);

    public DisplayName {
        parts = List.copyOf(parts);
    }

    @Override
    public @NotNull Component asComponent() {
        return build();
    }

    public @NotNull Component build() {
        return build(DEFAULT_COLOR);
    }

    /// `defaultColor` colors the username when the display name has no explicit color
    /// (eg white on name tags instead of the usual gray).
    public @NotNull Component build(@NotNull TextColor defaultColor) {
        var builder = Component.text();
        for (var part : parts) {
            switch (part.type) {
                case "username" -> {
                    //todo get colors from placeholder file
                    var color = defaultColor;
                    if (part.color != null && !part.color.isEmpty()) color = TextColor.fromCSSHexString(part.color);

                    builder.append(Component.text(part.text, color));
                }
                case "badge" -> {
                    var icon = part.text.contains("hypercube") ? "icon/" + part.text : "icon/staff/" + part.text;
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

    public int getTabListOrder() {
        return switch (getBadgeName()) {
            case "dev_3", "mod_3", "ct_3" -> 5;
            case "dev_2", "mod_2", "ct_2" -> 4;
            case "dev_1", "mod_1", "ct_1" -> 3;
            case "media" -> 2;
            case "hypercube/gold" -> 1;
            case null, default -> 0;
        };
    }

}

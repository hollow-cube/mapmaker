package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
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
        PLAIN
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
                    if (part.color != null) color = TextColor.fromCSSHexString(part.color);
                    builder.append(Component.text(part.text, color));
                }
                case "badge" -> {
                    if (context == Context.PLAIN) continue;
                    var sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("icon/staff/" + part.text), "unknown badge sprite " + part.text);
                    builder.append(Component.text(sprite.fontChar() + FontUtil.computeOffset(1), FontUtil.NO_SHADOW));
                }
                default -> throw new IllegalArgumentException("Unknown part type: " + part.type);
            }
        }
        return builder.build();
    }

}

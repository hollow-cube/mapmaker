package net.hollowcube.ipc.player;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.object.ObjectContents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DisplayName(List<Part> parts) implements ComponentLike {
    private static final TextColor DEFAULT_COLOR = TextColor.color(0xB0B0B0);
    // Resolved against the server font by LanguageProviderV2 before the component is sent.
    private static final Key SPRITES = Key.key("mapmaker", "sprites");
    private static final Key SPACE = Key.key("mapmaker", "space");

    /// What a player the api does not know renders as.
    public static final DisplayName UNKNOWN = of("Unknown");

    public static DisplayName of(String username) {
        return new DisplayName(List.of(new Part.Username(username, null)));
    }

    public DisplayName {
        parts = List.copyOf(parts);
    }

    /// Written as `{"type": "username", ...}`; a part a server is too old to know decodes to
    /// [Unknown] and renders as nothing.
    public sealed interface Part {

        /// The name itself, in `color` (a css hex string) when the player has one.
        record Username(String text, @Nullable String color) implements Part {}

        /// A staff or hypercube badge by icon name, eg `mod_2` or `hypercube/gold`.
        record Badge(String name) implements Part {}

        record Unknown(@Nullable String type) implements Part {}
    }

    public @Nullable String badge() {
        for (var part : parts) {
            if (part instanceof Part.Badge badge) return badge.name;
        }
        return null;
    }

    public @Nullable String username() {
        for (var part : parts) {
            if (part instanceof Part.Username username) return username.text;
        }
        return null;
    }

    @Override
    public Component asComponent() {
        return render();
    }

    public Component render() {
        return render(DEFAULT_COLOR);
    }

    /// `defaultColor` colors the username when the display name has no explicit color
    /// (eg white on name tags instead of the usual gray).
    public Component render(TextColor defaultColor) {
        var builder = Component.text();
        for (var part : parts) {
            switch (part) {
                case Part.Username username -> {
                    var color = username.color == null || username.color.isEmpty()
                        ? defaultColor
                        : TextColor.fromCSSHexString(username.color);
                    builder.append(Component.text(username.text, color));
                }
                case Part.Badge badge -> {
                    var icon = badge.name.contains("hypercube")
                        ? "icon/" + badge.name
                        : "icon/staff/" + badge.name;
                    builder.append(
                        Component.object(ObjectContents.sprite(SPRITES, Key.key("mapmaker", icon)))
                            .color(NamedTextColor.WHITE)
                            .hoverEvent(
                                HoverEvent.showText(
                                    Component.translatable("badge." + badge.name + ".lore")
                                )
                            )
                    );
                    builder.append(
                        Component.object(ObjectContents.sprite(SPACE, Key.key("mapmaker", "1")))
                    );
                }
                case Part.Unknown _ -> {}
            }
        }
        return builder.build();
    }
}

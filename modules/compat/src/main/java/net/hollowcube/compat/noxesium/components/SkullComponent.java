package net.hollowcube.compat.noxesium.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class SkullComponent {

    public static TranslatableComponent uuid(String uuid, boolean grayscale, int advance, int ascent, float scale) {
        return Component.translatable(
                "%%nox_uuid%%%s,%s,%s,%s,%f".formatted(uuid, grayscale, advance, ascent, scale),
                ""
        ).color(NamedTextColor.WHITE);
    }

    public static TranslatableComponent texture(String texture, boolean grayscale, int advance, int ascent, float scale) {
        return Component.translatable(
                "%%nox_raw%%%s,%s,%s,%s,%f".formatted(texture, grayscale, advance, ascent, scale),
                ""
        ).color(NamedTextColor.WHITE);
    }

}

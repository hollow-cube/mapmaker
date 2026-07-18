package net.hollowcube.mapmaker.hub.feature.misc;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Demo of the anchored text shader: one label in each of the nine screen anchors,
 * delivered through a single (first) boss bar composed to net-zero advance.
 */
public final class HudAnchorDemo {
    private static final int MARGIN = 2;
    private static final int TEXT_HEIGHT = 8;

    private static final TextColor[] TINTS = {
            NamedTextColor.WHITE, TextColor.color(0xFF5555), TextColor.color(0xFFAA00),
            TextColor.color(0x55FF55), TextColor.color(0x55FFFF), TextColor.color(0x5555FF),
            TextColor.color(0xFF55FF), TextColor.color(0xFFFF55), TextColor.color(0xAAAAAA),
    };

    public static final BossBar BAR = BossBar.bossBar(buildName(), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    private HudAnchorDemo() {
    }

    private static @NotNull Component buildName() {
        var name = Component.text();
        int cursor = 0;
        for (var anchor : HudAnchor.values()) {
            var label = anchor.name().toLowerCase(Locale.ROOT).replace("_", "");
            int width = FontUtil.measureText(label);
            int startX = switch (anchor.ordinal() % 3) {
                case 0 -> MARGIN;
                case 1 -> -width / 2;
                default -> -width - MARGIN;
            };
            int yOffset = switch (anchor.ordinal() / 3) {
                case 0 -> MARGIN;
                case 1 -> -TEXT_HEIGHT / 2;
                default -> -TEXT_HEIGHT - MARGIN;
            };

            name.append(Component.text(FontUtil.computeOffset(startX - cursor)));
            name.append(HudText.text(label, anchor, yOffset, TINTS[anchor.ordinal()]));
            cursor = startX + width;
        }
        name.append(Component.text(FontUtil.computeOffset(-cursor)));
        return name.build();
    }
}

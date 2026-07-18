package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudBar;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FinishedModeHud implements HudBar.Module {
    // The old actionbar row relative to the bottom anchor.
    private static final int OFFSET_Y = -72;
    private static final BadSprite FINISHED_SPRITE = BadSprite.require("hud/finished");

    public static final FinishedModeHud INSTANCE = new FinishedModeHud();

    private FinishedModeHud() {
    }

    @Override
    public int cacheKey(Player player) {
        return 0;
    }

    @Override
    public @NotNull Component render(@NotNull Player player) {
        var builder = new FontUIBuilder();
        builder.pushColor(HudText.KILL);
        builder.pushShadowColor(HudText.marker(HudAnchor.BOTTOM, OFFSET_Y, NamedTextColor.WHITE));
        builder.pos(-FINISHED_SPRITE.width() / 2).drawInPlace(FINISHED_SPRITE);
        builder.popShadowColor();
        builder.popColor();
        return builder.build(true);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FinishedModeHud;
    }

    @Override
    public int hashCode() {
        return 31 * FinishedModeHud.class.hashCode();
    }
}

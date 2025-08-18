package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;

public class FinishedModeHud implements ActionBar.Provider {
    private static final BadSprite FINISHED_SPRITE = BadSprite.require("hud/finished");

    public static final FinishedModeHud INSTANCE = new FinishedModeHud();

    private FinishedModeHud() {
    }

    @Override
    public void provide(Player player, FontUIBuilder builder) {
        builder.pushShadowColor(ShadowColor.none());
        builder.pos(-FINISHED_SPRITE.width() / 2).drawInPlace(FINISHED_SPRITE);
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

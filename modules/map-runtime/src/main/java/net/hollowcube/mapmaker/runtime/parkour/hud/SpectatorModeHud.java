package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;

public class SpectatorModeHud implements ActionBar.Provider {
    private static final BadSprite SPECTATOR_SPRITE = BadSprite.require("hud/spectator");

    public static final SpectatorModeHud INSTANCE = new SpectatorModeHud();

    private SpectatorModeHud() {
    }

    @Override
    public void provide(Player player, FontUIBuilder builder) {
        builder.pushShadowColor(ShadowColor.none());
        builder.pos(-SPECTATOR_SPRITE.width() / 2).drawInPlace(SPECTATOR_SPRITE);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SpectatorModeHud;
    }

    @Override
    public int hashCode() {
        return 31 * SpectatorModeHud.class.hashCode();
    }
}

package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudNode;
import net.hollowcube.common.hud.PlayerHud;
import net.minestom.server.entity.Player;

public class SpectatorModeHud implements PlayerHud.Module {
    // The old actionbar row relative to the bottom anchor.
    private static final int OFFSET_Y = -72;

    private static final HudNode.Anchored CONTENT = HudNode.sprite("hud/spectator")
        .frame(0, HudNode.Align.CENTER)
        .offset(0, OFFSET_Y)
        .anchored(HudAnchor.BOTTOM);

    public static final SpectatorModeHud INSTANCE = new SpectatorModeHud();

    private SpectatorModeHud() {
    }

    @Override
    public HudNode.Anchored render(Player player) {
        return CONTENT;
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

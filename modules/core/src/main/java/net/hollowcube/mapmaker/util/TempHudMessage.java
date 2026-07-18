package net.hollowcube.mapmaker.util;

import net.hollowcube.common.hud.HudAnchor;
import net.hollowcube.common.hud.HudBar;
import net.hollowcube.common.hud.HudText;
import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TempHudMessage implements HudBar.Module {
    // The old actionbar row relative to the bottom anchor.
    private static final int OFFSET_Y = -72;

    private final String message;
    private final int width;
    private final long expiration;

    public TempHudMessage(@NotNull String message, long expiration) {
        this.message = message;
        this.width = FontUtil.measureText(message);
        this.expiration = System.currentTimeMillis() + expiration;
    }

    @Override
    public int cacheKey(@NotNull Player player) {
        return message.hashCode();
    }

    @Override
    public long expiration() {
        return expiration;
    }

    @Override
    public @NotNull Component render(@NotNull Player player) {
        var builder = new FontUIBuilder();
        builder.pushColor(HudText.KILL);
        builder.pushShadowColor(HudText.marker(HudAnchor.BOTTOM, OFFSET_Y, NamedTextColor.WHITE));
        builder.pos((-width / 2) - 1);
        builder.append(message);
        builder.popShadowColor();
        builder.popColor();
        return builder.build(true);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TempHudMessage;
    }

    @Override
    public int hashCode() {
        return TempHudMessage.class.hashCode() ^ 1205125;
    }
}

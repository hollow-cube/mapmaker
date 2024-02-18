package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GenericTempActionBarProvider implements ActionBar.Provider {
    private final String message;
    private final int width;
    private final long expiration;

    public GenericTempActionBarProvider(@NotNull String message, long expiration) {
        this.message = message;
        this.width = FontUtil.measureText(message);
        this.expiration = System.currentTimeMillis() + expiration;
    }

    @Override
    public long expiration() {
        return expiration;
    }

    @Override
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
        builder.pos((-width / 2) - 1);
        builder.append(message);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GenericTempActionBarProvider;
    }

    @Override
    public int hashCode() {
        return GenericTempActionBarProvider.class.hashCode() ^ 1205125;
    }
}

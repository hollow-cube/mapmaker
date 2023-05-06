package net.hollowcube.mapmaker.service;

import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.common.facet.Facet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.ServerProcess;
import org.jetbrains.annotations.NotNull;

public class PlayerServiceImpl implements PlayerService, Facet {

    @Override
    public void hook(@NotNull ServerProcess server, @NotNull ConfigProvider configProvider) {
    }

    @Override
    public @NotNull Component getDisplayName(@NotNull String playerId) {
        // Implementation Note:
        // This service should aggressively cache the results of this method as it is called very frequently.
        // As a base, all online players should be cached in every server (note: in the future this would likely
        // just be MM players cached in MM), and fetched players should be kept in cache for a reasonable amount
        // of time.
        if (playerId.equals("aceb326f-da15-45bc-bf2f-11940c21780c")) {
            return Component.text("\uEff4 ", NamedTextColor.WHITE).append(Component.text("notmattw", TextColor.color(0xFF2D2D)));
        }
        return Component.text(playerId);
    }

}

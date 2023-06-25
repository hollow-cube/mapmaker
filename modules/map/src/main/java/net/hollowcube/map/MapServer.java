package net.hollowcube.map;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public interface MapServer {

    @NotNull PlayerService playerService();

    @NotNull SessionService sessionService();

    @NotNull MapService mapService();

    @NotNull List<FeatureProvider> features();

    void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider);

}

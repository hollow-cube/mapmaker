package net.hollowcube.mapmaker.map;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.metrics.MetricWriter;
import net.hollowcube.mapmaker.misc.noop.NoopMapService;
import net.hollowcube.mapmaker.misc.noop.NoopPlayerService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

final class TestMapServer implements MapServer {
    private final FeatureList features = FeatureList.load(ConfigLoaderV3.loadFromText(new byte[0], Map.of()));

    private final MapService mapService = new NoopMapService();
    private final PlayerService playerService = new NoopPlayerService();

    @Override
    public @NotNull MetricWriter metrics() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull SessionService sessionService() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull PlayerService playerService() {
        return playerService;
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

    @Override
    public @NotNull PermManager permManager() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull PunishmentService punishmentService() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull PlayerInviteService inviteService() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull MapAllocator allocator() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull SessionManager sessionManager() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull ServerBridge bridge() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull Controller guiController() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @NotNull Scheduler scheduler() {
        throw new UnsupportedOperationException("not implemented");
    }

    @SuppressWarnings("unchecked") @Override
    public <T> @NotNull T facet(@NotNull Class<T> type) {
        if (type == FeatureList.class) return (T) features;
        throw new UnsupportedOperationException("no such feature " + type);
    }

    @Override
    public void showView(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        throw new UnsupportedOperationException("not implemented");
    }
}

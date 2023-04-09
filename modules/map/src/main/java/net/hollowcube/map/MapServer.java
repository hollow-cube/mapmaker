package net.hollowcube.map;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.MetricStorage;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.world.WorldManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public interface MapServer {

    @NotNull WorldManager worldManager();

    @NotNull MetricStorage metricStorage();

    @NotNull MapStorage mapStorage();

    @NotNull SaveStateStorage saveStateStorage();

    @NotNull PlatformPermissionManager platformPermissions();

    @NotNull List<FeatureProvider> features();

    void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider);

}

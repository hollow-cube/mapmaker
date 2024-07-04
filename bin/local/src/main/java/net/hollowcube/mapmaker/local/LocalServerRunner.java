package net.hollowcube.mapmaker.local;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.local.svc.LocalMapService;
import net.hollowcube.mapmaker.local.svc.LocalPlayerService;
import net.hollowcube.mapmaker.local.svc.LocalSessionService;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.util.MapJoinInfo;
import net.hollowcube.mapmaker.map.world.LocalEditingMapWorld;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LocalServerRunner extends MapServerRunner {
    public static final String DUMMY_MAP_ID = "0557bb41-225a-4556-af2e-51f72c0a005c"; // PigianaJones

    private final Path workspace;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionService sessionService;

    LocalServerRunner(@NotNull Path workspace, @NotNull ConfigLoaderV3 config) {
        super(config);
        this.workspace = workspace;

        this.mapService = new LocalMapService(workspace);
        this.playerService = new LocalPlayerService();
        this.sessionService = new LocalSessionService();
    }

    @Override
    protected @NotNull String name() {
        return "local-mapmaker";
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

    @Override
    public @NotNull PlayerService playerService() {
        return playerService;
    }

    @Override
    public @NotNull SessionService sessionService() {
        return sessionService;
    }

    @Override
    protected @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId, boolean deleteCompleted) {
        return CompletableFuture.completedFuture(new MapJoinInfo(playerId, DUMMY_MAP_ID, "editing"));
    }

    @Override
    protected @NotNull AbstractMapWorld createMapWorld(@NotNull MapData map, @NotNull String state) {
        return Objects.requireNonNull(FutureUtil.getUnchecked(allocator().create(map, LocalEditingMapWorld.class)));
    }
}

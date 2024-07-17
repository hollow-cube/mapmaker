package net.hollowcube.mapmaker.local;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.local.config.LocalWorkspace;
import net.hollowcube.mapmaker.local.svc.*;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.util.MapJoinInfo;
import net.hollowcube.mapmaker.map.world.LocalEditingMapWorld;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.storage.TerraformStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LocalServerRunner extends MapServerRunner {
    public static final String DUMMY_MAP_ID = "0557bb41-225a-4556-af2e-51f72c0a005c"; // PigianaJones

    public static LocalServerRunner instance;

    private final LocalWorkspace workspace;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionService sessionService;

    LocalServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        LocalServerRunner.instance = this;
        this.workspace = config.get(LocalWorkspace.class);

        this.mapService = new LocalMapService(activeProjectDirectory());
        this.playerService = new LocalPlayerService();
        this.sessionService = new LocalSessionService();
    }

    @Override
    protected @NotNull String name() {
        return "local-mapmaker";
    }

    public @NotNull LocalWorkspace workspace() {
        return workspace;
    }

    public @NotNull Path activeProjectDirectory() {
        return workspace.path();
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
    protected @NotNull ServerBridge createBridge() {
        return new LocalServerBridge();
    }

    @Override
    protected @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId, boolean deleteCompleted) {
        return CompletableFuture.completedFuture(new MapJoinInfo(playerId, DUMMY_MAP_ID, "editing"));
    }

    @Override
    protected @NotNull AbstractMapWorld createMapWorld(@NotNull MapData map, @NotNull String state) {
        return Objects.requireNonNull(FutureUtil.getUnchecked(allocator().create(map, LocalEditingMapWorld.class)));
    }

    @Override
    protected @NotNull TerraformModule[] extraTfModules() {
        return new TerraformModule[]{new TerraformModule() {
            @Override
            public @NotNull Set<Class<? extends TerraformStorage>> storageTypes() {
                return Set.of(LocalTerraformStorage.class);
            }
        }};
    }
}

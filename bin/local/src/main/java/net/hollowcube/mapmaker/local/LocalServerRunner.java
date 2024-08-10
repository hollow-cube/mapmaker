package net.hollowcube.mapmaker.local;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.local.command.WorkspaceCommand;
import net.hollowcube.mapmaker.local.config.LocalWorkspace;
import net.hollowcube.mapmaker.local.proj.Project;
import net.hollowcube.mapmaker.local.svc.*;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.util.MapJoinInfo;
import net.hollowcube.mapmaker.map.world.LocalEditingMapWorld;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LocalServerRunner extends MapServerRunner {
    public static LocalServerRunner instance;

    private final LocalWorkspace workspace;
    private final Path stateDir;

    private final MapService mapService;
    private final PlayerService playerService;
    private final SessionService sessionService;

    LocalServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        LocalServerRunner.instance = this;
        this.workspace = config.get(LocalWorkspace.class);
        this.stateDir = workspace.path().resolve(".workspacestate");

        this.mapService = new LocalMapService(workspace);
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
        return new LocalServerBridge(this);
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        commandManager().register(createInstance(WorkspaceCommand.class));
    }

    @Override
    protected @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId, boolean deleteCompleted) {
        try {
            var player = MinecraftServer.getConnectionManager().getConfigPlayers().stream()
                    .filter(p -> p.getUuid().toString().equals(playerId))
                    .findFirst().orElse(null);
            Check.notNull(player, "Player not found");

            String mapId = player.getTag(LocalServerBridge.TARGET_SERVER_TAG);
            var playerStatePath = stateDir.resolve(playerId + ".json");
            if (mapId == null) {
                // todo workspace default project option
                mapId = "0_0_cubed_prologue";
                if (Files.exists(playerStatePath)) {
                    var state = Project.GSON.fromJson(Files.readString(playerStatePath), JsonObject.class);
                    var lastMapId = state.get("lastMapId").getAsString();
                    if (Files.exists(workspace.path().resolve(lastMapId)))
                        mapId = lastMapId;
                }
            } else {
                // Update the player state file
                var state = new JsonObject();
                state.addProperty("lastMapId", mapId);
                Files.createDirectories(playerStatePath.getParent());
                Files.writeString(playerStatePath, Project.GSON.toJson(state));
            }

            return CompletableFuture.completedFuture(new MapJoinInfo(playerId, mapId, "editing"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    @Override
    protected @NotNull DebugCommand createDebugCommand() {
        var cmd = super.createDebugCommand();
        cmd.createPermissionlessSubcommand("killall", (sender, context) -> {
            int i = 0;
            for (var entity : sender.getInstance().getEntities()) {
                if (entity instanceof Player) continue;
                entity.remove();
                i++;
            }
            sender.sendMessage("Killed " + i + " entities");
        }, "Kills all entities in the world");
        return cmd;
    }
}

package net.hollowcube.mapmaker.dev;

import com.github.luben.zstd.Zstd;
import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.common.util.*;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.dev.commands.PlayNbsCommand;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.runtime.building.BuildingMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.ping.Status;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static net.minestom.server.network.PolarBufferAccessWidener.networkBufferAddress;

public class DevServer extends AbstractMultiMapServer {

    // Hub stuff
    private HubMapWorld hubWorld;

    // Map stuff
    private Terraform terraform;

    // Common stuff
    private final CommandManager hubCommandManager = new CommandManagerImpl(super.commandManager());
    private final CommandManager mapCommandManager = new CommandManagerImpl(super.commandManager());

    public DevServer(@NotNull ConfigLoaderV3 config) {
        super(config);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("dev-init")
            .addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin)
            .addListener(ServerListPingEvent.class, this::handleServerListPing));
    }

    @Override
    protected @NotNull String name() {
        return "mapmaker-dev";
    }

    @Override
    protected @NotNull ServerBridge createBridge() {
        return new DevServerBridge(this);
    }

    @Override
    protected @NotNull Future<AbstractMapWorld<?, ?>> createWorldForRequest(@NotNull MapJoinInfo joinInfo) {
        var map = joinInfo.mapId().equals(MapData.SPAWN_MAP_ID)
            ? HubServer.HUB_MAP_DATA
            : api().maps.get(joinInfo.mapId());

        final boolean isEditor = Presence.MAP_BUILDING_STATES.contains(joinInfo.state());
        return createWorld(map, isEditor, _ -> {
            if (isEditor) return new EditorMapWorld(this, map, terraform);
            return switch (map.settings().getVariant()) {
                case PARKOUR -> new ParkourMapWorld(this, map);
                case BUILDING -> new BuildingMapWorld(this, map);
                default -> throw new IllegalStateException("No world for map variant " + map.settings().getVariant());
            };
        }, true);
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) -> new MapPlayer(connection, gameProfile) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                var world = MapWorld.forPlayer(this);
                return world == null || world instanceof HubMapWorld
                    ? hubCommandManager : mapCommandManager;
            }
        });

        performMapInit(); // Map first so placements are registered
        performHubInit();
    }

    private void performHubInit() {
        this.hubWorld = FutureUtil.getUnchecked(createWorld(HubServer.HUB_MAP_DATA,
            false, map -> new HubMapWorld(this, map), false));
        addBinding(HubMapWorld.class, hubWorld, "world", "hubWorld", "hubMapWorld");

        HubServer.registerCommands(this, hubCommandManager, hubWorld, MinecraftServer.getSchedulerManager());
        hubCommandManager.register(PlayNbsCommand.INSTANCE);
        HubServer.loadHubFeatures(this, hubWorld);
    }

    private void performMapInit() {
        Predicate<InstanceEvent> filter = event -> {
            if (!(MapWorld.forInstance(event.getInstance()) instanceof EditorMapWorld editor))
                return false;
            if (event instanceof PlayerEvent playerEvent && !(editor.getPlayerState(playerEvent.getPlayer()) instanceof EditorState.Building))
                return false;

            return true;
        };
        var terraformEvents = EventNode.event("tf-events", EventFilter.INSTANCE, filter);
        var interactionEvents = EventNode.event("tf-events", EventFilter.INSTANCE, filter);
        this.terraform = MapMapServer.initBuildLogic(mapService(), mapCommandManager, terraformEvents, interactionEvents);

        MinecraftServer.getGlobalEventHandler().addChild(terraformEvents).addChild(interactionEvents);

        mapCommandManager.register(PlayNbsCommand.INSTANCE);
        MapMapServer.registerCommands(this, mapCommandManager, mapService());
    }

    protected void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        // DevServer is not running behind a proxy, so we need to handle the proxy side of the session interaction
        // on our own here.
        // Note that we dont transfer here, its deferred to config phase (and reconfig)

        var profile = event.getGameProfile();
        var playerId = profile.uuid().toString();
        net.minestom.server.entity.PlayerSkin skin = MojangUtil.getSkinFromUuid(playerId);

        try {
            sessionService().createSession(
                playerId,
                "devserver-integrated",
                profile.name(),
                "127.0.0.1",
                new PlayerSkin(
                    OpUtils.map(skin, net.minestom.server.entity.PlayerSkin::textures),
                    OpUtils.map(skin, net.minestom.server.entity.PlayerSkin::signature)
                ),
                ProtocolVersions.getProtocolName(event.getConnection().getProtocolVersion()),
                event.getConnection().getProtocolVersion()
            );

            addPendingJoin(playerId, HubServer.HUB_MAP_DATA.id(), "playing");
        } catch (SessionService.SessionCreationDeniedError error) {
            PlayerUtil.disconnect(event.getConnection(), error.reason());
        }
    }

    @Override
    protected void handlePlayerDisconnect(@NotNull Player player) {
        super.handlePlayerDisconnect(player);

        // Again, need to implement the proxy part of the delete session flow
        FutureUtil.submitVirtual(() -> sessionService().deleteSession(player.getUuid().toString()));
    }

    protected void handleServerListPing(@NotNull ServerListPingEvent event) {
        var favicon = Base64.getDecoder().decode("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");
        var description = "                     <color:#dbdbdb>Hollow Cube</color> <color:#696969>|</color> <color:#bfbfbf>" + MinecraftServer.VERSION_NAME + "</color>\n                  <color:#fa4141>ᴍᴀᴘ ᴍᴀᴋᴇʀ ᴅᴇᴠ ѕᴇʀᴠᴇʀ</color>";
        event.setStatus(Status.builder()
            .favicon(favicon)
            .description(MiniMessage.miniMessage().deserialize(description))
            .build());
    }

    @Override
    protected @NotNull DebugCommand createDebugCommand() {
        var cmd = super.createDebugCommand();

        cmd.createPermissionedSubcommand("replay", (player, context) -> {
            try {
                var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/workspace/replay-test/test1-compact.dat");
                var bytes = Files.readAllBytes(path);
                var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);

                System.out.println("file: " + path.getFileName().toString() + " (" + bytes.length + " bytes)");

                var header = new ReplayHeader(buffer);
                System.out.println("version: " + header.version());
                System.out.println("worldId: " + header.worldId());
                System.out.println("worldVersion: " + header.worldVersion());
                System.out.println("timestamp: " + header.timestamp());
                System.out.println("dictionary: " + header.dictionary());
                System.out.println("metadataLength: " + header.metadataLength());
                System.out.println("indexLength: " + header.indexLength());
                System.out.println("tickCount: " + header.tickCount());
                System.out.println("chunkCount: " + header.chunkCount());

                var metadata = buffer.read(NetworkBuffer.NBT_COMPOUND);
                System.out.println("\nmetadata: " + MinestomAdventure.tagStringIO().asString(metadata));

                var index = new ChunkIndex[header.chunkCount()];
                for (int i = 0; i < header.chunkCount(); i++) {
                    index[i] = buffer.read(ChunkIndex.NETWORK_TYPE);
                }

                var scratch = NetworkBuffer.resizableBuffer(2048);

                var entity = new LivingEntity(EntityType.MANNEQUIN) {
                    {
                        setNoGravity(true);
                        hasPhysics = false;
                    }

                    @Override
                    protected void movementTick() {
                        // nothing
                    }
                };
                entity.setInstance(player.getInstance(), new Pos(0, 40, 0));

                player.scheduler().submitTask(new Supplier<>() {
                    int tick = 0;
                    int chunkIndex = 0;

                    private boolean newChunk = true;

                    @Override
                    public TaskSchedule get() {
                        var chunk = index[chunkIndex];

                        if (newChunk) {
                            newChunk = false;
                            System.out.println("\nchunk: " + chunk);

                            scratch.clear();
                            scratch.ensureWritable(chunk.uncompressedLength());
                            Zstd.decompressUnsafe(
                                networkBufferAddress(scratch), chunk.uncompressedLength(),
                                networkBufferAddress(buffer) + chunk.byteOffset(), chunk.compressedLength()
                            );
                        }

                        var tickIndex = scratch.read(NetworkBuffer.VAR_INT);
                        var eventCount = scratch.read(NetworkBuffer.SHORT);
                        System.out.println(tick + " (" + tickIndex + "): " + eventCount + " events");

                        for (int j = 0; j < eventCount; j++) {
                            var typeId = scratch.read(NetworkBuffer.VAR_INT);
                            var entityId = scratch.read(NetworkBuffer.VAR_INT);
                            var delta = scratch.read(NetworkBuffer.POS);
                            System.out.println("  " + typeId + " " + entityId + " " + delta);

                            entity.teleport(entity.getPosition().add(delta));
                        }

                        tick++;
                        if (tick >= chunk.startTick() + chunk.tickCount()) {
                            chunkIndex++;
                            newChunk = true;
                        }
                        if (chunkIndex >= header.chunkCount()) {
                            System.out.println("DONE");
                            entity.remove();
                            return TaskSchedule.stop();
                        }
                        return TaskSchedule.nextTick();
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "replay testing locally");

        return cmd;
    }
}

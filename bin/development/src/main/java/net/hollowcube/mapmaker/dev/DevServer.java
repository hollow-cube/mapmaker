package net.hollowcube.mapmaker.dev;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.MojangUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.editor.hdb.HeadDatabase;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.hollowcube.mapmaker.runtime.building.BuildingMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.ping.Status;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.concurrent.Future;
import java.util.function.Predicate;

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
                : mapService().getMap(joinInfo.playerId(), joinInfo.mapId());

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

        var hdb = new HeadDatabase(otel);
        addBinding(HeadDatabase.class, hdb, "headDatabase", "hdb");
        MapMapServer.registerCommands(this, mapCommandManager, hdb);
    }

    protected void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        // DevServer is not running behind a proxy, so we need to handle the proxy side of the session interaction
        // on our own here.
        // Note that we dont transfer here, its deferred to config phase (and reconfig)

        var profile = event.getGameProfile();
        var playerId = profile.uuid().toString();
        net.minestom.server.entity.PlayerSkin skin = MojangUtil.getSkinFromUuid(playerId);

        var pd = sessionService().createSession(
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
    }

    @Override
    protected void handlePlayerDisconnect(@NotNull Player player) {
        super.handlePlayerDisconnect(player);

        // Again, need to implement the proxy part of the delete session flow
        FutureUtil.submitVirtual(() -> sessionService().deleteSession(player.getUuid().toString()));
    }

    protected void handleDisconnect(@NotNull PlayerDisconnectEvent event) {
        var player = event.getPlayer();
        super.handlePlayerDisconnect(player);
    }

    protected void handleServerListPing(@NotNull ServerListPingEvent event) {
        var favicon = Base64.getDecoder().decode("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");
        var description = "                     <color:#dbdbdb>Hollow Cube</color> <color:#696969>|</color> <color:#bfbfbf>" + MinecraftServer.VERSION_NAME + "</color>\n                  <color:#fa4141>ᴍᴀᴘ ᴍᴀᴋᴇʀ ᴅᴇᴠ ѕᴇʀᴠᴇʀ</color>";
        event.setStatus(Status.builder()
                .favicon(favicon)
                .description(MiniMessage.miniMessage().deserialize(description))
                .build());
    }

}

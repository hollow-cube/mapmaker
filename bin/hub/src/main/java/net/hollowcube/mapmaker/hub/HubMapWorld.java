package net.hollowcube.mapmaker.hub;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.hub.feature.event.christmas.AdventCalendarItem;
import net.hollowcube.mapmaker.hub.feature.event.christmas.PresentObjectHandler;
import net.hollowcube.mapmaker.hub.item.*;
import net.hollowcube.mapmaker.hub.util.HubTransferData;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.scripting.WorldScriptContext;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.player.PlayerMoveEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.Objects;
import java.util.Random;

public class HubMapWorld extends AbstractMapWorld<HubPlayerState, HubMapWorld> {
    private static final Pos MIN_SPAWN_POINT = new Pos(-1, 40, -1, 90, 0);

    private static final Vec HUB_BB_MIN = new Vec(-250, -40, -150);
    private static final Vec HUB_BB_MAX = new Vec(60, 160, 150);

    public static Pos spawnPointFor(Player player) {
        var seeded = new Random(player.getUuid().getLeastSignificantBits());
        return MIN_SPAWN_POINT.add(
            (seeded.nextDouble() * 10) % 3,
            0,
            (seeded.nextDouble() * 10) % 3
        );
    }

    private final WorldScriptContext scriptContext;

    public HubMapWorld(MapServer server, MapData map) {
        super(server, map, makeMapInstance(map, 'h', null),
            HubPlayerState.class);

        itemRegistry().register(new PlayMapsItem(server.playerService(), server.mapService(), server.bridge()));
        itemRegistry().register(new CreateMapsItem(server.mapService(), server.guiController()));
        itemRegistry().register(new OrgMapsItem());
        itemRegistry().register(new OpenCosmeticsMenuItem(server.playerService()));
        itemRegistry().register(OpenStoreItem.INSTANCE);
        itemRegistry().register(new AdventCalendarItem());
        itemRegistry().register(OpenNotificationsItem.INSTANCE);

        objectEntityHandlers().registerForInteractions(PresentObjectHandler.ID, PresentObjectHandler::new);

        eventNode().addChild(EventUtil.READ_ONLY_NODE)
            .addListener(PlayerChangeHeldSlotEvent.class, this::handleSwitchSlot)
            .addListener(PlayerMoveEvent.class, this::handlePlayerMove);

        // Load scripting engine
        if (ServerRuntime.getRuntime().isDevelopment()) {
            var playerScript = Objects.requireNonNull(HubMapWorld.class.getResource("/scripts/player.luau"));
            var baseUrl = URI.create(playerScript.toString().substring(0, playerScript.toString().lastIndexOf('/')));
            this.scriptContext = new WorldScriptContext(this, baseUrl, false);
        } else {
            var zipUrl = Objects.requireNonNull(HubMapWorld.class.getResource("/net.hollowcube.scripting/hub.zip"));
            this.scriptContext = new WorldScriptContext(this, URI.create(zipUrl.toString()), true);
        }
    }

    @Override
    protected HubPlayerState configurePlayer(Player player) {
        player.removeResourcePacks(MapWorldHelpers.MAP_WORLD_RESOURCE_PACK_UUID);

        // Load the state from the previous hub they were on if present.
        var existingState = ProxySupport.getTransferData(player, HubTransferData.class);
        if (existingState != null) {
            player.setRespawnPoint(existingState.position());
            player.scheduleNextTick(ignored -> player.setHeldItemSlot((byte) existingState.slot()));
        } else {
            player.setRespawnPoint(spawnPointFor(player));
        }

        return new HubPlayerState.Default();
    }

    @Override
    protected void loadWorldData() {
        ReadableMapData mapWorldData = null;
        try {
            mapWorldData = server().mapService().getMapWorldAsStream(map().id(), false);
        } catch (MapService.NotFoundError error) {
            if (!map().id().equals(MapData.SPAWN_MAP_ID)) {
                throw error;
            }
        }
        if (mapWorldData == null) {
            try (var is = getClass().getResourceAsStream("/spawn/hcspawn.polar")) {
                if (is == null) throw new IOException("hcspawn.polar not found");
                var worldFileContent = Objects.requireNonNull(is.readAllBytes());
                mapWorldData = new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(worldFileContent)), worldFileContent.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        instance().loadStream(mapWorldData, new ReadWorldAccess(this));
    }

    @Override
    public void spawnPlayer(Player player) {
        super.spawnPlayer(player);

        scriptContext.initializePlayer((MapPlayer) player);
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);

        scriptContext.destroyPlayer((MapPlayer) player);
    }

    private void handleSwitchSlot(PlayerChangeHeldSlotEvent event) {
        var playerData = PlayerData.fromPlayer(event.getPlayer());
        playerData.setSetting(PlayerSettings.HUB_SELECTED_SLOT, (int) event.getNewSlot());
    }

    private void handlePlayerMove(PlayerMoveEvent event) {
        Pos playerPos = event.getPlayer().getPosition();
        if (playerPos.x() < HUB_BB_MIN.x() || playerPos.x() > HUB_BB_MAX.x() ||
            playerPos.y() < HUB_BB_MIN.y() || playerPos.y() > HUB_BB_MAX.y() ||
            playerPos.z() < HUB_BB_MIN.z() || playerPos.z() > HUB_BB_MAX.z()) {
            event.getPlayer().teleport(spawnPointFor(event.getPlayer()));
        }
    }
}

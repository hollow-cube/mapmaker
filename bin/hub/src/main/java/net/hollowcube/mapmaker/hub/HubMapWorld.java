package net.hollowcube.mapmaker.hub;

import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.hub.item.*;
import net.hollowcube.mapmaker.hub.util.HubTransferData;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.scripting.WorldScriptContext;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;

public class HubMapWorld extends AbstractMapWorld<HubPlayerState, HubMapWorld> {
    private static final Pos MIN_SPAWN_POINT = new Pos(-1, 40, -1, 90, 0);

    private static final Vec HUB_BB_MIN = new Vec(-250, -30, -100);
    private static final Vec HUB_BB_MAX = new Vec(60, 130, 150);

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
        super(server, map, makeMapInstance(map, 'h', MapInstance.LightingMode.FULL_BRIGHT),
            HubPlayerState.class);

        itemRegistry().register(new PlayMapsItem(server.playerService(), server.mapService(), server.bridge()));
        itemRegistry().register(new CreateMapsItem(server.guiController()));
        itemRegistry().register(new OrgMapsItem(server.guiController()));
        itemRegistry().register(new OpenCosmeticsMenuItem(server.guiController()));
        itemRegistry().register(OpenStoreItem.INSTANCE);

        eventNode().addChild(EventUtil.READ_ONLY_NODE)
            .addListener(PlayerChangeHeldSlotEvent.class, this::handleSwitchSlot)
            .addListener(PlayerMoveEvent.class, this::handlePlayerMove);
        //todo
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn);

        {
            var playerScript = Objects.requireNonNull(HubMapWorld.class.getResource("/scripts/player.luau"));

            var baseUrl = URI.create(playerScript.toString().substring(0, playerScript.toString().lastIndexOf('/')));
            this.scriptContext = new WorldScriptContext(baseUrl);
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

    private void handlePlayerSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        var playerScript = Objects.requireNonNull(HubMapWorld.class.getResource("/scripts/player.luau"));
        var state = scriptContext.createThread();

        try (var is = playerScript.openStream()) {
            var source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var bytecode = LuauCompiler.DEFAULT.compile(source);
            state.load("/player.luau", bytecode);
            state.call(0, 0);
        } catch (Exception e) {
            event.getPlayer().kick("Failed to spawn player");
            throw new RuntimeException("failed to spawn player", e);
        }
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

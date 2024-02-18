package net.hollowcube.mapmaker.hub;

import com.google.inject.Inject;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.Uuids;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.hub.feature.misc.DoubleJumpFeature;
import net.hollowcube.mapmaker.hub.item.CreateMapsItem;
import net.hollowcube.mapmaker.hub.item.OpenCosmeticsMenuItem;
import net.hollowcube.mapmaker.hub.item.OrgMapsItem;
import net.hollowcube.mapmaker.hub.item.PlayMapsItem;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.hollowcube.mapmaker.util.NoopChunkLoader;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarReader;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class HubMapWorld extends AbstractMapWorld {
    private static final Logger logger = LoggerFactory.getLogger(HubMapWorld.class);

    private static final PlayerSetting<Integer> SELECTED_SLOT = PlayerSetting.Int("selected_slot", 0);

    private static final Pos MIN_SPAWN_POINT = new Pos(-1, 40, -1, 90, 0);
    public static final MapData HUB_MAP_DATA = new MapData(
            MapData.SPAWN_MAP_ID == null ? Uuids.ZERO : MapData.SPAWN_MAP_ID, Uuids.ZERO,
            new MapSettings(), 0, Instant.now()
    );

    private static final Vec HUB_BB_MIN = new Vec(-250, -30, -100);
    private static final Vec HUB_BB_MAX = new Vec(60, 130, 100);

    private static HubMapWorld instance; // Currently just ensures there is only ever one hub per runtime.

    private final EventNode<InstanceEvent> eventNode = EventNode.type("hub-events", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true))
            .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true))
            .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerMoveEvent.class, this::handlePlayerMove)
            .addListener(PlayerChangeHeldSlotEvent.class, this::handleSwitchSlot);

    @Inject
    public HubMapWorld(@NotNull MapServer server) {
        super(server, HUB_MAP_DATA, new MapInstance(HUB_MAP_DATA.id()));
        Check.stateCondition(HubMapWorld.instance != null, "HubMapWorld already created");
        HubMapWorld.instance = this;

        instance().setGenerator(MapGenerators.voidWorld());

        instance().eventNode().addChild(eventNode); // Needs spectators, so register on instance.

        itemRegistry().register(server().createInstance(PlayMapsItem.class));
        itemRegistry().register(server().createInstance(CreateMapsItem.class));
        itemRegistry().register(server().createInstance(OrgMapsItem.class));
        itemRegistry().register(server().createInstance(OpenCosmeticsMenuItem.class));
    }

    @Override
    public @NotNull Pos spawnPoint(@NotNull Player player) {
        var seeded = new Random(player.getUuid().getLeastSignificantBits());
        return MIN_SPAWN_POINT.add(
                (seeded.nextDouble() * 10) % 3,
                0,
                (seeded.nextDouble() * 10) % 3
        );
    }

    @Override
    public void load() {
        byte[] mapWorldData;
        if (!ServerRuntime.getRuntime().isDevelopment()) {
            mapWorldData = server().mapService().getMapWorld(map().id(), false);
            Check.notNull(mapWorldData, "No world generated for hub world!");
        } else {
            try (var is = getClass().getResourceAsStream("/spawn/hcspawn.polar")) {
                if (is == null) throw new IOException("hcspawn.polar not found");
                mapWorldData = Objects.requireNonNull(is.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        var instance = (MapInstance) instance();
        instance.setChunkLoader(new PolarLoader(PolarReader.read(mapWorldData)).setLoadLighting(false));

        var loadingChunks = new ArrayList<CompletableFuture<Chunk>>();
        ChunkUtils.forChunksInRange(0, 0, 16, (x, z) -> loadingChunks.add(instance.loadChunk(x, z)));
        CompletableFuture.allOf(loadingChunks.toArray(CompletableFuture[]::new))
                .thenRun(() -> logger.info("Loaded spawn chunks"));

        // Since we never save this world, delete the polar world and associated copy of the world
        instance.setChunkLoader(NoopChunkLoader.INSTANCE);
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);

        var playerData = PlayerDataV2.fromPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setFlyingSpeed(player.getTag(DoubleJumpFeature.TAG) ? 0 : 0.05f);
        player.setHeldItemSlot(playerData.getSetting(SELECTED_SLOT).byteValue());

        // Hotbar items
        var inventory = player.getInventory();
        inventory.setItemStack(0, itemRegistry().getItemStack(PlayMapsItem.ID, null));
        inventory.setItemStack(1, itemRegistry().getItemStack(CreateMapsItem.ID, null));
        if (CoreFeatureFlags.ORGANIZATIONS.test(player)) {
            inventory.setItemStack(2, itemRegistry().getItemStack(OrgMapsItem.ID, null));
        }

        if (CoreFeatureFlags.COSMETICS.test(player)) {
            inventory.setItemStack(8, itemRegistry().getItemStack(OpenCosmeticsMenuItem.ID, null));
        }
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        // Write their settings to the database
        var playerData = PlayerDataV2.fromPlayer(player);
        playerData.writeUpdatesUpstream(server().playerService());

        super.removePlayer(player);
    }

    private void handleSwitchSlot(@NotNull PlayerChangeHeldSlotEvent event) {
        var playerData = PlayerDataV2.fromPlayer(event.getPlayer());
        playerData.setSetting(SELECTED_SLOT, (int) event.getSlot());
    }

    private void handlePlayerMove(@NotNull PlayerMoveEvent event) {
        Pos playerPos = event.getPlayer().getPosition();
        if (playerPos.x() < HUB_BB_MIN.x() || playerPos.x() > HUB_BB_MAX.x() ||
                playerPos.y() < HUB_BB_MIN.y() || playerPos.y() > HUB_BB_MAX.y() ||
                playerPos.z() < HUB_BB_MIN.z() || playerPos.z() > HUB_BB_MAX.z()) {
            event.getPlayer().teleport(spawnPoint(event.getPlayer()));
        }
    }
}

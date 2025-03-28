package net.hollowcube.mapmaker.hub;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.Uuids;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.hub.entity.marker.HubMarkerLoader;
import net.hollowcube.mapmaker.hub.feature.misc.DoubleJumpFeature;
import net.hollowcube.mapmaker.hub.item.*;
import net.hollowcube.mapmaker.hub.util.HubPlayerState;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.instance.EmptyChunk;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.polar.LoadingWorldAccess;
import net.hollowcube.mapmaker.map.polar.PolarDataFixer;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@SuppressWarnings("UnstableApiUsage")
public class HubMapWorld extends AbstractMapWorld {
    private static final Logger logger = LoggerFactory.getLogger(HubMapWorld.class);

    private static final PlayerSetting<Integer> SELECTED_SLOT = PlayerSetting.Int("selected_slot", 0);

    private static final AttributeModifier REACH_MOD = new AttributeModifier("mapmaker:hub_reach", 40 - Attribute.ENTITY_INTERACTION_RANGE.defaultValue(), AttributeOperation.ADD_VALUE);

    private static final Pos MIN_SPAWN_POINT = new Pos(-1, 40, -1, 90, 0);
    public static final MapData HUB_MAP_DATA = new MapData(
            MapData.SPAWN_MAP_ID == null ? Uuids.ZERO : MapData.SPAWN_MAP_ID, Uuids.ZERO,
            new MapSettings(), 0, Instant.now()
    );

    private static final List<BossBar> BOSS_BARS = List.of(
            BossBars.createLine1(Component.text(FontUtil.rewrite("bossbar_ascii_1", "Map Maker Early Access"), TextColor.color(0x3895FF))),
            BossBars.ADDRESS_LINE
    );

    private static final Vec HUB_BB_MIN = new Vec(-250, -30, -100);
    private static final Vec HUB_BB_MAX = new Vec(60, 130, 150);

    public static final Constructor<HubMapWorld> CTOR = AbstractMapWorld.ctor(HubMapWorld::new, HubMapWorld.class);

    private static HubMapWorld instance; // Currently just ensures there is only ever one hub per runtime.

    private final EventNode<InstanceEvent> eventNode = EventNode.type("hub-events", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
            .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true))
            .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerMoveEvent.class, this::handlePlayerMove)
            .addListener(PlayerChangeHeldSlotEvent.class, this::handleSwitchSlot);

    public HubMapWorld(@NotNull MapServer server, @NotNull MapData map) {
        super(server, HUB_MAP_DATA, new MapInstance(HUB_MAP_DATA.id(), false));
        Check.stateCondition(HubMapWorld.instance != null, "HubMapWorld already created");
        HubMapWorld.instance = this;

        instance().setGenerator(MapGenerators.voidWorld());

        instance().eventNode().addChild(eventNode); // Needs spectators, so register on instance.

        itemRegistry().register(new PlayMapsItem(server.guiController()));
        itemRegistry().register(new CreateMapsItem(server.guiController()));
        itemRegistry().register(new OrgMapsItem(server.guiController()));
        itemRegistry().register(new OpenCosmeticsMenuItem(server.guiController()));
        itemRegistry().register(new OpenStoreItem(server.guiController()));
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
        logger.info("Loading hub world (map id = {})", map().id());
        ReadableMapData mapWorldData;
        if (!map().id().equals(Uuids.ZERO)) {
            mapWorldData = server().mapService().getMapWorldAsStream(map().id(), false);
            Check.notNull(mapWorldData, "No world generated for hub world!");
        } else {
            try (var is = getClass().getResourceAsStream("/spawn/hcspawn.polar")) {
                if (is == null) throw new IOException("hcspawn.polar not found");
                var worldFileContent = Objects.requireNonNull(is.readAllBytes());
                mapWorldData = new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(worldFileContent)), worldFileContent.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        PolarLoader.streamLoad(
                (InstanceContainer) instance(),
                mapWorldData.data(),
                mapWorldData.length(),
                PolarDataFixer.INSTANCE,
                new LoadingWorldAccess(new ReadWorldAccess(this, new HubMarkerLoader()), this::onDataLoaded),
                false
        ).join();
        instance().setChunkSupplier(EmptyChunk::new);
    }

    @Override
    public void preAddPlayer(@NotNull AsyncPlayerConfigurationEvent event) {
        final Player player = event.getPlayer();

        // Load the state from the previous hub they were on if present.
        var existingState = ProxySupport.getTransferData(player, HubPlayerState.class);
        if (existingState != null) {
            player.setRespawnPoint(existingState.position());
            player.scheduleNextTick(ignored -> player.setHeldItemSlot((byte) existingState.slot()));
        } else {
            player.setRespawnPoint(spawnPoint(player));
        }
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);

        var playerData = PlayerDataV2.fromPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
        player.setFlyingSpeed(player.getTag(DoubleJumpFeature.TAG) ? 0 : 0.05f);
        player.setHeldItemSlot(playerData.getSetting(SELECTED_SLOT).byteValue());
        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).addModifier(REACH_MOD);

        // Hotbar items
        var inventory = player.getInventory();
        inventory.setItemStack(0, itemRegistry().getItemStack(PlayMapsItem.ID, null));
        inventory.setItemStack(1, itemRegistry().getItemStack(CreateMapsItem.ID, null));
        if (CoreFeatureFlags.ORGANIZATIONS.test(player)) {
            inventory.setItemStack(2, itemRegistry().getItemStack(OrgMapsItem.ID, null));
        }

        inventory.setItemStack(7, itemRegistry().getItemStack(OpenStoreItem.ID, null));

        inventory.setItemStack(8, itemRegistry().getItemStack(OpenCosmeticsMenuItem.ID, null));

        BossBars.clear(player);
        BOSS_BARS.forEach(player::showBossBar);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        // Write their settings to the database
        var playerData = PlayerDataV2.fromPlayer(player);
        playerData.writeUpdatesUpstream(server().playerService());

        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).removeModifier(REACH_MOD);

        super.removePlayer(player);
    }

    private void handleSwitchSlot(@NotNull PlayerChangeHeldSlotEvent event) {
        var playerData = PlayerDataV2.fromPlayer(event.getPlayer());
        playerData.setSetting(SELECTED_SLOT, (int) event.getNewSlot());
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

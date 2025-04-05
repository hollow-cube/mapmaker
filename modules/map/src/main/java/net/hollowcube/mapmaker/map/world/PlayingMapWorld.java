package net.hollowcube.mapmaker.map.world;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerStartFinishedEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerStartSpectatorEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.polar.LoadingWorldAccess;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.map.util.CustomizableHotbarManager;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class PlayingMapWorld extends AbstractMapMakerMapWorld {
    private final Logger logger = LoggerFactory.getLogger(PlayingMapWorld.class);

    private final EventNode<InstanceEvent> eventNode = EventNode.type("playing-events", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
            // If customizable hotbar is active, allow that to handle the pre click event.
            .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(!CustomizableHotbarManager.isActive(event.getPlayer())))
            .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true));

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();

    private Component ownerBossBarName = Component.empty();

    private static final BadSprite SPECTATOR_SPRITE = BadSprite.require("hud/spectator");
    private final ActionBar.Provider spectatingActionBarProvider = this::buildSpectatorWidget;
    private static final BadSprite FINISHED_SPRITE = BadSprite.require("hud/finished");
    private final ActionBar.Provider finishedActionBarProvider = this::buildFinishedWidget;

    public static final Constructor<PlayingMapWorld> CTOR = AbstractMapWorld.ctor(PlayingMapWorld::new, PlayingMapWorld.class);

    public PlayingMapWorld(@NotNull MapServer server, @NotNull MapData map) {
        super(server, map, new MapInstance(map.createDimensionName('p'), false));
        instance.setGenerator(MapGenerators.voidWorld());

        instance.eventNode().addChild(eventNode); // Needs spectators, so register on instance.
    }

    @Override
    public void load() {
        features().preinitMap(this);

        loadWorld();

        super.load();

        try {
            this.ownerBossBarName = server().playerService().getPlayerDisplayName2(map().owner())
                    .build(DisplayName.Context.BOSS_BAR);
        } catch (Exception e) {
            ExceptionReporter.reportException(e);
            this.ownerBossBarName = Component.text("!error!", NamedTextColor.RED);
        }
    }

    protected void loadWorld() {
        // Load the map itself (eg blocks, if present)
        var mapData = server().mapService().getMapWorldAsStream(map().id(), true);
        if (mapData != null) {
            instance.loadStream(mapData, new LoadingWorldAccess(new ReadWorldAccess(this), this::onDataLoaded));
        }
    }

    @Override
    public void close(@Nullable Component reason) {
        super.close(reason); // Remove players & spectators
        instance.scheduleNextTick(ignored -> instance.unload());
    }

    public void preAddPlayer(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();
        var saveState = getOrCreateSaveState(player);

        // We must set the respawn point during config so that their spawn chunks are sent there.
        // This prevents falling through the floor when joining.
        player.setRespawnPoint(saveState.state(PlayState.class).pos().orElse(map().settings().getSpawnPoint()));
        player.setTag(FIRST_JOIN_TAG, true);
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) {
            // We need to handle missing save states here because they do not reenter configuration to reset
            // a map after completing it.
            saveState = getOrCreateSaveState(player);

            // Teleport to the spawn point of the map, and update the pose to what the server thinks it should be.
            // This is to prevent a but where the player can remain in crawling state 1 tick after leaving spec
            // and go into a 1 block gap.
            final var finalSaveState = saveState;
            player.acquirable().sync(localPlayer -> {
                localPlayer.sendPacket(new BundlePacket());
                localPlayer.teleport(finalSaveState.state(PlayState.class).pos().orElse(map().settings().getSpawnPoint()),
                        Vec.ZERO, null, RelativeFlags.NONE);
                ((MapPlayerImplImpl) player).updatePose();
                localPlayer.sendPacket(new EntityMetaDataPacket(localPlayer.getEntityId(), Map.of(6, Metadata.Pose(localPlayer.getPose()))));
                localPlayer.sendPacket(new BundlePacket());
            });
        }

        super.addPlayer(player); // Add to player list & reset inventory.

        var isMapJoin = player.getAndSetTag(FIRST_JOIN_TAG, null) != null;
        EventDispatcher.call(new MapPlayerInitEvent(this, player, true, isMapJoin));
        if (saveState.getPlaytime() > 0) {
            // If the playtime is non-zero (ie they have played before) start timing immediately.
            // Otherwise, we will start timing when they move the first time.
            saveState.setPlayStartTime(System.currentTimeMillis());
        }
    }

    protected @NotNull SaveState getOrCreateSaveState(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        var stateType = map().verification() == MapVerification.PENDING ? SaveStateType.VERIFYING : SaveStateType.PLAYING;
        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id(), stateType, PlayState.SERIALIZER);
        player.setTag(SaveState.TAG, saveState);
        return saveState;
    }

    @Override
    public void addSpectator(@NotNull Player player) {
        addSpectator(player, false);
    }

    public void addSpectator(@NotNull Player player, boolean isFinishedMode) {
        super.addSpectator(player); // Add to spectator list & reset inventory.

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true);
//        player.setInvisible(true);

        callEvent(isFinishedMode
                ? new MapPlayerStartFinishedEvent(this, player)
                : new MapPlayerStartSpectatorEvent(this, player));

        ActionBar.forPlayer(player).addProvider(isFinishedMode
                ? finishedActionBarProvider
                : spectatingActionBarProvider);
//        instance.eventNode().call(new MapPlayerStartSpectatorEvent(this, player));
//        if (teleport) player.teleport(map.settings().getSpawnPoint()).join();
    }

//    public @Blocking void startFinished(@NotNull Player player, boolean teleport) {
//
//        player.setTag(TAG_PLAYING, false); // Sanity
//        player.setTag(MapHooks.PLAYING, false); // Sanity
//
//        spectatingPlayers.add(player);
//
//        net.hollowcube.mapmaker.map.world.MapWorldHelpers.resetPlayer(player);
//        player.setGameMode(GameMode.ADVENTURE);
//        player.setAllowFlying(true);
//        player.setInvisible(true);
//        ActionBar.forPlayer(player).addProvider(finishedActionBarProvider);
//
//        instance.eventNode().call(new MapPlayerStartFinishedEvent(this, player));
//
//        if (teleport) player.teleport(map.settings().getSpawnPoint()).join();

    /// /        player.sendMessage("Now spectating " + map.settings().getName());
//    }
    @Override
    public void removePlayer(@NotNull Player player) {
        if (isPlaying(player)) {
            preRemoveActivePlayer(player);
            removeActivePlayer(player);
        } else if (isSpectating(player)) removeSpectatingPlayer(player);
    }

    public void preRemoveActivePlayer(@NotNull Player player) {
        callEvent(new MapWorldPlayerStopPlayingEvent(this, player));
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) return; // Sanity check

        saveState.updatePlaytime();
        var playState = saveState.state(PlayState.class);
        playState.setPos(player.getPosition());
    }

    public @Nullable SaveStateUpdateResponse removeActivePlayer(@NotNull Player player) {
//        if (!isPlaying(player)) return null; //todo cannot enable this, see comment in PlayCompletionFeatureProvider where this is called.

        super.removePlayer(player); // Remove from player list

        // Update the playtime and playing state to the current state
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) return null; // Sanity check
        var update = saveState.createUpdateRequest();

        // Write the save state to the database
        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var resp = server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), update);
            logger.info("Updated savestate for {}", player.getUuid());
            return resp;
        } catch (Exception e) {
            logger.error("Failed to save player state for {}", player.getUuid(), e);
            return null;
        } finally {
            player.removeTag(SaveState.TAG);
        }
    }

    @Override
    public @Nullable MapWorld playWorld() {
        return this;
    }

    private void removeSpectatingPlayer(@NotNull Player player) {
        if (!isSpectating(player)) return;

        super.removePlayer(player); // Remove from player list & reset

        ActionBar.forPlayer(player).removeProvider(spectatingActionBarProvider);
        ActionBar.forPlayer(player).removeProvider(finishedActionBarProvider);
    }

    private void buildSpectatorWidget(@NotNull Player player, @NotNull FontUIBuilder builder) {
        builder.pushShadowColor(ShadowColor.none());
        builder.pos(-SPECTATOR_SPRITE.width() / 2).drawInPlace(SPECTATOR_SPRITE);
    }

    private void buildFinishedWidget(@NotNull Player player, @NotNull FontUIBuilder builder) {
        builder.pushShadowColor(ShadowColor.none());
        builder.pos(-FINISHED_SPRITE.width() / 2).drawInPlace(FINISHED_SPRITE);
    }

    @Override
    protected @Nullable BossBar buildBossBarLine1(@NotNull Player player) {

        final String verb = map().verification() == MapVerification.PENDING ? "verifying" : "playing";
        var builder = Component.text()
                .append(Component.text(FontUtil.rewrite("bossbar_small_1", verb) + " ", NamedTextColor.WHITE))
                .append(MapData.rewriteWithQualityFont(map().quality(), FontUtil.rewrite("bossbar_ascii_1", map().name())));

        if ("playing".equals(verb)) {
            builder.append(Component.text(" " + FontUtil.rewrite("bossbar_small_1", "by") + " ", TextColor.color(0xB0B0B0)))
                    .append(ownerBossBarName);
        }

        return BossBars.createLine1(builder.build());
    }

    @Override
    protected @Nullable BossBar buildBossBarLine2(@NotNull Player player) {
        if (!map().isPublished()) return super.buildBossBarLine2(player);

        return BossBars.createLine2(Component.text()
                .append(Component.text(FontUtil.rewrite("bossbar_small_2", "/play"), NamedTextColor.WHITE))
                .appendSpace()
                .append(Component.text(FontUtil.rewrite("bossbar_small_2", map().publishedIdString()), NamedTextColor.WHITE))
                .appendSpace()
                .append(Component.text(FontUtil.rewrite("bossbar_small_2", "on"), TextColor.color(0xB0B0B0)))
                .appendSpace()
                .append(Component.text(FontUtil.rewrite("bossbar_small_2", "hollowcube.net"), NamedTextColor.WHITE))
                .build());
    }
}

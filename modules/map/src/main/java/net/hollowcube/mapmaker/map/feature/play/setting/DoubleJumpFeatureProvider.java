package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

@AutoService(FeatureProvider.class)
public class DoubleJumpFeatureProvider extends AbstractSettingFeatureProvider {

    private static final Tag<Integer> DOUBLE_JUMP_COUNT = Tag.Integer("mapmaker:player/double_jump_count").defaultValue(0);
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/double_jump", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerUpdateStateEvent.class, this::playerUpdated)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::removePlayer)
            .addListener(PlayerStartFlyingEvent.class, this::handleStartFlying)
            .addListener(PlayerMoveEvent.class, this::playerMoved);

    @Override
    protected EventNode<InstanceEvent> getEvents() {
        return eventNode;
    }

    private static int getDoubleJumpCount(@NotNull Player player, MapWorld world) {
        if (!world.isPlaying(player)) return 0;

        var state = SaveState.optionalFromPlayer(player);
        if (state == null) return 0;

        var playstate = state.state(PlayState.class);
//        return playstate.settings().get(MapSettings.DOUBLE_JUMP, world.map().settings());
        return 0; // todo
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.player();
        if (!event.mapWorld().isPlaying(player)) return;
        var mapDoubleJumpCount = getDoubleJumpCount(player, event.mapWorld());
        if (mapDoubleJumpCount <= 0) return;
        if (!event.isMapJoin()) return;

        player.sendMessage(TranslatableBuilder
                .of("map.join.warning.setting.double_jump")
                .with(mapDoubleJumpCount)
                .build()
        );
    }


    public void playerMoved(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;
        if (!player.isOnGround()) return;
        int doubleJumpCount = getDoubleJumpCount(player, world);
        player.setTag(DOUBLE_JUMP_COUNT, doubleJumpCount);
        player.setAllowFlying(doubleJumpCount > 0);
    }

    public void handleStartFlying(@NotNull PlayerStartFlyingEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        player.setFlying(false);
        if (player.getTag(DOUBLE_JUMP_COUNT) <= 0) return;

        var boostVelocity = player.getPosition().direction().mul(20.0).withY(20.0);
        player.setVelocity(boostVelocity);
        if (player.updateAndGetTag(DOUBLE_JUMP_COUNT, it -> it - 1) <= 0) {
            player.setAllowFlying(false);
        }

        var randomPitch = ThreadLocalRandom.current().nextFloat(0.9f, 1f);
        player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_TAKEOFF, Sound.Source.PLAYER, 0.4f, randomPitch), Sound.Emitter.self());
    }

    public void playerUpdated(@NotNull MapPlayerUpdateStateEvent event) {
        updatePlayer(event.player());
    }

    public void removePlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        event.player().removeTag(DOUBLE_JUMP_COUNT);
        updatePlayer(event.player());
    }

    private void updatePlayer(@NotNull Player player) {
        var world = MapWorld.forPlayer(player);
        var doubleJumpCount = getDoubleJumpCount(player, world);
        player.setAllowFlying(doubleJumpCount > 0);
        player.setFlyingSpeed(doubleJumpCount > 0 ? 0f : 0.05f);
    }
}

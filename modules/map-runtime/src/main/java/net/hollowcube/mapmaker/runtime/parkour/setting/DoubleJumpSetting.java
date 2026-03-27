package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class DoubleJumpSetting {
    private static final int DEFAULT_COUNT = 0;
    private static final Tag<Integer> DOUBLE_JUMP_COUNT = Tag.Integer("mapmaker:player/double_jump_count").defaultValue(DEFAULT_COUNT);

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, DoubleJumpSetting::updatePlayer)
            .addListener(PlayerMoveEvent.class, DoubleJumpSetting::handlePlayerMove)
            .addListener(PlayerStartFlyingEvent.class, DoubleJumpSetting::handleStartFlying);

    public static int getDoubleJumpCount(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return DEFAULT_COUNT;

        return getDoubleJumpCount(world, p.saveState().state(PlayState.class));
    }

    public static int getDoubleJumpCount(ParkourMapWorld world, PlayState playState) {
        return Objects.requireNonNullElse(playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.DOUBLE_JUMP, world.map().settings()), DEFAULT_COUNT);
    }

    private static void updatePlayer(ParkourMapPlayerUpdateStateEvent event) {
        final var world = event.world();
        final var player = event.player();

        var doubleJumpCount = event.isReset() ? 0 : getDoubleJumpCount(world, player);
        if (event.isMapJoin() && doubleJumpCount > 0) {
            player.sendMessage(TranslatableBuilder
                    .of("map.join.warning.setting.double_jump")
                    .with(doubleJumpCount)
                    .build()
            );
        }

        player.setAllowFlying(doubleJumpCount > 0);
        player.setFlyingSpeed(doubleJumpCount > 0 ? 0f : 0.05f);
        if (doubleJumpCount <= 0) player.removeTag(DOUBLE_JUMP_COUNT);
    }

    private static void handlePlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null || !player.isOnGround()) return;

        int doubleJumpCount = getDoubleJumpCount(world, player);
        player.setTag(DOUBLE_JUMP_COUNT, doubleJumpCount);
        player.setAllowFlying(doubleJumpCount > 0);
    }

    private static void handleStartFlying(PlayerStartFlyingEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

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

}

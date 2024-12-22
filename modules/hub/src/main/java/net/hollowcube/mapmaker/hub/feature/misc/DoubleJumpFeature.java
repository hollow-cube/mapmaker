package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

@AutoService(HubFeature.class)
public class DoubleJumpFeature implements HubFeature {
    public static final Tag<Boolean> TAG = net.minestom.server.tag.Tag.Boolean("mapmaker:hub-double-jump").defaultValue(true);
    private static final Tag<Boolean> COOLDOWN_TAG = Tag.Boolean("mapmaker:hub-double-jump-cooldown");

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        world.eventNode().addListener(PlayerStartFlyingEvent.class, this::handleStartFlying)
                .addListener(PlayerMoveEvent.class, this::handleMovement);
    }

    private void handleStartFlying(@NotNull PlayerStartFlyingEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (!player.getTag(TAG)) return; // Explicitly disabled to allow normal flight
        player.setFlying(false);
        if (player.hasTag(COOLDOWN_TAG)) return;

        var boostVelocity = player.getPosition().direction().mul(20.0).withY(20.0);
        player.setVelocity(boostVelocity);
        player.setTag(COOLDOWN_TAG, true);
        player.setAllowFlying(false);

        var randomPitch = ThreadLocalRandom.current().nextFloat(0.9f, 1f);
        player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_TAKEOFF, Sound.Source.PLAYER, 0.4f, randomPitch), Sound.Emitter.self());
    }

    private void handleMovement(@NotNull PlayerMoveEvent event) {
        if (event.isOnGround() && event.getPlayer().hasTag(COOLDOWN_TAG)) {
            event.getPlayer().removeTag(COOLDOWN_TAG);
            event.getPlayer().setAllowFlying(true);
        }
    }

}

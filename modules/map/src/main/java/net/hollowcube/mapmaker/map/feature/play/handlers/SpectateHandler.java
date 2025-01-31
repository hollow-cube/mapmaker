package net.hollowcube.mapmaker.map.feature.play.handlers;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

public final class SpectateHandler {

    public static Tag<Pos> SPECTATOR_CHECKPOINT = Tag.Transient("mapmaker:spectator_checkpoint");

    public static void setSpectating(Player player, boolean spectating) {
        setSpectating(player, TriState.byBoolean(spectating));
    }

    public static void setSpectating(Player player, TriState state) {
        var world = MapWorld.forPlayerOptional(player);
        if (!(world instanceof PlayingMapWorld playWorld)) return;

        if (state.toBooleanOrElse(!playWorld.isSpectating(player))) {
            if (world.map().getSetting(MapSettings.NO_SPECTATOR)) return;

            // Must be standing on the ground (not falling) to enter spectator mode
            if (!player.isOnGround()) {
                player.sendMessage(Component.translatable("map.spectator_mode.solid_ground"));
                return;
            }

            // Save the position immediately not after virtual thread delay.
            final var savePosition = player.getPosition();

            // Remove the playing tags immediately
            playWorld.preRemoveActivePlayer(player);
            playWorld.removePlayerImmediate(player);

            // Perform the updates based on the removal.
            FutureUtil.submitVirtual(() -> {
                playWorld.removeActivePlayer(player);
                world.addSpectator(player);

                player.setTag(SPECTATOR_CHECKPOINT, savePosition);
            });
        } else {
            FutureUtil.submitVirtual(() -> {
                player.removeTag(SPECTATOR_CHECKPOINT);
                world.removePlayer(player); // Remove spectator
                world.addPlayer(player); // Add back as playing player
            });
        }
    }

    public static void clearSpectatorCheckpoint(Player player) {
        player.removeTag(SPECTATOR_CHECKPOINT);
    }

    public static void setSpectatorCheckpoint(Player player, Pos position) {
        player.setTag(SPECTATOR_CHECKPOINT, position);
    }

    public static Pos getSpectatorCheckpoint(Player player) {
        return player.getTag(SPECTATOR_CHECKPOINT);
    }
}

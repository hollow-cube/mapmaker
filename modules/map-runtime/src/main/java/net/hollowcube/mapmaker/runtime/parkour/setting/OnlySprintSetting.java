package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStopSprintingEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.tag.Tag;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class OnlySprintSetting {
    private static final Tag<Point> ONLY_SPRINT_START_POSITION = Tag.Transient("mapmaker:parkour/os/play");

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, OnlySprintSetting::updatePlayer)
            .addListener(PlayerStopSprintingEvent.class, OnlySprintSetting::onStopSprinting)
            .addListener(PlayerMoveEvent.class, OnlySprintSetting::onPlayerMove);

    public static boolean canSprint(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true; // Sanity check, allow sprinting by default.

        return canSprint(world, p.saveState().state(PlayState.class));
    }

    public static boolean canSprint(ParkourMapWorld world, PlayState state) {
        return !state.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.ONLY_SPRINT, world.map().settings());
    }

    private static void updatePlayer(ParkourMapPlayerUpdateStateEvent event) {
        if (canSprint(event.world(), event.playState())) return;

        var player = event.player();
        if (event.isMapJoin()) {
            player.sendMessage(Component.translatable("map.join.warning.setting.only_sprint"));
        }

        player.setTag(ONLY_SPRINT_START_POSITION, player.getPosition());
    }

    private static void onStopSprinting(PlayerStopSprintingEvent event) {
        var player = event.getPlayer();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null || canSprint(world, player)) return;

        player.removeTag(ONLY_SPRINT_START_POSITION);
        world.softResetPlayer(player);
    }

    private static void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null || canSprint(world, player)) return;

        var startPosition = player.getTag(ONLY_SPRINT_START_POSITION);
        if ((player.isSprinting() && !player.isSneaking()) || startPosition == null) return;

        var startPos = player.getTag(ONLY_SPRINT_START_POSITION);
        if (startPos.distanceSquared(event.getNewPosition()) < 1)
            return;

        // They moved >1 block before starting sprinting, reset them
        player.removeTag(ONLY_SPRINT_START_POSITION);
        world.softResetPlayer(player);
    }
}

package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class NoSprintSetting {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, NoSprintSetting::updatePlayer);

    public static boolean canSprint(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true;

        return canSprint(world, p.saveState().state(PlayState.class));
    }

    public static boolean canSprint(ParkourMapWorld world, PlayState playState) {
        return !playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.NO_SPRINT, world.map().settings());
    }

    private static void updatePlayer(ParkourMapPlayerUpdateStateEvent event) {
        final var world = event.world();
        final var player = event.player();

        final var canSprint = event.isReset() || canSprint(world, player);
        if (event.isMapJoin() && !canSprint) {
            player.sendMessage(Component.translatable("map.join.warning.setting.no_sprint"));
        }

        event.player().setFood(canSprint ? 20 : 6);
    }

}

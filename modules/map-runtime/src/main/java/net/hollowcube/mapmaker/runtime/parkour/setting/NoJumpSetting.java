package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class NoJumpSetting {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, NoJumpSetting::updatePlayer);

    public static boolean canJump(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true;

        return canJump(world, p.saveState().state(PlayState.class));
    }

    public static boolean canJump(ParkourMapWorld world, PlayState playState) {
        return !playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.NO_JUMP, world.map().settings());
    }

    private static void updatePlayer(ParkourMapPlayerUpdateStateEvent event) {
        final var world = event.world();
        final var player = event.player();

        boolean canJump = event.isReset() || canJump(world, player);
        if (event.isMapJoin() && !canJump) {
            player.sendMessage(Component.translatable("map.join.warning.setting.no_jump"));
        }

        var newStrength = canJump ? Attribute.JUMP_STRENGTH.defaultValue() : 0;
        player.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(newStrength);
    }

}

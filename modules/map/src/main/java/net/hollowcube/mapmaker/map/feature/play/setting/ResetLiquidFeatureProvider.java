package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.util.PlayerLiquidExtension;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class ResetLiquidFeatureProvider extends AbstractSettingFeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/reset_liquid", EventFilter.INSTANCE)
            .addListener(PlayerTickEvent.class, this::handlePlayerTick);

    @Override
    protected EventNode<InstanceEvent> getEvents() {
        return eventNode;
    }

    private static boolean canGoInWater(@NotNull Player player, @NotNull MapWorld world) {
        var state = SaveState.fromPlayer(player);
        var playstate = state.state(PlayState.class);
//        return !playstate.settings().get(MapSettings.RESET_IN_WATER, world.map().settings());
        return true; // todo
    }

    private static boolean canGoInLava(@NotNull Player player, @NotNull MapWorld world) {
        var state = SaveState.fromPlayer(player);
        var playstate = state.state(PlayState.class);
//        return !playstate.settings().get(MapSettings.RESET_IN_LAVA, world.map().settings());
        return true; // todo
    }

    private void handlePlayerTick(@NotNull PlayerTickEvent event) {
        var player = event.getPlayer();
        if (!(player instanceof PlayerLiquidExtension ple) || !(ple.isInWater() || ple.isInLava())) return;

        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return; // Sanity

        boolean isWaterReset = !canGoInWater(player, world) && ple.isInWater();
        boolean isLavaReset = isWaterReset || !canGoInLava(player, world) && ple.isInLava();
        if (isWaterReset || isLavaReset) {
            world.callEvent(new MapPlayerResetEvent(player, world, true));
        }
    }
}
